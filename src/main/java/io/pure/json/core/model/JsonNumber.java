package io.pure.json.core.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public record JsonNumber(String value) implements JsonValue {
    public JsonNumber {
        Objects.requireNonNull(value, "value");
    }

    public BigDecimal asBigDecimal() {
        return new BigDecimal(value);
    }

    public BigInteger asBigInteger() {
        return new BigDecimal(value).toBigIntegerExact();
    }
}
