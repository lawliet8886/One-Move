"""Real SDK client/server handshake and read-only/idempotency test. Never creates approval."""
import asyncio,json,sys,time
from pathlib import Path
from mcp import Client
from mcp.client.stdio import StdioServerParameters

async def main():
    params=StdioServerParameters(command=sys.executable,args=[str(Path(__file__).with_name('mcp_server.py'))])
    async with Client(params,read_timeout_seconds=45) as client:
        listed=await client.list_tools()
        names=[t.name for t in listed.tools]
        assert 'studio_status' in names and 'studio_submit_approved_pip' in names
        times=[];balances=[]
        for _ in range(3):
            start=time.perf_counter();r=await client.call_tool('studio_status',{})
            assert not r.is_error
            obj=r.structured_content or json.loads(next(c.text for c in r.content if hasattr(c,'text')));obj=obj.get('result',obj)
            times.append(round((time.perf_counter()-start)*1000,2));balances.append(obj.get('balance'))
        r=await client.call_tool('studio_submit_approved_pip',{'operation_id':'pip-20260922-55-v2'})
        assert not r.is_error
        obj=r.structured_content or json.loads(next(c.text for c in r.content if hasattr(c,'text')));obj=obj.get('result',obj)
        assert obj['submitted_now'] is False and obj['reason']=='IDEMPOTENT_NO_RECLICK'
        denied=await client.call_tool('studio_submit_approved_pip',{'operation_id':'unapproved-test'})
        assert denied.is_error
        print(json.dumps({'protocol':'official MCP SDK over stdio','tools':names,
            'status_round_trip_ms':times,'visible_balances':balances,'duplicate_submit_blocked':True,
            'unapproved_submit_blocked':True,'paid_actions_during_test':0}))

asyncio.run(asyncio.wait_for(main(),70))
