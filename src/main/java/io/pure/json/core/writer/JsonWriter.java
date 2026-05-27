package io.pure.json.core.writer;

import java.math.BigDecimal;

import io.pure.json.core.model.JsonArray;
import io.pure.json.core.model.JsonBoolean;
import io.pure.json.core.model.JsonNull;
import io.pure.json.core.model.JsonNumber;
import io.pure.json.core.model.JsonObject;
import io.pure.json.core.model.JsonString;
import io.pure.json.core.model.JsonValue;

public final class JsonWriter {
    private final boolean prettyPrint;
    private final String indent;

    public JsonWriter() {
        this(false, "  ");
    }

    public JsonWriter(boolean prettyPrint, String indent) {
        this.prettyPrint = prettyPrint;
        this.indent = indent;
    }

    public String write(JsonValue value) {
        StringBuilder builder = new StringBuilder();
        writeValue(value, builder, 0);
        return builder.toString();
    }

    private void writeValue(JsonValue value, StringBuilder builder, int level) {
        switch (value) {
            case JsonObject object -> writeObject(object, builder, level);
            case JsonArray array -> writeArray(array, builder, level);
            case JsonString string -> writeString(string.value(), builder);
            case JsonNumber number -> writeNumber(number.value(), builder);
            case JsonBoolean bool -> builder.append(bool.value());
            case JsonNull ignored -> builder.append("null");
        }
    }

    private void writeObject(JsonObject object, StringBuilder builder, int level) {
        builder.append('{');
        if (object.size() == 0) {
            builder.append('}');
            return;
        }
        boolean first = true;
        for (var entry : object.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            newlineAndIndent(builder, level + 1);
            writeString(entry.getKey(), builder);
            builder.append(prettyPrint ? ": " : ":");
            writeValue(entry.getValue(), builder, level + 1);
            first = false;
        }
        newlineAndIndent(builder, level);
        builder.append('}');
    }

    private void writeArray(JsonArray array, StringBuilder builder, int level) {
        builder.append('[');
        if (array.size() == 0) {
            builder.append(']');
            return;
        }
        for (int i = 0; i < array.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            newlineAndIndent(builder, level + 1);
            writeValue(array.get(i), builder, level + 1);
        }
        newlineAndIndent(builder, level);
        builder.append(']');
    }

    private void writeString(String value, StringBuilder builder) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch <= 0x1F) {
                        appendHexEscape(ch, builder);
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        builder.append('"');
    }

    private void writeNumber(String value, StringBuilder builder) {
        if ("NaN".equals(value) || "Infinity".equals(value) || "-Infinity".equals(value)) {
            throw new JsonWriteException("Non-finite numbers are not allowed in JSON");
        }
        try {
            new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new JsonWriteException("Invalid JSON number: " + value);
        }
        builder.append(value);
    }

    private void newlineAndIndent(StringBuilder builder, int level) {
        if (!prettyPrint) {
            return;
        }
        builder.append('\n');
        builder.append(indent.repeat(level));
    }

    private void appendHexEscape(char ch, StringBuilder builder) {
        builder.append("\\u");
        builder.append(toHex((ch >> 12) & 0xF));
        builder.append(toHex((ch >> 8) & 0xF));
        builder.append(toHex((ch >> 4) & 0xF));
        builder.append(toHex(ch & 0xF));
    }

    private char toHex(int value) {
        return (char) (value < 10 ? '0' + value : 'A' + (value - 10));
    }
}
