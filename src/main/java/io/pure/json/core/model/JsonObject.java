package io.pure.json.core.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class JsonObject implements JsonValue {
    private final LinkedHashMap<String, JsonValue> values;

    public JsonObject() {
        this.values = new LinkedHashMap<>();
    }

    public JsonObject(Map<String, JsonValue> values) {
        this.values = new LinkedHashMap<>(values);
    }

    public JsonValue put(String key, JsonValue value) {
        return values.put(key, value);
    }

    public JsonValue get(String key) {
        return values.get(key);
    }

    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    public int size() {
        return values.size();
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(values.keySet());
    }

    public Set<Map.Entry<String, JsonValue>> entrySet() {
        return Collections.unmodifiableSet(values.entrySet());
    }

    public Collection<JsonValue> values() {
        return Collections.unmodifiableCollection(values.values());
    }

    public Map<String, JsonValue> asMap() {
        return Collections.unmodifiableMap(values);
    }
}
