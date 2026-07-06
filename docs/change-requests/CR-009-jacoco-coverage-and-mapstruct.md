# CR-009: JaCoCo Coverage and MapStruct

## Status

Proposed.

## Context

Actual coverage should replace heuristic checks such as "public method is called by a test." JaCoCo is the target coverage provider. Coverage requirements can vary by class type, package, annotation, and glob pattern.

MapStruct mappers need first-class handling: coverage should be calculated from mapper interfaces and generated implementations.

## Goals

- Consume fresh existing JaCoCo reports when possible.
- Run tests through the Docker mvnd runner when reports are stale or absent.
- Support class/package/glob/annotation-based thresholds.
- Support first-class MapStruct coverage attribution.
- Use generated implementations as context, not validation targets.

## Coverage Service

```java
interface CoverageService {
    CoverageSnapshot currentCoverage(CoverageRequest request);
    CoverageFreshness freshness(CoverageRequest request);
    CoverageSnapshot refreshCoverage(CoverageRequest request);
}
```

Prefer JaCoCo XML reports over `.exec` because XML is easier to inspect and map.

## Freshness

Freshness can be estimated by comparing report timestamps to:

- relevant changed source files
- relevant changed test files
- module `pom.xml`
- root `pom.xml`
- generated source context where needed
- compiled class files if available

If freshness is unknown, rerun tests.

Supported modes:

```yaml
coverage:
  freshnessMode: reuse-if-fresh
```

Future modes:

- `always-run`
- `never-run`
- `reuse-if-present`

## Test Rerun Policy

- Passing tests use a higher debounce between reruns.
- Failing tests can be retried more often.
- Targeted tests should be used where possible.
- Full module or full reactor runs are fallback behavior.

## Threshold Policy

Thresholds may be selected by:

- class type
- package
- annotation
- glob pattern

MapStruct mappers should default to near-100% branch coverage.

Example future config shape:

```yaml
coverage:
  thresholds:
    - match:
        annotation: "org.mapstruct.Mapper"
      branch: 0.99
      line: 0.99
    - match:
        package: "com.example.service..*"
      branch: 0.80
      line: 0.90
```

## MapStruct Handling

MapStruct support should be first-class:

1. Detect mapper interfaces by `@Mapper`.
2. Locate generated implementation, usually `*Impl`.
3. Treat generated implementation as context.
4. Attribute implementation coverage to the mapper concept/interface.
5. Report diagnostics at the mapper interface source file.

Coverage for MapStruct mappers must be calculated from both:

- source interface
- generated implementation

## Requirements

- Coverage checks use JaCoCo.
- Existing reports can be reused only when fresh enough.
- Stale or absent reports trigger Docker mvnd test execution.
- MapStruct mappers are a built-in coverage policy.
- Generated implementations are not directly validated.
- Coverage diagnostics should integrate with normal validation results.

## Acceptance Criteria

- Fresh JaCoCo XML is consumed without rerunning tests.
- Stale JaCoCo XML causes targeted or full test execution.
- Mapper interface diagnostics reflect generated implementation coverage.
- Coverage thresholds can be selected by at least annotation and glob in the design.
- Coverage failures appear in normal daemon/CLI diagnostics.

## Open Questions

- What exact default threshold should MapStruct use: 99%, 100%, or configurable only?
- Should coverage diagnostics be HIGH or MEDIUM by default?
- How should partial targeted-test coverage be distinguished from full coverage?
- Should branch coverage be mandatory for all classes or only selected policies?

