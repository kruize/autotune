/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This builder class helps in retrying specific task using exponential backoff approach.
 */
public final class ExponentialBackOff {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExponentialBackOff.class);
    // Maximum total time in Millis to complete exponential backoff approach. Default set to 10sec
    public static long defaultMaxElapsedTimeMillis = 10 * 1000;   //1o Seconds
    // Default time to wait for each retry
    public static long defaultTimeToWait = 1000;
    // increases the back off period for each retry attempt using a randomization function that grows exponentially.
    // randomized_interval = retryIntervalMillis * (random value in range [1 - randomization_factor, 1 + randomization_factor])
    public static double defaultRandomizationFactor = 0.5;
    // Some task need initial sleep before proceedings . defaults to zero.
    public static long defaultInitialIntervalMillis = 0;
    // Set retry after backoff exhausted with MaxElapsedTimeMillis , default is set to zero that means no retries.
    private final int numberOfRetries;
    // Some task need initial sleep before proceedings .
    private final long initialIntervalMillis;
    // Maximum total time in Millis to complete exponential backoff approach.
    private final long maxElapsedTimeMillis;
    private final Random random = new Random();
    //  increases the back off period for each retry attempt using a randomization function that grows exponentially.
    private final double randomizationFactor;
    private int numberOfTriesLeft;
    private double retryIntervalMillis;
    private double randomizedIntervalMillis;
    private double totalRetryIntervalMillis = 0;


    public ExponentialBackOff(Builder builder) {
        this.numberOfRetries = builder.numberOfRetries;
        this.numberOfTriesLeft = builder.numberOfRetries;
        this.initialIntervalMillis = (builder.initialIntervalMillis > 0) ? builder.initialIntervalMillis : defaultInitialIntervalMillis;
        this.maxElapsedTimeMillis = (builder.maxElapsedTimeMillis > 0) ? builder.maxElapsedTimeMillis : defaultMaxElapsedTimeMillis;
        this.randomizationFactor = (builder.randomizationFactor > 0) ? builder.randomizationFactor : defaultRandomizationFactor;
        this.retryIntervalMillis = (builder.retryIntervalMillis > 0) ? builder.retryIntervalMillis : defaultTimeToWait;
    }

    public boolean shouldRetry() {
        boolean shouldRetry = false;
        if (this.totalRetryIntervalMillis <= this.maxElapsedTimeMillis)
            shouldRetry = true;
        else if (numberOfRetries > 0) {
            if (numberOfTriesLeft > 0) {
                this.totalRetryIntervalMillis = 0;
                numberOfTriesLeft -= 1;
                shouldRetry = true;
            }
        }
        return shouldRetry;
    }

    public void validateBackoff() {
        waitUntilNextTry();
        //randomizationFactor
        double origin = 1 - randomizationFactor;
        double bound = 1 + randomizationFactor;
        double rand = ThreadLocalRandom.current().nextDouble(origin, bound);
        this.randomizedIntervalMillis = this.retryIntervalMillis * rand;
        this.totalRetryIntervalMillis += this.randomizedIntervalMillis;
    }

    public void waitBeforeFirstTry() {
        try {
            if ((initialIntervalMillis > 0) && (numberOfTriesLeft == numberOfRetries)) {
                LOGGER.debug("Sleeping for {} Millis before first attempt", initialIntervalMillis);
                Thread.sleep(initialIntervalMillis);
            } else {
                LOGGER.debug("No Initial wait period is set");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void waitUntilNextTry() {
        try {
            Thread.sleep((long) this.randomizedIntervalMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public double getRetryIntervalMillis() {
        return retryIntervalMillis;
    }

    public void doNotRetry() {
        numberOfTriesLeft = 0;
        this.totalRetryIntervalMillis = this.maxElapsedTimeMillis + 1;
    }

    public void reset() {
        this.numberOfTriesLeft = numberOfRetries;
        this.retryIntervalMillis = defaultTimeToWait;
    }

    public long getMaxElapsedTimeMillis() {
        return maxElapsedTimeMillis;
    }

    public double getTotalRetryIntervalMillis() {
        return totalRetryIntervalMillis;
    }

    public static class Builder {
        private int numberOfRetries;
        private int numberOfTriesLeft;
        private int initialIntervalMillis;
        private int maxElapsedTimeMillis;
        private double randomizationFactor;
        private long retryIntervalMillis;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder setNumberOfRetries(int numberOfRetries) {
            this.numberOfRetries = numberOfRetries;
            this.numberOfTriesLeft = numberOfRetries;
            return this;
        }

        public Builder setInitialIntervalMillis(int initialIntervalMillis) {
            this.initialIntervalMillis = initialIntervalMillis;
            return this;
        }

        public Builder setMaxElapsedTimeMillis(int maxElapsedTimeMillis) {
            this.maxElapsedTimeMillis = maxElapsedTimeMillis;
            return this;
        }

        public Builder setRandomizationFactor(double randomizationFactor) {
            this.randomizationFactor = randomizationFactor;
            return this;
        }

        public Builder setRetryIntervalMillis(long retryIntervalMillis) {
            this.retryIntervalMillis = retryIntervalMillis;
            return this;
        }

        public ExponentialBackOff build() {
            return new ExponentialBackOff(this);
        }
    }

}
