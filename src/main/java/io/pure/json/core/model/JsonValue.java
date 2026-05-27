package io.pure.json.core.model;

public sealed interface JsonValue permits JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean, JsonNull {
    default boolean isObject() {
        return this instanceof JsonObject;
    }

    default boolean isArray() {
        return this instanceof JsonArray;
    }

    default boolean isString() {
        return this instanceof JsonString;
    }

    default boolean isNumber() {
        return this instanceof JsonNumber;
    }

    default boolean isBoolean() {
        return this instanceof JsonBoolean;
    }

    default boolean isNull() {
        return this == JsonNull.INSTANCE;
    }
}
