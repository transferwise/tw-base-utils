package com.transferwise.common.baseutils.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used by {@linkplain AnnotationClassOrderer} to determine order
 * of execution of JUnit 5.8+ test classes.
 *
 * <pre>{@code
 * @ClassOrder(1)
 * class MyTestClass {
 *
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ClassOrder {
  int value() default Integer.MAX_VALUE;
}
