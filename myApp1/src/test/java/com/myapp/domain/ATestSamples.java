package com.myapp.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class ATestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static A getASample1() {
        return new A().id(1L);
    }

    public static A getASample2() {
        return new A().id(2L);
    }

    public static A getARandomSampleGenerator() {
        return new A().id(longCount.incrementAndGet());
    }
}
