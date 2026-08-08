# ore

## Development

### Prerequisites
- Ensure localstack script is executable: `chmod +x localstack/init.sh`

### Pre-commit hook
Run `make lint` automatically before every commit (not part of CI). Enable it once per clone:

```
git config core.hooksPath .githooks
```