package com.autotune.experimentManager;

import com.autotune.experimentManager.core.ExperimentManager;
import com.autotune.experimentManager.utils.EMConstants;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class EMMain {

    public static void main(String[] args) {
        ServletContextHandler context = null;

        disableServerLogging();

        Server server = new Server(EMConstants.APIPaths.PORT);
        context = new ServletContextHandler();
        context.setContextPath(EMConstants.APIPaths.ROOT);
        server.setHandler(context);


        ExperimentManager.launch(context);

        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void disableServerLogging() {
        /* The jetty server creates a lot of server log messages that are unnecessary.
         * This disables jetty logging. */
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
    }
}
