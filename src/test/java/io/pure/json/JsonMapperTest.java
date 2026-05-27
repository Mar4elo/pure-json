package io.pure.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.pure.json.annotation.JsonCreator;
import io.pure.json.annotation.JsonIgnore;
import io.pure.json.annotation.JsonInclude;
import io.pure.json.annotation.JsonProperty;
import io.pure.json.annotation.JsonValue;
import io.pure.json.binding.mapper.JsonMapper;
import io.pure.json.binding.mapper.JsonMappingException;
import io.pure.json.binding.mapper.JsonTypeRef;
import io.pure.json.core.model.JsonObject;

class JsonMapperTest {
    private final JsonMapper mapper = JsonMapper.createDefault();

    @Test
    void serializesAndDeserializesRecord() {
        User user = new User("Ann", 30, List.of("admin", "editor"));
        String json = mapper.writeValueAsString(user);
        User roundTrip = mapper.readValue(json, User.class);
        assertEquals(user, roundTrip);
    }

    @Test
    void serializesPojoWithAnnotations() {
        Account account = new Account("A-1", "secret", new BigDecimal("12.50"));
        String json = mapper.writeValueAsString(account);
        assertEquals("{\"account_id\":\"A-1\",\"balance\":12.50}", json);
    }

    @Test
    void deserializesPojoWithJsonCreator() {
        Person person = mapper.readValue("{\"first_name\":\"Ada\",\"last_name\":\"Lovelace\"}", Person.class);
        assertEquals("Ada", person.firstName());
        assertEquals("Lovelace", person.lastName());
    }

    @Test
    void supportsGenericCollections() {
        List<User> users = mapper.readValue(
            "[{\"name\":\"Ann\",\"age\":30,\"roles\":[\"admin\"]}]",
            new JsonTypeRef<List<User>>() {
            }
        );
        assertEquals(1, users.size());
        assertEquals("Ann", users.getFirst().name());
    }

    @Test
    void supportsMapsAndOptionalValues() {
        JsonMapper localMapper = JsonMapper.builder().writeNulls(false).build();
        Settings settings = new Settings(Map.of("theme", "light"), Optional.empty());
        String json = localMapper.writeValueAsString(settings);
        Settings roundTrip = localMapper.readValue(json, Settings.class);
        assertTrue(roundTrip.flags().containsKey("theme"));
        assertFalse(roundTrip.description().isPresent());
    }

    @Test
    void reportsUnknownProperties() {
        JsonMappingException exception = assertThrows(
            JsonMappingException.class,
            () -> mapper.readValue("{\"name\":\"Ann\",\"age\":30,\"roles\":[],\"extra\":1}", User.class)
        );
        assertTrue(exception.getMessage().contains("Unknown property 'extra'"));
    }

    @Test
    void rejectsNonFiniteNumbersOnWrite() {
        assertThrows(JsonMappingException.class, () -> mapper.writeValueAsString(Map.of("value", Double.NaN)));
    }

    @Test
    void canReadJsonTreeDirectly() {
        JsonObject object = mapper.readValue("{\"a\":1}", JsonObject.class);
        assertInstanceOf(JsonObject.class, object);
        assertTrue(object.containsKey("a"));
    }

    @Test
    void canReadAndWriteNestedMapTree() {
        Object tree = mapper.readTree("{\"user\":{\"name\":\"Ann\",\"roles\":[\"admin\",{\"scope\":\"all\"}]}}");
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) tree;
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) root.get("user");
        assertEquals("Ann", user.get("name"));

        String json = mapper.writeTree(tree);
        assertEquals("{\"user\":{\"name\":\"Ann\",\"roles\":[\"admin\",{\"scope\":\"all\"}]}}", json);
    }

    @Test
    void rejectsCyclicPojoGraph() {
        Node node = new Node();
        node.next = node;
        JsonMappingException exception = assertThrows(JsonMappingException.class, () -> mapper.writeValueAsString(node));
        assertTrue(exception.getMessage().contains("Cyclic references"));
    }

    record User(String name, int age, List<String> roles) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static final class Account {
        @JsonProperty("account_id")
        private final String id;
        @JsonIgnore
        private final String secret;
        private final BigDecimal balance;

        Account(String id, String secret, BigDecimal balance) {
            this.id = id;
            this.secret = secret;
            this.balance = balance;
        }
    }

    static final class Person {
        private final String firstName;
        private final String lastName;

        @JsonCreator
        Person(@JsonProperty("first_name") String firstName, @JsonProperty("last_name") String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        String firstName() {
            return firstName;
        }

        String lastName() {
            return lastName;
        }
    }

    record Settings(Map<String, String> flags, Optional<String> description) {
    }

    enum Status {
        ACTIVE,
        DISABLED
    }

    static final class AsValue {
        private final Status status;

        AsValue(Status status) {
            this.status = status;
        }

        @JsonValue
        public String raw() {
            return status.name().toLowerCase();
        }
    }

    static final class Node {
        Node next;
    }
}
