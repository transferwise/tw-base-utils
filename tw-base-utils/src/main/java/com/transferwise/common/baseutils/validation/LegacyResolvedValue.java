package com.transferwise.common.baseutils.validation;

import com.transferwise.common.baseutils.validation.LegacyResolvedValue.LegacyResolvedValueValidator;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Map;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

/**
 * Allows to avoid mistakes where unresolved configuration is applied.
 *
 * <p>E.g. using property="${anotherProperty}"
 */
@Documented
@Constraint(validatedBy = LegacyResolvedValueValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LegacyResolvedValue {

  String message() default "Unresolved value.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class LegacyResolvedValueValidator implements ConstraintValidator<LegacyResolvedValue, Object> {

    @Override
    public void initialize(LegacyResolvedValue contactNumber) {
      // Nothing to initialize
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isValid(Object field, ConstraintValidatorContext ctx) {
      if (field instanceof String) {
        return isValidString((String) field);
      } else if (field instanceof Map) {
        var map = (Map<?, ?>) field;
        for (var entry : map.entrySet()) {
          var key = entry.getKey();
          if (key instanceof String) {
            return isValidString((String) key);
          }
          var value = entry.getValue();
          if (value instanceof String) {
            return isValidString((String) value);
          }
        }
      } else if (field instanceof Collection) {
        for (var val : (Collection<?>) field) {
          if (val instanceof String) {
            return isValidString((String) val);
          }
        }
      }
      return true;
    }

    protected boolean isValidString(String val) {
      return val == null || !val.contains("${");
    }
  }
}