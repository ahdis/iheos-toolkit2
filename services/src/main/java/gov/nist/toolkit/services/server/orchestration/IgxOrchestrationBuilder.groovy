package gov.nist.toolkit.services.server.orchestration

import gov.nist.toolkit.actortransaction.shared.ActorType
import gov.nist.toolkit.configDatatypes.server.SimulatorProperties
import gov.nist.toolkit.configDatatypes.client.*
import gov.nist.toolkit.results.client.TestInstance
import gov.nist.toolkit.results.client.SiteBuilder
import gov.nist.toolkit.services.client.*
import gov.nist.toolkit.services.server.RawResponseBuilder
import gov.nist.toolkit.services.server.ToolkitApi
import gov.nist.toolkit.session.server.Session
import gov.nist.toolkit.simcommon.client.SimId
import gov.nist.toolkit.simcommon.client.SimulatorConfig
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement
import gov.nist.toolkit.simcommon.server.SimCache
import gov.nist.toolkit.sitemanagement.client.Site
import groovy.transform.TypeChecked
/**
 * Build environment for testing Initiating Gateway SUT.
 *
 */
@TypeChecked
class IgxOrchestrationBuilder extends AbstractOrchestrationBuilder {
    Session session
    IgxOrchestrationRequest request
    ToolkitApi api
    Util util
    List<SimulatorConfig> rgConfigs = []
    SimulatorConfig igConfig = null
    OrchestrationProperties orchProps

    public IgxOrchestrationBuilder(ToolkitApi api, Session session, IgxOrchestrationRequest request) {
        super(session, request)
        this.api = api
        this.session = session
        this.request = request
        this.util = new Util(api)
    }

