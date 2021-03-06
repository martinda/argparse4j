/*
 * Copyright (C) 2013 Tatsuhiro Tsujikawa
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.sourceforge.argparse4j.impl.type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.sourceforge.argparse4j.helper.TextHelper;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.ArgumentType;

/**
 * <p>
 * This implementation converts String value into given type using type's
 * {@code valueOf(java.lang.String)} static method or its constructor.
 * </p>
 */
public class ReflectArgumentType<T> implements ArgumentType<T> {

    private Class<T> type_;

    /**
     * <p>
     * Creates {@link ReflectArgumentType} object with given {@code type}.
     * </p>
     * <p>
     * This object first tries to convert given String using
     * {@code valueOf(java.lang.String)} static method of given {@code type}. If
     * that failed, then use constructor of given {@code type} for conversion.
     * {@code valueOf()} method and/or constructor must be declared as public.
     * Otherwise, they cannot be invoked. The constructor of {@code type} must
     * accept 1 String argument.
     * </p>
     * <p>
     * If error occurred inside the {@code valueOf} static method or
     * constructor, {@link ArgumentParserException} will be thrown. If error
     * occurred in other locations, subclass of {@link RuntimeException} will be
     * thrown.
     * </p>
     * <p>
     * This object works with enums as well. The enums in its nature have
     * limited number of members. In
     * {@link #convert(ArgumentParser, Argument, String)}, string value will be
     * converted to one of them. If it cannot be converted,
     * {@link #convert(ArgumentParser, Argument, String)} will throw
     * {@link ArgumentParserException}. This means it already act like a
     * {@link Argument#choices(Object...)}.
     * </p>
     * 
     * @param type
     *            The type String value should be converted to.
     */
    public ReflectArgumentType(Class<T> type) {
        type_ = type;
    }

    @Override
    public T convert(ArgumentParser parser, Argument arg, String value)
            throws ArgumentParserException {
        // Handle enums separately. Enum.valueOf() is very convenient here.
        // It somehow can access private enum values, where normally T.valueOf()
        // cannot without setAccessible(true).
        if (type_.isEnum()) {
            try {
                return (T) Enum.valueOf((Class<Enum>) type_, value);
            } catch (IllegalArgumentException e) {
                throwArgumentParserException(parser, arg, value, e);
            }
        }
        Method m = null;
        try {
            m = type_.getMethod("valueOf", String.class);
        } catch (NoSuchMethodException e) {
            // If no valueOf static method found, try constructor.
            return convertUsingConstructor(parser, arg, value);
        } catch (SecurityException e) {
            handleInstatiationError(e);
        }
        // Only interested in static valueOf method.
        if (!Modifier.isStatic(m.getModifiers())
                || !type_.isAssignableFrom(m.getReturnType())) {
            return convertUsingConstructor(parser, arg, value);
        }
        Object obj = null;
        try {
            obj = m.invoke(null, value);
        } catch (IllegalAccessException e) {
            return convertUsingConstructor(parser, arg, value);
        } catch (IllegalArgumentException e) {
            handleInstatiationError(e);
        } catch (InvocationTargetException e) {
            throwArgumentParserException(parser, arg, value,
                    e.getCause() == null ? e : e.getCause());
        }
        return (T) obj;
    }

    private T convertUsingConstructor(ArgumentParser parser, Argument arg,
            String value) throws ArgumentParserException {
        T obj = null;
        try {
            obj = type_.getConstructor(String.class).newInstance(value);
        } catch (InstantiationException e) {
            handleInstatiationError(e);
        } catch (IllegalAccessException e) {
            handleInstatiationError(e);
        } catch (InvocationTargetException e) {
            throwArgumentParserException(parser, arg, value,
                    e.getCause() == null ? e : e.getCause());
        } catch (NoSuchMethodException e) {
            handleInstatiationError(e);
        }
        return obj;
    }

    private void throwArgumentParserException(ArgumentParser parser,
            Argument arg, String value, Throwable t)
            throws ArgumentParserException {
        throw new ArgumentParserException(String.format(TextHelper.LOCALE_ROOT,
                "could not convert '%s' to %s (%s)", value,
                type_.getSimpleName(), t.getMessage()), t, parser, arg);
    }

    private void handleInstatiationError(Exception e) {
        throw new IllegalArgumentException("reflect type conversion error", e);
    }
}
