package com.autotune.analyzer.KruizeCache;

import com.autotune.analyzer.kruizeObject.KruizeObject;

import java.util.concurrent.ConcurrentHashMap;

public class KruizeCache {

    private static volatile KruizeCache instance;
    private static volatile ConcurrentHashMap<String, KruizeObject> experimentMap;

    private KruizeCache() {
        experimentMap = new ConcurrentHashMap<>();
    }

    public static KruizeCache getInstance() {
        if (instance == null) {
            synchronized (KruizeCache.class) {
                if (instance == null) {
                    instance = new KruizeCache();
                }
            }
        }
        return instance;
    }

    public void putExperiment(String experimentName, KruizeObject kruizeObject) {
        experimentMap.put(experimentName, kruizeObject);
    }

    public KruizeObject getExperiment(String experimentName) {
        return experimentMap.get(experimentName);
    }

    public boolean containsExperiment(String experimentName) {
        return experimentMap.containsKey(experimentName);
    }
}
