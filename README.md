# Uetlib Error Prone Checks

Compile-time checks for uetlib conventions, delivered as Error Prone `BugChecker`s.

Note these are **javac plugins, not annotation processors** — they merely travel on
`annotationProcessorPath`, because that is the classpath Maven exposes to javac plugins.

## Checks

| Check                      | Enforces                                                                                                                                                                                         |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `OpenEnumStructure`        | An `OpenEnumValue` interface is sealed over exactly two subtypes: an `enum Known` and a `record Unknown` (or final class) implementing `UnknownOpenEnumValue`, plus a `static OpenEnumSet` field |
| `OpenEnumNestedTypeImport` | The nested `Known`/`Unknown` types are not imported directly (a bare `Known.RED` hides which open enum it belongs to). Importing the constants — `Color.Known.*` — is fine                       |

Open enums are resolved reflectively by integrations (Jackson, OpenAPI), so a malformed one
otherwise fails at runtime on the first read of that type — late, and easy to miss.

## Design notes

- **Standalone pom, no `uetlib-parent`.** The parent puts this artifact on every project's
  `annotationProcessorPath`, so inheriting it would make this module's compilation depend on a
  released copy of itself.
- **No dependency on `uetlib-core`.** Checks match types by fully-qualified name, so there is no
  dependency edge back into the code they inspect — which would be circular.
- `source`/`target` rather than `release`, since the `add-exports` flags needed to compile against
  javac internals are rejected alongside `release`.

## Wiring

In `uetlib-parent`'s `maven-compiler-plugin` configuration:

```xml
<annotationProcessorPaths>
    <path>
        <groupId>ch.ubique</groupId>
        <artifactId>uetlib-errorprone</artifactId>
    </path>
</annotationProcessorPaths>
```

and, because `-XepDisableAllChecks` is set, enable them explicitly:

```
-Xep:OpenEnumStructure:ERROR
-Xep:OpenEnumNestedTypeImport:ERROR
```

Loading checks from the processor path additionally requires
`--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED`, which `uetlib-parent`
already passes.

Suppress a check where a deviation is deliberate with `@SuppressWarnings("OpenEnumStructure")`.

## Changelog

See [CHANGELOG.md](CHANGELOG.md)
