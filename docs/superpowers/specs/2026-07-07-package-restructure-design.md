# Package Restructure Design

Date: 2026-07-07
Status: approved

## Goal

Replace the flat 15-package layout under `de.zorro909.codecheck` with a five-group structure
(`cli`, `daemon`, `core`, `infra`, `legacy`) that separates contracts from technology-specific
implementations and corrals the pre-CR-005 pipeline under `legacy`. Pure move/rename refactoring:
no class renames, no behavior changes, no wiring changes.

## Target Layout And Class Mapping

All packages are relative to `de.zorro909.codecheck`.

### cli

| From | Classes |
| --- | --- |
| (root) | GitCommitCodeCheckCommand, CliOptionCondition, RequiresCliOption |
| command | CodeCheckCommandService, CommandOutcome, AssistantDaemonController, LocalAssistantDaemonController |

### daemon

| From | Classes |
| --- | --- |
| daemon | DaemonServer, DaemonMetadata, DaemonMetadataStore, DaemonProcessRegistry, FileWatcher |

### core

| To | From | Classes |
| --- | --- | --- |
| core | (root) | RepositoryPathProvider (shared by config, daemon, changeset, runner, project model, watcher) |
| core.config | config | all 6 classes, unchanged |
| core.diagnostic | validation | Diagnostic, DiagnosticKind, SourcePosition |
| core.diagnostic | checks | ValidationError (its nested Severity enum is the severity type used by Diagnostic, reporting, coverage, and the parser service; keeping it in legacy would force core to import legacy) |
| core.changeset | changeset | ChangeSet, ChangeSetEntry, ChangeSetService, GitFileStatus |
| core.validation | validation | ValidationEngine, DefaultValidationEngine, ValidationContext, ValidationMode, ValidationResult, FileValidationResult |
| core.validation.rule | validation | Rule, RuleId, RuleMetadata, RuleRegistry, DefaultRuleRegistry, FileInterest, WatchPlan |
| core.validation.fix | validation | Fixer, FixerId, FixerMetadata, FixPlan, FixResult, FixInteraction, FixApplicationService |
| core.project | java | JavaParserService, DefaultJavaParserService, ParseOutcome, ProjectModel, ProjectModelService, MavenModule, ModuleId, MavenProjectModelService |
| core.coverage | coverage | ClassCoverage, CoverageMetric, CoverageSnapshot, CoverageFreshness, CoverageRequest, CoverageService, CoverageDiagnosticService, CoverageThreshold, CoverageThresholdMatch, CoverageThresholdPolicy |
| core.testrun | runner | TestRunner, TestRunRequest, TestRunResult |
| core.watch | watcher | all 10 classes |
| core.reporting | reporting | ModeSeverityPolicy, TerminalDiagnosticRenderer |

### infra

| To | From | Classes |
| --- | --- | --- |
| infra.git | changeset | GitChangeSetService, GitCommandRunner, GitCommandException |
| infra.docker | runner | DockerMvndTestRunner, DockerCommandExecutor, ProcessDockerCommandExecutor, CommandResult |
| infra.jacoco | coverage | JacocoCoverageService, JacocoXmlParser, MapStructCoverageAttributor |

### legacy

| To | From | Classes |
| --- | --- | --- |
| legacy | (root) | ValidationCheckPipeline, FileLoader |
| legacy.checks.** | checks.** | subtree moves intact (CodeCheck, java checks) — except ValidationError, which moves to core.diagnostic |
| legacy.actions.** | actions.** | subtree moves intact |
| legacy.selector.** | selector.** | subtree moves intact |
| legacy.editor.** | editor.** | subtree moves intact (used only by legacy fix flow) |
| legacy.utils | utils | CompilationUnitExtensions, MethodDeclarationExtensions |
| legacy.adapter | validation | CodeCheckRuleAdapter, FixActionFixerAdapter |

Design invariant: after the move, no class under `core`, `cli`, `daemon`, or `infra` imports
anything from `legacy` except the four composition points that already bridge the two
generations (verified against current imports):

- `cli.CodeCheckCommandService` (legacy pipeline, FileSelector)
- `daemon.DaemonServer` (legacy pipeline, FileSelector)
- `core.validation.rule.DefaultRuleRegistry` (registers legacy checks/fix actions via the adapters)
- `core.validation.fix.FixApplicationService` (runs legacy PostAction after fixes)

## Rationale

- The 25-class `validation` package held four concepts: engine, rule model, fixer model, and
  diagnostic model. Diagnostics are consumed by coverage, reporting, project, and watch, so they
  are a shared kernel and move to `core.diagnostic`.
- Contracts stay in `core`; technology-specific implementations (Git, Docker, JaCoCo) move to
  `infra`, matching the boundary-first approach in the CR-011 migration roadmap.
- Everything reachable only from the old pipeline lands under `legacy`, making its future removal
  a package deletion.
- `java` as a package name was both awkward and uninformative; its contents are the Maven/parser
  project model, hence `core.project`.

## Mechanics

1. `git mv` per the mapping table (history-preserving), main and test trees alike; test classes
   mirror their subjects' new packages.
2. Rewrite `package` and `import` statements across `src/main/java` and `src/test/java`.
3. Rewrite `de.zorro909.codecheck.*` FQCNs in `src/main/resources/native-image/reflect-config.json`
   with the same mapping. `updateTrace.sh` remains the canonical regeneration path; the textual
   rewrite is exact for a pure rename.
4. `-Amicronaut.processing.group=de.zorro909.codecheck` in `pom.xml` is prefix-based and unaffected,
   but `exec.mainClass` names the command class and must become
   `de.zorro909.codecheck.cli.GitCommitCodeCheckCommand`; a jar `--help` smoke test proves the
   manifest main class survives the move.

## Verification

- Full test suite passes (179 tests at time of writing).
- `./mvnw spring-javaformat:validate` passes.
- `grep` proves no references to the old flat package coordinates remain in `src/` (sources and
  resources): `checks`, `actions`, `selector`, `editor`, `utils`, `command`, `config`,
  `changeset`, `coverage`, `java`, `reporting`, `runner`, `validation`, `watcher` directly under
  `de.zorro909.codecheck`. `daemon` is the one package that keeps its coordinate and is excluded
  from this check. Accepted exception: two pre-existing stale entries in
  `native-image/reflect-config.json` (`java.doc.$JavaDocCheck$Definition`,
  `java.test.$TestClassCheck$Definition`) reference classes that did not exist at those
  coordinates even before this restructure; they are left untouched per Mechanics item 3 and
  disappear on the next `updateTrace.sh` regeneration.

## Out Of Scope

- Class renames or behavior changes.
- Wiring the CR-007/CR-008/CR-009 scaffolding into execution (stays on the roadmap follow-up list).
- Deleting legacy code.
- Running `updateTrace.sh` (requires GraalVM tracing agent and a live run).
