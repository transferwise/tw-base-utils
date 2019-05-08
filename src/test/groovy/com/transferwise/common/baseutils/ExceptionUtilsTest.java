package com.transferwise.common.baseutils;

import lombok.SneakyThrows;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;

import static com.transferwise.common.baseutils.ExceptionUtils.callUnchecked;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ExceptionUtilsTest {

    @Test
    public void testUncheckedCall() {
        //Given
        Throwable myT = new Throwable("1");
        RuntimeException myRe = new RuntimeException("Ahoy");
        Error myError = new Error("Memory leak.");
        String myString = "Hello World!";
        //When
        String result = callUnchecked(() -> myString);
        //Then
        assertEquals(myString, result);
        //When
        Throwable t = catchAndReturn(() -> callUnchecked(() -> {
            throw myRe;
        }));
        //Then
        assertSame(myRe, t);
        //When
        t = catchAndReturn(() -> callUnchecked(() -> {
            throw myError;
        }));
        //Then
        assertSame(myError, t);
        //When
        t = catchAndReturn(() -> callUnchecked(() -> {
            throw new UndeclaredThrowableException(myT);
        }));
        //Then
        assertEquals(RuntimeException.class, t.getClass());
        assertSame(myT, t.getCause()); // Is cause wrapped out from UndeclaredThrowableException.
        //When
        t = catchAndReturn(() -> callUnchecked(() -> {
            throw new InvocationTargetException(myT);
        }));
        //Then
        assertEquals(RuntimeException.class, t.getClass());
        assertSame(myT, t.getCause()); // Is cause wrapped out from UndeclaredThrowableException.
        //When
        t = catchAndReturn(() -> callUnchecked(() -> {
            duckTapersDoingSneakyStuff(myT);
            return null;
        }));
        //Then
        assertEquals(RuntimeException.class, t.getClass());
        assertSame(myT, t.getCause());
    }

    @SneakyThrows
    private void duckTapersDoingSneakyStuff(Throwable t) {
        throw t;
    }

    private Throwable catchAndReturn(Callable callable) {
        try {
            callable.call();
            throw new IllegalStateException("Who is the captain of this failboat?");
        } catch (Throwable t) {
            return t;
        }
    }
}
