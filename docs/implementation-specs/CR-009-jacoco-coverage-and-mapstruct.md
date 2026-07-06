# CR-009 Implementation Specification: JaCoCo Coverage and MapStruct

## Scope

This increment introduces JaCoCo XML consumption, report freshness checks, test-runner refresh, and
MapStruct mapper attribution. Coverage diagnostics are produced as structured validation
diagnostics so daemon/CLI renderers can display them through the existing validation result path.

## Coverage Service

`JacocoCoverageService` implements:

- `currentCoverage(CoverageRequest)`
- `freshness(CoverageRequest)`
- `refreshCoverage(CoverageRequest)`

Fresh reports are parsed directly. Missing, stale, or unknown reports trigger `TestRunner` through
`refreshCoverage`.

## Freshness

Freshness compares the newest matching JaCoCo XML report against source files, test files, module
POMs, and generated context files in the request. Missing reports are `ABSENT`; older reports are
`STALE`.

## JaCoCo XML

`JacocoXmlParser` consumes XML class counters and stores line/branch coverage ratios by VM-style
class name, for example:

```text
com/example/UserService
```

## Thresholds And Diagnostics

`CoverageThresholdPolicy` supports threshold matching by:

- annotation
- class name
- package pattern
- source glob

`CoverageDiagnosticService` emits `COVERAGE_FAILURE` diagnostics for classes below threshold.

## MapStruct Attribution

`MapStructCoverageAttributor` detects mapper interfaces by `@Mapper`, finds the generated
`<MapperName>Impl` class, and attributes implementation coverage back to the mapper interface.

Generated implementation files remain context; diagnostics are reported on mapper interfaces.

## Tests

- Fresh JaCoCo XML is consumed without rerunning tests.
- Stale JaCoCo XML triggers the Docker/mvnd test runner.
- MapStruct mapper diagnostics use generated implementation coverage and report against the mapper
  interface.
- Threshold matching supports annotation and glob.
