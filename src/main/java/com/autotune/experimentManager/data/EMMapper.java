package com.autotune.experimentManager.data;

import com.autotune.common.experiments.ExperimentTrial;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class EMMapper {
    private static volatile EMMapper emMapper = null;
    private volatile ConcurrentHashMap<String, ExperimentTrialData> emMap = null;
    private volatile ConcurrentHashMap<String, LinkedList<String>> emDeploymentRunIdMap = null;

    /**
     * The  key for the emExpTrialMap is `experiment name`
     * The key for the map which is maintained at a experiment level is the `trial number`
     */
    private volatile ConcurrentHashMap<String, HashMap<String, ExperimentTrial>> emExpTrialMap = null;

    private EMMapper() {
        emMap = new ConcurrentHashMap<String, ExperimentTrialData>();
        emDeploymentRunIdMap = new ConcurrentHashMap<String, LinkedList<String>>();
        emExpTrialMap = new ConcurrentHashMap<String, HashMap<String, ExperimentTrial>>();
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

    public ConcurrentHashMap getExpTrialMap() { return emExpTrialMap; }
}
