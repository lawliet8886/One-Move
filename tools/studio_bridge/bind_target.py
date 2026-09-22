"""Explicitly bind a known Studio tab by observed target id. Never closes/navigates tabs."""
import argparse,json,re,urllib.request
from pathlib import Path
from urllib.parse import urlsplit
from bridge import STATE,HOST

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--target-id')
a=p.parse_args()
with urllib.request.urlopen('http://127.0.0.1:9222/json/list',timeout=4) as response:
    raw=json.load(response)
targets=[{'id':t['id'],'path':urlsplit(t['url']).path} for t in raw
         if t.get('type')=='page' and urlsplit(t.get('url','')).hostname==HOST
         and urlsplit(t['url']).scheme=='https']
if a.target_id:
    matches=[t for t in targets if t['id']==a.target_id]
    if len(matches)!=1:raise SystemExit('Specified Studio target does not exist; binding unchanged')
    STATE.mkdir(parents=True,exist_ok=True)
    record={'target_id':a.target_id,'bound_path':matches[0]['path'],'method':'explicit observed target id'}
    tmp=STATE/'target.tmp';tmp.write_text(json.dumps(record,indent=2),encoding='utf-8');tmp.replace(STATE/'target.json')
    print(json.dumps({'bound':record,'other_tabs_preserved':True}))
else:print(json.dumps({'studio_targets':targets,'instruction':'Pass an exact observed target id to bind; nothing changed.'}))
