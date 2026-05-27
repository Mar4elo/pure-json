# io.pure:json

A lightweight JSON library for Java 21 with:

- RFC-oriented parsing and writing
- Java object marshalling and unmarshalling
- a typed JSON tree model
- a plain nested `Map<String, Object>` / `List<Object>` representation

This project was built as a compact, dependency-free implementation of JSON processing focused on clarity, correctness, and practical extensibility.

## Why this project

Most Java teams already know large JSON libraries such as Jackson or Gson. This project has a different goal:

- provide a small and readable codebase
- keep the implementation close to the JSON RFC grammar
- support both low-level and high-level APIs
- offer a good educational reference for how a JSON library is structured internally

It is suitable for:

- educational use
- experiments and internal tools
- small services or libraries that prefer zero external runtime dependencies
- teams that want a transparent implementation they can fully understand and extend

## Standards and Compatibility

The implementation follows the JSON syntax defined in:

- [RFC 4627](https://www.ietf.org/rfc/rfc4627.txt)
- [RFC 7159](https://www.ietf.org/rfc/rfc7159.txt)
- [RFC 8259](https://www.ietf.org/rfc/rfc8259.txt)

Important behavioral notes:

- top-level JSON texts may be any valid JSON value, not only objects or arrays
- duplicate object member names are rejected by default
- `NaN`, `Infinity`, and `-Infinity` are not allowed
- strings support standard JSON escaping and Unicode surrogate pairs

## Features

- JSON lexer and parser based on RFC grammar
- JSON writer with compact and pretty-print output
- typed JSON tree model:
  - `JsonValue`
  - `JsonObject`
  - `JsonArray`
  - `JsonString`
  - `JsonNumber`
  - `JsonBoolean`
  - `JsonNull`
- plain Java tree representation:
  - `Map<String, Object>`
  - `List<Object>`
  - `String`
  - `BigDecimal`
  - `Boolean`
  - `null`
- object mapping for:
  - Java records
  - POJO classes
  - enums
  - arrays
  - collections
  - `Map<String, ?>`
  - `Optional`
- annotation-based mapping customization
- generic type support via `JsonTypeRef`
- custom adapters via `TypeAdapter`
- duplicate key strategy support
- cycle detection during serialization

## Project Structure

The code is organized into three main layers.

### `io.pure.json.core`

Low-level JSON processing primitives.

- lexer
- parser
- tree model
- tree conversion utilities
- writer

### `io.pure.json.binding`

Java object mapping layer.

- `JsonMapper`
- type adapters
- generic type references
- reflection-based class introspection

### `io.pure.json.annotation`

Optional annotations for mapping control.

- `@JsonProperty`
- `@JsonIgnore`
- `@JsonCreator`
- `@JsonValue`
- `@JsonInclude`

## Requirements

- JDK 21
- Maven 3.9+

## Build and Test

Run the full test suite:

```bash
mvn test
```

Package the project:

```bash
mvn package
```

## Quick Start

### 1. Parse JSON into the typed tree model

```java
import io.pure.json.core.Json;
import io.pure.json.core.model.JsonObject;
import io.pure.json.core.model.JsonString;

JsonObject root = (JsonObject) Json.parse("""
    {
      "name": "Alice",
      "active": true
    }
    """);

JsonString name = (JsonString) root.get("name");
System.out.println(name.value()); // Alice
```

### 2. Serialize the typed tree model back to JSON

```java
import io.pure.json.core.Json;
import io.pure.json.core.model.JsonArray;
import io.pure.json.core.model.JsonBoolean;
import io.pure.json.core.model.JsonNumber;
import io.pure.json.core.model.JsonObject;

JsonObject root = new JsonObject();
JsonArray values = new JsonArray();
values.add(new JsonNumber("1"));
values.add(JsonBoolean.TRUE);

root.put("values", values);

String compact = Json.stringify(root);
String pretty = Json.stringify(root, true);
```

### 3. Use plain nested `Map/List` trees

```java
import java.util.List;
import java.util.Map;

import io.pure.json.core.Json;

Map<String, Object> tree = Json.parseObjectTree("""
    {
      "user": {
        "name": "Alice",
        "roles": ["admin", "editor"]
      }
    }
    """);

Map<String, Object> user = (Map<String, Object>) tree.get("user");
List<Object> roles = (List<Object>) user.get("roles");

String json = Json.stringifyTree(tree);
```

### 4. Map JSON to Java objects

```java
import java.util.List;

import io.pure.json.binding.mapper.JsonMapper;

record User(String name, int age, List<String> roles) {}

JsonMapper mapper = JsonMapper.createDefault();

String json = """
    {
      "name": "Alice",
      "age": 30,
      "roles": ["admin", "editor"]
    }
    """;

User user = mapper.readValue(json, User.class);
String serialized = mapper.writeValueAsString(user);
```

## Usage Guide

### Typed Tree API

Use the typed tree model when you want:

- explicit JSON types
- fine-grained low-level control
- a tree representation independent of your business classes

Example:

```java
import io.pure.json.core.Json;
import io.pure.json.core.model.JsonArray;
import io.pure.json.core.model.JsonNumber;
import io.pure.json.core.model.JsonObject;

JsonObject root = (JsonObject) Json.parse("{\"items\":[1,2,3]}");
JsonArray items = (JsonArray) root.get("items");
JsonNumber first = (JsonNumber) items.get(0);

System.out.println(first.asBigDecimal()); // 1
```

### Plain Java Tree API

Use the nested `Map/List` representation when you want:

- dynamic JSON traversal
- easy interoperability with generic Java code
- no dependency on custom JSON model classes

Numbers in this mode are represented as `BigDecimal` by default.

### Object Mapping API

Use `JsonMapper` when you want:

- Java object serialization
- Java object deserialization
- generic collection mapping
- annotation-driven customization

#### Basic POJO and Record Mapping

```java
import java.math.BigDecimal;
import java.util.List;

import io.pure.json.binding.mapper.JsonMapper;

record Product(String id, String name, BigDecimal price, List<String> tags) {}

JsonMapper mapper = JsonMapper.createDefault();

Product product = new Product("p-100", "Keyboard", new BigDecimal("99.99"), List.of("tech", "peripheral"));
String json = mapper.writeValueAsString(product);
Product copy = mapper.readValue(json, Product.class);
```

#### Generic Types

```java
import java.util.List;

import io.pure.json.binding.mapper.JsonMapper;
import io.pure.json.binding.mapper.JsonTypeRef;

record User(String name, int age) {}

JsonMapper mapper = JsonMapper.createDefault();

List<User> users = mapper.readValue(
    "[{\"name\":\"Alice\",\"age\":30},{\"name\":\"Bob\",\"age\":28}]",
    new JsonTypeRef<List<User>>() {}
);
```

#### Reading and Writing Dynamic Trees with `JsonMapper`

```java
import java.util.Map;

import io.pure.json.binding.mapper.JsonMapper;

JsonMapper mapper = JsonMapper.createDefault();

Object tree = mapper.readTree("{\"meta\":{\"version\":1}}");
String json = mapper.writeTree(tree);

Map<String, Object> objectTree = mapper.readObjectTree("{\"ok\":true}");
```

## Annotation Support

### `@JsonProperty`

Rename a field, record component, or constructor parameter.

```java
import io.pure.json.annotation.JsonProperty;

record Account(
    @JsonProperty("account_id") String id,
    String owner
) {}
```

### `@JsonIgnore`

Exclude a field or accessor from mapping.

```java
import io.pure.json.annotation.JsonIgnore;

final class Credentials {
    String username;

    @JsonIgnore
    String password;
}
```

### `@JsonCreator`

Use a constructor for deserialization.

```java
import io.pure.json.annotation.JsonCreator;
import io.pure.json.annotation.JsonProperty;

final class Person {
    private final String firstName;
    private final String lastName;

    @JsonCreator
    Person(
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
```

### `@JsonValue`

Serialize an object using a single derived value.

```java
import io.pure.json.annotation.JsonValue;

final class StatusWrapper {
    private final String status;

    StatusWrapper(String status) {
        this.status = status;
    }

    @JsonValue
    public String raw() {
        return status;
    }
}
```

### `@JsonInclude`

Control null inclusion.

```java
import io.pure.json.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
final class Profile {
    String nickname;
    String bio;
}
```

## Configuration

Create a customized mapper with `JsonMapper.builder()`:

```java
import io.pure.json.annotation.JsonInclude;
import io.pure.json.binding.mapper.JsonMapper;

JsonMapper mapper = JsonMapper.builder()
    .failOnUnknownProperties(true)
    .failOnDuplicateKeys(true)
    .writeNulls(false)
    .prettyPrint(true)
    .defaultInclusion(JsonInclude.Include.ALWAYS)
    .build();
```

Available options:

- `failOnUnknownProperties(boolean)`
- `failOnDuplicateKeys(boolean)`
- `writeNulls(boolean)`
- `prettyPrint(boolean)`
- `defaultInclusion(JsonInclude.Include)`

## Duplicate Key Handling

By default, duplicate object member names are rejected because they are a known interoperability problem.

You can choose a different behavior:

```java
import io.pure.json.core.Json;
import io.pure.json.core.model.JsonObject;
import io.pure.json.core.parser.DuplicateKeyStrategy;

JsonObject firstWins = (JsonObject) Json.parse(
    "{\"a\":1,\"a\":2}",
    DuplicateKeyStrategy.FIRST_WINS
);

JsonObject lastWins = (JsonObject) Json.parse(
    "{\"a\":1,\"a\":2}",
    DuplicateKeyStrategy.LAST_WINS
);
```

## Custom Type Adapters

You can register custom serialization and deserialization logic.

```java
import java.lang.reflect.Type;
import java.time.LocalDate;

import io.pure.json.binding.adapter.TypeAdapter;
import io.pure.json.binding.mapper.JsonMapper;
import io.pure.json.core.model.JsonString;
import io.pure.json.core.model.JsonValue;

JsonMapper mapper = JsonMapper.createDefault();

mapper.registerAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
    @Override
    public JsonValue toJson(LocalDate value, JsonMapper mapper) {
        return new JsonString(value.toString());
    }

    @Override
    public LocalDate fromJson(JsonValue value, Type type, JsonMapper mapper, String path) {
        return LocalDate.parse(((JsonString) value).value());
    }
});
```

## Error Handling

The library throws runtime exceptions with path or position details where possible:

- `JsonParseException` for JSON syntax errors
- `JsonWriteException` for invalid JSON generation
- `JsonMappingException` for object mapping errors

Examples:

- invalid number syntax
- malformed escape sequences
- duplicate object keys in strict mode
- unknown properties during deserialization
- cyclic references during serialization

## Testing

The project includes an RFC-oriented test suite covering:

- valid and invalid number grammar
- valid and invalid string escaping
- Unicode surrogate pairs
- top-level scalar values
- whitespace handling
- object and array syntax errors
- duplicate key strategies
- object mapping behavior
- cycle detection
- plain tree round-trips

Run tests with:

```bash
mvn test
```

## Limitations

This project intentionally favors clarity over maximum throughput.

Current limitations:

- parsing is DOM-based rather than streaming
- deserialization from text first builds an internal JSON tree
- deeply nested JSON may still be limited by JVM stack depth
- only `Map<String, ?>` is supported for object mapping
- the plain tree representation uses `BigDecimal` for numeric values

If you need extremely high throughput, streaming APIs, or a broader ecosystem of modules, a larger production-oriented library may be a better fit.

## Design Goals

This project aims to be:

- small
- readable
- easy to debug
- easy to extend
- close to the RFC grammar
- practical for both library users and learners

## Roadmap Ideas

Possible future improvements:

- streaming parser and generator APIs
- configurable naming strategies
- deeper performance optimizations for large payloads
- more advanced date/time adapters
- benchmark suite
- publishing to Maven Central

## Contributing

Issues, suggestions, and pull requests are welcome.

If you contribute, please:

- keep the code dependency-free unless there is a strong reason not to
- preserve RFC-oriented behavior
- add or update tests for all behavior changes
- prefer readable, well-scoped changes

## License

This project is licensed under the Apache License 2.0.

See the [LICENSE](LICENSE) file for the full license text.
