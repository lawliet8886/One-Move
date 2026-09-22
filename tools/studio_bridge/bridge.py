"""Task-specific Tripo Studio adapter: visible DOM only, bounded actions, no secret access."""
from __future__ import annotations
import asyncio, argparse, base64, hashlib, json, os, re, time
from pathlib import Path
from urllib.parse import urlsplit
from playwright.async_api import async_playwright
from guard import Ledger, GuardError, fingerprint

LAB=Path.home()/'OneMove-Lab'
STATE=LAB/'.local-lab/tripo-bridge-state'
HOST='studio.tripo3d.ai'
REF=LAB/'.local-lab/tripo-asset-prep-20260922/art/asset_factory/references/pip_current.png'

# One bounded DOM pass replaces hundreds of Playwright round trips. No input values are read.
VISIBLE_JS='''() => {
 const visible=e=>{const r=e.getBoundingClientRect(),s=getComputedStyle(e);return r.width>0&&r.height>0&&r.top<innerHeight&&r.bottom>0&&s.visibility!=='hidden'&&s.display!=='none'};
 const text=e=>(e.innerText||'').trim().replace(/\\s+/g,' ');
 const buttons=[...document.querySelectorAll('button')].filter(visible);
 const up=buttons.find(e=>text(e)==='Upgrade'); let balance=null;
 if(up){let el=up;for(let i=0;i<4&&el;i++,el=el.parentElement){const t=text(el);const m=t.match(/^(\\d+)\\s+Upgrade(?:\\s+DCC Bridge)?$/);if(m){balance=Number(m[1]);break;}}}
 const gen=buttons.filter(e=>/^Gerar(?:\\s+\\d+)?$/.test(text(e)));
 return {page_path:location.pathname,balance,authenticated:balance!==null&&!buttons.some(e=>/Registrar\\/Entrar|Sign in/.test(text(e))),
 generate_labels:gen.map(text),quality:buttons.map(text).find(t=>t==='Qualidade Máxima')||null,
 topology:buttons.map(text).find(t=>t.startsWith('Topologia Limpa'))||null,
 controls:buttons.map(e=>({text:text(e).slice(0,100),label:e.getAttribute('aria-label')||'',enabled:!e.disabled})).filter(x=>x.text||x.label).slice(0,65)};
}'''

