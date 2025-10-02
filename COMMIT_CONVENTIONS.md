# Commit Message Conventions

Following these conventions enables automatic changelog generation and helps maintain a clean git history.

## Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

## Types (lowercase)

- **feat**: new feature
- **fix**: bug fix
- **docs**: documentation changes
- **style**: code style changes (formatting, semicolons, etc.)
- **refactor**: code refactoring without feature changes
- **perf**: performance improvements
- **test**: adding or updating tests
- **build**: build system or dependencies
- **ci**: ci/cd configuration changes
- **chore**: other changes that don't modify src or test files
- **revert**: reverts a previous commit

## Scope (Optional, lowercase)

The scope should be the name of the module affected:

- `infra`, `fn`, `api`, `auth`, etc.
- `deps` for dependencies
- `build` for build configuration
- `*` for changes affecting multiple modules

## Subject (lowercase)

- **all lowercase** - no capital letters
- use imperative mood ("add" not "added" or "adds")
- no period at the end
- maximum 50 characters

## Examples

### Feature
```
feat(api): add user profile endpoint

add new user profile api endpoint including:
- get user profile data
- update user preferences
- avatar upload support

closes #123
```

### Bug Fix
```
fix(auth): resolve token refresh issue

the cognito token refresh was failing due to incorrect
configuration of the refresh token expiration time

fixes #456
```

### Breaking Change
```
feat(infra)!: change cognito user pool configuration

BREAKING CHANGE: user pool now requires email verification
before account activation

migration:
update cdk.context.json to include email verification settings
```

### Documentation
```
docs(readme): add quickstart guide

add comprehensive quickstart section with:
- installation instructions
- basic usage examples
- common patterns
```

### Performance
```
perf(fn): optimize lambda cold start by 40%

implement shared layer for common dependencies
reducing bundle size and initialization time
```

### Dependencies
```
build(deps): update aws sdk to 2.33.11

update includes:
- security patches
- new dynamodb features
- performance improvements
```

## Commit Message Template

Save this as `.gitmessage` in your project root:

```
# <type>(<scope>): <subject>
#
# <body>
#
# <footer>
#
# type: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert
# scope: infra, fn, api, auth, deps, build, or *
# subject: all lowercase, imperative mood, max 50 chars
# body: explain what and why (not how), wrap at 72 chars
# footer: references to issues, breaking changes (BREAKING CHANGE in caps)
```

Configure git to use it:
```bash
git config commit.template .gitmessage
```

## Tools

### Commitizen
Install commitizen for interactive commit message creation:

```bash
npm install -g commitizen
npm install -g cz-conventional-changelog
echo '{ "path": "cz-conventional-changelog" }' > ~/.czrc
```

Then use `git cz` instead of `git commit`.

### Commit Linting

Add to `.github/workflows/commit-lint.yml`:

```yaml
name: Lint Commits
on: [pull_request]

jobs:
  commitlint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - uses: wagoid/commitlint-github-action@v5
```

## Benefits

Following these conventions enables:

1. **Automatic changelog generation** based on commit types
2. **Cleaner git history** that's easy to navigate
3. **Better collaboration** through clear communication
4. **Semantic versioning** decisions based on commit types
5. **Easy identification** of breaking changes
6. **Automated release notes** categorized by type

## Quick Reference

| type | release | changelog section | version bump |
|------|---------|------------------|--------------|
| feat | ✅ | features | minor |
| fix | ✅ | bug fixes | patch |
| docs | ❌ | documentation | none |
| style | ❌ | - | none |
| refactor | ❌ | - | none |
| perf | ✅ | performance | patch |
| test | ❌ | - | none |
| build | ❌ | - | none |
| ci | ❌ | - | none |
| chore | ❌ | - | none |
| revert | ✅ | reverts | patch |
| BREAKING | ✅ | breaking changes | major |

## Summary

✅ **always use lowercase** for:
- commit types (feat, fix, docs)
- scopes (vpc, eks, deps)
- subject lines (entire first line after colon)
- commit body text

❌ **only use uppercase** for:
- BREAKING CHANGE: keyword in footer
- acronyms when necessary (AWS, CDK, IAM)