package io.pure.json.binding.introspect;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public record BeanDescriptor(
    Class<?> type,
    Map<String, BeanProperty> properties,
    CreatorDescriptor creator,
    Method jsonValueMethod
) {
}
