# Agent instructions

## Git policy: confirm before writing history

- Never run `git commit` or `git push` without first asking the user and receiving an explicit yes.
- Before asking, show the exact list of files to be staged/committed and the full proposed commit message.
- Also show the changes themselves, not just filenames: the full unified diff (`git diff`, or `git diff --cached` once staged) for every modified file, and the complete contents of every new file.
- Never ask for approval of a change the user has not been shown. Filenames and a message alone are not enough.
- One approval covers that single commit only. Do not carry an earlier yes over to a second commit, an amended commit, or a push.
- Do not bundle the approval request into unrelated work: ask it as a separate, clear question.
- Do not run `git reset --hard`, `git rebase`, `git filter-branch`, `git push --force`, or any other history-rewriting or destructive command without explicit confirmation.
- Never commit or push as a side effect of another request (for example, "fix this bug" does not authorize a commit).
- Read-only git commands are always allowed without asking: `git status`, `git diff`, `git log`, `git show`, `git branch`.
- Leave uncommitted changes in the working tree by default; staging is not permission to commit.

## Commit messages

- Match the style of the repository's existing history (`git log`).
- One or two concise sentences explaining why the change was made.
