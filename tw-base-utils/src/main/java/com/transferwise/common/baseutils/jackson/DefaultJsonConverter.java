package com.transferwise.common.baseutils.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.transferwise.common.baseutils.ExceptionUtils;
import java.util.List;
import org.springframework.stereotype.Component;


public final class DefaultJsonConverter implements JsonConverter {

  private final ObjectMapper objectMapper;

  public DefaultJsonConverter(ObjectMapper injectedObjectMapper) {
    this.objectMapper = injectedObjectMapper;
    objectMapper.registerModule(JavaTimeModuleFactory.consistentMillisecondsTimeModule());
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T toObject(String json, Class<T> clazz) {
    return ExceptionUtils.doUnchecked(() -> objectMapper.readerFor(clazz).readValue(json));
  }

  @Override
  public <T> T toObject(byte[] json, Class<T> clazz) {
    return ExceptionUtils.doUnchecked(() -> objectMapper.readValue(json, clazz));
  }

  @Override
  public String fromObject(Object o) {
    return ExceptionUtils.doUnchecked(() -> objectMapper.writer().writeValueAsString(o));
  }

  @Override
  public byte[] fromObjectToBytes(Object o) {
    return ExceptionUtils.doUnchecked(() -> objectMapper.writeValueAsBytes(o));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> toList(String json, TypeReference<List<T>> typeRef) {
    return ExceptionUtils.doUnchecked(() -> objectMapper.readValue(json, typeRef));
  }

  @Override
  public <T> List<T> toList(byte[] json, TypeReference<List<T>> typeRef) {
    return ExceptionUtils.doUnchecked(() -> objectMapper.readValue(json, typeRef));
  }
}
