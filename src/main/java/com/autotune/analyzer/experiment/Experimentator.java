package com.autotune.analyzer.experiment;

import java.util.HashMap;

/**
 * Registry for Kruize experiments
 */
public class Experimentator {
    public static HashMap<String, KruizeExperiment> experimentsMap = new HashMap<>();

    // Private constructor to prevent instantiation
    private Experimentator() {
    }
}
