package io.pure.json.core.lexer;

public record JsonToken(JsonTokenType type, String lexeme, int position) {
}
