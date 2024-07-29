package com.myapp.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class DTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static D getDSample1() {
        return new D().id(1L);
    }

    public static D getDSample2() {
        return new D().id(2L);
    }

    public static D getDRandomSampleGenerator() {
        return new D().id(longCount.incrementAndGet());
    }
}
