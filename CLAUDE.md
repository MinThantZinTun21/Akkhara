# Akkhara Keyboard

An Android keyboard (input method) for English and Myanmar (Burmese).

## Agent skills

### Issue tracker

Issues and specs live in GitHub Issues for `MinThantZinTun21/keyboard`, via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Domain docs

Single-context: `GLOSSARY.md` at the repo root and decisions in `docs/adr/`. See `docs/agents/domain.md`.

## Working rules

- Base branch is `main`. Work lands through pull requests.
- Install and test on the `Small_Phone` emulator only. Do not install on a physical device unless the owner asks.
- The app must never request the `INTERNET` permission (see `docs/adr/0002-no-network-permission.md`).
