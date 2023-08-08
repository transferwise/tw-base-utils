package com.transferwise.common.baseutils.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * Solves undesired behaviour of default formatter that will skip milliseconds in the string at exact seconds, i.e. `2012-06-30T12:30:40Z` is
 * searalised as `2012-06-30T12:30:40.000Z`. Starting from Java 9 timestamps have microseconds, and starting from Java 15 timestamps have nanoseconds
 * (dependent on OS). This module allows serialising all timestamps with consistent millisecond precision.
 */
public final class JavaTimeModuleFactory {

  public static JavaTimeModule consistentMillisecondsTimeModule() {
    JavaTimeModule javaTimeModule = new JavaTimeModule();

    DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

    ZonedDateTimeSerializer dateTimeSerializer = new ZonedDateTimeSerializer(formatter);
    javaTimeModule.addSerializer(ZonedDateTime.class, dateTimeSerializer);

    javaTimeModule.addSerializer(Instant.class, new InstantSerializer(formatter));

    return javaTimeModule;
  }

  public static class InstantSerializer extends JsonSerializer<Instant> {

    private final DateTimeFormatter dateFormatter;

    public InstantSerializer(DateTimeFormatter dateFormatter) {
      this.dateFormatter = dateFormatter;
    }

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      String str = dateFormatter.format(value);
      gen.writeString(str);
    }
  }
}