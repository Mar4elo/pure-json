package io.pure.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.pure.json.binding.mapper.JsonMappingException;
import io.pure.json.core.Json;
import io.pure.json.core.model.JsonBoolean;
import io.pure.json.core.model.JsonNull;
import io.pure.json.core.model.JsonArray;
import io.pure.json.core.model.JsonNumber;
import io.pure.json.core.model.JsonObject;
import io.pure.json.core.model.JsonString;
import io.pure.json.core.parser.DuplicateKeyStrategy;
import io.pure.json.core.parser.JsonParseException;

class JsonCoreTest {
    @Test
    void parsesTopLevelValues() {
        assertInstanceOf(JsonString.class, Json.parse("\"hello\""));
        assertInstanceOf(JsonNumber.class, Json.parse("42"));
        assertInstanceOf(JsonBoolean.class, Json.parse("true"));
        assertInstanceOf(JsonNull.class, Json.parse("null"));
        assertInstanceOf(JsonArray.class, Json.parse("[1,2,3]"));
        assertInstanceOf(JsonObject.class, Json.parse("{\"a\":1}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0",
        "-0",
        "1",
        "-1",
        "10",
        "1234567890",
        "0.1",
        "-0.1",
        "1.0",
        "1e1",
        "1E1",
        "1e+1",
        "1e-1",
        "-12.34e+56"
    })
    void parsesValidNumbers(String json) {
        JsonNumber value = (JsonNumber) Json.parse(json);
        assertEquals(json, value.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "00",
        "01",
        "-01",
        ".1",
        "1.",
        "1e",
        "1e+",
        "1e-",
        "--1",
        "+1",
        "NaN",
        "Infinity",
        "-Infinity"
    })
    void rejectsInvalidNumbers(String json) {
        assertThrows(JsonParseException.class, () -> Json.parse(json));
    }

    @Test
    void rejectsLeadingZero() {
        assertThrows(JsonParseException.class, () -> Json.parse("01"));
    }

    @Test
    void parsesUnicodeSurrogatePair() {
        JsonString value = (JsonString) Json.parse("\"\\uD834\\uDD1E\"");
        assertEquals("\uD834\uDD1E", value.value());
    }

    @Test
    void parsesEscapedStrings() {
        JsonString escaped = (JsonString) Json.parse("\"\\\\\\\"\\/\\b\\f\\n\\r\\t\"");
        assertEquals("\\\"/\b\f\n\r\t", escaped.value());

        JsonString simple = (JsonString) Json.parse("\"simple\"");
        assertEquals("simple", simple.value());

        JsonString empty = (JsonString) Json.parse("\"\"");
        assertEquals("", empty.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\"\\uD834\"",
        "\"\\uDD1E\"",
        "\"\\uD834x\"",
        "\"\\uZZZZ\"",
        "\"\\x\"",
        "\"line\nbreak\""
    })
    void rejectsInvalidStrings(String json) {
        assertThrows(JsonParseException.class, () -> Json.parse(json));
    }

    @Test
    void rejectsDuplicateKeysByDefault() {
        assertThrows(JsonParseException.class, () -> Json.parse("{\"a\":1,\"a\":2}"));
    }

    @Test
    void allowsDuplicateKeysWithLastWinsStrategy() {
        JsonObject value = (JsonObject) Json.parse("{\"a\":1,\"a\":2}", DuplicateKeyStrategy.LAST_WINS);
        assertEquals("2", ((JsonNumber) value.get("a")).value());
    }

    @Test
    void allowsDuplicateKeysWithFirstWinsStrategy() {
        JsonObject value = (JsonObject) Json.parse("{\"a\":1,\"a\":2}", DuplicateKeyStrategy.FIRST_WINS);
        assertEquals("1", ((JsonNumber) value.get("a")).value());
    }

    @Test
    void ignoresBomWhenParsing() {
        JsonObject value = (JsonObject) Json.parse("\uFEFF{\"ok\":true}");
        assertTrue(value.containsKey("ok"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        " { \"a\" : [ true , false , null ] } ",
        "\n\t{\"a\":1,\r\"b\":2}\n",
        "[ 1 , 2 , 3 ]"
    })
    void acceptsInsignificantWhitespace(String json) {
        assertTrue(Json.parse(json) != null);
    }

    @Test
    void parsesEmptyStructures() {
        JsonObject object = (JsonObject) Json.parse("{}");
        JsonArray array = (JsonArray) Json.parse("[]");
        assertEquals(0, object.size());
        assertEquals(0, array.size());
    }

    @Test
    void stringifiesEscapedValues() {
        JsonObject object = new JsonObject();
        object.put("text", new JsonString("line\n\"quoted\""));
        assertEquals("{\"text\":\"line\\n\\\"quoted\\\"\"}", Json.stringify(object));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{,}",
        "{\"a\"}",
        "{\"a\",1}",
        "{\"a\":1,}",
        "{\"a\":}",
        "[,]",
        "[1,]",
        "[,1]",
        "[1 2]",
        "{\"a\":1 \"b\":2}",
        "truE",
        "nul",
        "\"unterminated",
        "{\"a\":1}]"
    })
    void rejectsInvalidJsonSyntax(String json) {
        assertThrows(JsonParseException.class, () -> Json.parse(json));
    }

    @Test
    void parsesIntoNestedMapRepresentation() {
        Map<String, Object> tree = Json.parseObjectTree("{\"a\":{\"b\":[1,true,null,\"x\"]}}");
        Map<String, Object> nested = castMap(tree.get("a"));
        List<Object> values = castList(nested.get("b"));
        assertEquals(new BigDecimal("1"), values.get(0));
        assertEquals(true, values.get(1));
        assertEquals(null, values.get(2));
        assertEquals("x", values.get(3));
    }

    @Test
    void stringifiesNestedMapRepresentation() {
        List<Object> items = new ArrayList<>();
        items.add("x");
        items.add(true);
        items.add(null);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("version", new BigDecimal("1"));
        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("meta", meta);
        tree.put("items", items);
        assertEquals("{\"meta\":{\"version\":1},\"items\":[\"x\",true,null]}", Json.stringifyTree(tree));
    }

    @Test
    void rejectsCyclicMapTree() {
        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("self", tree);
        assertThrows(JsonMappingException.class, () -> Json.stringifyTree(tree));
    }

    @Test
    void roundTripsRfcStyleCompositeDocument() {
        String json = """
            {
              "array": [1, -2.5, 3e4, true, false, null, "text", {"nested":"value"}],
              "object": {"emptyObject": {}, "emptyArray": []},
              "unicode": "\\uD834\\uDD1E"
            }
            """;
        assertEquals(
            "{\"array\":[1,-2.5,3e4,true,false,null,\"text\",{\"nested\":\"value\"}],\"object\":{\"emptyObject\":{},\"emptyArray\":[]},\"unicode\":\"𝄞\"}",
            Json.stringify(Json.parse(json))
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castList(Object value) {
        return (List<Object>) value;
    }
}
