# CR-011 Implementation Specification: Migration Roadmap

## Scope

This increment records the migration path from the original CLI/check pipeline toward the
daemon-first architecture established by CR-001 through CR-010. It does not introduce runtime code;
it documents phase ordering, branch traceability, completed capabilities, and deferred follow-up
work.

## Deliverable

`docs/architecture/migration-roadmap.md` is the canonical roadmap for this migration. It maps each
phase from the change request to:

- the implementing CR branch
- the primary boundary introduced
- the validation performed
- residual work that remains intentionally deferred

## Traceability

The roadmap links the conceptual phases to the stacked implementation order:

1. Product modes and command model
2. Configuration model
3. Daemon lifecycle and transport
4. Git change set policy
5. Validation engine, rules, and fixers
6. Java project model and parsing
7. Watcher and incremental state
8. Docker mvnd test runner
9. JaCoCo coverage and MapStruct attribution
10. Severity reporting and pre-commit behavior

## Verification

This is a documentation-only increment. The code stack was already verified on CR-010 with the full
test suite after the final runtime changes.
