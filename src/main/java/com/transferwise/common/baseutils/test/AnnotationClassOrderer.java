package com.transferwise.common.baseutils.test;

import java.util.Comparator;
import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;

/**
 * Usage in junit-platform.properties: ```junit.jupiter.testclass.order.default=org.example.tests.AnnotationClassOrderer```
 * <p>
 * Used in conjunction with {@linkplain ClassOrder} annotations to explicitly define test ordering in JUnit 5.8+
 */
public class AnnotationClassOrderer implements ClassOrderer {

  @Override
  public void orderClasses(ClassOrdererContext context) {
    context.getClassDescriptors().sort((Comparator<ClassDescriptor>) (d1, d2) -> {
      ClassOrder a1 = d1.getTestClass().getDeclaredAnnotation(ClassOrder.class);
      ClassOrder a2 = d2.getTestClass().getDeclaredAnnotation(ClassOrder.class);
      if (a1 == null) {
        return 1;
      }

      if (a2 == null) {
        return -1;
      }

      return Integer.compare(a1.value(), a2.value());
    });
  }
}
