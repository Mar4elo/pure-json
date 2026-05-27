package io.pure.json.core.lexer;

import io.pure.json.core.parser.JsonParseException;

public final class JsonLexer {
    private final String text;
    private int index;

    public JsonLexer(String text) {
        this.text = stripBom(text);
        this.index = 0;
    }

    public JsonToken nextToken() {
        skipWhitespace();
        if (index >= text.length()) {
            return new JsonToken(JsonTokenType.EOF, "", index);
        }

        int position = index;
        char ch = text.charAt(index);
        return switch (ch) {
            case '{' -> singleChar(JsonTokenType.BEGIN_OBJECT, position);
            case '}' -> singleChar(JsonTokenType.END_OBJECT, position);
            case '[' -> singleChar(JsonTokenType.BEGIN_ARRAY, position);
            case ']' -> singleChar(JsonTokenType.END_ARRAY, position);
            case ':' -> singleChar(JsonTokenType.COLON, position);
            case ',' -> singleChar(JsonTokenType.COMMA, position);
            case '"' -> readString();
            case 't' -> readLiteral("true", JsonTokenType.TRUE);
            case 'f' -> readLiteral("false", JsonTokenType.FALSE);
            case 'n' -> readLiteral("null", JsonTokenType.NULL);
            default -> {
                if (ch == '-' || isDigit(ch)) {
                    yield readNumber();
                }
                throw error("Unexpected character '" + ch + "'", position);
            }
        };
    }

    private JsonToken singleChar(JsonTokenType type, int position) {
        index++;
        return new JsonToken(type, "", position);
    }

    private JsonToken readLiteral(String literal, JsonTokenType type) {
        int position = index;
        if (!text.startsWith(literal, index)) {
            throw error("Unexpected token", position);
        }
        index += literal.length();
        return new JsonToken(type, literal, position);
    }

    private JsonToken readString() {
        int start = index++;
        StringBuilder builder = new StringBuilder();
        while (index < text.length()) {
            char ch = text.charAt(index++);
            if (ch == '"') {
                return new JsonToken(JsonTokenType.STRING, builder.toString(), start);
            }
            if (ch == '\\') {
                if (index >= text.length()) {
                    throw error("Unterminated escape sequence", start);
                }
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> builder.append(escaped);
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> builder.append(readUnicodeEscape(start));
                    default -> throw error("Invalid escape sequence \\" + escaped, index - 1);
                }
                continue;
            }
            if (ch <= 0x1F) {
                throw error("Control characters must be escaped", index - 1);
            }
            builder.append(ch);
        }
        throw error("Unterminated string", start);
    }

    private String readUnicodeEscape(int stringStart) {
        if (index + 4 > text.length()) {
            throw error("Incomplete unicode escape", stringStart);
        }
        int codeUnit = 0;
        for (int i = 0; i < 4; i++) {
            char ch = text.charAt(index++);
            if (!isHex(ch)) {
                throw error("Invalid unicode escape", index - 1);
            }
            codeUnit = (codeUnit << 4) | hexValue(ch);
        }
        char first = (char) codeUnit;
        if (Character.isHighSurrogate(first)) {
            if (index + 6 > text.length() || text.charAt(index) != '\\' || text.charAt(index + 1) != 'u') {
                throw error("High surrogate must be followed by low surrogate", index);
            }
            index += 2;
            int lowCodeUnit = 0;
            for (int i = 0; i < 4; i++) {
                char ch = text.charAt(index++);
                if (!isHex(ch)) {
                    throw error("Invalid unicode escape", index - 1);
                }
                lowCodeUnit = (lowCodeUnit << 4) | hexValue(ch);
            }
            char second = (char) lowCodeUnit;
            if (!Character.isLowSurrogate(second)) {
                throw error("Invalid low surrogate", index - 1);
            }
            return new String(new char[]{first, second});
        }
        if (Character.isLowSurrogate(first)) {
            throw error("Unexpected low surrogate without leading high surrogate", index - 1);
        }
        return String.valueOf(first);
    }

    private JsonToken readNumber() {
        int start = index;
        if (peek('-')) {
            index++;
        }
        readIntPart(start);
        if (peek('.')) {
            index++;
            if (!hasDigit()) {
                throw error("Fraction part must contain digits", index);
            }
            while (hasDigit()) {
                index++;
            }
        }
        if (peek('e') || peek('E')) {
            index++;
            if (peek('+') || peek('-')) {
                index++;
            }
            if (!hasDigit()) {
                throw error("Exponent part must contain digits", index);
            }
            while (hasDigit()) {
                index++;
            }
        }
        return new JsonToken(JsonTokenType.NUMBER, text.substring(start, index), start);
    }

    private void readIntPart(int start) {
        if (peek('0')) {
            index++;
            if (hasDigit()) {
                throw error("Leading zeroes are not allowed", start);
            }
            return;
        }
        if (!hasDigitOneToNine()) {
            throw error("Invalid number", start);
        }
        index++;
        while (hasDigit()) {
            index++;
        }
    }

    private void skipWhitespace() {
        while (index < text.length()) {
            char ch = text.charAt(index);
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                index++;
            } else {
                return;
            }
        }
    }

    private boolean peek(char ch) {
        return index < text.length() && text.charAt(index) == ch;
    }

    private boolean hasDigit() {
        return index < text.length() && isDigit(text.charAt(index));
    }

    private boolean hasDigitOneToNine() {
        return index < text.length() && text.charAt(index) >= '1' && text.charAt(index) <= '9';
    }

    private boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private boolean isHex(char ch) {
        return (ch >= '0' && ch <= '9')
            || (ch >= 'a' && ch <= 'f')
            || (ch >= 'A' && ch <= 'F');
    }

    private int hexValue(char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'a' && ch <= 'f') {
            return 10 + (ch - 'a');
        }
        return 10 + (ch - 'A');
    }

    private JsonParseException error(String message, int position) {
        return new JsonParseException(message + " at position " + position);
    }

    private static String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }
}
