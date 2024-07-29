package com.myapp.domain;

import static com.myapp.domain.ATestSamples.*;
import static com.myapp.domain.BTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.myapp.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ATest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(A.class);
        A a1 = getASample1();
        A a2 = new A();
        assertThat(a1).isNotEqualTo(a2);

        a2.setId(a1.getId());
        assertThat(a1).isEqualTo(a2);

        a2 = getASample2();
        assertThat(a1).isNotEqualTo(a2);
    }

    @Test
    void hashCodeVerifier() {
        A a = new A();
        assertThat(a.hashCode()).isZero();

        A a1 = getASample1();
        a.setId(a1.getId());
        assertThat(a).hasSameHashCodeAs(a1);
    }

    @Test
    void bTest() {
        A a = getARandomSampleGenerator();
        B bBack = getBRandomSampleGenerator();

        a.addB(bBack);
        assertThat(a.getBs()).containsOnly(bBack);
        assertThat(bBack.getA()).isEqualTo(a);

        a.removeB(bBack);
        assertThat(a.getBs()).doesNotContain(bBack);
        assertThat(bBack.getA()).isNull();

        a.bs(new HashSet<>(Set.of(bBack)));
        assertThat(a.getBs()).containsOnly(bBack);
        assertThat(bBack.getA()).isEqualTo(a);

        a.setBs(new HashSet<>());
        assertThat(a.getBs()).doesNotContain(bBack);
        assertThat(bBack.getA()).isNull();
    }
}
