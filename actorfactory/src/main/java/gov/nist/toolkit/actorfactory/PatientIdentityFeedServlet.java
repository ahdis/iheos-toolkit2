package gov.nist.toolkit.actorfactory;

import gov.nist.toolkit.actortransaction.shared.ActorType;
import gov.nist.toolkit.adt.ListenerFactory;
import gov.nist.toolkit.configDatatypes.server.SimulatorProperties;
import gov.nist.toolkit.installation.server.Installation;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.simcommon.client.NoSimException;
import gov.nist.toolkit.simcommon.client.SimId;
import gov.nist.toolkit.simcommon.client.SimulatorConfig;
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement;
import gov.nist.toolkit.simcommon.server.GenericSimulatorFactory;
import gov.nist.toolkit.simcommon.server.SimDb;
import gov.nist.toolkit.xdsexception.client.ToolkitRuntimeException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.util.List;

/**
 * Setup and teardown Patient Identity Feed listeners
 * Created by bill on 9/8/15.
 */
public class PatientIdentityFeedServlet extends HttpServlet {
    static Logger logger = Logger.getLogger(PatientIdentityFeedServlet.class.getName());
//    File warHome;
//    File simDbDir;

    private static final long serialVersionUID = 1L;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
//        warHome = new File(config.getServletContext().getRealPath("/"));
//        logger.info("PatientIdentityFeedServlet ...warHome is " + warHome);
        initPatientIdentityFeed();
    }

    public void initPatientIdentityFeed() {
        logger.info("Initializing AdtServlet");
        try {
//            Installation.instance().warHome(warHome);

            logger.info("Initializing ADT Listeners...");

            // Initialize available port range
            List<String> portRange = Installation.instance().propertyServiceManager().getListenerPortRange();
            logger.info("Port range is " + portRange);
            int from = Integer.parseInt(portRange.get(0));
            int to = Integer.parseInt(portRange.get(1));

            ListenerFactory.init(from, to);

//            generateCurrentlyConfiguredListeners();
        } catch (ToolkitRuntimeException e) {
            logger.log(Level.SEVERE, "Cannot start listeners: ", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot start listeners: ", e);
        }

        logger.info("AdtServlet initialized");
    }



    public static void generateCurrentlyConfiguredListeners() throws IOException, NoSimException, ClassNotFoundException {
        for (TestSession testSession : Installation.instance().getTestSessions()) {
            List<SimId> simIds = new SimDb().getSimulatorIdsforActorType(ActorType.REGISTRY, testSession);
            generateListeners(simIds);
        }
    }

    public static void terminateCurrentlyConfiguredListeners() throws IOException, NoSimException {
        for (TestSession testSession : Installation.instance().getTestSessions()) {
            List<SimId> simIds = new SimDb().getSimulatorIdsforActorType(ActorType.REGISTRY, testSession);
            for (SimId simId : simIds)
                ListenerFactory.terminate(simId.toString());
        }
    }

    public static void generateListeners(List<SimId> simIds) throws NoSimException, IOException, ClassNotFoundException {
        for (SimId simId : simIds) {
            generateListener(simId);
        }
    }

    // returns port
    public static int generateListener(SimId simId) {
        try {
            return generateListener(GenericSimulatorFactory.loadSimulator(simId, false));
        } catch (Exception e) {
            throw new ToolkitRuntimeException("Error generating PIF Listener", e);
        }
    }

    public static int generateListener(SimulatorConfig simulatorConfig) {
        SimId simId = simulatorConfig.getId();
        String portString = portFromSimulatorConfig(simulatorConfig);
        if (portString == null) return 0;
        int port = Integer.parseInt(portString);
        logger.info("Create V2 PIF listener for " + simId + " on port " + port);
        ListenerFactory.generateListener(simId.toString(), port, new PifHandler());
        return port;
    }

    public static void deleteListener(SimulatorConfig simulatorConfig) {
        SimId simId = simulatorConfig.getId();
        ListenerFactory.terminate(simId.toString());
    }

    static String portFromSimulatorConfig(SimulatorConfig simulatorConfig) {
        SimulatorConfigElement sce = simulatorConfig.get(SimulatorProperties.PIF_PORT);
        SimId simId = simulatorConfig.getId();
        if (sce == null) return null;
//            throw new ToolkitRuntimeException("Simulator " + simId + " is a Registry simulator but has no Patient ID Feed port configured");
        String portString = sce.asString();
        if (portString == null || portString.equals(""))
            throw new ToolkitRuntimeException("Simulator " + simId + " is a Registry simulator but has no Patient ID Feed port configured");
        return portString;
    }


    public void destroy() {
        try {
            terminateCurrentlyConfiguredListeners();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot terminate listeners: ", e);
        }
    }

}
