package io.pure.json.binding.mapper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class JsonTypeRef<T> {
    private final Type type;

    protected JsonTypeRef() {
        Type superType = getClass().getGenericSuperclass();
        if (!(superType instanceof ParameterizedType parameterizedType)) {
            throw new IllegalStateException("JsonTypeRef must be created with generic type information");
        }
        this.type = parameterizedType.getActualTypeArguments()[0];
    }

    public final Type getType() {
        return type;
    }
}
