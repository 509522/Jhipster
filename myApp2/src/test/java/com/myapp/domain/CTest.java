package com.myapp.domain;

import static com.myapp.domain.CTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(C.class);
        C c1 = getCSample1();
        C c2 = new C();
        assertThat(c1).isNotEqualTo(c2);

        c2.setId(c1.getId());
        assertThat(c1).isEqualTo(c2);

        c2 = getCSample2();
        assertThat(c1).isNotEqualTo(c2);
    }

    @Test
    void hashCodeVerifier() {
        C c = new C();
        assertThat(c.hashCode()).isZero();

        C c1 = getCSample1();
        c.setId(c1.getId());
        assertThat(c).hasSameHashCodeAs(c1);
    }
}
