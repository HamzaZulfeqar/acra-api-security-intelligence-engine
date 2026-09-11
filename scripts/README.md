# scripts

The retained Bash scripts cover core and S2–S4 verification. The following PowerShell scripts support the S5 defensive continuation on Windows without modifying global PATH or deleting earlier build output.

- `verify-sprint5-defensive.ps1 -Compiler <javac-path> -Java <java-path>` compiles all core sources/tests for Java 21, runs offline regression and focused defensive tests, scans assessment network/process dependencies, and writes dated command/output/source-digest evidence under `docs/testing/artifacts/`.
- `package-sprint5-continuation.ps1 -BaselineArchive <canonical-zip> -OutputDirectory <new-directory-outside-repository>` verifies the known canonical hash, generates manifests, packages all non-disposable repository files, fully decompresses and clean-unpacks the ZIP, and compares every file hash. It refuses to overwrite an existing archive or package a deleted baseline source file.

The verification script does not start ACRA-Lab or run Maven/Burp. An installed JDK supporting `--release 21` is required. Exact JDK 21 runtime verification is a separate lane when a different compiler/runtime is selected. Package outputs must be outside the repository; build/cache files are excluded, and historical documentation/tests/lab/configuration are retained.

If the original ZIP is unavailable after earlier verified extraction, the explicit `-CurrentTreeOnly` packaging mode exports and verifies the present tree and labels the unavailable baseline comparison BLOCKED. In that mode file status comes from `docs/sprints/sprint-05-change-journal.json`; it must not be described as a fresh byte diff against the original ZIP.
