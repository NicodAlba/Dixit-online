"""One authorized six-account downgrade; no delete/reset/password/JWT-key operation."""
import argparse,hashlib,json,re
from pathlib import Path
from supabase_check import Operator


def authorized_candidates(path):
    if path.stat().st_mode & 0o077: raise SystemExit('Inventory must have private permissions')
    records=json.loads(path.read_text())['candidate_accounts']
    admins=[row for row in records if row['authority']=='ADMIN']
    if len(admins)!=6 or len({row['id'] for row in admins})!=6: raise SystemExit('Exactly six unique inventoried ADMINs required')
    for row in admins:
        if not isinstance(row['id'],int) or row['id']<=0 or not re.fullmatch(r'security_[0-9a-f]{32}',row['username']):
            raise SystemExit('Unexpected candidate identity; no changes allowed')
    return admins


def verified_backup(path):
    if path.stat().st_mode & 0o077: raise SystemExit('Backup must be private')
    metadata=json.loads(path.with_suffix(path.suffix+'.json').read_text())
    if not metadata.get('restored_and_app_verified') or metadata['encrypted_sha256']!=hashlib.sha256(path.read_bytes()).hexdigest():
        raise SystemExit('A verified encrypted backup is required before downgrade')


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config',type=Path,required=True);parser.add_argument('--pg-bin',type=Path,required=True)
    parser.add_argument('--project-ref',required=True);parser.add_argument('--inventory',type=Path,required=True)
    parser.add_argument('--verified-backup',type=Path,required=True)
    parser.add_argument('--acknowledge-six-admin-downgrade',action='store_true',required=True)
    args=parser.parse_args();admins=authorized_candidates(args.inventory);verified_backup(args.verified_backup)
    operator=Operator(args.config,args.pg_bin,args.project_ref)
    before=operator.fingerprint()
    # Passwords and all non-authority appuser columns must also remain unchanged.
    unchanged="SELECT md5(coalesce(string_agg((to_jsonb(u)-'authority_id')::text,E'\\n' ORDER BY id),'')) FROM dixit.appusers u"
    users_before=operator.query(unchanged,runtime=True)
    entries=','.join("("+str(row['id'])+",'"+row['username']+"')" for row in admins)
    sql="""BEGIN;
SELECT pg_advisory_xact_lock(hashtextextended('dixit.first-administrator',0));
CREATE TEMP TABLE approved_admins(id integer PRIMARY KEY,username text UNIQUE) ON COMMIT DROP;
INSERT INTO approved_admins VALUES """+entries+""";
SELECT u.id FROM dixit.appusers u JOIN approved_admins e USING(id) FOR UPDATE OF u;
DO $$ BEGIN
 IF (SELECT count(*) FROM dixit.appusers u JOIN approved_admins e ON u.id=e.id AND u.username=e.username JOIN dixit.authorities a ON a.id=u.authority_id WHERE a.authority='ADMIN')<>6
 OR (SELECT count(*) FROM dixit.appusers u JOIN dixit.authorities a ON a.id=u.authority_id WHERE a.authority='ADMIN')<>6
 OR (SELECT count(*) FROM dixit.authorities WHERE authority='PLAYER')<>1 THEN
 RAISE EXCEPTION 'Inventory/current roles differ; no downgrade allowed'; END IF;
END $$;
UPDATE dixit.appusers u SET authority_id=(SELECT id FROM dixit.authorities WHERE authority='PLAYER') FROM approved_admins e WHERE u.id=e.id AND u.username=e.username;
DO $$ BEGIN IF (SELECT count(*) FROM dixit.appusers u JOIN approved_admins e USING(id) JOIN dixit.authorities a ON a.id=u.authority_id WHERE a.authority='PLAYER')<>6 THEN RAISE EXCEPTION 'Downgrade count mismatch'; END IF; END $$;
COMMIT;
"""
    operator.query(sql,runtime=True)
    after=operator.fingerprint()
    assert all(value==after[key] for key,value in before.items() if key!='appusers'), 'Unexpected table/sequence change'
    assert users_before==operator.query(unchanged,runtime=True), 'Non-role account columns changed'
    assert operator.query("select count(*) from dixit.appusers u join dixit.authorities a on a.id=u.authority_id where a.authority='ADMIN'",runtime=True)=='0'
    print('Exactly six inventoried ADMIN accounts changed to PLAYER; all other tables, sequence, passwords and account fields unchanged')

if __name__=='__main__':main()
