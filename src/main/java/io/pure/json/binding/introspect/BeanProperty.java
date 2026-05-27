package io.pure.json.binding.introspect;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;

import io.pure.json.annotation.JsonInclude;

public record BeanProperty(
    String jsonName,
    String javaName,
    Type type,
    Accessor accessor,
    Mutator mutator,
    JsonInclude.Include include
) {
    public interface Accessor {
        Object get(Object target);
    }

    public interface Mutator {
        void set(Object target, Object value);
    }
}
