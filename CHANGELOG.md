# CHANGELOG

## 1.0.0 (unreleased)

### Breaking changes

- Move Java packages from `ch.ubique.backend` to `ch.ubique.uetlib`, including class-name resources. Existing Spring configuration property keys are unchanged.
- Remove the inherited Ubique Artifactory repository and deployment destination; private consumers must configure their own registry.
- Add Apache-2.0 licensing for original code, preserve third-party licenses, and add public project metadata and contribution/security guidance.

- Move Maven coordinates from `ch.ubique:uetlib-*` to `ch.uetlib:uetlib-*`.
- Align uetlib parent, BOM and library references with the migration releases.

### Migration

After the migration releases and `uetlib-rewrite:0.1.0` are published, run this from the consuming project, then review the diff and run its tests:

```sh
mvn org.openrewrite.maven:rewrite-maven-plugin:6.46.1:run \
  -Drewrite.recipeArtifactCoordinates=ch.uetlib:uetlib-rewrite:0.1.0 \
  -Drewrite.activeRecipes=ch.ubique.uetlib.MigrateToPublicUetlib
```

The recipe updates Maven coordinates/versions, Java references and known class-name resources. Review application-owned packages under the old namespace, Kotlin, reflection patterns, native-access module flags and persisted class names separately. Use `:dryRun` instead of `:run` to preview a patch.


## 0.1.0

- New module: compile-time checks for uetlib conventions, as Error Prone `BugChecker`s
- `OpenEnumStructure`: an `OpenEnumValue` interface must be sealed over exactly two subtypes, an
  `enum Known` and a `record Unknown` (or final class) implementing `UnknownOpenEnumValue`, and
  must declare a `static OpenEnumSet` field
- `OpenEnumNestedTypeImport`: the nested `Known`/`Unknown` types must not be imported directly;
  importing their constants (`Color.Known.*`) stays allowed
