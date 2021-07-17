package gov.nist.toolkit.simcommon.server.factories

import gov.nist.toolkit.actortransaction.shared.ActorType
import gov.nist.toolkit.actortransaction.client.ParamType
import gov.nist.toolkit.configDatatypes.client.TransactionType
import gov.nist.toolkit.configDatatypes.server.SimulatorProperties
import gov.nist.toolkit.simcommon.client.SimId
import gov.nist.toolkit.simcommon.client.Simulator
import gov.nist.toolkit.simcommon.client.SimulatorConfig
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement
import gov.nist.toolkit.simcommon.server.AbstractActorFactory
import gov.nist.toolkit.simcommon.server.IActorFactory
import gov.nist.toolkit.simcommon.server.SimManager
import gov.nist.toolkit.sitemanagement.client.Site
import gov.nist.toolkit.sitemanagement.client.TransactionBean
import gov.nist.toolkit.xdsexception.NoSimulatorException
import groovy.transform.TypeChecked

/**
 * A singleton factory for creating Sim Proxy
 *
 * This actually creates a pair of simulators.  First, using the SimId provided,
 * is the sim that receives the input message.  The second simulator sends the
 * message on to the configured endpoint.
 *
 * That is the theory, it is not coded that way.  The first simulator does all the work
 * and has all the configuration information.  The second simulator is just the container
 * for the logs holding the messages to the configured endpoint.
 *
 * Runtime is handled by the reverse proxy code base which runs on a different port. The
 * calls to addFixedFhirEndpoint below configure the port number for this service.
 */
@TypeChecked
class SimProxyFactory extends AbstractActorFactory implements IActorFactory{
    static final List<TransactionType> incomingTransactions = TransactionType.asList()  // accepts all known transactions

    SimProxyFactory() {}

    @Override
    protected Simulator buildNew(SimManager simm, SimId simId, String environment, boolean configureBase) throws Exception {
        ActorType actorType = getActorType(); //ActorType.SIM_PROXY
        SimulatorConfig config
        if (configureBase)
            config = configureBaseElements(actorType, simId, simId.testSession, environment)
        else
            config = new SimulatorConfig()

        SimId simId2 = new SimId(simId.testSession, simId.id + '_be')   // 'be' for back end
        SimulatorConfig config2
        if (configureBase)
            config2 = configureBaseElements(actorType, simId2, simId2.testSession, environment)
        else
            config2 = new SimulatorConfig()

        addFixedConfig(config, SimulatorProperties.isProxyFrontEnd, ParamType.BOOLEAN, true)
        addFixedConfig(config2, SimulatorProperties.isProxyFrontEnd, ParamType.BOOLEAN, false)

        actorType.transactions.each { TransactionType transactionType ->
            /*
            if (transactionType.isFhir()) {
                if (transactionType.endpointSimPropertyName)
                    addFixedFhirEndpoint(config, transactionType.endpointSimPropertyName, actorType, transactionType, false, true)
//                addFixedFhirEndpoint(config, transactionType.tlsEndpointSimPropertyName, actorType, transactionType, true)
            } else {

             */
                if (transactionType.endpointSimPropertyName) {
                    addFixedEndpoint(config, transactionType.endpointSimPropertyName, actorType, transactionType, false)
                    addFixedEndpoint(config, transactionType.tlsEndpointSimPropertyName, actorType, transactionType, true)
                }
//            }
        }

        // link the two sims making up the front end and the back end of the simproxy
        addFixedConfig(config, SimulatorProperties.proxyPartner, ParamType.SELECTION, simId2.toString())
        addFixedConfig(config2, SimulatorProperties.proxyPartner, ParamType.SELECTION, simId.toString())
        addEditableConfig(config, SimulatorProperties.simProxyTransformations, ParamType.LIST, actorType.proxyTransforms)
//        addEditableConfig(config, SimulatorProperties.simProxyResponseTransformations, ParamType.LIST, actorType.proxyResponseTransformClassNames)

        List<SimulatorConfig> configs = buildExtensions(simm, config, config2)
//        addEditableConfig(config, SimulatorProperties.proxyForwardSite, ParamType.SELECTION, "");

        isSimProxy = true;

        return new Simulator(configs)
    }

    // This is separate so it can be overriden by an extension class
    List<SimulatorConfig> buildExtensions(SimManager simm, SimulatorConfig config, SimulatorConfig config2) {
        addEditableConfig(config, SimulatorProperties.proxyForwardSite, ParamType.SELECTION, "");
        return [config, config2]
    }

    @Override
    protected void verifyActorConfigurationOptions(SimulatorConfig config) {

    }

    // this only works because this is a singleton class
    private boolean locked = false;

    /**
     *
     * @param asc
     * @param site
     * @return
     * @throws NoSimulatorException
     */
    @Override
    Site buildActorSite(SimulatorConfig asc, Site site) throws NoSimulatorException {
        Site aSite = (site) ? site : new Site(asc.id.toString(), asc.id.testSession)

        if (!asc.get(SimulatorProperties.isProxyFrontEnd).asBoolean())
            return aSite  // back end gets no transactions

        boolean isAsync = false

        ActorType actorType = ActorType.findActor(asc.actorType)
        actorType.transactions.each { TransactionType transactionType ->
            asc.elements.findAll { SimulatorConfigElement sce ->
                sce.type == ParamType.ENDPOINT && sce.transType == transactionType
            }.each { SimulatorConfigElement sce ->
                aSite.addTransaction(new TransactionBean(
                        transactionType.getCode(),
                        TransactionBean.RepositoryType.NONE,
                        sce.asString(),   // endpoint
                        false,
                        isAsync
                ))
            }
        }

        return aSite
    }

    @Override
    List<TransactionType> getIncomingTransactions() {
        return incomingTransactions
    }
}
