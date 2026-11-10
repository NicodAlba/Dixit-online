"""Exercise cleanup against a disposable PostgreSQL database, never Supabase.

CI: --container ID (the existing isolated CI PostgreSQL service).
Local: --pg-bin /path/to/pg17/bin (creates and stops its own loopback cluster).
"""
import argparse
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import tempfile
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "operations"))
from retire_test_accounts import cleanup_sql

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
choice = parser.add_mutually_exclusive_group(required=True)
choice.add_argument("--container")
choice.add_argument("--pg-bin", type=Path)
args = parser.parse_args()
name = "dixit_cleanup_fixture"
with tempfile.TemporaryDirectory(prefix="dixit-cleanup-fixture-") as folder:
    temporary = Path(folder)
    if args.container:
        prefix = ["docker", "exec", "-i", args.container]
        subprocess.run([*prefix, "createdb", "-U", "postgres", name], check=True, stdout=subprocess.DEVNULL)
        command = [*prefix, "psql", "-U", "postgres", "-d", name]
        env = os.environ.copy()
    else:
        data = temporary / "data"
        with socket.socket() as listener:
            listener.bind(("127.0.0.1", 0)); port = listener.getsockname()[1]
        subprocess.run([str(args.pg_bin / "initdb"), "-D", str(data), "-U", "dixit_cleanup_owner", "-A", "trust", "--no-locale"], check=True, stdout=subprocess.DEVNULL)
        subprocess.run([str(args.pg_bin / "pg_ctl"), "-D", str(data), "-l", str(temporary / "postgres.log"), "-o", f"-h 127.0.0.1 -p {port} -k {temporary}", "-w", "start"], check=True, stdout=subprocess.DEVNULL)
        env = dict(os.environ, PGHOST="127.0.0.1", PGPORT=str(port), PGUSER="dixit_cleanup_owner", PGDATABASE="postgres", PGSSLMODE="disable")
        subprocess.run([str(args.pg_bin / "createdb"), name], env=env, check=True)
        env["PGDATABASE"] = name
        command = [str(args.pg_bin / "psql")]
    def query(sql, expect_success=True):
        result = subprocess.run([*command, "-X", "-qAt", "-v", "ON_ERROR_STOP=1"], input=sql, text=True, env=env, capture_output=True)
        if expect_success:
            assert result.returncode == 0, result.stderr
        else:
            assert result.returncode != 0, "Unsafe cleanup unexpectedly succeeded"
        return result.stdout.strip()
    def fingerprint():
        return query("SELECT jsonb_object_agg(name, value) FROM (" + " UNION ALL ".join(
            f"SELECT '{table}' name,coalesce(jsonb_agg(to_jsonb(t) ORDER BY to_jsonb(t)::text),'[]') value FROM (SELECT * FROM dixit.{table}) t"
            for table in ("appusers","authorities","images","badge_images","games","players","turns","cards","player_votes","game_winners","messages","game_spectators","game_invitations","friend_requests","achievements","entity_sequence")) + ") f")
    rows = [{"id": i, "username": "security_" + format(i, "032x"), "authority": "PLAYER"} for i in (3,4)]
    try:
        query("CREATE SCHEMA dixit;" + (ROOT / "src/main/resources/db/migration/V1__initial_schema.sql").read_text())
        query("""INSERT INTO dixit.authorities VALUES(1,'ADMIN'),(2,'PLAYER');
INSERT INTO dixit.images VALUES(0,'/Cards/0.png');
INSERT INTO dixit.badge_images VALUES(1,'WIN','badge.png');
INSERT INTO dixit.appusers VALUES(1,1,'owner-hash','owner'),(2,2,'normal-hash','normal'),
(2,3,'synthetic-hash','security_00000000000000000000000000000003'),(2,4,'synthetic-hash','security_00000000000000000000000000000004');
INSERT INTO dixit.games(id,leader_id,cancelled,start_date,end_date) VALUES(10,3,false,now(),now()),(11,1,false,NULL,NULL);
INSERT INTO dixit.players(id,user_id,game_id,points) VALUES(20,3,10,7),(21,4,10,5),(22,2,11,0);
INSERT INTO dixit.turns(id,game_id,storyteller_id,state) VALUES(40,10,20,'FINISHED');
INSERT INTO dixit.cards(id,game_id,image_id,player_id,turn_id,user_id) VALUES(30,10,0,20,40,3);
UPDATE dixit.turns SET answer_id=30 WHERE id=40;
UPDATE dixit.players SET player_vote_id=30 WHERE id=21;
INSERT INTO dixit.player_votes VALUES(30,21);
INSERT INTO dixit.game_winners VALUES(10,20);
INSERT INTO dixit.messages(id,game_id,player_id,text) VALUES(50,10,20,'synthetic chat');
INSERT INTO dixit.game_invitations(id,game_id,invited_id,inviter) VALUES(60,11,3,'owner'),(61,11,2,'owner');
INSERT INTO dixit.friend_requests(id,sender_id,receiver_id) VALUES(70,2,3),(71,1,2);
INSERT INTO dixit.achievements(id,name,description,dtype,user_id,badge_image_id,threshold) VALUES(80,'normal','normal','WinAchievement',2,1,1),(81,'test','test','WinAchievement',3,1,1);
""")
        before = fingerprint()
        query(cleanup_sql(rows)); assert fingerprint() == before
        print("PASS: exact deletion rehearsal rolls back every table and sequence")
        query("INSERT INTO dixit.game_spectators VALUES(10,2)")
        mixed = fingerprint()
        query(cleanup_sql(rows, True), False); assert fingerprint() == mixed
        query("DELETE FROM dixit.game_spectators WHERE game_id=10")
        print("PASS: mixed ordinary spectator aborts before any deletion")
        query("UPDATE dixit.games SET end_date=NULL WHERE id=10")
        active = fingerprint()
        query(cleanup_sql(rows, True), False); assert fingerprint() == active
        query("UPDATE dixit.games SET end_date=now() WHERE id=10")
        print("PASS: active synthetic game cannot be removed")
        query("UPDATE dixit.appusers SET authority_id=1 WHERE id=3")
        changed = fingerprint()
        query(cleanup_sql(rows, True), False); assert fingerprint() == changed
        query("UPDATE dixit.appusers SET authority_id=2 WHERE id=3")
        print("PASS: changed identity/role aborts atomically")
        query(cleanup_sql(rows, True))
        assert query("SELECT string_agg(username,',' ORDER BY id) FROM dixit.appusers") == "owner,normal"
        assert query("SELECT count(*) FROM dixit.games WHERE id=11") == "1"
        assert query("SELECT count(*) FROM dixit.achievements WHERE id=80") == "1"
        assert query("SELECT count(*) FROM dixit.friend_requests WHERE id=71") == "1"
        assert query("SELECT count(*) FROM dixit.game_invitations WHERE id=61") == "1"
        assert query("SELECT count(*) FROM dixit.cards") == "0"
        assert query("SELECT last_value FROM dixit.entity_sequence") == "100"
        query(cleanup_sql(rows, True), False)
        print("PASS: only test graph removed, normal users/history/social data/sequence retained; repeat rejected")
    finally:
        if args.container:
            subprocess.run([*prefix, "dropdb", "-U", "postgres", name], check=True, stdout=subprocess.DEVNULL)
        else:
            subprocess.run([str(args.pg_bin / "pg_ctl"), "-D", str(data), "-m", "fast", "-w", "stop"], check=True, stdout=subprocess.DEVNULL)
