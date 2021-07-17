package gov.nist.toolkit.services.server.orchestration

import gov.nist.toolkit.actortransaction.shared.ActorOption
import gov.nist.toolkit.actortransaction.shared.ActorType
import gov.nist.toolkit.configDatatypes.client.Pid
import gov.nist.toolkit.configDatatypes.client.PidBuilder
import gov.nist.toolkit.configDatatypes.server.SimulatorProperties
import gov.nist.toolkit.services.client.FhirSupportOrchestrationResponse
import gov.nist.toolkit.services.client.RawResponse
import gov.nist.toolkit.services.client.RecOrchestrationRequest
import gov.nist.toolkit.services.client.RecOrchestrationResponse
import gov.nist.toolkit.services.server.RawResponseBuilder
import gov.nist.toolkit.services.server.ToolkitApi
import gov.nist.toolkit.session.server.Session
import gov.nist.toolkit.simcommon.client.SimId
import gov.nist.toolkit.simcommon.client.SimulatorConfig
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement
import gov.nist.toolkit.simcommon.server.SimCache
import groovy.transform.TypeChecked
//import gov.nist.toolkit.testengine.transactions.ProvideDocumentBundleTransaction

/**
 *
 */
@TypeChecked
class RecOrchestrationBuilder extends AbstractOrchestrationBuilder {
    ToolkitApi api
    private Session session
    private RecOrchestrationRequest request
    private Util util
    private ActorOption actorOption = new ActorOption()

    public RecOrchestrationBuilder(ToolkitApi api, Session session, RecOrchestrationRequest request) {
        super(session, request)
        this.api = api
        this.request = request
        this.session = session
        this.util = new Util(api)
        this.actorOption.copyFrom(request.actorOption)
    }

    RawResponse buildTestEnvironment() {

        // depends on Fhir Support server running and initialized
//        FhirSupportOrchestrationBuilder supportBuilder = new FhirSupportOrchestrationBuilder(api, session, request.testSession, request.useExistingState)
//        FhirSupportOrchestrationResponse supportResponse = supportBuilder.buildTestEnvironment()

//        if (actorOption.optionId == OptionType.XDS_ON_FHIR.toString()) {
//            RawResponse res =  setupXdsOnFhir(supportResponse)
//            RecOrchestrationResponse rres = (RecOrchestrationResponse) res
//            rres.additionalDocumentation = ProvideDocumentBundleTransaction.additionalDocumentation()
//            return res
//        } else {
            RecOrchestrationResponse response = new RecOrchestrationResponse()
//            if (actorOption.profileId == IheItiProfile.MHD)
//                response.additionalDocumentation = ProvideDocumentBundleTransaction.additionalDocumentation()

//            response.supportResponse = supportResponse
            Map<String, TestInstanceManager> pidNameMap = [
                    // RecPIF does not really exist as a test.  It will never be sent.  Just a
                    // name for the TestInstanceManager to use in storing the property
                    // in Orch Props
                    RecPIF:  new TestInstanceManager(request, response, 'RecPIF')
            ]

            boolean forceNewPatientIds = !request.isUseExistingState()

            OrchestrationProperties orchProps = new OrchestrationProperties(session, request.testSession, ActorType.DOCUMENT_RECIPIENT, pidNameMap.keySet(), forceNewPatientIds)

            Pid registerPid
            if (forceNewPatientIds) {
                registerPid = session.allocateNewPid()
                orchProps.setProperty("RecPIF", registerPid.asString())
            } else {
                registerPid = PidBuilder.createPid(orchProps.getProperty("RecPIF"))
            }

            response.setRegisterPid(registerPid)

            orchProps.save();

            return response

//        }
    }

/**
 * MHD orchestration requirements:
 *
 * SUT is MHD Document Recipient with XdsOnFhir option
 * Build Registry/Repository with Patient ID requirement disabled
 * advertise the details so recipient can be configured
 *
 */
    RawResponse setupXdsOnFhir(FhirSupportOrchestrationResponse supportResponse) {
        try {
            String supportIdName = 'xdsonfhir_test_support'
            SimId rrSimId
            SimulatorConfig rrSimConfig

            RecOrchestrationResponse response = new RecOrchestrationResponse()
            response.supportResponse = supportResponse
            Map<String, TestInstanceManager> pidNameMap = [
                    pid:  new TestInstanceManager(request, response, ''), // No testId needed since PIF won't be sent
            ]

            boolean forceNewPatientIds = !request.isUseExistingState()

            boolean reuse = false  // updated as we progress
            rrSimId = new SimId(request.testSession, supportIdName, ActorType.REPOSITORY_REGISTRY.name, request.environmentName)
            OrchestrationProperties orchProps = new OrchestrationProperties(session, request.testSession, ActorType.REPOSITORY_REGISTRY, pidNameMap.keySet(), !request.useExistingState)

            if (!request.isUseExistingSimulator()) {
                api.deleteSimulatorIfItExists(rrSimId)
                orchProps.clear()
            }
            if (api.simulatorExists(rrSimId)) {
                rrSimConfig = api.getConfig(rrSimId)
                reuse = true
            } else {
                rrSimConfig = api.createSimulator(rrSimId).getConfig(0)
            }

            if (!request.isUseExistingSimulator()) {
                // disable checking of Patient Identity Feed
                SimulatorConfigElement idsEle = rrSimConfig.getConfigEle(SimulatorProperties.VALIDATE_AGAINST_PATIENT_IDENTITY_FEED)
                idsEle.setBooleanValue(false)
                api.saveSimulator(rrSimConfig)

            }
            orchProps.save()

           // response.setRegisterPid(PidBuilder.createPid('fake^^^&1.2.3&ISO'))
            response.rrConfig = rrSimConfig
            response.RRSite = SimCache.getSite(rrSimId.toString(), request.testSession)
            response.supportSite = SimCache.getSite(rrSimId.toString(), request.testSession)

            return response
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }
}
