package ch.ubique.uetlib.errorprone;

import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenEnumStructureTest {

    /**
     * Stubs of the uetlib-core types, rather than a dependency on uetlib-core: uetlib-core compiles
     * against this check, so depending on it here would be circular.
     */
    private static final String[] OPEN_ENUM_STUBS = {
        "ch/ubique/uetlib/core/openenum/OpenEnumValue.java",
        """
        package ch.ubique.uetlib.core.openenum;

        public interface OpenEnumValue<V> {
            V value();
        }
        """,
        "ch/ubique/uetlib/core/openenum/UnknownOpenEnumValue.java",
        """
        package ch.ubique.uetlib.core.openenum;

        public interface UnknownOpenEnumValue<V> extends OpenEnumValue<V> {}
        """,
        "ch/ubique/uetlib/core/openenum/OpenEnumSet.java",
        """
        package ch.ubique.uetlib.core.openenum;

        public final class OpenEnumSet<T extends OpenEnumValue<V>, V> {
            public static <T extends OpenEnumValue<V>, V> OpenEnumSet<T, V> of(
                    Class<?> knownType, Object unknownFactory) {
                return new OpenEnumSet<>();
            }
        }
        """
    };

    private CompilationTestHelper helper;

    @BeforeEach
    void setUp() {
        helper = CompilationTestHelper.newInstance(OpenEnumStructure.class, getClass());

        for (int i = 0; i < OPEN_ENUM_STUBS.length; i += 2) {
            helper = helper.addSourceLines(OPEN_ENUM_STUBS[i], OPEN_ENUM_STUBS[i + 1]);
        }
    }

    @Test
    void acceptsAWellFormedOpenEnum() {
        helper.addSourceLines(
                        "Color.java",
                        """
                        import ch.ubique.uetlib.core.openenum.*;

                        public sealed interface Color extends OpenEnumValue<String> {

                            OpenEnumSet<Color, String> KNOWN_VALUES =
                                    OpenEnumSet.of(Known.class, null);

                            enum Known implements Color {
                                RED;
                                public String value() { return "red"; }
                            }

                            record Unknown(String value)
                                    implements Color, UnknownOpenEnumValue<String> {}
                        }
                        """)
                .doTest();
    }

    @Test
    void rejectsANonSealedInterface() {
        helper.addSourceLines(
                        "Color.java",
                        """
                        import ch.ubique.uetlib.core.openenum.*;

                        // BUG: Diagnostic contains: must be sealed
                        public interface Color extends OpenEnumValue<String> {
                            OpenEnumSet<Color, String> KNOWN_VALUES = null;
                        }
                        """)
                .doTest();
    }

    @Test
    void rejectsAMissingUnknownCarrier() {
        helper.addSourceLines(
                        "Color.java",
                        """
                        import ch.ubique.uetlib.core.openenum.*;

                        // BUG: Diagnostic contains: must permit exactly two subtypes
                        public sealed interface Color extends OpenEnumValue<String> {

                            OpenEnumSet<Color, String> KNOWN_VALUES = null;

                            enum Known implements Color {
                                RED;
                                public String value() { return "red"; }
                            }
                        }
                        """)
                .doTest();
    }

    @Test
    void rejectsAMisnamedConstantsEnum() {
        helper.addSourceLines(
                        "Color.java",
                        """
                        import ch.ubique.uetlib.core.openenum.*;

                        // BUG: Diagnostic contains: must name its constants enum `Known`
                        public sealed interface Color extends OpenEnumValue<String> {

                            OpenEnumSet<Color, String> KNOWN_VALUES = null;

                            enum KnownColor implements Color {
                                RED;
                                public String value() { return "red"; }
                            }

                            record Unknown(String value)
                                    implements Color, UnknownOpenEnumValue<String> {}
                        }
                        """)
                .doTest();
    }

    @Test
    void rejectsAMisnamedUnknownCarrier() {
        helper.addSourceLines(
                        "Color.java",
                        """
                        import ch.ubique.uetlib.core.openenum.*;

                        // BUG: Diagnostic contains: must name its unknown carrier `Unknown`
                        public sealed interface Color extends OpenEnumValue<String> {

                            OpenEnumSet<Color, String> KNOWN_VALUES = null;

                            enum Known implements Color {
                                RED;
                                public String value() { return "red"; }
                            }

                            record UnknownColor(String value)
                                    implements Color, UnknownOpenEnumValue<String> {}
                        }
                        """)
                .doTest();
    }

    @Test
    void rejectsAnUnknownCarrierThatIsNotMarked() {
        helper.addSourceLines(
                        "Color.java",
                        """
                        import ch.ubique.uetlib.core.openenum.*;

                        // BUG: Diagnostic contains: must implement UnknownOpenEnumValue
                        public sealed interface Color extends OpenEnumValue<String> {

                            OpenEnumSet<Color, String> KNOWN_VALUES = null;

                            enum Known implements Color {
                                RED;
                                public String value() { return "red"; }
                            }

                            record Unknown(String value) implements Color {}
                        }
                        """)
                .doTest();
    }

    @Test
    void rejectsAMissingOpenEnumSet() {
        helper.addSourceLines(
                        "Color.java",
                        """
                        import ch.ubique.uetlib.core.openenum.*;

                        // BUG: Diagnostic contains: must declare a static OpenEnumSet field
                        public sealed interface Color extends OpenEnumValue<String> {

                            enum Known implements Color {
                                RED;
                                public String value() { return "red"; }
                            }

                            record Unknown(String value)
                                    implements Color, UnknownOpenEnumValue<String> {}
                        }
                        """)
                .doTest();
    }

    @Test
    void ignoresTheLibrarysOwnInterfaces() {
        // UnknownOpenEnumValue is itself a non-sealed interface extending OpenEnumValue, so the
        // check must exempt the package it lives in or it would flag the library itself.
        helper.doTest();
    }
}
