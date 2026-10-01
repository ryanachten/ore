# ore

## Development

### Prerequisites
- JDK 25 (Gradle provisions the toolchain if a different JDK is on `PATH`)
- [Bun](https://bun.sh) for the frontend. The version is pinned in [`frontend/.bun-version`](./frontend/.bun-version) and CI reads the same file, so local and CI always agree. Upgrade it deliberately with `bun upgrade` and commit the bumped file.
- Ensure localstack script is executable: `chmod +x localstack/init.sh`
- Copy `.env.example` to `.env` and set `LOCALSTACK_AUTH_TOKEN` (required by `compose.yaml`, so `make up` needs it set): `cp .env.example .env`
- Install frontend dependencies once: `make install-frontend`

### Commands
- `make build` — autofix formatting, then build the backend. Mutates files, so it's a local loop, not a gate.
- `make verify` — what CI runs: lint (check-only, both stacks), test, frontend build, backend build. Never mutates files.

### Pre-commit hook
Runs `make lint` before every commit, catching style violations in seconds. It is a fast subset of `make verify`, not a replacement for it. Enable it once per clone:

```
git config core.hooksPath .githooks
```

### CI
[`.github/workflows/ci.yml`](./.github/workflows/ci.yml) runs two parallel jobs on every push to `main` and every PR:

| Job | What it does |
| --- | --- |
| `backend` | `make lint-backend`, then `make build-backend` (Gradle `build`, which includes checkstyle and tests) |
| `frontend` | `make install-frontend`, then `make lint-frontend`, `make test-frontend`, `make build-frontend` |

Each step calls a Makefile target rather than an inlined command, so local and CI can't drift. Both jobs fail the PR independently.