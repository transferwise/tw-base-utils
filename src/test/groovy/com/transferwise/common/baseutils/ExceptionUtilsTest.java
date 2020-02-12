package com.transferwise.common.baseutils;

import static com.transferwise.common.baseutils.ExceptionUtils.doUnchecked;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;
import lombok.SneakyThrows;
import org.junit.Test;

public class ExceptionUtilsTest {

  @Test
  public void testUncheckedCall() {
    //Given
    final Throwable myT = new Throwable("1");
    RuntimeException myRe = new RuntimeException("Ahoy");
    Error myError = new Error("Memory leak.");
    String myString = "Hello World!";
    //When
    String result = doUnchecked(() -> myString);
    //Then
    assertEquals(myString, result);
    //When
    Throwable t = catchAndReturn(() -> doUnchecked(() -> {
      throw myRe;
    }));
    //Then
    assertSame(myRe, t);
    //When
    t = catchAndReturn(() -> doUnchecked(() -> {
      throw myError;
    }));
    //Then
    assertSame(myError, t);
    //When
    t = catchAndReturn(() -> doUnchecked(() -> {
      throw new UndeclaredThrowableException(myT);
    }));
    //Then
    assertEquals(RuntimeException.class, t.getClass());
    assertSame(myT, t.getCause()); // Is cause wrapped out from UndeclaredThrowableException.
    //When
    t = catchAndReturn(() -> doUnchecked(() -> {
      throw new InvocationTargetException(myT);
    }));
    //Then
    assertEquals(RuntimeException.class, t.getClass());
    assertSame(myT, t.getCause()); // Is cause wrapped out from UndeclaredThrowableException.
    //When
    t = catchAndReturn(() -> doUnchecked(() -> {
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
      throw new IllegalStateException("Who is the captain of this fail boat?");
    } catch (Throwable t) {
      return t;
    }
  }
}
