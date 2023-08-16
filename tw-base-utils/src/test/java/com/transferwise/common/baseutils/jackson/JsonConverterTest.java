package com.transferwise.common.baseutils.jackson;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JsonConverterTest {
  private JsonConverter jsonConverter;
  private Integer[] array = {1, 2};

  @BeforeEach
  public void setUp() {
    jsonConverter = new DefaultJsonConverter(new ObjectMapper());
  }

  @Test
  public void testFromJsonMapping() {
    Species dragon = new Species("dragon", 7);
    String jsonString = jsonConverter.fromObject(dragon);
    assertEquals(jsonString, "{\"name\":\"dragon\",\"headCount\":7}");
  }

  @Test
  public void testFromObjectToBytesMapping() {
    Species dragon = new Species("dragon", 7);
    byte[] json = jsonConverter.fromObjectToBytes(dragon);
    assertEquals("{\"name\":\"dragon\",\"headCount\":7}", new String(json, StandardCharsets.UTF_8));
  }

  @Test
  public void testToObjectMapping() {
    String jsonString = "{\"name\": \"human\", \"headCount\": 1}";
    Species human = jsonConverter.toObject(jsonString, Species.class);
    assertEquals(human.getName(), "human");
    assertEquals(human.getHeadCount(), (Integer) 1);
  }

  @Test
  public void testToObjectFromBytesMapping() {
    byte[] json = "{\"name\": \"human\", \"headCount\": 1}".getBytes(StandardCharsets.UTF_8);
    Species human = jsonConverter.toObject(json, Species.class);
    assertEquals("human", human.getName());
    assertEquals((Integer) 1, human.getHeadCount());
  }

  @Test
  public void testToListMapping() {
    String jsonString = "[1, 2]";
    List<Integer> list = jsonConverter.toList(jsonString, new TypeReference<List<Integer>>() {
    });
    assertArrayEquals(list.toArray(), array);
  }

  @Test
  public void testToListFromBytesMapping() {
    byte[] json = "[1, 2]".getBytes(StandardCharsets.UTF_8);
    List<Integer> list = jsonConverter.toList(json, new TypeReference<List<Integer>>() {
    });
    assertArrayEquals(array, list.toArray());
  }

  @Test
  public void testTimestampBackwardCompatibleReading() {
    JsonConverter oldJsonConverter = getOldJsonConverter();
    Instant sourceTimestamp = Instant.now();
    String jsonString = oldJsonConverter.fromObject(sourceTimestamp);
    Instant targetTimestamp = jsonConverter.toObject(jsonString, Instant.class);
    assertEquals(sourceTimestamp, targetTimestamp);
  }

  @Test
  public void testTimestampBackwardCompatibleWriting() {
    JsonConverter oldJsonConverter = getOldJsonConverter();
    // Depending on java version Instant.now may produce micro or nano seconds, but that's not the point of the test, therefore truncating
    Instant sourceTimestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    String jsonString = jsonConverter.fromObject(sourceTimestamp);
    Instant targetTimestamp = oldJsonConverter.toObject(jsonString, Instant.class);
    assertEquals(sourceTimestamp, targetTimestamp);
  }

  @Test
  public void testConsistentZonedDateTimeMillisecondsWriting() {
    String timeWithMilliseconds = jsonConverter.fromObject(ZonedDateTime.parse("2012-06-30T12:30:40.123Z"));
    String timeWithoutMilliseconds = jsonConverter.fromObject(ZonedDateTime.parse("2012-06-30T12:30:40Z"));
    assertEquals("\"2012-06-30T12:30:40.000Z\"", timeWithoutMilliseconds);
    assertEquals("\"2012-06-30T12:30:40.123Z\"", timeWithMilliseconds);
  }

  @Test
  public void testConsistentInstantMillisecondsWriting() {
    String timeWithMicroseconds = jsonConverter.fromObject(Instant.parse("2012-06-30T12:30:40.123456Z"));
    String timeWithMilliseconds = jsonConverter.fromObject(Instant.parse("2012-06-30T12:30:40.123Z"));
    String timeWithoutMilliseconds = jsonConverter.fromObject(Instant.parse("2012-06-30T12:30:40Z"));

    assertEquals("\"2012-06-30T12:30:40.123Z\"", timeWithMicroseconds);
    assertEquals("\"2012-06-30T12:30:40.000Z\"", timeWithoutMilliseconds);
    assertEquals("\"2012-06-30T12:30:40.123Z\"", timeWithMilliseconds);
  }

  private ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    return objectMapper;
  }

  private JsonConverter getOldJsonConverter() {
    return new DefaultJsonConverter(objectMapper());
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class Species {

    private String name;
    private Integer headCount;
  }
}
