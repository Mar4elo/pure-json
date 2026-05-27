package io.pure.json.binding.adapter;

import java.lang.reflect.Type;

import io.pure.json.binding.mapper.JsonMapper;
import io.pure.json.core.model.JsonValue;

public interface TypeAdapter<T> {
    JsonValue toJson(T value, JsonMapper mapper);

    T fromJson(JsonValue value, Type type, JsonMapper mapper, String path);
}
