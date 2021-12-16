package gov.nist.toolkit.services.server.orchestration;

import gov.nist.toolkit.actortransaction.shared.ActorType;
import gov.nist.toolkit.actortransaction.client.ParamType;
import gov.nist.toolkit.configDatatypes.server.SimulatorProperties;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.services.client.IdsOrchestrationRequest;
import gov.nist.toolkit.services.client.IdsOrchestrationResponse;
import gov.nist.toolkit.services.client.RawResponse;
import gov.nist.toolkit.services.server.RawResponseBuilder;
import gov.nist.toolkit.services.server.ToolkitApi;
import gov.nist.toolkit.session.server.Session;
import gov.nist.toolkit.simcommon.client.SimId;
import gov.nist.toolkit.simcommon.client.SimulatorConfig;
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement;
import java.util.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
* Build environment for testing Imaging Document Source SUT.
*/

class IdsOrchestrationBuilder extends AbstractOrchestrationBuilder {
   static Logger log = Logger.getLogger(IdsOrchestrationBuilder.class.getName());

   static final String sutSimulatorName = "simulator_ids";
   public static final String rrSimulatorName = "rr";
   
    Session session;
    IdsOrchestrationRequest request;
    ToolkitApi api;
    Util util;
    List<SimulatorConfig> simConfigs = new ArrayList<>();
    SimulatorConfig sutSimulatorConfig = null;
    SimulatorConfig rrSimulatorConfig = null;

    public IdsOrchestrationBuilder(ToolkitApi api, Session session, IdsOrchestrationRequest request) {
        super(session, request);
        this.api = api;
        this.session = session;
        this.request = request;
        this.util = new Util(api);
    }
        
        public RawResponse buildTestEnvironment() {
           
           String env = request.getEnvironmentName();
           TestSession testSession = request.getTestSession();
           
           try {
              for (Orchestra sim : Orchestra.values()) {
                 SimulatorConfig simConfig = null;
                 SimId simId = new SimId(testSession, sim.name(), sim.actorType.getName(), env);
                 if (!request.isUseExistingSimulator() || !api.simulatorExists(simId)) {
                 api.deleteSimulatorIfItExists(simId);
                 log.fine("Creating " + simId.toString());
                 simConfig = api.createSimulator(simId).getConfig(0);
                 // plug our special parameter values
                 for (SimulatorConfigElement chg : sim.elements) {
                    chg.setEditable(true);
                    // lists of simulators may need specific user plugged in
                    if (chg.hasList()) {
                          List <String> list = chg.asList();
                          for (int i = 0; i < list.size(); i++ ) {
                             String s = list.get(i);
                             s = StringUtils.replace(s, "${user}", testSession.getValue());
                             list.set(i, s);
                          }
                          chg.setStringListValue(list);
                    }
                    simConfig.replace(chg);
                 }
                 api.saveSimulator(simConfig);
                 } else {
                    simConfig = api.getConfig(simId);
                 }
                 simConfigs.add(simConfig);
                 if (sim.name().equals(sutSimulatorName)) sutSimulatorConfig = simConfig;
                 if (sim.name().equals(rrSimulatorName)) rrSimulatorConfig = simConfig;
              }
              IdsOrchestrationResponse response = new IdsOrchestrationResponse();
              response.setIdsSimulatorConfig(sutSimulatorConfig);
              response.setRRConfig(rrSimulatorConfig);
              response.setSimulatorConfigs(simConfigs);
              return response;

           } catch (Exception e) {
              return RawResponseBuilder.build(e);
           }
        } // EO build test environment
        
        
        public enum Orchestra {
           
           simulator_ids ("Simulated IDS SUT", ActorType.IMAGING_DOC_SOURCE, new SimulatorConfigElement[] {
              new SimulatorConfigElement(SimulatorProperties.idsRepositoryUniqueId, ParamType.TEXT, "1.3.6.1.4.1.21367.13.80.110"),
              new SimulatorConfigElement(SimulatorProperties.idsImageCache, ParamType.TEXT, "ids-repository")}),
           
           rr ("Repository Registry", ActorType.REPOSITORY_REGISTRY, new SimulatorConfigElement[] {
              new SimulatorConfigElement(SimulatorProperties.VALIDATE_AGAINST_PATIENT_IDENTITY_FEED, ParamType.BOOLEAN, false),
              new SimulatorConfigElement(SimulatorProperties.repositoryUniqueId, ParamType.TEXT, "1.3.6.1.4.1.21367.13.71.101.1")}),
           
           idc ("Imaging Document Consumer", ActorType.IMAGING_DOC_CONSUMER, new SimulatorConfigElement[] {
           });      
           
           public final String title;
           public final ActorType actorType;
           public final SimulatorConfigElement[] elements;      
           
           Orchestra (String title, ActorType actorType, SimulatorConfigElement[] elements) {
              this.title = title;
              this.actorType = actorType;
              this.elements = elements;
           }
           
           public ActorType getActorType() {
              return actorType;
           }
           public SimulatorConfigElement[] getElements() {
              return elements;
           }
           public String[] getDisplayProps() {
              switch (actorType) {
                 case IMAGING_DOC_SOURCE:
                    return new String[] {
                       SimulatorProperties.idsRepositoryUniqueId,
                       SimulatorProperties.idsrEndpoint,
                       SimulatorProperties.idsImageCache,
                    };
                 case REPOSITORY_REGISTRY:
                    return new String[] {
                       SimulatorProperties.pnrEndpoint,
                       SimulatorProperties.retrieveEndpoint,
                       SimulatorProperties.updateEndpoint,
                       SimulatorProperties.storedQueryEndpoint,
                       SimulatorProperties.repositoryUniqueId,
                    };
                 case IMAGING_DOC_CONSUMER:
                    return new String[] {
                       SimulatorProperties.retrieveEndpoint,
                       SimulatorProperties.storedQueryEndpoint,
                    };
                    default:
                       log.severe("Unknown ActorType");
              }
              return new String[0];
           }
           
        } // EO Orchestra enum
        

     } // EO IigOrchestration builder
