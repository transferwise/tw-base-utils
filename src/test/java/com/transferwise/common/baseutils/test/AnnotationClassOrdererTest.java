package com.transferwise.common.baseutils.test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationClassOrdererTest {

  @ClassOrder(2)
  static class TestA {

  }

  @ClassOrder(3)
  static class TestB {

  }

  @ClassOrder(1)
  static class TestC {

  }

  @RequiredArgsConstructor
  static class MockClassDescriptor implements ClassDescriptor {

    private final Class<?> aClass;

    @Override
    public Class<?> getTestClass() {
      return aClass;
    }

    @Override
    public String getDisplayName() {
      return aClass.getName();
    }

    @Override
    public boolean isAnnotated(Class<? extends Annotation> annotationType) {
      return aClass.getAnnotations().length > 0;
    }

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) {
      try {
        return Optional.of(aClass.getAnnotation(annotationType));
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

  @Test
  void test() {
    ClassOrdererContext context = new ClassOrdererContext() {
      private final List<MockClassDescriptor> values = new ArrayList<>();

      @Override
      public List<? extends ClassDescriptor> getClassDescriptors() {
        if (values.isEmpty()) {
          values.add(new MockClassDescriptor(TestA.class));
          values.add(new MockClassDescriptor(TestB.class));
          values.add(new MockClassDescriptor(TestC.class));
        }

        return values;
      }

      @Override
      public Optional<String> getConfigurationParameter(String key) {
        return Optional.empty();
      }
    };

    orderer.orderClasses(context);
    assertThat(context.getClassDescriptors()).hasSize(3);
    assertThat(context.getClassDescriptors().get(0).getTestClass()).isEqualTo(TestC.class);
    assertThat(context.getClassDescriptors().get(1).getTestClass()).isEqualTo(TestA.class);
    assertThat(context.getClassDescriptors().get(2).getTestClass()).isEqualTo(TestB.class);
  }
}
