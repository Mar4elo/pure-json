package io.pure.json.core;

import java.util.Map;

import io.pure.json.core.model.JsonValue;
import io.pure.json.core.parser.DuplicateKeyStrategy;
import io.pure.json.core.parser.JsonParser;
import io.pure.json.core.writer.JsonWriter;

public final class Json {
    private Json() {
    }

    public static JsonValue parse(String text) {
        return new JsonParser(text).parse();
    }

    public static JsonValue parse(String text, DuplicateKeyStrategy duplicateKeyStrategy) {
        return new JsonParser(text, duplicateKeyStrategy).parse();
    }

    public static Object parseTree(String text) {
        return JsonTree.fromJsonValue(parse(text));
    }

    public static Map<String, Object> parseObjectTree(String text) {
        return JsonTree.asObject(parseTree(text));
    }

    public static String stringify(JsonValue value) {
        return new JsonWriter().write(value);
    }

    public static String stringify(JsonValue value, boolean prettyPrint) {
        return new JsonWriter(prettyPrint, "  ").write(value);
    }

    public static String stringifyTree(Object tree) {
        return stringify(JsonTree.toJsonValue(tree));
    }

    public static String stringifyTree(Object tree, boolean prettyPrint) {
        return stringify(JsonTree.toJsonValue(tree), prettyPrint);
    }
}
