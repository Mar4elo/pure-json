package io.pure.json.core.parser;

import io.pure.json.core.lexer.JsonLexer;
import io.pure.json.core.lexer.JsonToken;
import io.pure.json.core.lexer.JsonTokenType;
import io.pure.json.core.model.JsonArray;
import io.pure.json.core.model.JsonBoolean;
import io.pure.json.core.model.JsonNull;
import io.pure.json.core.model.JsonNumber;
import io.pure.json.core.model.JsonObject;
import io.pure.json.core.model.JsonString;
import io.pure.json.core.model.JsonValue;

public final class JsonParser {
    private final JsonLexer lexer;
    private final DuplicateKeyStrategy duplicateKeyStrategy;
    private JsonToken current;

    public JsonParser(String text) {
        this(text, DuplicateKeyStrategy.FAIL);
    }

    public JsonParser(String text, DuplicateKeyStrategy duplicateKeyStrategy) {
        this.lexer = new JsonLexer(text);
        this.duplicateKeyStrategy = duplicateKeyStrategy;
        this.current = lexer.nextToken();
    }

    public JsonValue parse() {
        JsonValue value = parseValue();
        expect(JsonTokenType.EOF);
        return value;
    }

    private JsonValue parseValue() {
        return switch (current.type()) {
            case BEGIN_OBJECT -> parseObject();
            case BEGIN_ARRAY -> parseArray();
            case STRING -> consumeString();
            case NUMBER -> consumeNumber();
            case TRUE -> consumeBoolean(true);
            case FALSE -> consumeBoolean(false);
            case NULL -> consumeNull();
            default -> throw error("Expected JSON value");
        };
    }

    private JsonObject parseObject() {
        expect(JsonTokenType.BEGIN_OBJECT);
        JsonObject object = new JsonObject();
        if (current.type() == JsonTokenType.END_OBJECT) {
            advance();
            return object;
        }
        while (true) {
            JsonToken keyToken = expect(JsonTokenType.STRING);
            expect(JsonTokenType.COLON);
            JsonValue value = parseValue();
            putObjectValue(object, keyToken.lexeme(), value, keyToken.position());
            if (current.type() == JsonTokenType.COMMA) {
                advance();
                continue;
            }
            expect(JsonTokenType.END_OBJECT);
            return object;
        }
    }

    private JsonArray parseArray() {
        expect(JsonTokenType.BEGIN_ARRAY);
        JsonArray array = new JsonArray();
        if (current.type() == JsonTokenType.END_ARRAY) {
            advance();
            return array;
        }
        while (true) {
            array.add(parseValue());
            if (current.type() == JsonTokenType.COMMA) {
                advance();
                continue;
            }
            expect(JsonTokenType.END_ARRAY);
            return array;
        }
    }

    private JsonValue consumeString() {
        JsonToken token = expect(JsonTokenType.STRING);
        return new JsonString(token.lexeme());
    }

    private JsonValue consumeNumber() {
        JsonToken token = expect(JsonTokenType.NUMBER);
        return new JsonNumber(token.lexeme());
    }

    private JsonValue consumeBoolean(boolean value) {
        expect(value ? JsonTokenType.TRUE : JsonTokenType.FALSE);
        return JsonBoolean.of(value);
    }

    private JsonValue consumeNull() {
        expect(JsonTokenType.NULL);
        return JsonNull.INSTANCE;
    }

    private void putObjectValue(JsonObject object, String key, JsonValue value, int position) {
        boolean exists = object.containsKey(key);
        if (!exists || duplicateKeyStrategy == DuplicateKeyStrategy.LAST_WINS) {
            object.put(key, value);
            return;
        }
        if (duplicateKeyStrategy == DuplicateKeyStrategy.FIRST_WINS) {
            return;
        }
        throw new JsonParseException("Duplicate key '" + key + "' at position " + position);
    }

    private JsonToken expect(JsonTokenType expected) {
        if (current.type() != expected) {
            throw error("Expected " + expected + " but got " + current.type());
        }
        JsonToken token = current;
        advance();
        return token;
    }

    private void advance() {
        current = lexer.nextToken();
    }

    private JsonParseException error(String message) {
        return new JsonParseException(message + " at position " + current.position());
    }
}
