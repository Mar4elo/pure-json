package io.pure.json.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.pure.json.binding.mapper.JsonMappingException;
import io.pure.json.core.model.JsonArray;
import io.pure.json.core.model.JsonBoolean;
import io.pure.json.core.model.JsonNull;
import io.pure.json.core.model.JsonNumber;
import io.pure.json.core.model.JsonObject;
import io.pure.json.core.model.JsonString;
import io.pure.json.core.model.JsonValue;

public final class JsonTree {
    private JsonTree() {
    }

    public static Object fromJsonValue(JsonValue value) {
        return switch (value) {
            case JsonObject object -> toMap(object);
            case JsonArray array -> toList(array);
            case JsonString string -> string.value();
            case JsonNumber number -> new BigDecimal(number.value());
            case JsonBoolean bool -> bool.value();
            case JsonNull ignored -> null;
        };
    }

    public static JsonValue toJsonValue(Object value) {
        return toJsonValue(value, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static JsonValue toJsonValue(Object value, java.util.Set<Object> visited) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof JsonValue jsonValue) {
            return jsonValue;
        }
        if (value instanceof Map<?, ?> map) {
            ensureNoCycle(value, visited);
            JsonObject object = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new JsonMappingException("JSON object tree requires String keys in Map");
                }
                object.put(key, toJsonValue(entry.getValue(), visited));
            }
            visited.remove(value);
            return object;
        }
        if (value instanceof List<?> list) {
            ensureNoCycle(value, visited);
            JsonArray array = new JsonArray();
            for (Object item : list) {
                array.add(toJsonValue(item, visited));
            }
            visited.remove(value);
            return array;
        }
        if (value instanceof String string) {
            return new JsonString(string);
        }
        if (value instanceof Character character) {
            return new JsonString(String.valueOf(character));
        }
        if (value instanceof Boolean bool) {
            return JsonBoolean.of(bool);
        }
        if (value instanceof BigDecimal decimal) {
            return new JsonNumber(decimal.toString());
        }
        if (value instanceof Number number) {
            if (number instanceof Double doubleValue && !Double.isFinite(doubleValue)) {
                throw new JsonMappingException("Non-finite numbers are not allowed in JSON");
            }
            if (number instanceof Float floatValue && !Float.isFinite(floatValue)) {
                throw new JsonMappingException("Non-finite numbers are not allowed in JSON");
            }
            return new JsonNumber(number.toString());
        }
        throw new JsonMappingException("Unsupported JSON tree value type: " + value.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new JsonMappingException("Expected JSON object tree");
        }
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object value) {
        if (!(value instanceof List<?> list)) {
            throw new JsonMappingException("Expected JSON array tree");
        }
        return (List<Object>) list;
    }

    private static Map<String, Object> toMap(JsonObject object) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonValue> entry : object.entrySet()) {
            result.put(entry.getKey(), fromJsonValue(entry.getValue()));
        }
        return result;
    }

    private static List<Object> toList(JsonArray array) {
        ArrayList<Object> result = new ArrayList<>(array.size());
        for (JsonValue item : array) {
            result.add(fromJsonValue(item));
        }
        return result;
    }

    private static void ensureNoCycle(Object value, java.util.Set<Object> visited) {
        if (!visited.add(value)) {
            throw new JsonMappingException("Cyclic references are not supported in JSON tree serialization");
        }
    }
}
