package com.autotune.dependencyAnalyzer.util;

public class Util
{
	public static int generateID(Object object) {
		return object.toString().hashCode();
	}
}
