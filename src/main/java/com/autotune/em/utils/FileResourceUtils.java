package com.autotune.em.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class FileResourceUtils {
	
	
    public static JSONObject getFileFromResourceAsStream(Class cls, String fileName) {

        ClassLoader classLoader = cls.getClassLoader();
        InputStream instr = classLoader.getResourceAsStream(fileName);
        
        if (instr == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
        	String inputJsonStr = new BufferedReader(new InputStreamReader(instr, StandardCharsets.UTF_8))
            		.lines()
            		.collect(Collectors.joining("\n"));
            
            return new JSONObject(inputJsonStr);
        }

    }
}
