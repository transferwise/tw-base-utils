package com.transferwise.common.baseutils.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JavaTimeModuleFactoryTest {

  static ObjectMapper oldObjectMapper;
  static ObjectMapper objectMapper;

  @BeforeAll
  public static void setup() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(JavaTimeModuleFactory.consistentMillisecondsTimeModule());
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    oldObjectMapper = new ObjectMapper();
    oldObjectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  public void testTimestampBackwardCompatibleReading() throws Exception {
    Instant sourceTimestamp = Instant.now();
    String jsonString = oldObjectMapper.writeValueAsString(sourceTimestamp);
    Instant targetTimestamp = objectMapper.readValue(jsonString, Instant.class);
    assertEquals(sourceTimestamp, targetTimestamp);
  }

  @Test
  public void testTimestampBackwardCompatibleWriting() throws Exception {
    // Depending on java version Instant.now may produce micro or nanoseconds, but that's not the point of the test, therefore truncating
    Instant sourceTimestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    String jsonString = objectMapper.writeValueAsString(sourceTimestamp);
    Instant targetTimestamp = oldObjectMapper.readValue(jsonString, Instant.class);
    assertEquals(sourceTimestamp, targetTimestamp);
  }

  @Test
  public void testConsistentZonedDateTimeMillisecondsWriting() throws Exception {
    String timeWithMilliseconds = objectMapper.writeValueAsString(ZonedDateTime.parse("2012-06-30T12:30:40.123Z"));
    String timeWithoutMilliseconds = objectMapper.writeValueAsString(ZonedDateTime.parse("2012-06-30T12:30:40Z"));
    assertEquals("\"2012-06-30T12:30:40.000Z\"", timeWithoutMilliseconds);
    assertEquals("\"2012-06-30T12:30:40.123Z\"", timeWithMilliseconds);
  }

  @Test
  public void testConsistentInstantMillisecondsWriting() throws Exception {
    String timeWithMicroseconds = objectMapper.writeValueAsString(Instant.parse("2012-06-30T12:30:40.123456Z"));
    String timeWithMilliseconds = objectMapper.writeValueAsString(Instant.parse("2012-06-30T12:30:40.123Z"));
    String timeWithoutMilliseconds = objectMapper.writeValueAsString(Instant.parse("2012-06-30T12:30:40Z"));

    assertEquals("\"2012-06-30T12:30:40.123Z\"", timeWithMicroseconds);
    assertEquals("\"2012-06-30T12:30:40.123Z\"", timeWithMilliseconds);
    assertEquals("\"2012-06-30T12:30:40.000Z\"", timeWithoutMilliseconds);
  }

}
