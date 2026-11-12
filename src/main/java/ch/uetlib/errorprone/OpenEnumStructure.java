package ch.uetlib.errorprone;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import org.jspecify.annotations.Nullable;

/**
 * Checks that a domain {@code OpenEnumValue} interface has the shape the pattern depends on: sealed
 * over exactly two subtypes — an {@code enum Known} carrying the known constants and a {@code
 * record Unknown} (or final class) carrying an unrecognized raw value — plus a {@code static
 * OpenEnumSet} field.
 *
 * <p>Integrations resolve an open enum by walking this shape reflectively, so a malformed one fails
 * at runtime on the first read of that particular type. That is late and easy to miss: a type is
 * only exercised once some test or request happens to touch it. This check moves it to compile
 * time.
 *
 * <p>The names are fixed rather than merely conventional because they are read at call sites all
 * over consuming code — {@code Color.Known.RED}, {@code case Color.Unknown _} — so consistency is
 * what makes those readable across domains.
 *
 * <p>Types are matched by fully-qualified name so that this module needs no dependency on
 * uetlib-core — which would otherwise be circular, since uetlib-core compiles against this check.
 */
@BugPattern(
        summary =
                "An OpenEnumValue interface must be sealed over exactly two subtypes, an"
                        + " `enum Known` holding the known constants and a `record Unknown`"
                        + " carrying an unrecognized value, and must declare a static OpenEnumSet"
                        + " field",
        severity = ERROR,
        linkType = BugPattern.LinkType.NONE)
public final class OpenEnumStructure extends BugChecker implements BugChecker.ClassTreeMatcher {

    private static final String PACKAGE = "ch.uetlib.core.openenum.";
    private static final String OPEN_ENUM_VALUE = PACKAGE + "OpenEnumValue";
    private static final String UNKNOWN_OPEN_ENUM_VALUE = PACKAGE + "UnknownOpenEnumValue";
    private static final String OPEN_ENUM_SET = PACKAGE + "OpenEnumSet";

    private static final String KNOWN = "Known";
    private static final String UNKNOWN = "Unknown";

    @Override
    public Description matchClass(final ClassTree tree, final VisitorState state) {
        final var symbol = ASTHelpers.getSymbol(tree);

        if (symbol == null || !symbol.isInterface() || !isDomainOpenEnum(symbol, state)) {
            return Description.NO_MATCH;
        }

        if ((symbol.flags() & Flags.SEALED) == 0) {
            return report(
                    tree,
                    "must be sealed, so the known constants and the unknown carrier are the only"
                            + " two implementations");
        }

        final var permitted = symbol.getPermittedSubclasses();
        final var permittedCount = permitted == null ? 0 : permitted.size();

        if (permitted == null || permittedCount != 2) {
            return report(
                    tree,
                    "must permit exactly two subtypes (one enum, one unknown carrier) but permits "
                            + permittedCount);
        }

        final var enums = permitted.stream().filter(OpenEnumStructure::isEnum).toList();

        if (enums.size() != 1) {
            return report(
                    tree,
                    "must permit exactly one enum holding the known constants but permits "
                            + enums.size());
        }

        final var known = enums.getFirst();

        if (!simpleName(known).equals(KNOWN)) {
            return report(
                    tree,
                    "must name its constants enum `%s`, not `%s`"
                            .formatted(KNOWN, simpleName(known)));
        }

        final var carrier = permitted.stream().filter(type -> !isEnum(type)).findFirst();

        if (carrier.isEmpty()) {
            return Description.NO_MATCH;
        }

        if (!isRecordOrFinal(carrier.get())) {
            return report(
                    tree,
                    "the unknown carrier %s must be a record or a final class"
                            .formatted(simpleName(carrier.get())));
        }

        if (!simpleName(carrier.get()).equals(UNKNOWN)) {
            return report(
                    tree,
                    "must name its unknown carrier `%s`, not `%s`"
                            .formatted(UNKNOWN, simpleName(carrier.get())));
        }

        if (!isSubtypeOfNamed(carrier.get(), UNKNOWN_OPEN_ENUM_VALUE, state)) {
            return report(
                    tree,
                    ("the unknown carrier %s must implement UnknownOpenEnumValue, which is what"
                                    + " marks a value as unrecognized")
                            .formatted(simpleName(carrier.get())));
        }

        if (!declaresOpenEnumSet(tree, state)) {
            return report(
                    tree,
                    "must declare a static OpenEnumSet field; integrations resolve raw values"
                            + " through it");
        }

        return Description.NO_MATCH;
    }

    /** An open enum declared by a consumer, as opposed to the library's own base interfaces. */
    private static boolean isDomainOpenEnum(
            final Symbol.ClassSymbol symbol, final VisitorState state) {

        return !symbol.getQualifiedName().toString().startsWith(PACKAGE)
                && isSubtypeOfNamed(symbol.type, OPEN_ENUM_VALUE, state);
    }

    private static boolean isEnum(final Type type) {
        return type.tsym != null && (type.tsym.flags() & Flags.ENUM) != 0;
    }

    private static boolean isRecordOrFinal(final Type type) {
        if (type.tsym == null) {
            return false;
        }

        final var flags = type.tsym.flags();

        return (flags & Flags.RECORD) != 0 || (flags & Flags.FINAL) != 0;
    }

    private static boolean declaresOpenEnumSet(final ClassTree tree, final VisitorState state) {
        final var openEnumSet = state.getTypeFromString(OPEN_ENUM_SET);

        if (openEnumSet == null) {
            return true;
        }

        for (final var member : tree.getMembers()) {
            if (member instanceof final VariableTree variable) {
                final var variableSymbol = ASTHelpers.getSymbol(variable);

                // Fields on an interface are implicitly static, but spell it out so this also
                // holds if the pattern is ever used on an abstract class.
                if (variableSymbol != null
                        && variableSymbol.isStatic()
                        && variableSymbol.type != null
                        && ASTHelpers.isSameType(
                                state.getTypes().erasure(variableSymbol.type),
                                openEnumSet,
                                state)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Resolves the named type against the compilation's classpath. A null result means the type is
     * absent, in which case nothing can be concluded and the check stays silent rather than
     * throwing — an Error Prone check must never break a build it does not understand.
     */
    private static boolean isSubtypeOfNamed(
            final @Nullable Type type, final String fullyQualifiedName, final VisitorState state) {

        final var target = state.getTypeFromString(fullyQualifiedName);

        return type != null && target != null && ASTHelpers.isSubtype(type, target, state);
    }

    private static String simpleName(final Type type) {
        return type.tsym == null ? "<unknown>" : type.tsym.getSimpleName().toString();
    }

    private Description report(final ClassTree tree, final String problem) {
        final var symbol = ASTHelpers.getSymbol(tree);
        final var name = symbol == null ? "This open enum" : symbol.getSimpleName().toString();

        return buildDescription(tree).setMessage("%s %s".formatted(name, problem)).build();
    }
}
