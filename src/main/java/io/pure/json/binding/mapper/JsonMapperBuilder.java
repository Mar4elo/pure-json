package io.pure.json.binding.mapper;

import io.pure.json.annotation.JsonInclude;

public final class JsonMapperBuilder {
    private boolean failOnUnknownProperties = true;
    private boolean failOnDuplicateKeys = true;
    private boolean writeNulls = true;
    private boolean prettyPrint = false;
    private JsonInclude.Include defaultInclusion = JsonInclude.Include.ALWAYS;

    JsonMapperBuilder() {
    }

    public JsonMapperBuilder failOnUnknownProperties(boolean value) {
        this.failOnUnknownProperties = value;
        return this;
    }

    public JsonMapperBuilder failOnDuplicateKeys(boolean value) {
        this.failOnDuplicateKeys = value;
        return this;
    }

    public JsonMapperBuilder writeNulls(boolean value) {
        this.writeNulls = value;
        return this;
    }

    public JsonMapperBuilder prettyPrint(boolean value) {
        this.prettyPrint = value;
        return this;
    }

    public JsonMapperBuilder defaultInclusion(JsonInclude.Include value) {
        this.defaultInclusion = value;
        return this;
    }

    public JsonMapper build() {
        return new JsonMapper(new JsonMapperConfig(
            failOnUnknownProperties,
            failOnDuplicateKeys,
            writeNulls,
            prettyPrint,
            defaultInclusion
        ));
    }
}
