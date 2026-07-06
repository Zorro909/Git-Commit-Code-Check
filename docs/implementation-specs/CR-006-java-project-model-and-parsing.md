# CR-006 Implementation Specification: Java Project Model and Parsing

## Scope

This increment introduces a Maven-aware project model and moves JavaParser ownership into a
dedicated parser service. Existing Java rules continue to extend `JavaChecker`, but `JavaChecker`
now delegates parsing, parser caches, and symbol diagnostics to `JavaParserService`.

## Project Model

`MavenProjectModelService` builds a model from the repository root:

- root Maven project
- direct Maven reactor modules from `<modules><module>...`
- module main and test source roots
- Maven default generated source and generated test source roots
- detected language level from configuration

Generated source roots are context roots only. They are not included in validation source roots.

## Parser Service

`DefaultJavaParserService` owns:

- parse cache
- parser cache
- module-aware type solver setup

It uses concurrent maps, does not lowercase paths, and supports daemon-safe invalidation by file
or module.

## Diagnostics

- Parse failures produce `HIGH` `PARSE_ERROR` diagnostics.
- Unresolved class/interface types produce `MEDIUM` `SYMBOL_WARNING` diagnostics.
- Parser diagnostics are surfaced through `ParseOutcome` and included in legacy `JavaChecker`
  validation output.

## Tests

- Multi-module Maven roots are modeled.
- Generated roots are context-only.
- Module source files parse with module source roots.
- Module test files resolve main source types.
- Generated implementation files can be parsed as context.
- Parse errors become HIGH diagnostics.
- Unresolved symbols become MEDIUM diagnostics.
