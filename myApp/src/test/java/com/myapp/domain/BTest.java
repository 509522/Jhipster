package com.myapp.domain;

import static com.myapp.domain.ATestSamples.*;
import static com.myapp.domain.BTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class BTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(B.class);
        B b1 = getBSample1();
        B b2 = new B();
        assertThat(b1).isNotEqualTo(b2);

        b2.setId(b1.getId());
        assertThat(b1).isEqualTo(b2);

        b2 = getBSample2();
        assertThat(b1).isNotEqualTo(b2);
    }

    @Test
    void aTest() {
        B b = getBRandomSampleGenerator();
        A aBack = getARandomSampleGenerator();

        b.setA(aBack);
        assertThat(b.getA()).isEqualTo(aBack);

        b.a(null);
        assertThat(b.getA()).isNull();
    }
}
