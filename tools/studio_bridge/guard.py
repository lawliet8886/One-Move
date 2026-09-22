"""One-use Studio spending guard. No API keys, billing calls or browser secrets."""
from __future__ import annotations
import hashlib, json, sqlite3, time
from pathlib import Path

class GuardError(RuntimeError): pass

def fingerprint(quote: dict) -> str:
    fields = {k: quote.get(k) for k in ('reference_sha256','credits','balance','quality','topology','page_path')}
    return hashlib.sha256(json.dumps(fields,sort_keys=True).encode()).hexdigest()

def validate(quote: dict, permit: dict, now: float | None = None) -> None:
    now = time.time() if now is None else now
    if not quote.get('authenticated'): raise GuardError('Studio login required')
    if quote.get('reference_sha256') != permit['reference_sha256']: raise GuardError('Wrong reference')
    price, balance = quote.get('credits'), quote.get('balance')
    if type(price) is not int or not 0 < price <= permit['max_credits']: raise GuardError('Unknown or excessive quote')
    if type(balance) is not int or balance != permit['expected_balance'] or balance < price: raise GuardError('Wallet changed or insufficient')
    if quote.get('quality') != permit['quality'] or quote.get('topology') != permit['topology']: raise GuardError('Settings changed')
    if now > permit['expires_at']: raise GuardError('Approval expired')
    if not 0 <= now - quote['observed_at'] <= 120: raise GuardError('Quote is stale')
    if quote.get('fingerprint') != fingerprint(quote): raise GuardError('Quote integrity failed')

class Ledger:
    def __init__(self, path: Path):
        path.parent.mkdir(parents=True,exist_ok=True)
        self.db = sqlite3.connect(path,timeout=3)
        self.db.execute('CREATE TABLE IF NOT EXISTS operations (id TEXT PRIMARY KEY, status TEXT NOT NULL, quote TEXT NOT NULL, details TEXT NOT NULL)')
    def reserve(self, operation_id: str, quote: dict, permit: dict) -> bool:
        validate(quote,permit)
        try:
            self.db.execute('BEGIN IMMEDIATE')
            if self.db.execute('SELECT 1 FROM operations WHERE id=?',(operation_id,)).fetchone():
                self.db.rollback(); return False
            if self.db.execute("SELECT 1 FROM operations WHERE status IN ('reserved','uncertain')").fetchone():
                raise GuardError('An unresolved operation exists; inspect before another submission')
            self.db.execute('INSERT INTO operations VALUES (?,?,?,?)',
                (operation_id,'reserved',json.dumps(quote),json.dumps({'reserved_at':time.time()})))
            self.db.commit(); return True
        except BaseException:
            self.db.rollback(); raise
    def finish(self, operation_id: str, status: str, details: dict):
        if status not in ('submitted','uncertain','complete','failed','reviewed'): raise GuardError('Unknown state')
        with self.db:
            self.db.execute('UPDATE operations SET status=?,details=? WHERE id=?',
                            (status,json.dumps(details),operation_id))
    def get(self, operation_id: str) -> dict | None:
        row=self.db.execute('SELECT id,status,quote,details FROM operations WHERE id=?',(operation_id,)).fetchone()
        return None if row is None else dict(id=row[0],status=row[1],quote=json.loads(row[2]),details=json.loads(row[3]))
    def close(self): self.db.close()
