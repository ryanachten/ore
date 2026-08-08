# ore

## Development

### Prerequisites
- Ensure localstack script is executable: `chmod +x localstack/init.sh`
- Copy `.env.example` to `.env` and set `LOCALSTACK_AUTH_TOKEN` (required by `compose.yaml`, so `make up` needs it set): `cp .env.example .env`

### Pre-commit hook
Run `make lint` automatically before every commit (not part of CI). Enable it once per clone:

```
git config core.hooksPath .githooks
```