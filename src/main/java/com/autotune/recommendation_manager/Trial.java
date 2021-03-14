package com.autotune.recommendation_manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Trial
{
	String id;
	int trialNumber;
	String trialRun;
	String deploymentName;

	ArrayList<Config> updateConfig = new ArrayList<>();
	Map<String, Query> queries = new HashMap<>();
}
