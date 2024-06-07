package com.autotune.metrics;

import com.autotune.analyzer.recommendations.RecommendationNotification;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class NotificationDecorator {
    public static void addNotification(Object target, Method method, RecommendationNotification recommendationNotification) {
        // Check if the method is annotated with @LogNotification
        if (method.isAnnotationPresent(LogNotification.class)) {
            // Logging functionality before calling the original method
            System.out.println("Logging notification: " + recommendationNotification.toString());
        }

        try {
            // Invoke the method on the target object
            method.invoke(target, recommendationNotification);

            // Additional logging or functionality after the original method
            System.out.println("Notification added successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            // Handle exception if needed
        }
    }

}
