package io.pure.json.binding.mapper;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.pure.json.annotation.JsonInclude;
import io.pure.json.binding.adapter.TypeAdapter;
import io.pure.json.binding.introspect.BeanDescriptor;
import io.pure.json.binding.introspect.BeanProperty;
import io.pure.json.binding.introspect.ClassIntrospector;
import io.pure.json.binding.introspect.CreatorDescriptor;
import io.pure.json.binding.reflect.Types;
import io.pure.json.core.Json;
import io.pure.json.core.JsonTree;
import io.pure.json.core.model.JsonArray;
import io.pure.json.core.model.JsonBoolean;
import io.pure.json.core.model.JsonNull;
import io.pure.json.core.model.JsonNumber;
import io.pure.json.core.model.JsonObject;
import io.pure.json.core.model.JsonString;
import io.pure.json.core.model.JsonValue;

public final class JsonMapper {
    private final JsonMapperConfig config;
    private final ClassIntrospector introspector = new ClassIntrospector();
    private final ConcurrentHashMap<Type, TypeAdapter<?>> adapters = new ConcurrentHashMap<>();

    public JsonMapper() {
        this(JsonMapperConfig.defaults());
    }

    JsonMapper(JsonMapperConfig config) {
        this.config = config;
    }

    public static JsonMapper createDefault() {
        return new JsonMapper();
    }

    public static JsonMapperBuilder builder() {
        return new JsonMapperBuilder();
    }

    public <T> void registerAdapter(Type type, TypeAdapter<T> adapter) {
        adapters.put(type, adapter);
    }

    public String writeValueAsString(Object value) {
        return Json.stringify(toJsonValue(value), config.prettyPrint());
    }

    public JsonValue toJsonValue(Object value) {
        return toJsonValueInternal(value, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    public Object readTree(String json) {
        return JsonTree.fromJsonValue(Json.parse(json, config.duplicateKeyStrategy()));
    }

    public Map<String, Object> readObjectTree(String json) {
        return JsonTree.asObject(readTree(json));
    }

    public String writeTree(Object tree) {
        return Json.stringify(JsonTree.toJsonValue(tree), config.prettyPrint());
    }

    public <T> T readValue(String json, Class<T> type) {
        return readValue(Json.parse(json, config.duplicateKeyStrategy()), type);
    }

    public <T> T readValue(String json, JsonTypeRef<T> typeRef) {
        return readValue(Json.parse(json, config.duplicateKeyStrategy()), typeRef.getType());
    }

    public <T> T readValue(JsonValue value, Class<T> type) {
        return type.cast(readValue(value, (Type) type));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonValue value, Type type) {
        return (T) readValueInternal(value, type, "$");
    }

    JsonMapperConfig config() {
        return config;
    }

    @SuppressWarnings("unchecked")
    private JsonValue toJsonValueInternal(Object value) {
        return toJsonValueInternal(value, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    @SuppressWarnings("unchecked")
    private JsonValue toJsonValueInternal(Object value, java.util.Set<Object> visited) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }

        TypeAdapter<Object> adapter = (TypeAdapter<Object>) adapters.get(value.getClass());
        if (adapter != null) {
            return adapter.toJson(value, this);
        }

        if (value instanceof JsonValue jsonValue) {
            return jsonValue;
        }
        if (value instanceof String string) {
            return new JsonString(string);
        }
        if (value instanceof Character character) {
            return new JsonString(String.valueOf(character));
        }
        if (value instanceof Boolean bool) {
            return JsonBoolean.of(bool);
        }
        if (value instanceof Enum<?> enumValue) {
            return new JsonString(enumValue.name());
        }
        if (value instanceof Number number) {
            return new JsonNumber(numberToJson(number));
        }
        if (value instanceof Optional<?> optional) {
            return optional.map(item -> toJsonValueInternal(item, visited)).orElse(JsonNull.INSTANCE);
        }
        if (value instanceof Map<?, ?> map) {
            ensureNoCycle(value, visited);
            try {
                return mapToJson(map, visited);
            } finally {
                visited.remove(value);
            }
        }
        if (value instanceof Collection<?> collection) {
            ensureNoCycle(value, visited);
            try {
                return collectionToJson(collection, visited);
            } finally {
                visited.remove(value);
            }
        }
        if (value.getClass().isArray()) {
            ensureNoCycle(value, visited);
            try {
                return arrayToJson(value, visited);
            } finally {
                visited.remove(value);
            }
        }

        BeanDescriptor descriptor = introspector.describe(value.getClass());
        if (descriptor.jsonValueMethod() != null) {
            try {
                return toJsonValueInternal(descriptor.jsonValueMethod().invoke(value), visited);
            } catch (ReflectiveOperationException exception) {
                throw new JsonMappingException("Cannot invoke @JsonValue for " + value.getClass().getName(), exception);
            }
        }

        ensureNoCycle(value, visited);
        JsonObject object = new JsonObject();
        try {
            JsonInclude.Include defaultInclusion = config.defaultInclusion();
            for (BeanProperty property : descriptor.properties().values()) {
                Object propertyValue = property.accessor().get(value);
                JsonInclude.Include inclusion = property.include() == JsonInclude.Include.ALWAYS ? defaultInclusion : property.include();
                if (!config.writeNulls() && propertyValue == null) {
                    continue;
                }
                if (inclusion == JsonInclude.Include.NON_NULL && propertyValue == null) {
                    continue;
                }
                object.put(property.jsonName(), toJsonValueInternal(propertyValue, visited));
            }
            return object;
        } finally {
            visited.remove(value);
        }
    }

    private JsonObject mapToJson(Map<?, ?> map, java.util.Set<Object> visited) {
        JsonObject object = new JsonObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new JsonMappingException("Only Map<String, ?> is supported for JSON objects");
            }
            object.put(key, toJsonValueInternal(entry.getValue(), visited));
        }
        return object;
    }

