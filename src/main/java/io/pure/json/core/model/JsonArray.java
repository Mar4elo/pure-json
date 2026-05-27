package io.pure.json.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class JsonArray implements JsonValue, Iterable<JsonValue> {
    private final ArrayList<JsonValue> values;

    public JsonArray() {
        this.values = new ArrayList<>();
    }

    public JsonArray(List<JsonValue> values) {
        this.values = new ArrayList<>(values);
    }

    public void add(JsonValue value) {
        values.add(value);
    }

    public JsonValue get(int index) {
        return values.get(index);
    }

    public int size() {
        return values.size();
    }

    public List<JsonValue> asList() {
        return Collections.unmodifiableList(values);
    }

    @Override
    public Iterator<JsonValue> iterator() {
        return asList().iterator();
    }
}
