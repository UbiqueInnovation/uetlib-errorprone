# CHANGELOG

## 0.1.2

- Use shared Maven build and publish workflows at `v1.0.0` to enforce formatting checks in CI.
- Apply Spotless formatting to Error Prone sources and tests.

## 0.1.1

- Clarify configuration-property migration guidance for consumers.

## 0.1.0

- Move Java packages and class references from `ch.ubique.uetlib` to `ch.uetlib`.

### Breaking changes

- Move Java packages from `ch.ubique.backend` to `ch.uetlib`, including class-name resources. The combined OpenRewrite
  migration also renames known Spring configuration keys to `ch.uetlib.*`.
- Remove the inherited Ubique Artifactory repository and deployment destination; private consumers must configure their
  own registry.
- Add Apache-2.0 licensing for original code, preserve third-party licenses, and add public project metadata and
  contribution/security guidance.

- Move Maven coordinates from `ch.ubique:uetlib-*` to `ch.uetlib:uetlib-*`.
- Align uetlib parent, BOM and library references with the migration releases.

### Migration

After the migration releases and `uetlib-rewrite` are published, run this from the consuming project, then review the
diff and run its tests:

```sh
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.46.1:run \
  -Drewrite.recipeArtifactCoordinates=ch.uetlib:uetlib-rewrite:RELEASE \
  -Drewrite.activeRecipes=ch.uetlib.MigrateToPublicUetlib
```

The recipe updates Maven coordinates/versions, Java references and known class-name resources. Review application-owned
packages under the old namespace, Kotlin, reflection patterns, native-access module flags and persisted class names
separately. Use `:dryRun` instead of `:run` to preview a patch.

## 0.1.0

- New module: compile-time checks for uetlib conventions, as Error Prone `BugChecker`s
- `OpenEnumStructure`: an `OpenEnumValue` interface must be sealed over exactly two subtypes, an
  `enum Known` and a `record Unknown` (or final class) implementing `UnknownOpenEnumValue`, and
  must declare a `static OpenEnumSet` field
- `OpenEnumNestedTypeImport`: the nested `Known`/`Unknown` types must not be imported directly;
  importing their constants (`Color.Known.*`) stays allowed
