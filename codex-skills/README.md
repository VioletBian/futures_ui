# Codex Skills Sync

This directory is a git-tracked mirror of selected local Codex skills.

Default behavior:

- `scripts/export_skills.sh` copies custom skills from `$CODEX_HOME/skills` into this repo.
- `scripts/install_skills_from_repo.sh` copies tracked skills from this repo back into `$CODEX_HOME/skills`.
- The `.system` skill set is excluded by default because it is usually managed by Codex itself.

Typical workflow:

1. Update or add a local skill under `~/.codex/skills`.
2. Run `./scripts/export_skills.sh`.
3. Commit and push this repo to GitHub.
4. On another machine, pull this repo and run `./scripts/install_skills_from_repo.sh`.

Optional:

- Run `./scripts/export_skills.sh --include-system` if you explicitly want to mirror `.system` as well.