    RawResponse buildTestEnvironment() {
        try {
            IgxOrchestrationResponse response = new IgxOrchestrationResponse()
            Map<String, TestInstanceManager> pidNameMap = [
                    oneDocPid       : new TestInstanceManager(request, response, '15824'),
                    twoDocPid       : new TestInstanceManager(request, response, '15825'),
                    twoRgPid        : new TestInstanceManager(request, response, '15826'),
                    registryErrorPid: new TestInstanceManager(request, response, '15827'),
                    noAdOptionPid     : new TestInstanceManager(request, response, '15828'),
            ]

            boolean forceNewPatientIds = !request.isUseExistingState()

            orchProps = new OrchestrationProperties(session, request.testSession, ActorType.INITIATING_GATEWAY, pidNameMap.keySet(), forceNewPatientIds)

            Pid oneDocPid = PidBuilder.createPid(orchProps.getProperty("oneDocPid"))
            Pid twoDocPid = PidBuilder.createPid(orchProps.getProperty("twoDocPid"))
            Pid twoRgPid = PidBuilder.createPid(orchProps.getProperty("twoRgPid"))
            Pid registryErrorPid = PidBuilder.createPid(orchProps.getProperty("registryErrorPid"))
            Pid noAdOptionPid = PidBuilder.createPid(orchProps.getProperty("noAdOptionPid"))

            response.setOneDocPid(oneDocPid)
            response.setTwoDocPid(twoDocPid)
            response.setTwoRgPid(twoRgPid)
            response.setUnknownPid(registryErrorPid)
            response.setNoAdOptionPid(noAdOptionPid)

            List<SimId> simIds = buildRGs(registryErrorPid)

            Site rg1Site = SimCache.getSite(session.getId(), simIds[0].toString(), simIds[0].testSession)
            Site rg2Site = SimCache.getSite(session.getId(), simIds[1].toString(), simIds[1].testSession)

            String home0 = rgConfigs.get(0).get(SimulatorProperties.homeCommunityId).asString()
            String home1 = rgConfigs.get(1).get(SimulatorProperties.homeCommunityId).asString()

            if (!request.useExistingState) {
                // send necessary Patient ID Feed messages
                request.setPifType(PifType.V2)
                new PifSender(api, request.testSession, rg1Site.siteSpec(), orchProps).send(PifType.V2, pidNameMap)
                new PifSender(api, request.testSession, rg2Site.siteSpec(), orchProps).send(PifType.V2, pidNameMap)

                TestInstance testInstance15807 = TestInstanceManager.initializeTestInstance(request.testSession, new TestInstance('15807', request.testSession))
                MessageItem itemOneDoc1 = response.addMessage(testInstance15807, true, "")
                MessageItem itemTwoDoc = response.addMessage(testInstance15807, true, "")
                MessageItem itemOneDoc2 = response.addMessage(testInstance15807, true, "")
                MessageItem itemOneDoc3 = response.addMessage(testInstance15807, true, "")
                MessageItem itemRegistryError = response.addMessage(testInstance15807, true, "")

                TestInstance testInstance12318 = TestInstanceManager.initializeTestInstance(request.testSession, new TestInstance('12318', request.testSession))
//                MessageItem item12318 = response.addMessage(testInstance12318, true, "")

                // Submit test data
                SiteBuilder.siteSpecFromSimId(rgConfigs.get(0).id).isTls = request.isUseTls()
                try {
                    util.submit(request.testSession, SiteBuilder.siteSpecFromSimId(rgConfigs.get(0).id), new TestInstance("15807", request.testSession), 'onedoc1', oneDocPid, home0)
                } catch (Exception e) {
                    itemOneDoc1.setMessage("Initialization of " + rgConfigs.get(0).id + " (section onedoc1) failed:\n" + e.getMessage());
                    itemOneDoc1.setSuccess(false)
                }
                try {
                    util.submit(request.testSession, SiteBuilder.siteSpecFromSimId(rgConfigs.get(0).id), new TestInstance("15807", request.testSession), 'twodoc', twoDocPid, home0)
                } catch (Exception e) {
                    itemTwoDoc.setMessage("Initialization of " + rgConfigs.get(0).id + " (section twodoc) failed:\n" + e.getMessage());
                    itemTwoDoc.setSuccess(false)
                }


                Map<String, String> params
                params = [
                        '$patientid$'     : twoRgPid.asString(),
                        '$testdata_home$' : home0,
                        '$testdata_repid$': rgConfigs[0].getConfigEle(SimulatorProperties.repositoryUniqueId).asString()]
                try {
                    util.submit(request.testSession.value, SiteBuilder.siteSpecFromSimId(rgConfigs.get(0).id), new TestInstance("15807", request.testSession), 'onedoc2', params)
                } catch (Exception e) {
                    itemOneDoc2.setMessage("Initialization of " + rgConfigs.get(0).id + " (section onedoc2) failed:\n" + e.getMessage());
                    itemOneDoc2.setSuccess(false)
                }

                params = [
                        '$patientid$'     : twoRgPid.asString(),
                        '$testdata_home$' : home1,
                        '$testdata_repid$': rgConfigs[1].getConfigEle(SimulatorProperties.repositoryUniqueId).asString()]
                SiteBuilder.siteSpecFromSimId(rgConfigs.get(1).id).isTls = request.isUseTls()
                try {
                    util.submit(request.testSession.value, SiteBuilder.siteSpecFromSimId(rgConfigs.get(1).id), new TestInstance("15807", request.testSession), 'onedoc3', params)
                } catch (Exception e) {
                    itemOneDoc3.setMessage("Initialization of " + rgConfigs.get(1).id + " (section onedoc3) failed:\n" + e.getMessage());
                    itemOneDoc3.setSuccess(false)
                }

                params = [
                        '$patientid$'     : registryErrorPid.asString(),
                        '$testdata_home$' : home1,
                        '$testdata_repid$': rgConfigs[1].getConfigEle(SimulatorProperties.repositoryUniqueId).asString()]
                try {
                    util.submit(request.testSession.value, SiteBuilder.siteSpecFromSimId(rgConfigs.get(1).id), new TestInstance("15807", request.testSession), 'registryError', params)
                } catch (Exception e) {
                    itemRegistryError.setMessage("Initialization of " + rgConfigs.get(1).id + " (section registryError) failed:\n" + e.getMessage());
                    itemRegistryError.setSuccess(false)
                }

//                params = [
//                        '$patientid$'     : noAdOptionPid.asString()]
//                try {
//                    util.submit(request.userName, SiteBuilder.siteSpecFromSimId(rgConfigs.get(0).id), new TestInstance("12318"), params)
//                } catch (Exception e) {
//                    item12318.setMessage("Initialization of " + rgConfigs.get(0).id + " failed:\n" + e.getMessage());
//                    item12318.setSuccess(false)
//                }
            }

            response.oneDocPid = oneDocPid
            response.twoDocPid = twoDocPid
            response.twoRgPid = twoRgPid
            response.unknownPid = registryErrorPid
            response.noAdOptionPid = noAdOptionPid
            response.simulatorConfigs = rgConfigs
            response.igSimulatorConfig = igConfig
            response.supportRG1 = SimCache.getSite(simIds[0].toString(), simIds[0].testSession)
            response.supportRG2 = SimCache.getSite(simIds[1].toString(), simIds[1].testSession)

            orchProps.save();

            return response
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    List<SimId> buildRGs(Pid unknownPid) {
        // build and initialize remote communities
        String id1 = 'community1'
        String id2 = 'community2'
        SimId rgSimId1 = new SimId(request.testSession, id1, ActorType.RESPONDING_GATEWAY_X.name, request.environmentName)
        SimId rgSimId2 = new SimId(request.testSession, id2, ActorType.RESPONDING_GATEWAY_X.name, request.environmentName)
        SimulatorConfig rgSimConfig1
        SimulatorConfig rgSimConfig2
        boolean reuse = false  // updated as we progress

        if (!request.isUseExistingState()) {
            try {
                api.deleteSimulator(rgSimId1);
            } catch (Exception e) {
                // ignore
            }
            try {
                api.deleteSimulator(rgSimId2);
            } catch (Exception e) {
                // ignore
            }
        }

        if (api.simulatorExists(rgSimId1)) {
            rgSimConfig1 = api.getConfig(rgSimId1)
            rgSimConfig2 = api.getConfig(rgSimId2)
            reuse = true
        } else {
            rgSimConfig1 = api.createSimulator(rgSimId1).getConfig(0)
            rgSimConfig2 = api.createSimulator(rgSimId2).getConfig(0)
        }


        SimulatorConfigElement rgEle

        //
        // Initialize both supporting Responding Gateways
        //

        if (!reuse) {
            // disable checking of Patient Identity Feed
//            rgEle = rgSimConfig1.getConfigEle(SimulatorProperties.VALIDATE_AGAINST_PATIENT_IDENTITY_FEED)
//            rgEle.setBooleanValue(false)

            // set fixed homeCommunityId
            rgEle = rgSimConfig1.getConfigEle(SimulatorProperties.homeCommunityId)
            rgEle.setStringValue('urn:oid:1.2.34.567.8.1')

            // config rg1 to return XDSRegistryError for registryErrorPid query requests
            rgEle = rgSimConfig1.getConfigEle(SimulatorProperties.errorForPatient)
            PatientErrorMap pem = new PatientErrorMap()
            PatientErrorList pel = new PatientErrorList()
            PatientError pe = new PatientError()
            pe.setPatientId(unknownPid)
            pe.setErrorCode('XDSRegistryError')
            pel.add(pe)
            pem.put(TransactionType.XC_QUERY.name, pel)
            rgEle.setPatientErrorMapValue(pem)

            api.saveSimulator(rgSimConfig1)

            // disable checking of Patient Identity Feed
//            rgEle = rgSimConfig2.getConfigEle(SimulatorProperties.VALIDATE_AGAINST_PATIENT_IDENTITY_FEED)
//            rgEle.setBooleanValue(false)
            // set fixed homeCommunityId
            rgEle = rgSimConfig2.getConfigEle(SimulatorProperties.homeCommunityId)
            rgEle.setStringValue('urn:oid:1.2.34.567.8.2')
            api.saveSimulator(rgSimConfig2)
        }

        rgConfigs << rgSimConfig1
        rgConfigs << rgSimConfig2

        if (request.includeLinkedIGX) {
            // create initiating gateway (SUT) to allow self test
            String igId = 'ig'
            SimId igSimId = new SimId(request.testSession, igId, ActorType.INITIATING_GATEWAY.name, request.environmentName)
            igConfig = api.createSimulator(igSimId).getConfig(0);

            // link all responding gateways to initiating gateway
            List<String> rgConfigIds = rgConfigs.collect() { SimulatorConfig rg -> rg.id.toString() }
            SimulatorConfigElement rgs = igConfig.getConfigEle(SimulatorProperties.respondingGateways)
            rgs.setStringListValue(rgConfigIds)
            api.saveSimulator(igConfig)
        }

        return [rgSimId1, rgSimId2]
    }
}
