"""Read-only CLI diagnostics. No credential scans, login, uploads or paid task commands."""
import argparse, json, re, subprocess
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--cli',type=Path,required=True)
args=parser.parse_args()
if not args.cli.is_file(): raise SystemExit('Pinned local CLI is missing')
report={'wallet':'API, separate from Studio','balance':None,'credits_spent':0,'checks':[]}
for command in ('doctor','balance'):
    try:
        result=subprocess.run(['node',str(args.cli),command,'--json','--no-open'],
            capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=45)
        output=result.stdout+'\n'+result.stderr
        output=re.sub(r'\x1b\[[0-9;]*m','',output)
        output=re.sub(r'(tsk_|sk-)[A-Za-z0-9_.*-]+','[REDACTED_CREDENTIAL]',output)
        output=re.sub(r'Bearer\s+\S+','Bearer [REDACTED]',output)
        report['checks'].append({'command':command,'exit_code':result.returncode,'output':output.strip()})
        if command=='balance' and result.returncode==0:
            try: report['balance']=json.loads(result.stdout).get('balance')
            except (ValueError,AttributeError): pass
    except subprocess.TimeoutExpired:
        report['checks'].append({'command':command,'exit_code':None,'output':'Timeout; no retry or generation'})
report['balance_status']='unknown-not-zero' if report['balance'] is None else 'observed-api-wallet'
report['ready_for_api']=all(c['exit_code']==0 for c in report['checks'])
print(json.dumps(report,indent=2))
