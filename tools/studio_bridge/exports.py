"""Download an existing, ledger-matched Studio candidate. No generation or paid conversion."""
import asyncio,hashlib,json,re,time
from pathlib import Path
from bridge import STATE
from guard import Ledger,GuardError

async def download_glb(studio,operation_id: str) -> dict:
    ledger=Ledger(STATE/'ledger.sqlite')
    try: record=ledger.get(operation_id)
    finally: ledger.close()
    if not record or record['status'] not in ('complete','reviewed'): raise GuardError('Model not completed in ledger')
    if (await studio.status())['page_path']!=record['details']['model_page']: raise GuardError('Wrong model page')
    existing=STATE/'assets'/operation_id/'pip-studio-v1.glb'
    if existing.exists(): return {'already_downloaded':True,'file':str(existing),'sha256':hashlib.sha256(existing.read_bytes()).hexdigest()}
    dialog=studio.page.get_by_role('dialog').filter(has_text='Nome do Arquivo')
    if await dialog.count()!=1 or not await dialog.is_visible(): raise GuardError('Open the reviewed export panel first')
    text=' '.join((await dialog.inner_text()).split())
    if not await dialog.get_by_role('combobox').filter(has_text=re.compile(r'^GLB$')).count(): raise GuardError('GLB format not selected')
    if 'Current' not in text or re.search(r'cr[eÃ©]dito|credit|Gerar',text,re.I): raise GuardError('Export must use current textures, without paid processing')
    output=STATE/'assets'/operation_id;output.mkdir(parents=True,exist_ok=True)
    destination=output/'pip-studio-v1.glb'
    if destination.exists(): return {'already_downloaded':True,'file':str(destination)}
    await dialog.get_by_role('textbox').fill('pip-studio-v1')
    cdp=await studio.browser.new_browser_cdp_session()
    transfer=output/'transfer';transfer.mkdir(exist_ok=True)
    finished=asyncio.get_running_loop().create_future();seen={}
    def begin(event):
        seen[event['guid']]={'name':event.get('suggestedFilename',''),'at':time.time()}
    def progress(event):
        if event.get('guid') in seen and event.get('state') in ('completed','canceled') and not finished.done(): finished.set_result(event)
    cdp.on('Browser.downloadWillBegin',begin);cdp.on('Browser.downloadProgress',progress)
    before=(await studio.status())['balance']
    try:
        await cdp.send('Browser.setDownloadBehavior',{'behavior':'allowAndName','downloadPath':str(transfer),'eventsEnabled':True})
        await dialog.get_by_role('button',name='Exportar',exact=True).press('Enter')
        event=await asyncio.wait_for(finished,90)
        if event['state']!='completed': raise GuardError('Browser canceled download')
        guid=event['guid']
        if not re.fullmatch(r'[a-zA-Z0-9-]{1,80}',guid): raise GuardError('Unexpected download identifier')
        source=transfer/guid
        if not source.is_file() or source.stat().st_size>160*1024*1024: raise GuardError('Missing or oversized download')
        with source.open('rb') as f:
            if f.read(4)!=b'glTF': raise GuardError('Downloaded payload is not GLB; preserved for inspection')
        source.rename(destination)
        result={'source':'Tripo Studio, v3.1 max quality','operation_id':operation_id,
            'model_page':record['details']['model_page'],'file':str(destination),'bytes':destination.stat().st_size,
            'sha256':hashlib.sha256(destination.read_bytes()).hexdigest(),'generation_credits':record['details']['credits_spent'],
            'balance_before_export':before,'balance_after_export':(await studio.status())['balance'],
            'approved_for_runtime':False,'created_at':time.time()}
        (output/'source-manifest.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
        return result
    finally:
        await cdp.send('Browser.setDownloadBehavior',{'behavior':'default','eventsEnabled':False})
        await cdp.detach()
