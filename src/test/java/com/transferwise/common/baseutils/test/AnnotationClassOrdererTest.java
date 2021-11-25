package com.transferwise.common.baseutils.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AnnotationClassOrdererTest {

  @RequiredArgsConstructor
  static class MockClassDescriptor implements ClassDescriptor {

    private final Class<?> theClass;

    @Override
    public Class<?> getTestClass() {
      return theClass;
    }

    @Override
    public String getDisplayName() {
      return theClass.getName();
    }

    @Override
    public boolean isAnnotated(Class<? extends Annotation> annotationType) {
      return theClass.getAnnotations().length > 0;
    }

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {
      try {
        return Optional.of(theClass.getAnnotation(annotationType));
      } catch (Exception e) {
        return Optional.empty();
      }
    }

    @Override
    public <A extends Annotation> List<A> findRepeatableAnnotations(Class<A> annotationType) {
      return List.of();
    }
  }

  private final AnnotationClassOrderer orderer = new AnnotationClassOrderer();

  @ClassOrder(2)
  static class TestA {

  }

  @ClassOrder(3)
  static class TestB {

  }

  @ClassOrder(1)
  static class TestC {

  }

  static class TestImplicitA {

  }

  @ClassOrder(1)
  static class TestImplicitB {

  }

  @ParameterizedTest(name = "{displayName}")
  @DisplayName("test class ordering - {0}")
  @MethodSource
  void testAnnotationOrdering(List<Class<?>> classes, List<Class<?>> expected) {
    ClassOrdererContext context = new ClassOrdererContext() {
      private final List<MockClassDescriptor> values = new ArrayList<>();

      @Override
      public List<? extends ClassDescriptor> getClassDescriptors() {
        if (values.isEmpty()) {
          for (Class<?> theClass : classes) {
            values.add(new MockClassDescriptor(theClass));
          }
        }

        return values;
      }

      @Override
      public Optional<String> getConfigurationParameter(String key) {
        return Optional.empty();
      }
    };

    orderer.orderClasses(context);
    assertThat(context.getClassDescriptors()).hasSize(expected.size());
    for (int i = 0; i < expected.size(); i++) {
      assertThat(context.getClassDescriptors().get(i).getTestClass()).isEqualTo(expected.get(i));
    }
  }

  static Stream<Arguments> testAnnotationOrdering() {
    return Stream.of(
        Arguments.of(
            List.of(TestA.class, TestB.class, TestC.class),
            List.of(TestC.class, TestA.class, TestB.class)),
        Arguments.of(
            List.of(TestImplicitA.class, TestImplicitB.class, TestImplicitA.class),
            List.of(TestImplicitB.class, TestImplicitA.class, TestImplicitA.class)));
  }
}
