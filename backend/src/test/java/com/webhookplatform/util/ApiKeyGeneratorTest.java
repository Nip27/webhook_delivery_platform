package com.webhookplatform.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ApiKeyGeneratorTest {

    @Test
    void generateKey_hasExpectedPrefix() {
        String key = ApiKeyGenerator.generateKey();
        assertThat(key).startsWith("whk_live_");
    }

    @Test
    void generateKey_isUnique() {
        String key1 = ApiKeyGenerator.generateKey();
        String key2 = ApiKeyGenerator.generateKey();
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void hashKey_isSha256Hex() {
        String hash = ApiKeyGenerator.hashKey("any-key");
        assertThat(hash).hasSize(64).matches("[a-f0-9]+");
    }

    @Test
    void hashKey_isDeterministic() {
        assertThat(ApiKeyGenerator.hashKey("key")).isEqualTo(ApiKeyGenerator.hashKey("key"));
    }

    @Test
    void extractPrefix_returnsFirst16Chars() {
        String key = "whk_live_abcdefghijklmnop";
        assertThat(ApiKeyGenerator.extractPrefix(key)).isEqualTo("whk_live_abcdefg");
    }
}
