"""Remove only explicitly inventoried acceptance accounts after a verified backup.

No application filter, username rewrite, schema change, sequence reset or CASCADE.
Mixed games abort the entire transaction. Ordinary user identities are preserved.
"""
import argparse
import json
import re
from pathlib import Path
from retire_acceptance_admins import verified_backup
from supabase_check import Operator


def candidates(path):
    if path.is_symlink() or path.stat().st_mode & 0o077:
        raise SystemExit("Use a private regular inventory (0600)")
    rows = json.loads(path.read_text())["candidate_accounts"]
    if not rows or len({r["id"] for r in rows}) != len(rows) or len({r["username"] for r in rows}) != len(rows):
        raise SystemExit("Inventory must contain unique exact identities")
    for row in rows:
        if (type(row.get("id")) is not int or row["id"] <= 0
                or row.get("authority") != "PLAYER"
                or not re.fullmatch(r"security_[0-9a-f]{32}|restart_[0-9a-f]{16}|demo_[0-9a-f]{20}_[0-3]", row.get("username", ""))):
            raise SystemExit("Only exact inventoried synthetic PLAYER identities are allowed")
        if row["username"].startswith("demo_"):
            proof = Path(row.get("evidence_file", ""))
            if not proof.is_file() or proof.is_symlink() or proof.stat().st_mode & 0o077:
                raise SystemExit("Demo identities require their private acceptance fixture")
            accounts = json.loads(proof.read_text()).get("accounts", [])
            if not any(a.get("id") == row["id"] and a.get("username") == row["username"]
                       and a.get("roles") == ["PLAYER"] for a in accounts):
                raise SystemExit("Identity does not match the acceptance fixture")
    return rows


