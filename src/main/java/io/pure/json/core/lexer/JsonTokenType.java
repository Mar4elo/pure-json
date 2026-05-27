package io.pure.json.core.lexer;

public enum JsonTokenType {
    BEGIN_OBJECT,
    END_OBJECT,
    BEGIN_ARRAY,
    END_ARRAY,
    COLON,
    COMMA,
    STRING,
    NUMBER,
    TRUE,
    FALSE,
    NULL,
    EOF
}
