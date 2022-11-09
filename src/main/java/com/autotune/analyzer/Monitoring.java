package com.autotune.analyzer;

import com.autotune.analyzer.deployment.AutotuneDeployment;
import com.autotune.analyzer.services.*;
import com.autotune.utils.ServerContext;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class Monitoring {
    public static void start(ServletContextHandler contextHandler) {
        try {
            addServlets(contextHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addServlets(ServletContextHandler context) {
        context.addServlet(ListExperiments.class, ServerContext.LIST_EXPERIMENTS);
        context.addServlet(ExperimentsSummary.class, ServerContext.EXPERIMENTS_SUMMARY);
        context.addServlet(CreateExperimentAPI.class, ServerContext.CREATE_EXPERIMENT);
        context.addServlet(updateResultsAPI.class, ServerContext.UPDATE_RESULTS);
        context.addServlet(RecommendationAPI.class,ServerContext.RECOMMEND_RESULTS);
    }
}
