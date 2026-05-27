package io.pure.json.binding.mapper;

public final class JsonMappingException extends RuntimeException {
    public JsonMappingException(String message) {
        super(message);
    }

    public JsonMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