class Studio:
    async def connect(self):
        STATE.mkdir(parents=True,exist_ok=True)
        self.pw=await async_playwright().start()
        self.browser=await self.pw.chromium.connect_over_cdp('http://127.0.0.1:9222',timeout=7000,
            no_defaults=True,is_local=True,artifacts_dir=str(STATE/'downloads'))
        pages=[pg for c in self.browser.contexts for pg in c.pages if urlsplit(pg.url).hostname==HOST]
        pin_path=STATE/'target.json'
        if pin_path.is_file():
            target=json.loads(pin_path.read_text(encoding='utf-8'))['target_id']
            selected=[]
            for page in pages:
                session=await page.context.new_cdp_session(page)
                try: info=await session.send('Target.getTargetInfo')
                finally: await session.detach()
                if info['targetInfo']['targetId']==target: selected.append(page)
            if len(selected)!=1: raise GuardError('Pinned Studio tab is closed. Explicitly rebind; never pick another tab silently.')
            self.page=selected[0]
        elif len(pages)==1: self.page=pages[0]
        else: raise GuardError('Choose one Studio target explicitly; other tabs are preserved')
        self.page.set_default_timeout(5000)
        return self
    async def disconnect(self):
        if hasattr(self,'pw'): await self.pw.stop()  # Disconnect driver; never close user's browser.
    async def status(self):
        if urlsplit(self.page.url).hostname!=HOST: raise GuardError('Left authorized Studio origin')
        start=time.perf_counter(); result=await self.page.evaluate(VISIBLE_JS)
        result.update(elapsed_ms=round((time.perf_counter()-start)*1000,2),observed_at=time.time())
        return result
    async def quote(self):
        q=await self.status()
        labels=q.pop('generate_labels'); q.pop('controls')
        if len(labels)!=1 or not re.fullmatch(r'Gerar\s+[0-9]+',labels[0]): raise GuardError('Missing unambiguous visible cost')
        q['credits']=int(labels[0].split()[-1])
        q['reference_sha256']=await self.page.evaluate('''async()=>{const imgs=[...document.images].filter(i=>i.src.startsWith('blob:')&&i.naturalWidth===192&&i.naturalHeight===192);if(imgs.length!==1)return null;const b=await(await fetch(imgs[0].src)).arrayBuffer();return [...new Uint8Array(await crypto.subtle.digest('SHA-256',b))].map(v=>v.toString(16).padStart(2,'0')).join('');}''')
        if q['reference_sha256']!=hashlib.sha256(REF.read_bytes()).hexdigest(): raise GuardError('Preview is not the reviewed Pip reference')
        q['fingerprint']=fingerprint(q)
        return q
    async def snapshot(self, label='studio'):
        if not re.fullmatch(r'[a-zA-Z0-9_-]{1,60}',label): raise GuardError('Unsafe evidence name')
        out=STATE/'evidence'; out.mkdir(exist_ok=True)
        path=out/f'{int(time.time()*1000)}-{label}.png'
        cdp=await self.page.context.new_cdp_session(self.page)
        try:
            result=await asyncio.wait_for(cdp.send('Page.captureScreenshot',
                {'format':'png','captureBeyondViewport':False,'fromSurface':True}),12)
            path.write_bytes(base64.b64decode(result['data']))
        finally: await cdp.detach()
        return {'path':str(path),'kind':'REAL TRIPO STUDIO; NOT ANDROID'}
    async def submit(self, operation_id: str):
        if not re.fullmatch(r'[a-z0-9-]{1,60}',operation_id): raise GuardError('Unsafe operation id')
        permit_path=STATE/'permits'/(operation_id+'.json')
        if not permit_path.is_file(): raise GuardError('No explicit one-use approval on disk')
        permit=json.loads(permit_path.read_text(encoding='utf-8'))
        ledger=Ledger(STATE/'ledger.sqlite')
        try:
            existing=ledger.get(operation_id)
            if existing: return {'submitted_now':False,'reason':'IDEMPOTENT_NO_RECLICK',
                'operation':{'id':existing['id'],'status':existing['status'],
                'credits_spent':existing['details'].get('credits_spent'),
                'model_page':existing['details'].get('model_page')}}
            q=await self.quote()
            button=self.page.get_by_role('button',name=f"Gerar {q['credits']}",exact=True)
            if not await button.is_enabled(): raise GuardError('Generate is disabled')
            await button.focus(timeout=5000)
            if not await button.evaluate('(e)=>document.activeElement===e'): raise GuardError('Generate did not receive keyboard focus')
            before=await self.snapshot(operation_id+'-before')
            if not ledger.reserve(operation_id,q,permit): raise GuardError('Operation already reserved')
            details={'before':before,'before_balance':q['balance'],'submitted_at':time.time()}
            try:
                current=await self.quote()
                if current['fingerprint']!=q['fingerprint']: raise GuardError('Quote changed before click')
                await button.press("Enter",timeout=5000)  # One native keyboard activation; no retry.
                details['click_returned']=True
                await self.page.wait_for_timeout(1200)
                details['after']=await self.status()
                details['after_screenshot']=await self.snapshot(operation_id+'-after')
                ledger.finish(operation_id,'submitted',details)
                return {'submitted_now':True,'operation':ledger.get(operation_id)}
            except BaseException as exc:
                details['error_type']=type(exc).__name__
                details['instruction']='Do not retry paid click. Inspect wallet and task history.'
                ledger.finish(operation_id,'uncertain',details)
                raise
        finally: ledger.close()
    async def open_completed_model(self, operation_id: str):
        ledger=Ledger(STATE/'ledger.sqlite')
        try: record=ledger.get(operation_id)
        finally: ledger.close()
        if not record or record['status'] not in ('complete','reviewed'): raise GuardError('No completed model')
        path=record['details']['model_page']
        if not re.fullmatch(r'/pt/workspace/generate/[a-f0-9-]{36}',path): raise GuardError('Unrecognized recorded model path')
        if not (await self.status())['authenticated']: raise GuardError('User login required')
        await self.page.goto('https://'+HOST+path,wait_until='domcontentloaded',timeout=15000)
        return await self.status()

    async def inspect(self):
        state=await self.status()
        state['visible_text']=await self.page.evaluate("() => document.body.innerText.slice(0,2400)")
        state['visible_text']=re.sub(r'[\w.%-]+@[\w.-]+','[EMAIL_REDACTED]',state['visible_text'])
        return state

async def execute(action: str, operation_id: str = 'pip-20260922-55-v1'):
    studio=Studio()
    try:
        await studio.connect()
        if action=='status': return await studio.status()
        if action=='quote': return await studio.quote()
        if action=='snapshot': return await studio.snapshot()
        if action=='inspect': return await studio.inspect()
        if action=='submit': return await studio.submit(operation_id)
        raise GuardError('Unsupported action')
    finally: await studio.disconnect()

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action',choices=['status','quote','snapshot','inspect','submit'])
    parser.add_argument('--operation-id',default='pip-20260922-55-v1')
    args=parser.parse_args()
    print(json.dumps(asyncio.run(asyncio.wait_for(execute(args.action,args.operation_id),40)),ensure_ascii=True))
