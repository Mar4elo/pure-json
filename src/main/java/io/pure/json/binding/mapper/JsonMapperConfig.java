package io.pure.json.binding.mapper;

import io.pure.json.annotation.JsonInclude;
import io.pure.json.core.parser.DuplicateKeyStrategy;

public record JsonMapperConfig(
    boolean failOnUnknownProperties,
    boolean failOnDuplicateKeys,
    boolean writeNulls,
    boolean prettyPrint,
    JsonInclude.Include defaultInclusion
) {
    public static JsonMapperConfig defaults() {
        return new JsonMapperConfig(true, true, true, false, JsonInclude.Include.ALWAYS);
    }

    public DuplicateKeyStrategy duplicateKeyStrategy() {
        return failOnDuplicateKeys ? DuplicateKeyStrategy.FAIL : DuplicateKeyStrategy.LAST_WINS;
    }
}
