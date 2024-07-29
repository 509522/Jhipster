package com.myapp.domain;

import static com.myapp.domain.DTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(D.class);
        D d1 = getDSample1();
        D d2 = new D();
        assertThat(d1).isNotEqualTo(d2);

        d2.setId(d1.getId());
        assertThat(d1).isEqualTo(d2);

        d2 = getDSample2();
        assertThat(d1).isNotEqualTo(d2);
    }

    @Test
    void hashCodeVerifier() {
        D d = new D();
        assertThat(d.hashCode()).isZero();

        D d1 = getDSample1();
        d.setId(d1.getId());
        assertThat(d).hasSameHashCodeAs(d1);
    }
}
