"""Local stdio MCP facade. No network server, arbitrary evaluation or credential tools."""
import asyncio
from contextlib import asynccontextmanager
from mcp.server.mcpserver.exceptions import ToolError
from mcp.server import MCPServer
from bridge import Studio,STATE
from guard import Ledger, GuardError
from exports import download_glb

@asynccontextmanager
async def lifespan(server):
    global _studio
    try: yield {}
    finally:
        if _studio is not None:
            await _studio.disconnect(); _studio=None
        await asyncio.sleep(0.2)

server=MCPServer('OneMove Tripo Studio',version='0.2.0',log_level='WARNING',lifespan=lifespan,
    instructions='Use visible Studio state. Paid generation requires a preapproved one-use operation id; never retry an uncertain submission. This is a local custom adapter, not an official Tripo API.')
_studio=None
_lock=asyncio.Lock()

async def call(method,*args):
    global _studio
    async with _lock:
        if _studio is None:
            candidate=Studio()
            try: _studio=await asyncio.wait_for(candidate.connect(),12)
            except GuardError as exc:
                await candidate.disconnect(); raise ToolError(str(exc)) from None
            except BaseException:
                await candidate.disconnect(); raise
        try:
            if method=='download': return await asyncio.wait_for(download_glb(_studio,*args),105)
            return await asyncio.wait_for(getattr(_studio,method)(*args),35)
        except GuardError as exc: raise ToolError(str(exc)) from None

@server.tool()
async def studio_status() -> dict:
    """Read visible wallet, login and controls without spending credits."""
    result=await call('status')
    return {k:result[k] for k in ('page_path','balance','authenticated','elapsed_ms','observed_at')}

@server.tool()
async def studio_quote() -> dict:
    """Verify reviewed Pip image hash, quality, topology and the current displayed credit quote."""
    return await call('quote')

@server.tool()
async def studio_snapshot(label: str = 'review') -> dict:
    """Capture the actual Studio page only, not the desktop or another website."""
    return await call('snapshot',label)

@server.tool()
async def studio_inspect() -> dict:
    """Read a bounded visible Studio panel to review progress; no response bodies or secrets."""
    return await call('inspect')

@server.tool()
async def studio_submit_approved_pip(operation_id: str) -> dict:
    """Consume ONE already-filed approval. Checks exact reference/settings/wallet/price; repeated ids never click again."""
    return await call('submit',operation_id)

@server.tool()
def studio_operation(operation_id: str) -> dict:
    """Read the durable local operation ledger. Does not contact Tripo or spend credits."""
    ledger=Ledger(STATE/'ledger.sqlite')
    try: return ledger.get(operation_id) or {'status':'not_found'}
    finally: ledger.close()

@server.tool()
async def studio_download_existing_glb(operation_id: str) -> dict:
    """Download only the ledger-matched completed model, using current GLB textures. Existing files are not overwritten."""
    return await call('download',operation_id)

@server.tool()
async def studio_open_completed_model(operation_id: str) -> dict:
    """Navigate only to the exact Studio model stored in the paid operation ledger. No other tab or new generation."""
    return await call('open_completed_model',operation_id)

if __name__=='__main__': server.run(transport='stdio')
