package ch.ubique.uetlib.errorprone;

import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenEnumNestedTypeImportTest {

    private CompilationTestHelper helper;

    @BeforeEach
    void setUp() {
        helper =
                CompilationTestHelper.newInstance(OpenEnumNestedTypeImport.class, getClass())
                        .addSourceLines(
                                "ch/ubique/uetlib/core/openenum/OpenEnumValue.java",
                                """
                                package ch.ubique.uetlib.core.openenum;

                                public interface OpenEnumValue<V> {
                                    V value();
                                }
                                """)
                        .addSourceLines(
                                "ch/ubique/uetlib/core/openenum/UnknownOpenEnumValue.java",
                                """
                                package ch.ubique.uetlib.core.openenum;

                                public interface UnknownOpenEnumValue<V>
                                        extends OpenEnumValue<V> {}
                                """)
                        .addSourceLines(
                                "colour/Color.java",
                                """
                                package colour;

                                import ch.ubique.uetlib.core.openenum.*;

                                public sealed interface Color extends OpenEnumValue<String> {

                                    enum Known implements Color {
                                        RED;
                                        public String value() { return "red"; }
                                    }

                                    record Unknown(String value)
                                            implements Color, UnknownOpenEnumValue<String> {}
                                }
                                """);
    }

    @Test
    void rejectsImportingTheNestedKnownType() {
        helper.addSourceLines(
                        "Use.java",
                        """
                        // BUG: Diagnostic contains: write Color.Known instead
                        import static colour.Color.Known;

                        class Use {
                            Object a = Known.RED;
                        }
                        """)
                .doTest();
    }

    @Test
    void rejectsImportingTheNestedUnknownType() {
        helper.addSourceLines(
                        "Use.java",
                        """
                        // BUG: Diagnostic contains: write Color.Unknown instead
                        import static colour.Color.Unknown;

                        class Use {
                            Object a = new Unknown("cyan");
                        }
                        """)
                .doTest();
    }

    @Test
    void allowsImportingTheConstantsOnDemand() {
        helper.addSourceLines(
                        "Use.java",
                        """
                        import static colour.Color.Known.RED;

                        class Use {
                            Object a = RED;
                        }
                        """)
                .doTest();
    }

    @Test
    void allowsImportingAllConstants() {
        helper.addSourceLines(
                        "Use.java",
                        """
                        import static colour.Color.Known.*;

                        class Use {
                            Object a = RED;
                        }
                        """)
                .doTest();
    }

    @Test
    void allowsImportingTheOwningType() {
        helper.addSourceLines(
                        "Use.java",
                        """
                        import colour.Color;

                        class Use {
                            Object a = Color.Known.RED;
                            Object b = new Color.Unknown("cyan");
                        }
                        """)
                .doTest();
    }

    @Test
    void leavesUnrelatedTypesNamedKnownAlone() {
        helper.addSourceLines(
                        "other/Holder.java",
                        """
                        package other;

                        public final class Holder {
                            public static final class Known {}
                        }
                        """)
                .addSourceLines(
                        "Use.java",
                        """
                        import other.Holder.Known;

                        class Use {
                            Object a = new Known();
                        }
                        """)
                .doTest();
    }
}
