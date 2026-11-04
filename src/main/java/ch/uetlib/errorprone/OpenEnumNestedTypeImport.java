package ch.uetlib.errorprone;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ImportTree;

/**
 * Forbids importing an open enum's nested {@code Known} or {@code Unknown} type directly.
 *
 * <p>Every open enum names them the same, so a bare {@code Known.RED} or {@code case Unknown _}
 * says nothing about which domain it belongs to — and in a file touching two open enums the import
 * cannot even be written twice. Qualifying through the owner keeps it legible:
 *
 * <pre>{@code
 * import static com.example.Color.Known;        // rejected: bare `Known.RED` at the use site
 * import com.example.Color;                     // then `Color.Known.RED`
 *
 * import static com.example.Color.Known.*;      // fine: imports the constants, so `RED`
 * import static com.example.Color.Known.RED;    // fine
 * }</pre>
 *
 * <p>Importing the constants is a different thing: {@code RED} is unambiguous in a way that {@code
 * Known} is not, and a clash between two domains' constants is a compile error rather than a silent
 * loss of context.
 */
@BugPattern(
        summary =
                "Do not import an open enum's nested Known/Unknown type; qualify it through the"
                        + " owning type instead (Color.Known.RED). Importing the constants"
                        + " themselves (Color.Known.*) is fine.",
        severity = ERROR,
        linkType = BugPattern.LinkType.NONE)
public final class OpenEnumNestedTypeImport extends BugChecker
        implements BugChecker.ImportTreeMatcher {

    private static final String OPEN_ENUM_VALUE = "ch.uetlib.core.openenum.OpenEnumValue";

    private static final String KNOWN = "Known";
    private static final String UNKNOWN = "Unknown";

    @Override
    public Description matchImport(final ImportTree tree, final VisitorState state) {
        final var imported = tree.getQualifiedIdentifier().toString();
        final var lastDot = imported.lastIndexOf('.');

        if (lastDot < 0) {
            return Description.NO_MATCH;
        }

        final var lastSegment = imported.substring(lastDot + 1);

        // `...Known.*` and `...Known.RED` end in the member, not in the nested type's name, so
        // only a direct type import matches here.
        if (!lastSegment.equals(KNOWN) && !lastSegment.equals(UNKNOWN)) {
            return Description.NO_MATCH;
        }

        final var owner = imported.substring(0, lastDot);
        final var ownerSimpleName = owner.substring(owner.lastIndexOf('.') + 1);

        if (!ownsAnOpenEnum(owner, state)) {
            return Description.NO_MATCH;
        }

        return buildDescription(tree)
                .setMessage(
                        ("Import %s and write %s.%s instead of importing %s directly; a bare `%s`"
                                        + " does not say which open enum it belongs to. Importing"
                                        + " the constants (%s.%s.*) is fine.")
                                .formatted(
                                        owner,
                                        ownerSimpleName,
                                        lastSegment,
                                        lastSegment,
                                        lastSegment,
                                        ownerSimpleName,
                                        lastSegment))
                .build();
    }

    /**
     * Only fires when the enclosing type really is an open enum, so unrelated classes that happen
     * to nest something called {@code Known} or {@code Unknown} are left alone.
     *
     * <p>Resolves the owner rather than the nested type itself: a nested type's binary name is
     * {@code Owner$Nested}, so looking up the dotted form from an import would not resolve.
     */
    private static boolean ownsAnOpenEnum(final String owner, final VisitorState state) {
        final var ownerType = state.getTypeFromString(owner);
        final var openEnumValue = state.getTypeFromString(OPEN_ENUM_VALUE);

        // Compare erasures: OpenEnumValue is generic, and the owner implements a parameterised
        // form of it (OpenEnumValue<String>), which is not a subtype of the raw type as resolved.
        return ownerType != null
                && openEnumValue != null
                && state.getTypes()
                        .isSubtype(
                                state.getTypes().erasure(ownerType),
                                state.getTypes().erasure(openEnumValue));
    }
}
