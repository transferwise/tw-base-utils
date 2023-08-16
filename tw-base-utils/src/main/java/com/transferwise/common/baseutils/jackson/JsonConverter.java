package com.transferwise.common.baseutils.jackson;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

public interface JsonConverter {

  <T> T toObject(String json, Class<T> clazz);

  <T> T toObject(byte[] json, Class<T> clazz);

  String fromObject(Object o);

  byte[] fromObjectToBytes(Object o);

  <T> List<T> toList(String json, TypeReference<List<T>> typeRef);

  <T> List<T> toList(byte[] json, TypeReference<List<T>> typeRef);
}
