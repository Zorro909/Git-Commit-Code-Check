# Package Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move all classes from the flat `de.zorro909.codecheck.*` layout to the five-group layout (`cli`, `daemon`, `core`, `infra`, `legacy`) defined in `docs/superpowers/specs/2026-07-07-package-restructure-design.md`, as a pure move/rename refactor.

**Architecture:** Each task moves one coherent cluster with `git mv` (history-preserving), rewrites `package`/`import` statements with `sed`, repairs same-package references that became cross-package (compiler-driven), and ends with the full 179-test suite green plus a formatter pass. No task changes behavior.

**Tech Stack:** Java 25, Maven (`./mvnw`), Micronaut, spring-javaformat, GNU sed.

## Global Constraints

- Pure moves only: no class renames, no signature changes, no behavior changes (spec "Out Of Scope").
- Every task ends with `Tests run: 179, Failures: 0, Errors: 0` and a clean `./mvnw -q spring-javaformat:validate`.
- Every commit message ends with `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.
- The user must have pre-approved the per-task commits before execution starts (their global git rules require explicit consent for `git commit`). Do not run any `rm`/`rmdir`/`find -delete` without asking the user (Task 8 Step 3).
- All commands run from the repo root `/var/home/zorro/git/Git-Commit-Code-Check`.
- TDD note: this is a pure refactor — the existing suite is the test harness. "Write failing test" steps are replaced by "prove the tree compiles and the suite passes".

## Shared Technique (referenced by name in tasks, commands always spelled out)

1. **Move**: `git mv` files/subtrees to new directories (`mkdir -p` targets first).
2. **Fix package lines**: for moved files, rewrite the `package …;` declaration per target directory *before* any global prefix sed, so prefix seds cannot mis-rewrite them.
3. **Rewrite references**: global seds over `src` — class-specific seds (with `\b`, longest name first within a family) *before* package-prefix seds (`\([.;]\)` capture so both `import x.y.Class;` and `package x.y;` forms match).
4. **Compile-driven import repair**: classes that shared a package now live in different packages and reference each other without imports. Run `./mvnw -q test-compile 2>&1 | grep -A2 "cannot find symbol\|does not exist"`, add the `import de.zorro909.codecheck.…;` line dictated by the task's mapping table for each missing symbol, repeat until compilation is clean. The mapping is total — every missing symbol has exactly one new home.
5. **Format, test, commit**: `./mvnw -q spring-javaformat:apply` (fixes import order), `./mvnw -q test`, then `git add -A && git commit`.

---

### Task 1: Corral the legacy generation under `legacy`, extract `ValidationError` to `core.diagnostic`

**Files:**
- Move (main): `checks/**` → `legacy/checks/**` (except `ValidationError.java` → `core/diagnostic/`), `actions/**` → `legacy/actions/**`, `selector/**` → `legacy/selector/**`, `editor/**` → `legacy/editor/**`, `utils/` → `legacy/utils/`, `FileLoader.java` + `ValidationCheckPipeline.java` → `legacy/`, `validation/CodeCheckRuleAdapter.java` + `validation/FixActionFixerAdapter.java` → `legacy/adapter/`
- Move (test): `checks/**`, `actions/**`, `selector/**` → `legacy/…`; `FileLoaderTest.java`, `ValidationCheckPipelineTest.java` → `legacy/`
- Modify: every file importing the moved classes (sed-driven)

**Interfaces:**
- Produces: packages `de.zorro909.codecheck.legacy.{checks,actions,selector,editor,utils,adapter}`, classes `de.zorro909.codecheck.legacy.{FileLoader,ValidationCheckPipeline}`, class `de.zorro909.codecheck.core.diagnostic.ValidationError`. Later tasks rely on these coordinates.

- [ ] **Step 1: Move files**

```bash
M=src/main/java/de/zorro909/codecheck; T=src/test/java/de/zorro909/codecheck
mkdir -p $M/legacy/adapter $M/core/diagnostic $T/legacy
git mv $M/checks/ValidationError.java $M/core/diagnostic/
git mv $M/validation/CodeCheckRuleAdapter.java $M/legacy/adapter/
git mv $M/validation/FixActionFixerAdapter.java $M/legacy/adapter/
git mv $M/FileLoader.java $M/ValidationCheckPipeline.java $M/legacy/
git mv $T/FileLoaderTest.java $T/ValidationCheckPipelineTest.java $T/legacy/
for p in checks actions selector editor utils; do git mv $M/$p $M/legacy/$p; done
for p in checks actions selector; do git mv $T/$p $T/legacy/$p; done
```

- [ ] **Step 2: Fix package lines of individually moved files**

```bash
sed -i 's/^package de\.zorro909\.codecheck\.checks;/package de.zorro909.codecheck.core.diagnostic;/' $M/core/diagnostic/ValidationError.java
sed -i 's/^package de\.zorro909\.codecheck\.validation;/package de.zorro909.codecheck.legacy.adapter;/' $M/legacy/adapter/*.java
sed -i 's/^package de\.zorro909\.codecheck;/package de.zorro909.codecheck.legacy;/' $M/legacy/FileLoader.java $M/legacy/ValidationCheckPipeline.java $T/legacy/FileLoaderTest.java $T/legacy/ValidationCheckPipelineTest.java
```

- [ ] **Step 3: Rewrite references (class-specific first, then prefixes)**

```bash
R() { grep -rl "$1" src --include="*.java" | xargs -r sed -i "s/$1/$2/g"; }
R 'de\.zorro909\.codecheck\.checks\.ValidationError\b' 'de.zorro909.codecheck.core.diagnostic.ValidationError'
R 'de\.zorro909\.codecheck\.validation\.CodeCheckRuleAdapter\b' 'de.zorro909.codecheck.legacy.adapter.CodeCheckRuleAdapter'
R 'de\.zorro909\.codecheck\.validation\.FixActionFixerAdapter\b' 'de.zorro909.codecheck.legacy.adapter.FixActionFixerAdapter'
R 'de\.zorro909\.codecheck\.FileLoader\b' 'de.zorro909.codecheck.legacy.FileLoader'
R 'de\.zorro909\.codecheck\.ValidationCheckPipeline\b' 'de.zorro909.codecheck.legacy.ValidationCheckPipeline'
for p in checks actions selector editor utils; do
  grep -rl "de\.zorro909\.codecheck\.$p[.;]" src --include="*.java" \
    | xargs -r sed -i "s/de\.zorro909\.codecheck\.$p\([.;]\)/de.zorro909.codecheck.legacy.$p\1/g"
done
```

- [ ] **Step 4: Compile-driven import repair**

Run: `./mvnw -q test-compile 2>&1 | grep -A2 "cannot find symbol\|does not exist"`
Expected missing symbols and their fixes (add the import to each failing file):
- `ValidationError` in `legacy/checks/CodeCheck.java` (was same-package) → `import de.zorro909.codecheck.core.diagnostic.ValidationError;`
- `Rule`, `RuleId`, `RuleMetadata`, `FileInterest`, `ValidationContext`, `Diagnostic`, or other validation types in `legacy/adapter/*.java` (were same-package) → `import de.zorro909.codecheck.validation.<Type>;` (old flat coordinate — still correct until Task 2)
- `FileLoader`/`ValidationCheckPipeline` referenced from root-package classes without import → `import de.zorro909.codecheck.legacy.<Type>;`

Repeat until `./mvnw -q test-compile` exits 0.

- [ ] **Step 5: Format, test**

Run: `./mvnw -q spring-javaformat:apply && ./mvnw test 2>&1 | grep "Tests run:" | tail -1`
Expected: `Tests run: 179, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "refactor: corral legacy generation under legacy package

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: Split `validation` into `core.validation` (+ `.rule`, `.fix`) and `core.diagnostic`

**Files:**
- Move (main), from `validation/`:
  - → `core/diagnostic/`: `Diagnostic.java`, `DiagnosticKind.java`, `SourcePosition.java`
  - → `core/validation/rule/`: `Rule.java`, `RuleId.java`, `RuleMetadata.java`, `RuleRegistry.java`, `DefaultRuleRegistry.java`, `FileInterest.java`, `WatchPlan.java`
  - → `core/validation/fix/`: `Fixer.java`, `FixerId.java`, `FixerMetadata.java`, `FixPlan.java`, `FixResult.java`, `FixInteraction.java`, `FixApplicationService.java`
  - → `core/validation/`: `ValidationEngine.java`, `DefaultValidationEngine.java`, `ValidationContext.java`, `ValidationMode.java`, `ValidationResult.java`, `FileValidationResult.java`
- Move (test), from `validation/`: `DefaultValidationEngineTest.java`, `ValidationResultSeverityStorageTest.java` → `core/validation/`; `NoMagicValuesRuleMetadataTest.java` → `core/validation/rule/`; `FixerRegistryAndApplicationTest.java` → `core/validation/fix/`
- Modify: every importer (sed-driven)

**Interfaces:**
- Consumes: `de.zorro909.codecheck.legacy.adapter.*`, `de.zorro909.codecheck.core.diagnostic.ValidationError` (Task 1)
- Produces: packages `de.zorro909.codecheck.core.validation`, `….core.validation.rule`, `….core.validation.fix`; completes `….core.diagnostic` (Diagnostic, DiagnosticKind, SourcePosition join ValidationError)

- [ ] **Step 1: Move files**

```bash
M=src/main/java/de/zorro909/codecheck; T=src/test/java/de/zorro909/codecheck
mkdir -p $M/core/validation/rule $M/core/validation/fix $T/core/validation/rule $T/core/validation/fix
git mv $M/validation/Diagnostic.java $M/validation/DiagnosticKind.java $M/validation/SourcePosition.java $M/core/diagnostic/
git mv $M/validation/Rule.java $M/validation/RuleId.java $M/validation/RuleMetadata.java $M/validation/RuleRegistry.java $M/validation/DefaultRuleRegistry.java $M/validation/FileInterest.java $M/validation/WatchPlan.java $M/core/validation/rule/
git mv $M/validation/Fixer.java $M/validation/FixerId.java $M/validation/FixerMetadata.java $M/validation/FixPlan.java $M/validation/FixResult.java $M/validation/FixInteraction.java $M/validation/FixApplicationService.java $M/core/validation/fix/
git mv $M/validation/ValidationEngine.java $M/validation/DefaultValidationEngine.java $M/validation/ValidationContext.java $M/validation/ValidationMode.java $M/validation/ValidationResult.java $M/validation/FileValidationResult.java $M/core/validation/
git mv $T/validation/DefaultValidationEngineTest.java $T/validation/ValidationResultSeverityStorageTest.java $T/core/validation/
git mv $T/validation/NoMagicValuesRuleMetadataTest.java $T/core/validation/rule/
git mv $T/validation/FixerRegistryAndApplicationTest.java $T/core/validation/fix/
```

- [ ] **Step 2: Fix package lines per target directory**

```bash
sed -i 's/^package de\.zorro909\.codecheck\.validation;/package de.zorro909.codecheck.core.diagnostic;/' $M/core/diagnostic/Diagnostic.java $M/core/diagnostic/DiagnosticKind.java $M/core/diagnostic/SourcePosition.java
sed -i 's/^package de\.zorro909\.codecheck\.validation;/package de.zorro909.codecheck.core.validation.rule;/' $M/core/validation/rule/*.java $T/core/validation/rule/*.java
sed -i 's/^package de\.zorro909\.codecheck\.validation;/package de.zorro909.codecheck.core.validation.fix;/' $M/core/validation/fix/*.java $T/core/validation/fix/*.java
```
(Engine files keep `package …validation;` — the prefix sed in Step 3 rewrites them to `core.validation`.)

- [ ] **Step 3: Rewrite references (longest names first within each family, then prefix)**

```bash
R() { grep -rl "$1" src --include="*.java" | xargs -r sed -i "s/$1/$2/g"; }
V='de\.zorro909\.codecheck\.validation'; N='de.zorro909.codecheck.core'
for c in Diagnostic DiagnosticKind SourcePosition; do R "$V\.$c\b" "$N.diagnostic.$c"; done
for c in RuleRegistry RuleMetadata RuleId DefaultRuleRegistry FileInterest WatchPlan Rule; do R "$V\.$c\b" "$N.validation.rule.$c"; done
for c in FixerMetadata FixerId Fixer FixApplicationService FixInteraction FixPlan FixResult; do R "$V\.$c\b" "$N.validation.fix.$c"; done
grep -rl "de\.zorro909\.codecheck\.validation[.;]" src --include="*.java" \
  | xargs -r sed -i 's/de\.zorro909\.codecheck\.validation\([.;]\)/de.zorro909.codecheck.core.validation\1/g'
```
Note: `DiagnosticKind` must be replaced before `Diagnostic` would otherwise eat its prefix — the loop above orders `Diagnostic` first BUT uses `\b`, so `Diagnostic\b` does not match `DiagnosticKind`. The `\b` anchors make ordering a belt-and-braces concern only for families where one name prefixes another without a word boundary (none here — `Rule`/`RuleId` etc. are all protected by `\b`).

- [ ] **Step 4: Compile-driven import repair**

Run: `./mvnw -q test-compile 2>&1 | grep -A2 "cannot find symbol\|does not exist"`
These 23 classes lived in one package; expect missing cross-references among the four new packages. Fix strictly by this table (symbol → import to add):
`Diagnostic`, `DiagnosticKind`, `SourcePosition` → `de.zorro909.codecheck.core.diagnostic.<Type>` · `Rule`, `RuleId`, `RuleMetadata`, `RuleRegistry`, `DefaultRuleRegistry`, `FileInterest`, `WatchPlan` → `de.zorro909.codecheck.core.validation.rule.<Type>` · `Fixer`, `FixerId`, `FixerMetadata`, `FixPlan`, `FixResult`, `FixInteraction`, `FixApplicationService` → `de.zorro909.codecheck.core.validation.fix.<Type>` · `ValidationEngine`, `DefaultValidationEngine`, `ValidationContext`, `ValidationMode`, `ValidationResult`, `FileValidationResult` → `de.zorro909.codecheck.core.validation.<Type>`
Also expected: `legacy/adapter/*.java` imports of old `…codecheck.validation.<Type>` were rewritten by Step 3 automatically. Repeat until `./mvnw -q test-compile` exits 0.

- [ ] **Step 5: Format, test**

Run: `./mvnw -q spring-javaformat:apply && ./mvnw test 2>&1 | grep "Tests run:" | tail -1`
Expected: `Tests run: 179, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "refactor: split validation package into core.validation, rule, fix, diagnostic

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: Split `changeset` into `core.changeset` and `infra.git`

**Files:**
- Move (main): `changeset/{GitChangeSetService,GitCommandRunner,GitCommandException}.java` → `infra/git/`; `changeset/{ChangeSet,ChangeSetEntry,ChangeSetService,GitFileStatus}.java` → `core/changeset/`
- Move (test): `changeset/{GitChangeSetServiceTest,GitChangeSetServiceBeanTest}.java` → `infra/git/`
- Modify: importers (sed-driven)

**Interfaces:**
- Produces: `de.zorro909.codecheck.core.changeset.{ChangeSet,ChangeSetEntry,ChangeSetService,GitFileStatus}`, `de.zorro909.codecheck.infra.git.{GitChangeSetService,GitCommandRunner,GitCommandException}`

- [ ] **Step 1: Move files**

```bash
M=src/main/java/de/zorro909/codecheck; T=src/test/java/de/zorro909/codecheck
mkdir -p $M/infra/git $M/core/changeset $T/infra/git
git mv $M/changeset/GitChangeSetService.java $M/changeset/GitCommandRunner.java $M/changeset/GitCommandException.java $M/infra/git/
git mv $M/changeset/ChangeSet.java $M/changeset/ChangeSetEntry.java $M/changeset/ChangeSetService.java $M/changeset/GitFileStatus.java $M/core/changeset/
git mv $T/changeset/GitChangeSetServiceTest.java $T/changeset/GitChangeSetServiceBeanTest.java $T/infra/git/
```

- [ ] **Step 2: Fix package lines of the infra files**

```bash
sed -i 's/^package de\.zorro909\.codecheck\.changeset;/package de.zorro909.codecheck.infra.git;/' $M/infra/git/*.java $T/infra/git/*.java
```

- [ ] **Step 3: Rewrite references (specific infra classes first, then prefix → core)**

```bash
R() { grep -rl "$1" src --include="*.java" | xargs -r sed -i "s/$1/$2/g"; }
for c in GitChangeSetService GitCommandRunner GitCommandException; do
  R "de\.zorro909\.codecheck\.changeset\.$c\b" "de.zorro909.codecheck.infra.git.$c"
done
grep -rl "de\.zorro909\.codecheck\.changeset[.;]" src --include="*.java" \
  | xargs -r sed -i 's/de\.zorro909\.codecheck\.changeset\([.;]\)/de.zorro909.codecheck.core.changeset\1/g'
```

- [ ] **Step 4: Compile-driven import repair**

Run: `./mvnw -q test-compile 2>&1 | grep -A2 "cannot find symbol\|does not exist"`
Expected: `infra/git/GitChangeSetService.java` (and its tests) referencing `ChangeSet`, `ChangeSetEntry`, `ChangeSetService`, `GitFileStatus` without import → add `import de.zorro909.codecheck.core.changeset.<Type>;`. Repeat until clean.

- [ ] **Step 5: Format, test**

Run: `./mvnw -q spring-javaformat:apply && ./mvnw test 2>&1 | grep "Tests run:" | tail -1`
Expected: `Tests run: 179, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "refactor: split changeset into core.changeset and infra.git

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: Split `coverage` into `core.coverage` and `infra.jacoco`

**Files:**
- Move (main): `coverage/{JacocoCoverageService,JacocoXmlParser,MapStructCoverageAttributor}.java` → `infra/jacoco/`; remaining 10 (`ClassCoverage`, `CoverageMetric`, `CoverageSnapshot`, `CoverageFreshness`, `CoverageRequest`, `CoverageService`, `CoverageDiagnosticService`, `CoverageThreshold`, `CoverageThresholdMatch`, `CoverageThresholdPolicy`) → `core/coverage/`
- Move (test): `coverage/JacocoCoverageServiceTest.java` → `infra/jacoco/`; `coverage/{CoverageDiagnosticServiceTest,CoverageThresholdPolicyTest}.java` → `core/coverage/`
- Modify: importers (sed-driven)

**Interfaces:**
- Consumes: `de.zorro909.codecheck.core.diagnostic.*` (Tasks 1–2)
- Produces: `de.zorro909.codecheck.core.coverage.*` (10 classes), `de.zorro909.codecheck.infra.jacoco.{JacocoCoverageService,JacocoXmlParser,MapStructCoverageAttributor}`

- [ ] **Step 1: Move files**

```bash
M=src/main/java/de/zorro909/codecheck; T=src/test/java/de/zorro909/codecheck
mkdir -p $M/infra/jacoco $M/core/coverage $T/infra/jacoco $T/core/coverage
git mv $M/coverage/JacocoCoverageService.java $M/coverage/JacocoXmlParser.java $M/coverage/MapStructCoverageAttributor.java $M/infra/jacoco/
git mv $M/coverage/*.java $M/core/coverage/
git mv $T/coverage/JacocoCoverageServiceTest.java $T/infra/jacoco/
git mv $T/coverage/CoverageDiagnosticServiceTest.java $T/coverage/CoverageThresholdPolicyTest.java $T/core/coverage/
```

- [ ] **Step 2: Fix package lines of the infra files**

```bash
sed -i 's/^package de\.zorro909\.codecheck\.coverage;/package de.zorro909.codecheck.infra.jacoco;/' $M/infra/jacoco/*.java $T/infra/jacoco/*.java
```

- [ ] **Step 3: Rewrite references (specific infra classes first, then prefix → core)**

```bash
R() { grep -rl "$1" src --include="*.java" | xargs -r sed -i "s/$1/$2/g"; }
for c in JacocoCoverageService JacocoXmlParser MapStructCoverageAttributor; do
  R "de\.zorro909\.codecheck\.coverage\.$c\b" "de.zorro909.codecheck.infra.jacoco.$c"
done
grep -rl "de\.zorro909\.codecheck\.coverage[.;]" src --include="*.java" \
  | xargs -r sed -i 's/de\.zorro909\.codecheck\.coverage\([.;]\)/de.zorro909.codecheck.core.coverage\1/g'
```

- [ ] **Step 4: Compile-driven import repair**

Run: `./mvnw -q test-compile 2>&1 | grep -A2 "cannot find symbol\|does not exist"`
Expected: `infra/jacoco/*.java` (and JacocoCoverageServiceTest) referencing the 10 core coverage types without import → add `import de.zorro909.codecheck.core.coverage.<Type>;`. Repeat until clean.

- [ ] **Step 5: Format, test**

Run: `./mvnw -q spring-javaformat:apply && ./mvnw test 2>&1 | grep "Tests run:" | tail -1`
Expected: `Tests run: 179, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "refactor: split coverage into core.coverage and infra.jacoco

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: Split `runner` into `core.testrun` and `infra.docker`

**Files:**
- Move (main): `runner/{TestRunner,TestRunRequest,TestRunResult}.java` → `core/testrun/`; `runner/{DockerMvndTestRunner,DockerCommandExecutor,ProcessDockerCommandExecutor,CommandResult}.java` → `infra/docker/`
- Move (test): `runner/{DockerMvndTestRunnerTest,DockerMvndTestRunnerBeanTest,ProcessDockerCommandExecutorTest}.java` → `infra/docker/`
- Modify: importers (sed-driven)

**Interfaces:**
- Produces: `de.zorro909.codecheck.core.testrun.{TestRunner,TestRunRequest,TestRunResult}`, `de.zorro909.codecheck.infra.docker.{DockerMvndTestRunner,DockerCommandExecutor,ProcessDockerCommandExecutor,CommandResult}`

- [ ] **Step 1: Move files**

```bash
M=src/main/java/de/zorro909/codecheck; T=src/test/java/de/zorro909/codecheck
mkdir -p $M/core/testrun $M/infra/docker $T/infra/docker
git mv $M/runner/TestRunner.java $M/runner/TestRunRequest.java $M/runner/TestRunResult.java $M/core/testrun/
git mv $M/runner/*.java $M/infra/docker/
git mv $T/runner/*.java $T/infra/docker/
```

- [ ] **Step 2: Fix package lines of the core files**

```bash
sed -i 's/^package de\.zorro909\.codecheck\.runner;/package de.zorro909.codecheck.core.testrun;/' $M/core/testrun/*.java
```

- [ ] **Step 3: Rewrite references (specific core classes first — longest first: `TestRunner\b` does not match `TestRunRequest` thanks to `\b` — then prefix → infra.docker)**

```bash
R() { grep -rl "$1" src --include="*.java" | xargs -r sed -i "s/$1/$2/g"; }
for c in TestRunRequest TestRunResult TestRunner; do
  R "de\.zorro909\.codecheck\.runner\.$c\b" "de.zorro909.codecheck.core.testrun.$c"
done
grep -rl "de\.zorro909\.codecheck\.runner[.;]" src --include="*.java" \
  | xargs -r sed -i 's/de\.zorro909\.codecheck\.runner\([.;]\)/de.zorro909.codecheck.infra.docker\1/g'
```

- [ ] **Step 4: Compile-driven import repair**

Run: `./mvnw -q test-compile 2>&1 | grep -A2 "cannot find symbol\|does not exist"`
Expected: `infra/docker/*.java` and its tests referencing `TestRunner`, `TestRunRequest`, `TestRunResult` without import → add `import de.zorro909.codecheck.core.testrun.<Type>;`. Repeat until clean.

- [ ] **Step 5: Format, test**

Run: `./mvnw -q spring-javaformat:apply && ./mvnw test 2>&1 | grep "Tests run:" | tail -1`
Expected: `Tests run: 179, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "refactor: split runner into core.testrun and infra.docker

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: Move `config`, `java`, `watcher`, `reporting`, and `RepositoryPathProvider` under `core`

**Files:**
- Move (main): `config/` → `core/config/`, `java/` → `core/project/`, `watcher/` → `core/watch/`, `reporting/` → `core/reporting/`, `RepositoryPathProvider.java` → `core/`
- Move (test): `config/` → `core/config/`, `java/` → `core/project/`, `watcher/` → `core/watch/`, `reporting/` → `core/reporting/`
- Modify: importers (sed-driven)

**Interfaces:**
- Produces: `de.zorro909.codecheck.core.{config,project,watch,reporting}.*`, `de.zorro909.codecheck.core.RepositoryPathProvider`

- [ ] **Step 1: Move files**

```bash
M=src/main/java/de/zorro909/codecheck; T=src/test/java/de/zorro909/codecheck
git mv $M/config $M/core/config
git mv $M/java $M/core/project
git mv $M/watcher $M/core/watch
git mv $M/reporting $M/core/reporting
git mv $M/RepositoryPathProvider.java $M/core/
git mv $T/config $T/core/config
git mv $T/java $T/core/project
git mv $T/watcher $T/core/watch
git mv $T/reporting $T/core/reporting
```

- [ ] **Step 2: Fix package line of RepositoryPathProvider**

```bash
sed -i 's/^package de\.zorro909\.codecheck;/package de.zorro909.codecheck.core;/' $M/core/RepositoryPathProvider.java
```

- [ ] **Step 3: Rewrite references (specific class first, then the four prefixes — note `java` → `project` and `watcher` → `watch` change the last segment)**

```bash
R() { grep -rl "$1" src --include="*.java" | xargs -r sed -i "s/$1/$2/g"; }
R 'de\.zorro909\.codecheck\.RepositoryPathProvider\b' 'de.zorro909.codecheck.core.RepositoryPathProvider'
grep -rl 'de\.zorro909\.codecheck\.config[.;]' src --include="*.java" | xargs -r sed -i 's/de\.zorro909\.codecheck\.config\([.;]\)/de.zorro909.codecheck.core.config\1/g'
grep -rl 'de\.zorro909\.codecheck\.java[.;]' src --include="*.java" | xargs -r sed -i 's/de\.zorro909\.codecheck\.java\([.;]\)/de.zorro909.codecheck.core.project\1/g'
grep -rl 'de\.zorro909\.codecheck\.watcher[.;]' src --include="*.java" | xargs -r sed -i 's/de\.zorro909\.codecheck\.watcher\([.;]\)/de.zorro909.codecheck.core.watch\1/g'
grep -rl 'de\.zorro909\.codecheck\.reporting[.;]' src --include="*.java" | xargs -r sed -i 's/de\.zorro909\.codecheck\.reporting\([.;]\)/de.zorro909.codecheck.core.reporting\1/g'
```

- [ ] **Step 4: Compile check (no splits here — expect zero repairs)**

Run: `./mvnw -q test-compile`
Expected: exit 0 with no errors. These are whole-package moves; imports were rewritten by Step 3. `RepositoryPathProvider` was in the root package whose other occupants are root-package classes that all import it — no same-package references existed (verified during design: all consumers are in other packages). If anything fails, fix per the produced coordinates above.

- [ ] **Step 5: Format, test**

Run: `./mvnw -q spring-javaformat:apply && ./mvnw test 2>&1 | grep "Tests run:" | tail -1`
Expected: `Tests run: 179, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "refactor: move config, project model, watch, reporting under core

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 7: Merge root command classes and `command` into `cli`; update pom mainClass and reflect-config

**Files:**
- Move (main): `GitCommitCodeCheckCommand.java`, `CliOptionCondition.java`, `RequiresCliOption.java`, `command/{CodeCheckCommandService,CommandOutcome,AssistantDaemonController,LocalAssistantDaemonController}.java` → `cli/`
- Move (test): `GitCommitCodeCheckCommandTest.java`, `command/CodeCheckCommandServiceTest.java` → `cli/`
- Modify: `pom.xml:21` (`exec.mainClass`), `src/main/resources/native-image/reflect-config.json`, importers (sed-driven)

**Interfaces:**
- Produces: `de.zorro909.codecheck.cli.*` (7 classes). The Maven manifest main class becomes `de.zorro909.codecheck.cli.GitCommitCodeCheckCommand`.

- [ ] **Step 1: Move files**

```bash
M=src/main/java/de/zorro909/codecheck; T=src/test/java/de/zorro909/codecheck
mkdir -p $M/cli $T/cli
git mv $M/GitCommitCodeCheckCommand.java $M/CliOptionCondition.java $M/RequiresCliOption.java $M/cli/
git mv $M/command/*.java $M/cli/
git mv $T/GitCommitCodeCheckCommandTest.java $T/cli/
git mv $T/command/*.java $T/cli/
```

- [ ] **Step 2: Fix package lines**

```bash
sed -i 's/^package de\.zorro909\.codecheck;/package de.zorro909.codecheck.cli;/' $M/cli/GitCommitCodeCheckCommand.java $M/cli/CliOptionCondition.java $M/cli/RequiresCliOption.java $T/cli/GitCommitCodeCheckCommandTest.java
```
(The `command`-package files are handled by the prefix sed in Step 3.)

- [ ] **Step 3: Rewrite references**

```bash
R() { grep -rl "$1" src --include="*.java" | xargs -r sed -i "s/$1/$2/g"; }
R 'de\.zorro909\.codecheck\.GitCommitCodeCheckCommand\b' 'de.zorro909.codecheck.cli.GitCommitCodeCheckCommand'
R 'de\.zorro909\.codecheck\.CliOptionCondition\b' 'de.zorro909.codecheck.cli.CliOptionCondition'
R 'de\.zorro909\.codecheck\.RequiresCliOption\b' 'de.zorro909.codecheck.cli.RequiresCliOption'
grep -rl 'de\.zorro909\.codecheck\.command[.;]' src --include="*.java" \
  | xargs -r sed -i 's/de\.zorro909\.codecheck\.command\([.;]\)/de.zorro909.codecheck.cli\1/g'
```

- [ ] **Step 4: Update pom.xml and reflect-config.json**

In `pom.xml`, change line 21:
```xml
<exec.mainClass>de.zorro909.codecheck.cli.GitCommitCodeCheckCommand</exec.mainClass>
```
Then rewrite the native-image reflection entries (handles the Micronaut `$…$Definition` variants):
```bash
sed -i 's/de\.zorro909\.codecheck\.\(\$*GitCommitCodeCheckCommand\)/de.zorro909.codecheck.cli.\1/g' src/main/resources/native-image/reflect-config.json
```
Known limitation (accepted in the spec): reflect-config.json contains stale pre-restructure entries (`$GitDiffLoader$Definition`, `de.zorro909.codecheck.java.doc.$JavaDocCheck$Definition`, `de.zorro909.codecheck.java.test.$TestClassCheck$Definition`) that reference classes which no longer exist at those coordinates — they predate this refactor, GraalVM tolerates missing entries (CI native builds already pass with them), and `updateTrace.sh` is the canonical regeneration path. Leave them untouched.

- [ ] **Step 5: Compile check**

Run: `./mvnw -q test-compile`
Expected: exit 0. Root classes and `command` classes merge into ONE package (`cli`) — merges cannot break same-package references; cross-package importers were rewritten in Step 3.

- [ ] **Step 6: Format, test, and jar smoke test**

Run: `./mvnw -q spring-javaformat:apply && ./mvnw test 2>&1 | grep "Tests run:" | tail -1`
Expected: `Tests run: 179, Failures: 0, Errors: 0, Skipped: 0`
Run: `./mvnw -q package -DskipTests && java -jar target/git-commit-code-check-0.1.jar --help; echo "exit: $?"`
Expected: picocli usage text for `git-commit-code-check` and `exit: 0` — proves the manifest main class is correct after the move.

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "refactor: merge command entry points into cli package

Updates exec.mainClass and native-image reflection entries.

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 8: Final verification of spec invariants

**Files:**
- No planned changes; fixups only if a check fails.

**Interfaces:**
- Consumes: everything above.

- [ ] **Step 1: No stale flat coordinates anywhere in src (sources AND resources)**

```bash
grep -rn 'de\.zorro909\.codecheck\.\(checks\|actions\|selector\|editor\|utils\|command\|config\|changeset\|coverage\|java\|reporting\|runner\|validation\|watcher\)[.;$]' src && echo "STALE REFS FOUND" || echo "CLEAN"
```
Expected: `CLEAN`, except the two accepted stale reflect-config entries under `de.zorro909.codecheck.java.*` (see Task 7 Step 4) — if those are the only hits, the check passes. (`daemon` keeps its coordinate by design and is excluded.)

- [ ] **Step 2: Legacy-isolation invariant**

```bash
grep -rln 'de\.zorro909\.codecheck\.legacy' src/main/java/de/zorro909/codecheck/cli src/main/java/de/zorro909/codecheck/core src/main/java/de/zorro909/codecheck/daemon src/main/java/de/zorro909/codecheck/infra
```
Expected output, exactly these four files (the spec's allowed composition points):
```
src/main/java/de/zorro909/codecheck/cli/CodeCheckCommandService.java
src/main/java/de/zorro909/codecheck/core/validation/rule/DefaultRuleRegistry.java
src/main/java/de/zorro909/codecheck/core/validation/fix/FixApplicationService.java
src/main/java/de/zorro909/codecheck/daemon/DaemonServer.java
```

- [ ] **Step 3: Empty directory sweep (requires user permission)**

```bash
find src -type d -empty
```
Expected: possibly a few empty old package directories. Per the user's global deletion rules, ASK THE USER before removing them; if approved run `find src -type d -empty -delete`, otherwise leave them (git does not track directories).

- [ ] **Step 4: Full build with CI profile (format enforcement + tests)**

Run: `CI=true ./mvnw clean verify 2>&1 | tail -5`
Expected: `BUILD SUCCESS`, with the spring-javaformat validate goal having run in the validate phase.

- [ ] **Step 5: Commit fixups if any**

Only if Steps 1–4 required changes:
```bash
git add -A && git commit -m "refactor: restructure verification fixups

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```
