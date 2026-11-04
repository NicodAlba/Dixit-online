"""Read-only public-demo continuity check using private synthetic PLAYER fixtures.

No infrastructure secrets, mutations or managed database connection. Never CI.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
from urllib.parse import urlsplit
from urllib.request import Request, urlopen


def normalize(value, field=''):
    if isinstance(value, dict):
        return {key: normalize(item, key) for key, item in value.items()}
    if isinstance(value, list):
        result = [normalize(item) for item in value]
        # Preserve playedCards order: its anonymity seed must survive a restart.
        if field in ('players', 'winners', 'votes', 'cards'):
            result.sort(key=lambda item: item.get('id', 0))
        return result
    return value


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', required=True)
    parser.add_argument('--fixture', type=Path, required=True)
    parser.add_argument('--save-baseline', type=Path)
    parser.add_argument('--compare', type=Path)
    args = parser.parse_args()
    assert not os.environ.get('CI') and not os.environ.get('GITHUB_ACTIONS'), 'Not an ordinary CI task'
    target = urlsplit(args.url)
    assert target.scheme == 'https' and target.hostname.endswith('.onrender.com')
    assert not target.username and not target.password and target.path in ('', '/') and not target.query and not target.fragment
    assert args.fixture.stat().st_mode & 0o077 == 0, 'Fixture must be private'
    fixture = json.loads(args.fixture.read_text())
    result = json.loads(args.fixture.with_suffix(args.fixture.suffix + '.result.json').read_text())
    accounts = fixture['accounts']
    assert len(accounts) == 4 and all(re.fullmatch(r'demo_[0-9a-f]{20}_[0-3]', a['username']) for a in accounts)
    game_id = result['gameId']
    assert isinstance(game_id, int) and game_id > 0
    base = args.url.rstrip('/')

    def get(path, actor=None):
        headers = {'Authorization': 'Bearer ' + actor['token']} if actor else {}
        with urlopen(Request(base + path, headers=headers), timeout=60) as response:
            assert response.status == 200
            return json.load(response)

    health = get('/actuator/health')
    assert health['status'] == 'UP' and 'components' not in health and 'details' not in health
    expected_authors = {a['id'] for a in accounts}
    fingerprints = {}
    for actor in accounts:
        game = get('/api/v1/games/' + str(game_id), actor)
        assert game['endDate'] and not game['cancelled'] and len(game['turns']) == 8
        assert len(game['players']) == 4
        for player in game['players']:
            assert player['handSize'] == 5
            assert len(player['cards']) == (5 if player['user']['id'] == actor['id'] else 0)
        for turn in game['turns']:
            assert turn['state'] == 'FINISHED' and len(turn['playedCards']) == 4
            assert {card['user']['id'] for card in turn['playedCards']} == expected_authors
            assert sum(len(card['votes']) for card in turn['playedCards']) == 3
            assert turn['answer'] and turn['answer']['id'] in {card['id'] for card in turn['playedCards']}
        assert len({card['id'] for turn in game['turns'] for card in turn['playedCards']}) == 32
        assert get('/api/v1/stats/user/' + str(actor['id']), actor)['gamesPlayed'] >= 1
        assert any(row['id'] == game_id for row in get('/api/v1/games/my', actor))
        fingerprints[str(actor['id'])] = hashlib.sha256(json.dumps(normalize(game), sort_keys=True).encode()).hexdigest()
    baseline = {'gameId': game_id, 'actorViews': fingerprints}
    if args.compare:
        assert json.loads(args.compare.read_text()) == baseline, 'Completed history or anonymous order changed'
    if args.save_baseline:
        fd = os.open(args.save_baseline, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, 'w') as output:
            json.dump(baseline, output, indent=2)
    print('PASS: stable JWTs, four private views, 8 rounds, 32 historical cards/authors, 24 votes, hands/history/statistics'
          + ('; baseline fingerprints identical' if args.compare else ''))


if __name__ == '__main__':
    main()
