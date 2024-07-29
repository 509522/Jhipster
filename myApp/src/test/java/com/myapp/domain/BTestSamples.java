package com.myapp.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class BTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static B getBSample1() {
        return new B().id(1L);
    }

    public static B getBSample2() {
        return new B().id(2L);
    }

    public static B getBRandomSampleGenerator() {
        return new B().id(longCount.incrementAndGet());
    }
}
