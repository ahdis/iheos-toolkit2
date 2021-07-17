package gov.nist.toolkit.itTests.support

import gov.nist.toolkit.adt.ListenerFactory
import gov.nist.toolkit.configDatatypes.client.Pid
import gov.nist.toolkit.grizzlySupport.GrizzlyController
import gov.nist.toolkit.installation.server.Installation
import gov.nist.toolkit.installation.server.TestSessionFactory
import gov.nist.toolkit.installation.shared.TestSession
import gov.nist.toolkit.results.client.*
import gov.nist.toolkit.services.server.ToolkitApi
import gov.nist.toolkit.services.server.UnitTestEnvironmentManager
import gov.nist.toolkit.session.server.Session
import gov.nist.toolkit.toolkitApi.SimulatorBuilder
import gov.nist.toolkit.toolkitServicesCommon.SimId
import gov.nist.toolkit.utilities.io.Io
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Shared
import spock.lang.Specification
/**
 *
 */

class ToolkitSpecification extends Specification {
    @Rule TestName name = new TestName()
    // these are usable by the specification that extend this class
    @Shared GrizzlyController server = null
    @Shared ToolkitApi api
    @Shared Session session
    @Shared static String remoteToolkitPort = '8889'
    @Shared static final boolean localServerMode = true

    /*
     * When running it-tests as part of the build, the it-tests plugin launches a one-time startup http server.
     *
     * When running a single IT test inside the IDE, a local server is required.
     *
     * This block below will decide which mode the test is running in to automatically use the server as needed.
    static {
        try {
            Client client = ClientBuilder.newClient()
            WebTarget target = client.target('http://localhost:' + remoteToolkitPort + '/testEnvPidPort?cmd=status')

            Response response = target
                    .request('text/xml')
                    .get()

            localServerMode = !(response.status == 200)
        } catch (Exception ex) {
            localServerMode = true
        }
    }
     */

    def setupSpec() {  // there can be multiple setupSpec() fixture methods - they all get run
        Installation.instance().setServletContextName("");
        session = UnitTestEnvironmentManager.setupLocalToolkit()
        api = UnitTestEnvironmentManager.localToolkitApi()

        Installation.setTestRunning(true)
        cleanupDir()
    }

    def setup() {
        println 'Running method: ' + name.methodName
    }

    // clean out simdb, testlogcache, and actors
    def cleanupDir() {
        if (localServerMode) {
            TestSessionFactory.inTestLogs().each { String testSessionName ->
                Io.delete(Installation.instance().testLogCache(new TestSession(testSessionName)))
            }
            Installation.instance().simDbFile(TestSession.DEFAULT_TEST_SESSION).mkdirs()
            Installation.instance().actorsDir(TestSession.DEFAULT_TEST_SESSION).mkdirs()
            Installation.instance().testLogCache(TestSession.DEFAULT_TEST_SESSION).mkdirs()
        }
    }

    def startGrizzly(String port) {
        remoteToolkitPort = port
        if (localServerMode) {
            server = new GrizzlyController()
            server.start(remoteToolkitPort);
            server.withToolkit()
        }
        Installation.instance().overrideToolkitPort(remoteToolkitPort)  // ignore toolkit.properties
    }

    def startGrizzlyWithFhir(String port) {
        remoteToolkitPort = port
        if (localServerMode) {
            server = new GrizzlyController()
            server.start(remoteToolkitPort);
            server.withToolkit()
//            server.withFhirServlet()
        }
        Installation.instance().overrideToolkitPort(remoteToolkitPort)  // ignore toolkit.properties
    }

    static String prefixNonce(String name) {
        if ("default".equals(name))
            throw new Exception("Default session cannot be prefixed with a nonce.")

        return name + TestSessionFactory.nonce()
    }


    SimulatorBuilder getSimulatorApi(String remoteToolkitPort) {
        String urlRoot = String.format("http://localhost:%s/xdstools2", remoteToolkitPort)
        new SimulatorBuilder(urlRoot)
    }

    SimulatorBuilder getSimulatorApi(String host, String remoteToolkitPort) {
        String urlRoot = String.format("http://%s:%s/xdstools2", host, remoteToolkitPort)
        new SimulatorBuilder(urlRoot)
    }

    def cleanupSpec() {  // one time shutdown when everything is done
        if (localServerMode) {
            if (server) {
                server.stop()
                server = null
            }
            ListenerFactory.terminateAll()
        }
    }

    def initializeRegistryWithPatientId(String testSession, SimId simId, Pid pid) {
        TestInstance testId = new TestInstance("15804")
        List<String> sections = new ArrayList<>()
        sections.add("section")
        Map<String, String> params = new HashMap<>()
        params.put('$patientid$', pid.toString())
        boolean stopOnFirstError = true

        List<Result> results = api.runTest(testSession, simId.fullId, testId, sections, params, stopOnFirstError)

        assert results.size() == 1
        assert results.get(0).passed()
    }

    TestLogs initializeRepository(String testSession, SimId simId, Pid pid, TestInstance testInstance) {
        List<String> sections = new ArrayList<>()
        Map<String, String> params = new HashMap<>()
        params.put('$patientid$', pid.toString())
        boolean stopOnFirstError = true

        List<Result> results = api.runTest(testSession, simId.fullId, testInstance, sections, params, stopOnFirstError)

        TestLogs testLogs = api.getTestLogs(testInstance)

        assert testLogs
        assert results.size() == 1
        assert results.get(0).passed()
        return testLogs
    }

    boolean assertionsContain(List<Result> results, String target) {
        boolean found = false

        results.each { Result result ->
            result.assertions.each { AssertionResults ars->
                ars.assertions.each { AssertionResult ar ->
                    if (ar.assertion.contains(target)) found = true
                }
            }
        }

        return found
    }

}