def cleanup_sql(rows, apply=False):
    entries = ",".join(f"({row['id']},'{row['username']}')" for row in rows)
    # Values enter SQL only after candidates() validates integer/hex-only identities.
    return """BEGIN;
SET LOCAL lock_timeout='5s';
SET LOCAL statement_timeout='30s';
LOCK TABLE dixit.appusers,dixit.players,dixit.games,dixit.turns,dixit.cards,
 dixit.player_votes,dixit.game_winners,dixit.messages,dixit.game_spectators,
 dixit.game_invitations,dixit.friend_requests,dixit.achievements IN SHARE ROW EXCLUSIVE MODE;
CREATE TEMP TABLE retired_accounts(id integer PRIMARY KEY,username text UNIQUE) ON COMMIT DROP;
INSERT INTO retired_accounts VALUES """ + entries + """;
DO $$ BEGIN
 IF (SELECT count(*) FROM retired_accounts) <> (SELECT count(*) FROM dixit.appusers u
 JOIN retired_accounts r ON u.id=r.id AND u.username=r.username
 JOIN dixit.authorities a ON a.id=u.authority_id WHERE a.authority='PLAYER') THEN
 RAISE EXCEPTION 'Exact identity/authority inventory changed'; END IF;
END $$;
CREATE TEMP TABLE retired_players ON COMMIT DROP AS SELECT id FROM dixit.players WHERE user_id IN (SELECT id FROM retired_accounts);
CREATE TEMP TABLE retired_games ON COMMIT DROP AS
 SELECT id FROM dixit.games WHERE leader_id IN (SELECT id FROM retired_accounts)
 UNION SELECT game_id FROM dixit.players WHERE id IN (SELECT id FROM retired_players) AND game_id IS NOT NULL
 UNION SELECT game_id FROM dixit.game_spectators WHERE user_id IN (SELECT id FROM retired_accounts)
 UNION SELECT game_id FROM dixit.cards WHERE user_id IN (SELECT id FROM retired_accounts) AND game_id IS NOT NULL
 UNION SELECT game_id FROM dixit.messages WHERE player_id IN (SELECT id FROM retired_players) AND game_id IS NOT NULL;
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM dixit.games WHERE id IN(SELECT id FROM retired_games) AND leader_id IS NOT NULL AND leader_id NOT IN(SELECT id FROM retired_accounts))
 OR EXISTS(SELECT 1 FROM dixit.players WHERE game_id IN(SELECT id FROM retired_games) AND user_id NOT IN(SELECT id FROM retired_accounts))
 OR EXISTS(SELECT 1 FROM dixit.game_spectators WHERE game_id IN(SELECT id FROM retired_games) AND user_id NOT IN(SELECT id FROM retired_accounts))
 OR EXISTS(SELECT 1 FROM dixit.cards WHERE game_id IN(SELECT id FROM retired_games) AND user_id IS NOT NULL AND user_id NOT IN(SELECT id FROM retired_accounts))
 OR EXISTS(SELECT 1 FROM dixit.messages m JOIN dixit.players p ON p.id=m.player_id WHERE m.game_id IN(SELECT id FROM retired_games) AND p.user_id NOT IN(SELECT id FROM retired_accounts))
 OR EXISTS(SELECT 1 FROM dixit.game_winners w JOIN dixit.players p ON p.id=w.player_id WHERE w.game_id IN(SELECT id FROM retired_games) AND p.user_id NOT IN(SELECT id FROM retired_accounts))
 OR EXISTS(SELECT 1 FROM dixit.player_votes v JOIN dixit.cards c ON c.id=v.card_id JOIN dixit.players p ON p.id=v.player_id WHERE c.game_id IN(SELECT id FROM retired_games) AND p.user_id NOT IN(SELECT id FROM retired_accounts)) THEN
 RAISE EXCEPTION 'Mixed game with an ordinary account: nothing may be removed'; END IF;
 IF EXISTS(SELECT 1 FROM dixit.games WHERE id IN(SELECT id FROM retired_games) AND start_date IS NOT NULL AND end_date IS NULL) THEN
 RAISE EXCEPTION 'An inventoried test game is active: stop and review'; END IF;
END $$;
CREATE TEMP TABLE retired_cards ON COMMIT DROP AS SELECT id FROM dixit.cards
 WHERE game_id IN(SELECT id FROM retired_games) OR player_id IN(SELECT id FROM retired_players) OR user_id IN(SELECT id FROM retired_accounts);
CREATE TEMP TABLE preserved_accounts ON COMMIT DROP AS SELECT * FROM dixit.appusers WHERE id NOT IN(SELECT id FROM retired_accounts);
CREATE TEMP TABLE preserved_games ON COMMIT DROP AS SELECT * FROM dixit.games WHERE id NOT IN(SELECT id FROM retired_games);
CREATE TEMP TABLE preserved_sequence ON COMMIT DROP AS SELECT * FROM dixit.entity_sequence;
SELECT json_build_object('accounts',(SELECT count(*) FROM retired_accounts),'games',(SELECT count(*) FROM retired_games),
 'players',(SELECT count(*) FROM retired_players),'cards',(SELECT count(*) FROM retired_cards));
DELETE FROM dixit.player_votes WHERE card_id IN(SELECT id FROM retired_cards) OR player_id IN(SELECT id FROM retired_players);
UPDATE dixit.players SET player_vote_id=NULL WHERE id IN(SELECT id FROM retired_players);
DELETE FROM dixit.game_winners WHERE game_id IN(SELECT id FROM retired_games) OR player_id IN(SELECT id FROM retired_players);
UPDATE dixit.turns SET answer_id=NULL WHERE game_id IN(SELECT id FROM retired_games);
DELETE FROM dixit.messages WHERE game_id IN(SELECT id FROM retired_games) OR player_id IN(SELECT id FROM retired_players);
DELETE FROM dixit.cards WHERE id IN(SELECT id FROM retired_cards);
DELETE FROM dixit.turns WHERE game_id IN(SELECT id FROM retired_games);
DELETE FROM dixit.game_invitations WHERE game_id IN(SELECT id FROM retired_games) OR invited_id IN(SELECT id FROM retired_accounts) OR inviter IN(SELECT username FROM retired_accounts);
DELETE FROM dixit.game_spectators WHERE game_id IN(SELECT id FROM retired_games) OR user_id IN(SELECT id FROM retired_accounts);
DELETE FROM dixit.players WHERE id IN(SELECT id FROM retired_players);
DELETE FROM dixit.games WHERE id IN(SELECT id FROM retired_games);
DELETE FROM dixit.friend_requests WHERE sender_id IN(SELECT id FROM retired_accounts) OR receiver_id IN(SELECT id FROM retired_accounts);
DELETE FROM dixit.achievements WHERE user_id IN(SELECT id FROM retired_accounts);
DELETE FROM dixit.appusers WHERE id IN(SELECT id FROM retired_accounts);
DO $$ BEGIN
 IF EXISTS((SELECT * FROM preserved_accounts EXCEPT SELECT * FROM dixit.appusers) UNION ALL
 (SELECT * FROM dixit.appusers EXCEPT SELECT * FROM preserved_accounts))
 OR EXISTS((SELECT * FROM preserved_games EXCEPT SELECT * FROM dixit.games) UNION ALL
 (SELECT * FROM dixit.games EXCEPT SELECT * FROM preserved_games))
 OR EXISTS((SELECT * FROM preserved_sequence EXCEPT SELECT * FROM dixit.entity_sequence) UNION ALL
 (SELECT * FROM dixit.entity_sequence EXCEPT SELECT * FROM preserved_sequence)) THEN
 RAISE EXCEPTION 'Ordinary identity/game or sequence changed: rolling back'; END IF;
END $$;
""" + ("COMMIT;" if apply else "ROLLBACK;")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, required=True)
    parser.add_argument("--pg-bin", type=Path, required=True)
    parser.add_argument("--project-ref", required=True)
    parser.add_argument("--inventory", type=Path, required=True)
    parser.add_argument("--verified-backup", type=Path, required=True)
    parser.add_argument("--apply", action="store_true", help="Commit; default rehearses the exact deletes and rolls back")
    parser.add_argument("--acknowledge-inventoried-test-deletion", action="store_true", required=True)
    args = parser.parse_args()
    rows = candidates(args.inventory)
    verified_backup(args.verified_backup)
    operator = Operator(args.config, args.pg_bin, args.project_ref)
    print(operator.query(cleanup_sql(rows, args.apply), runtime=True))
    print("Inventoried test-only data removed" if args.apply else "Rehearsal passed; transaction rolled back, no data removed")


if __name__ == "__main__":
    main()
