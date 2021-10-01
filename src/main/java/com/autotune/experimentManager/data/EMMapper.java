package com.autotune.experimentManager.data;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class EMMapper {
    private static volatile EMMapper emMapper = null;
    private volatile ConcurrentHashMap<String, ExperimentTrialData> emMap = null;
    private volatile ConcurrentHashMap<String, LinkedList<String>> emDeploymentRunIdMap = null;

    private EMMapper() {
        emMap = new ConcurrentHashMap<String, ExperimentTrialData>();
        emDeploymentRunIdMap = new ConcurrentHashMap<String, LinkedList<String>>();
    }

    public static EMMapper getInstance() {
        if (null == emMapper) {
            synchronized (EMMapper.class) {
                if (null == emMapper) {
                    emMapper = new EMMapper();
                }
            }
        }
        return emMapper;
    }

    public ConcurrentHashMap getMap() {
        return emMap;
    }

    public ConcurrentHashMap getDeploymentRunIdMap() {
        return emDeploymentRunIdMap;
    }
}
