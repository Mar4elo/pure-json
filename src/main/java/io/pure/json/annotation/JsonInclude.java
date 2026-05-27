package io.pure.json.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE, ElementType.RECORD_COMPONENT})
public @interface JsonInclude {
    Include value() default Include.ALWAYS;

    enum Include {
        ALWAYS,
        NON_NULL
    }
}
