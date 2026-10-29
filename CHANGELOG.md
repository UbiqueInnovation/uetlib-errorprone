# CHANGELOG

## 1.0.0 (unreleased)

### Breaking changes

- Move Maven coordinates from `ch.ubique:uetlib-*` to `ch.ubique.uetlib:uetlib-*`.
- Align uetlib parent, BOM and library references with the migration releases.

## 0.1.0

- New module: compile-time checks for uetlib conventions, as Error Prone `BugChecker`s
- `OpenEnumStructure`: an `OpenEnumValue` interface must be sealed over exactly two subtypes, an
  `enum Known` and a `record Unknown` (or final class) implementing `UnknownOpenEnumValue`, and
  must declare a `static OpenEnumSet` field
- `OpenEnumNestedTypeImport`: the nested `Known`/`Unknown` types must not be imported directly;
  importing their constants (`Color.Known.*`) stays allowed
