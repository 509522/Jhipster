package com.myapp.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class CTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static C getCSample1() {
        return new C().id(1L);
    }

    public static C getCSample2() {
        return new C().id(2L);
    }

    public static C getCRandomSampleGenerator() {
        return new C().id(longCount.incrementAndGet());
    }
}
