# AI Collaboration Protocol

Repository state overrides chat history.

Before ChatGPT, Claude, or another coding agent modifies ACRA:

1. Inspect the repository.
2. Read `PROJECT_STATE.md` and `sprint-status.md`.
3. Read the current sprint specification.
4. Read applicable requirements and accepted ADRs.
5. Inspect existing source and tests.
6. Verify claimed previous-sprint functionality.
7. Modify only the current sprint scope.
8. Run applicable gates.
9. Update tests, docs, changelog, project state, and sprint report.

No agent may assume a feature exists because it appeared in an earlier conversation.
