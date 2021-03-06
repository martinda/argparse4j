package net.sourceforge.argparse4j.impl.type;

import static org.junit.Assert.*;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.mock.MockArgument;

import org.junit.Before;
import org.junit.Test;

public class ReflectArgumentTypeTest {

    private MockArgument ma;

    @Before
    public void setup() throws Exception {
        ma = new MockArgument();
    }

    private <T> ReflectArgumentType<T> createRA(Class<T> type) {
        return new ReflectArgumentType<T>(type);
    }

    @Test
    public void testConvertInteger() throws ArgumentParserException {
        ReflectArgumentType<Integer> at = createRA(Integer.class);
        assertEquals((Integer)100, at.convert(null, null, "100"));
        try {
            at.convert(null, ma, "0x100");
            fail();
        } catch(ArgumentParserException e) {
            assertEquals("argument null: could not convert '0x100' to Integer (For input string: \"0x100\")",
                    e.getMessage());
        }
    }

    enum Lang {
        PYTHON, CPP, JAVA
    }
    @Test
    public void testConvertEnum() throws ArgumentParserException {
        ReflectArgumentType<Lang> at = createRA(Lang.class);
        assertEquals(Lang.CPP, at.convert(null, null, "CPP"));
        try {
            at.convert(null, ma, "C");
            fail();
        } catch(ArgumentParserException e) {
            assertTrue(e.getMessage().startsWith("argument null: could not convert 'C' to Lang ("));
        }
    }

    private static final class NoValueOfNoCtor {
    }

    @Test
    public void testConvertNoValueOfNoCtor() throws ArgumentParserException {
        ReflectArgumentType<NoValueOfNoCtor> at = createRA(NoValueOfNoCtor.class);
        try {
            at.convert(null,  ma, "foo");
            fail();
        } catch(IllegalArgumentException e) {
            assertEquals("reflect type conversion error", e.getMessage());
        }
    }

    private static final class NoValueOf {
        public NoValueOf(String arg) {
        }
    }

    @Test
    public void testConvertNoValueOf() throws ArgumentParserException {
        ReflectArgumentType<NoValueOf> at = createRA(NoValueOf.class);
        assertNotNull(at.convert(null, null, "foo"));
    }

    private static final class NoCtor {
        public static NoCtor valueOf(String arg) {
            return new NoCtor();
        }
    }

    @Test
    public void testConvertNoCtor() throws ArgumentParserException {
        ReflectArgumentType<NoCtor> at = createRA(NoCtor.class);
        assertNotNull(at.convert(null, null, "foo"));
    }

    private static final class NonStaticValueOf {
        public String arg_;

        public NonStaticValueOf(String arg) {
            arg_ = "From ctor";
        }

        public NonStaticValueOf valueOf(String arg) {
            NonStaticValueOf x = new NonStaticValueOf(arg);
            x.arg_ = "From valueOf";
            return x;
        }
    }

    @Test
    public void testConvertNonStaticValueOf() throws ArgumentParserException {
        ReflectArgumentType<NonStaticValueOf> at = createRA(NonStaticValueOf.class);
        assertEquals("From ctor", at.convert(null, null, "UNUSED").arg_);
    }

    private static final class WrongReturnTypeValueOf {
        public String arg_;

        public WrongReturnTypeValueOf(String arg) {
            arg_ = arg;
        }

        public static String valueOf(String arg) {
            return "Bad";
        }
    }

    @Test
    public void testConvertWrongReturnTypeValueOf() throws ArgumentParserException {
        ReflectArgumentType<WrongReturnTypeValueOf> at = createRA(WrongReturnTypeValueOf.class);
        assertEquals("Good", at.convert(null, null, "Good").arg_);
    }

    private static class Base {
        public static Derived valueOf(String arg) {
            return new Derived();
        }
    }

    private static class Derived extends Base {
    }

    @Test
    public void testConvertSupertype() throws ArgumentParserException {
        ReflectArgumentType<Base> at = createRA(Base.class);
        assertEquals(Derived.class, at.convert(null, null, "foo").getClass());
    }

    private static final class NonStringValueOf {
        public String arg_;

        public NonStringValueOf(String arg) {
            arg_ = arg;
        }

        public static NonStringValueOf valueOf(Object n) {
            return new NonStringValueOf("Bad");
        }
    }

    @Test
    public void testConvertNonStringValueOf() throws ArgumentParserException {
        ReflectArgumentType<NonStringValueOf> at = createRA(NonStringValueOf.class);
        assertEquals("Good", at.convert(null, null, "Good").arg_);
    }
}
