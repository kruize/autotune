package com.autotune.recommendation_manager;

import java.util.Map;

public class Config
{
	String layer;
	String configName;
	/**
	 * Key is name of the resource configures. Ex: requests, limits, envs etc.
	 * Value is a key-value pair of tunables and its recommended values.
	 */
	Map<String, Map<String, String>> tunablesConfig;
}
