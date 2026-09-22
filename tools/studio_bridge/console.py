"""Persistent local console -> official MCP stdio -> bounded Studio tools. EOF exits cleanly."""
import asyncio,json,sys,time
from pathlib import Path
from mcp import Client
from mcp.client.stdio import StdioServerParameters

async def main():
    params=StdioServerParameters(command=sys.executable,args=[str(Path(__file__).with_name('mcp_server.py'))])
    async with Client(params,read_timeout_seconds=115) as client:
        names={t.name for t in (await client.list_tools()).tools}
        print(json.dumps({'ready':True,'tools':sorted(names)}),flush=True)
        print('STUDIO_MCP_READY>',flush=True)
        while True:
            line=await asyncio.to_thread(sys.stdin.readline)
            if not line or line.strip()=='quit': break
            start=time.perf_counter()
            try:
                req=json.loads(line)
                if req.get('tool') not in names:raise ValueError('Unknown tool; arbitrary commands are not accepted')
                result=await client.call_tool(req['tool'],req.get('arguments',{}))
                content=result.structured_content
                if content is None:
                    texts=[c.text for c in result.content if hasattr(c,'text')]
                    try: content=json.loads(texts[0]) if len(texts)==1 else texts
                    except ValueError: content=texts
                print(json.dumps({'error':result.is_error,'elapsed_ms':round((time.perf_counter()-start)*1000,2),'result':content},ensure_ascii=True),flush=True)
            except Exception as exc:
                print(json.dumps({'error':True,'type':type(exc).__name__,'message':str(exc)[:300]}),flush=True)
            print('STUDIO_MCP_READY>',flush=True)

if __name__=='__main__':asyncio.run(main())
