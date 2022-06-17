package com.autotune.UserInterfaceSupport;

import com.autotune.UserInterfaceSupport.services.GetNamespaces;
import com.autotune.experimentManager.services.CreateExperimentTrial;
import com.autotune.experimentManager.services.ListTrialStatus;
import org.eclipse.jetty.servlet.ServletContextHandler;

import static com.autotune.analyzer.utils.ServerContext.*;

/**
 * The UISM (User Interface support Manager) provides the support API's and backed functionality
 * required to serve the UI components. It initialises the servlets for the API endpoints which are
 * contacted by the UI for it's data representation purposes.
 *
 * Some of the functionalities (but are not limited to) are listed here:
 *  - serve list of namespaces
 *  - serve list of deployments in a requested namespace
 *  - getting the config YAML from the frontend
 */
public class UISM {
    /**
     * Method: initializeUISM
     * Scope: A private method which needs to be called only from `start`
     * Description: This method contains the necessary functionality which is needed at the time of
     * UISM initialization
     * Status: Yet to be implemented based on the requirement
     */

    private static void initializeUISM() {
        // Need to implement any functionality required at the time of UISM initialization
        System.out.println("In Initialisation module");
    }


    /**
     * Method: start
     * Scope: A public method which needs to be called at a point where UISM needs to be started (Autotune.java -> main)
     * Description: Starts the UISM service
     *
     * @param contextHandler
     */
    public static void start(ServletContextHandler contextHandler) {
        initializeUISM();
        addUISMServlets(contextHandler);
    }

    /**
     * Method: addUISMServlets
     * Scope: A private method which needs to be called after initializing UISM service (UISM.java -> start)
     * Description: Adds the UISM related servlets to main Servlet context handler
     * @param context
     */

    private static void addUISMServlets(ServletContextHandler context) {
        context.addServlet(GetNamespaces.class,GET_NAMESPACES);
    }
}