    private JsonArray collectionToJson(Collection<?> collection, java.util.Set<Object> visited) {
        JsonArray array = new JsonArray();
        for (Object item : collection) {
            array.add(toJsonValueInternal(item, visited));
        }
        return array;
    }

    private JsonArray arrayToJson(Object array, java.util.Set<Object> visited) {
        JsonArray jsonArray = new JsonArray();
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            jsonArray.add(toJsonValueInternal(Array.get(array, i), visited));
        }
        return jsonArray;
    }

    private String numberToJson(Number number) {
        if (number instanceof Double value) {
            ensureFinite(value);
            return BigDecimal.valueOf(value).stripTrailingZeros().toString();
        }
        if (number instanceof Float value) {
            ensureFinite(value);
            return BigDecimal.valueOf(value.doubleValue()).stripTrailingZeros().toString();
        }
        if (number instanceof BigDecimal decimal) {
            return decimal.toString();
        }
        if (number instanceof BigInteger integer) {
            return integer.toString();
        }
        return number.toString();
    }

    private static void ensureFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new JsonMappingException("Non-finite numbers are not allowed in JSON");
        }
    }

    @SuppressWarnings("unchecked")
    private Object readValueInternal(JsonValue value, Type type, String path) {
        Objects.requireNonNull(type, "type");

        TypeAdapter<Object> adapter = (TypeAdapter<Object>) adapters.get(type);
        if (adapter != null) {
            return adapter.fromJson(value, type, this, path);
        }

        Class<?> rawClass = Types.rawClass(type);
        if (value == JsonNull.INSTANCE) {
            if (rawClass.isPrimitive()) {
                throw new JsonMappingException("Cannot assign null to primitive at " + path);
            }
            if (Optional.class.isAssignableFrom(rawClass)) {
                return Optional.empty();
            }
            return null;
        }

        if (rawClass == JsonValue.class || JsonValue.class.isAssignableFrom(rawClass)) {
            return value;
        }
        if (rawClass == String.class) {
            return expectString(value, path);
        }
        if (rawClass == char.class || rawClass == Character.class) {
            String string = expectString(value, path);
            if (string.length() != 1) {
                throw new JsonMappingException("Expected single character string at " + path);
            }
            return string.charAt(0);
        }
        if (rawClass == boolean.class || rawClass == Boolean.class) {
            return expectBoolean(value, path);
        }
        if (rawClass.isEnum()) {
            return readEnum(value, rawClass, path);
        }
        if (Number.class.isAssignableFrom(rawClass) || rawClass.isPrimitive() && rawClass != boolean.class && rawClass != char.class) {
            return readNumber(value, rawClass, path);
        }
        if (Optional.class.isAssignableFrom(rawClass)) {
            Type nestedType = ((ParameterizedType) type).getActualTypeArguments()[0];
            return Optional.ofNullable(readValueInternal(value, nestedType, path));
        }
        if (Map.class.isAssignableFrom(rawClass)) {
            return readMap(value, type, path);
        }
        if (Collection.class.isAssignableFrom(rawClass)) {
            return readCollection(value, type, rawClass, path);
        }
        if (rawClass.isArray()) {
            return readArray(value, rawClass.componentType(), path);
        }

        return readBean(value, rawClass, path);
    }

    private Object readBean(JsonValue value, Class<?> rawClass, String path) {
        if (!(value instanceof JsonObject object)) {
            throw new JsonMappingException("Expected object at " + path);
        }

        BeanDescriptor descriptor = introspector.describe(rawClass);
        if (descriptor.creator() == null) {
            throw new JsonMappingException("Type " + rawClass.getName() + " must declare no-args constructor or @JsonCreator constructor");
        }
        if (descriptor.creator().parameters().isEmpty()) {
            Object instance = descriptor.creator().invoker().apply(new Object[0]);
            for (Map.Entry<String, JsonValue> entry : object.entrySet()) {
                BeanProperty property = descriptor.properties().get(entry.getKey());
                if (property == null) {
                    handleUnknownProperty(entry.getKey(), path);
                    continue;
                }
                if (property.mutator() == null) {
                    continue;
                }
                Object propertyValue = readValueInternal(entry.getValue(), property.type(), path + "." + property.jsonName());
                property.mutator().set(instance, propertyValue);
            }
            return instance;
        }

        Object[] args = new Object[descriptor.creator().parameters().size()];
        for (int i = 0; i < descriptor.creator().parameters().size(); i++) {
            CreatorDescriptor.CreatorParameter parameter = descriptor.creator().parameters().get(i);
            JsonValue item = object.get(parameter.jsonName());
            args[i] = readValueInternal(item == null ? JsonNull.INSTANCE : item, parameter.type(), path + "." + parameter.jsonName());
        }
        if (config.failOnUnknownProperties()) {
            for (String key : object.keys()) {
                boolean known = descriptor.creator().parameters().stream().anyMatch(parameter -> parameter.jsonName().equals(key))
                    || descriptor.properties().containsKey(key);
                if (!known) {
                    handleUnknownProperty(key, path);
                }
            }
        }
        Object instance = descriptor.creator().invoker().apply(args);
        if (!rawClass.isRecord()) {
            for (Map.Entry<String, JsonValue> entry : object.entrySet()) {
                BeanProperty property = descriptor.properties().get(entry.getKey());
                if (property != null && property.mutator() != null) {
                    Object propertyValue = readValueInternal(entry.getValue(), property.type(), path + "." + property.jsonName());
                    property.mutator().set(instance, propertyValue);
                }
            }
        }
        return instance;
    }

    private void handleUnknownProperty(String key, String path) {
        if (config.failOnUnknownProperties()) {
            throw new JsonMappingException("Unknown property '" + key + "' at " + path);
        }
    }

    private Object readArray(JsonValue value, Class<?> componentType, String path) {
        JsonArray array = expectArray(value, path);
        Object result = Array.newInstance(componentType, array.size());
        for (int i = 0; i < array.size(); i++) {
            Array.set(result, i, readValueInternal(array.get(i), componentType, path + "[" + i + "]"));
        }
        return result;
    }

    private Object readCollection(JsonValue value, Type type, Class<?> rawClass, String path) {
        JsonArray array = expectArray(value, path);
        Type elementType = Object.class;
        if (type instanceof ParameterizedType parameterizedType) {
            elementType = unwrapWildcard(parameterizedType.getActualTypeArguments()[0]);
        }
        Collection<Object> result = instantiateCollection(rawClass);
        for (int i = 0; i < array.size(); i++) {
            result.add(readValueInternal(array.get(i), elementType, path + "[" + i + "]"));
        }
        return result;
    }

    private Object readMap(JsonValue value, Type type, String path) {
        JsonObject object = expectObject(value, path);
        Type mapValueType = Object.class;
        if (type instanceof ParameterizedType parameterizedType) {
            Type keyType = unwrapWildcard(parameterizedType.getActualTypeArguments()[0]);
            if (Types.rawClass(keyType) != String.class) {
                throw new JsonMappingException("Only Map<String, ?> is supported at " + path);
            }
            mapValueType = unwrapWildcard(parameterizedType.getActualTypeArguments()[1]);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonValue> entry : object.entrySet()) {
            result.put(entry.getKey(), readValueInternal(entry.getValue(), mapValueType, path + "." + entry.getKey()));
        }
        return result;
    }

    private Object readEnum(JsonValue value, Class<?> rawClass, String path) {
        String name = expectString(value, path);
        for (Object constant : rawClass.getEnumConstants()) {
            Enum<?> enumValue = (Enum<?>) constant;
            if (enumValue.name().equals(name)) {
                return enumValue;
            }
        }
        throw new JsonMappingException("Unknown enum constant '" + name + "' for " + rawClass.getName() + " at " + path);
    }

    private Object readNumber(JsonValue value, Class<?> rawClass, String path) {
        String lexical = expectNumber(value, path);
        try {
            if (rawClass == byte.class || rawClass == Byte.class) {
                return new BigDecimal(lexical).byteValueExact();
            }
            if (rawClass == short.class || rawClass == Short.class) {
                return new BigDecimal(lexical).shortValueExact();
            }
            if (rawClass == int.class || rawClass == Integer.class) {
                return new BigDecimal(lexical).intValueExact();
            }
            if (rawClass == long.class || rawClass == Long.class) {
                return new BigDecimal(lexical).longValueExact();
            }
            if (rawClass == float.class || rawClass == Float.class) {
                return new BigDecimal(lexical).floatValue();
            }
            if (rawClass == double.class || rawClass == Double.class) {
                return new BigDecimal(lexical).doubleValue();
            }
            if (rawClass == BigInteger.class) {
                return new BigDecimal(lexical).toBigIntegerExact();
            }
            if (rawClass == BigDecimal.class) {
                return new BigDecimal(lexical);
            }
        } catch (ArithmeticException exception) {
            throw new JsonMappingException("Number out of range at " + path + ": " + lexical, exception);
        }
        if (rawClass == Number.class) {
            return new BigDecimal(lexical);
        }
        throw new JsonMappingException("Unsupported numeric type " + rawClass.getName() + " at " + path);
    }

    private Collection<Object> instantiateCollection(Class<?> rawClass) {
        if (rawClass.isInterface()) {
            if (List.class.isAssignableFrom(rawClass) || Collection.class == rawClass) {
                return new ArrayList<>();
            }
            if (java.util.Set.class.isAssignableFrom(rawClass)) {
                return new LinkedHashSet<>();
            }
            throw new JsonMappingException("Unsupported collection type " + rawClass.getName());
        }
        try {
            @SuppressWarnings("unchecked")
            Collection<Object> collection = (Collection<Object>) rawClass.getDeclaredConstructor().newInstance();
            return collection;
        } catch (ReflectiveOperationException exception) {
            throw new JsonMappingException("Cannot instantiate collection type " + rawClass.getName(), exception);
        }
    }

    private JsonArray expectArray(JsonValue value, String path) {
        if (!(value instanceof JsonArray array)) {
            throw new JsonMappingException("Expected array at " + path);
        }
        return array;
    }

    private JsonObject expectObject(JsonValue value, String path) {
        if (!(value instanceof JsonObject object)) {
            throw new JsonMappingException("Expected object at " + path);
        }
        return object;
    }

    private String expectString(JsonValue value, String path) {
        if (!(value instanceof JsonString string)) {
            throw new JsonMappingException("Expected string at " + path);
        }
        return string.value();
    }

    private String expectNumber(JsonValue value, String path) {
        if (!(value instanceof JsonNumber number)) {
            throw new JsonMappingException("Expected number at " + path);
        }
        return number.value();
    }

    private boolean expectBoolean(JsonValue value, String path) {
        if (!(value instanceof JsonBoolean bool)) {
            throw new JsonMappingException("Expected boolean at " + path);
        }
        return bool.value();
    }

    private Type unwrapWildcard(Type type) {
        if (type instanceof WildcardType wildcardType) {
            return wildcardType.getUpperBounds()[0];
        }
        return type;
    }

    private static void ensureNoCycle(Object value, java.util.Set<Object> visited) {
        if (!visited.add(value)) {
            throw new JsonMappingException("Cyclic references are not supported during JSON serialization");
        }
    }
}
