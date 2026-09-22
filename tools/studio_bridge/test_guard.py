import json,tempfile,time,unittest
from pathlib import Path
from guard import GuardError,Ledger,fingerprint,validate

class SpendingContracts(unittest.TestCase):
    def setUp(self):
        self.now=time.time()
        self.q=dict(authenticated=True,reference_sha256='reference',credits=55,balance=3915,
                    quality='maximum',topology='clean',page_path='/pt',observed_at=self.now)
        self.q['fingerprint']=fingerprint(self.q)
        self.p=dict(reference_sha256='reference',max_credits=55,expected_balance=3915,
                    quality='maximum',topology='clean',expires_at=self.now+600)
    def test_valid_quote(self): validate(self.q,self.p,self.now)
    def test_over_budget(self):
        self.q['credits']=56
        with self.assertRaises(GuardError): validate(self.q,self.p,self.now)
    def test_missing_price(self):
        self.q['credits']=None
        with self.assertRaises(GuardError): validate(self.q,self.p,self.now)
    def test_wrong_image(self):
        self.q['reference_sha256']='other'
        with self.assertRaises(GuardError): validate(self.q,self.p,self.now)
    def test_logged_out(self):
        self.q['authenticated']=False
        with self.assertRaises(GuardError): validate(self.q,self.p,self.now)
    def test_stale_quote(self):
        with self.assertRaises(GuardError): validate(self.q,self.p,self.now+121)
    def test_expired_approval(self):
        with self.assertRaises(GuardError): validate(self.q,self.p,self.now+601)
    def test_wallet_changed(self):
        self.q['balance']=3860
        with self.assertRaises(GuardError): validate(self.q,self.p,self.now)
    def test_settings_changed(self):
        self.q['quality']='other'
        with self.assertRaises(GuardError): validate(self.q,self.p,self.now)
    def test_fingerprint_tamper(self):
        self.q['fingerprint']='bad'
        with self.assertRaises(GuardError): validate(self.q,self.p,self.now)
    def test_reconnect_cannot_repeat_click(self):
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp)/'ledger.sqlite'; a=Ledger(path)
            self.assertTrue(a.reserve('one',self.q,self.p)); a.finish('one','submitted',{})
            a.close(); b=Ledger(path)
            self.assertFalse(b.reserve('one',self.q,self.p)); b.close()
    def test_uncertain_result_blocks_new_submission(self):
        with tempfile.TemporaryDirectory() as tmp:
            a=Ledger(Path(tmp)/'ledger.sqlite'); a.reserve('one',self.q,self.p)
            a.finish('one','uncertain',{})
            with self.assertRaises(GuardError): a.reserve('two',self.q,self.p)
            a.close()
    def test_unfinished_reservation_blocks_competitor(self):
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp)/'ledger.sqlite'; a=Ledger(path); b=Ledger(path)
            a.reserve('one',self.q,self.p)
            with self.assertRaises(GuardError): b.reserve('two',self.q,self.p)
            a.close();b.close()

if __name__=='__main__': unittest.main(verbosity=2)
