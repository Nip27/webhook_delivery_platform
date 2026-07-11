package com.webhookplatform.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class HmacUtilTest {

    @Test
    void computeSignature_returnsHexStringWithPrefix() {
        String sig = HmacUtil.computeSignature("my-secret", "{\"hello\":\"world\"}");
        assertThat(sig).startsWith("sha256=");
        assertThat(sig).hasSize(7 + 64);
    }

    @Test
    void computeSignature_isDeterministic() {
        String sig1 = HmacUtil.computeSignature("secret", "payload");
        String sig2 = HmacUtil.computeSignature("secret", "payload");
        assertThat(sig1).isEqualTo(sig2);
    }

    @Test
    void computeSignature_differsBySecret() {
        String sig1 = HmacUtil.computeSignature("secret-a", "payload");
        String sig2 = HmacUtil.computeSignature("secret-b", "payload");
        assertThat(sig1).isNotEqualTo(sig2);
    }

    @Test
    void computeSignature_differsByPayload() {
        String sig1 = HmacUtil.computeSignature("secret", "payload-1");
        String sig2 = HmacUtil.computeSignature("secret", "payload-2");
        assertThat(sig1).isNotEqualTo(sig2);
    }
}
