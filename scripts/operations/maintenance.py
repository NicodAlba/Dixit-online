"""Interactive ADMIN control through Dixit REST; no tokens/passwords in arguments or files."""
import argparse
import getpass
import ipaddress
import json
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen


def origin(value):
    parsed = urlsplit(value)
    local = parsed.hostname == 'localhost'
    try:
        local = local or ipaddress.ip_address(parsed.hostname).is_loopback
    except ValueError:
        pass
    if (not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment
            or parsed.path not in ('', '/') or not (parsed.scheme == 'https' or (local and parsed.scheme == 'http'))):
        raise ValueError('Use an HTTPS application origin, or HTTP loopback for local operation.')
    return value.rstrip('/')


def request(base, path, body=None, token=None, method='GET'):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    data = None if body is None else json.dumps(body).encode()
    with urlopen(Request(base + path, data=data, headers=headers, method=method), timeout=30) as response:
        return json.load(response)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', type=origin, required=True)
    actions = parser.add_mutually_exclusive_group(required=True)
    actions.add_argument('--status', action='store_true')
    actions.add_argument('--enable', action='store_true')
    actions.add_argument('--disable', action='store_true')
    args = parser.parse_args()
    username = input('Administrator username: ')
    password = getpass.getpass('Administrator password: ')
    try:
        login = request(args.url, '/api/v1/auth/signin', {'username': username, 'password': password}, method='POST')
        password = None
        body = None if args.status else {'enabled': bool(args.enable)}
        result = request(args.url, '/api/v1/operations/maintenance', body, login['token'], 'GET' if args.status else 'PUT')
        print(json.dumps({key: result[key] for key in ('enabled', 'activeGameCount', 'waitingLobbyCount')}, indent=2))
    except HTTPError as error:
        raise SystemExit('Dixit operator request failed with HTTP ' + str(error.code) + '; no credentials printed.')
    except (URLError, TimeoutError):
        raise SystemExit('Dixit operator connection failed; no credentials printed.')


if __name__ == '__main__':
    main()
