package com.autotune.analyzer.experiment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.TreeMap;

import static com.autotune.utils.ExperimentMessages.RunExperiment.INITIAL_STATUS;

/**
 *
 */
public class Experimentator implements Runnable {
    public static HashMap<String, KruizeExperiment> experimentsMap = new HashMap<>();

    public static void start() {
        Experimentator experimentator = new Experimentator();
        Thread expThread = new Thread(experimentator);
        expThread.start();
    }

    @Override
    public void run() {

    }
}
