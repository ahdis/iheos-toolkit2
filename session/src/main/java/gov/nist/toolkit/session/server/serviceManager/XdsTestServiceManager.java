package gov.nist.toolkit.session.server.serviceManager;

import gov.nist.toolkit.commondatatypes.MetadataSupport;
import gov.nist.toolkit.configDatatypes.client.Pid;
import gov.nist.toolkit.installation.server.Installation;
import gov.nist.toolkit.installation.shared.TestCollectionCode;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.registrymetadata.MetadataParser;
import gov.nist.toolkit.registrymetadata.client.Document;
import gov.nist.toolkit.registrymsg.repository.RetrieveResponseParser;
import gov.nist.toolkit.registrymsg.repository.RetrievedDocumentModel;
import gov.nist.toolkit.registrymsg.repository.RetrievedDocumentsModel;
import gov.nist.toolkit.results.CommonService;
import gov.nist.toolkit.results.MetadataToMetadataCollectionParser;
import gov.nist.toolkit.results.ResultBuilder;
import gov.nist.toolkit.results.client.*;
import gov.nist.toolkit.session.client.ConformanceSessionValidationStatus;
import gov.nist.toolkit.session.client.logtypes.TestOverviewDTO;
import gov.nist.toolkit.session.client.logtypes.TestPartFileDTO;
import gov.nist.toolkit.session.server.CodesConfigurationBuilder;
import gov.nist.toolkit.session.server.MessageBuilder;
import gov.nist.toolkit.session.server.Session;
import gov.nist.toolkit.session.server.services.TestLogCache;
import gov.nist.toolkit.session.server.testlog.TestOverviewBuilder;
import gov.nist.toolkit.session.shared.Message;
import gov.nist.toolkit.sitemanagement.client.Site;
import gov.nist.toolkit.sitemanagement.client.SiteSpec;
import gov.nist.toolkit.testengine.engine.ResultPersistence;
import gov.nist.toolkit.testengine.engine.TestLogsBuilder;
import gov.nist.toolkit.testengine.engine.Xdstest2;
import gov.nist.toolkit.testenginelogging.LogFileContentBuilder;
import gov.nist.toolkit.testenginelogging.TestLogDetails;
import gov.nist.toolkit.testenginelogging.client.LogFileContentDTO;
import gov.nist.toolkit.testenginelogging.client.LogMapDTO;
import gov.nist.toolkit.testenginelogging.client.TestStepLogContentDTO;
import gov.nist.toolkit.testenginelogging.logrepository.LogRepository;
import gov.nist.toolkit.testkitutilities.TestDefinition;
import gov.nist.toolkit.testkitutilities.TestKit;
import gov.nist.toolkit.testkitutilities.TestKitSearchPath;
import gov.nist.toolkit.testkitutilities.client.Gather;
import gov.nist.toolkit.testkitutilities.client.SectionDefinitionDAO;
import gov.nist.toolkit.testkitutilities.client.TestCollectionDefinitionDAO;
import gov.nist.toolkit.utilities.id.UuidAllocator;
import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.utilities.xml.OMFormatter;
import gov.nist.toolkit.utilities.xml.Parse;
import gov.nist.toolkit.utilities.xml.Util;
import gov.nist.toolkit.utilities.xml.XmlFormatter;
import gov.nist.toolkit.utilities.xml.XmlUtil;
import gov.nist.toolkit.xdsexception.ExceptionUtil;
import gov.nist.toolkit.xdsexception.client.EnvironmentNotSelectedException;
import gov.nist.toolkit.xdsexception.client.ToolkitRuntimeException;
import gov.nist.toolkit.xdsexception.client.XdsInternalException;
import jdk.internal.org.xml.sax.SAXException;
import org.apache.axiom.om.OMElement;
import org.apache.log4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import gov.nist.toolkit.testengine.fhir.FhirSupport;
//import org.hl7.fhir.instance.model.api.IBaseResource;


public class XdsTestServiceManager extends CommonService {
	private CodesConfiguration codesConfiguration = null;
	public Session session;
	static Logger logger = Logger.getLogger(XdsTestServiceManager.class);
	static boolean allCiphersEnabled = false;

	public XdsTestServiceManager(Session session)  {
		this.session = session;
//		if (session != null)
//			logger.info("XdsTestServiceManager: using session " + session.getId());
	}

	public static Logger getLogger() {
		return logger;
	}



	public static TestLogCache getTestLogCache() throws IOException {
		return new TestLogCache(Installation.instance().propertyServiceManager().getTestLogCache());
	}

	/**
	 * Wrapper around run to be used for using the test-client as a utility.  The difference between using
	 * it as a utility or as a test is how and where the logs are stored.  For tests they are stored in
	 * the external cache under TestLogCache and for utilities they are stored in war/SessionCache.  Logs in
	 * TestLogCache are permanent (manually deleted) and in SessionCache they are delete when the session
	 * times out.
	 * @param testInstance
	 * @param sections
	 * @param params
	 * @param areas
	 * @param stopOnFirstFailure
	 * @return
	 */
	public Result xdstest(TestInstance testInstance, List<String> sections,
						  Map<String, String> params, Map<String, Object> params2, String[] areas,
						  boolean stopOnFirstFailure) {
		testInstance.setTestSession(session.getTestSession());
		TestKitSearchPath searchPath = session.getTestkitSearchPath();
		try {
			session.xt = new Xdstest2(Installation.instance().toolkitxFile(), searchPath, session, testInstance.getTestSession());
		} catch (Exception e) {
			Result result = new Result();
			result.addAssertion(e.getMessage(), false);
			return result;
		}
		return new UtilityRunner(this, TestRunType.UTILITY).run(session, params, params2, sections, testInstance, areas,
				stopOnFirstFailure);
	}

	/**
	 * Run a testplan(s) as a utility within a session.  This is different from
	 * run in that run stores logs in the external_cache and
	 * this call stores the logs within the session so they go away at the
	 * end of the end of the session.  Hence the label 'utility'.
	 * @param params
	 * @param sections
	 * @param testInstance
	 * @param stopOnFirstFailure
	 * @return
	 */
//	public Result runUtilityTest(Map<String, String> params, Map<String, Object> params2, List<String> SECTIONS,
//								 String testId, String[] areas, boolean stopOnFirstFailure) {
//
//		return utilityRunner.run(session, params, params2, SECTIONS, testId, areas, stopOnFirstFailure);
//	}
	public List<Result> runMesaTest(String environmentName,TestSession testSession, SiteSpec siteSpec, TestInstance testInstance, List<String> sections,
									Map<String, String> params, Map<String, Object> params2, boolean stopOnFirstFailure) throws Exception {
		testInstance.setTestSession(testSession);

		session.setTestSession(testSession);
		session.setCurrentEnvName(environmentName);
		TestKitSearchPath searchPath = new TestKitSearchPath(environmentName, testSession);
		session.xt = new Xdstest2(Installation.instance().toolkitxFile(), searchPath, session, testInstance.getTestSession());
		List<Result> results = new TestRunner(this).run(session, testSession, siteSpec, testInstance, sections, params, params2, stopOnFirstFailure);
		return results;
	}

	public TestOverviewDTO runTest(String environmentName, TestSession mesaTestSession, SiteSpec siteSpec, TestInstance testInstance, List<String> sections,
								   Map<String, String> params, Map<String, Object> params2, boolean stopOnFirstFailure) throws Exception {

		if (mesaTestSession == null)
			throw new ToolkitRuntimeException("TestSession is null");
		testInstance.setTestSession(mesaTestSession);
		TestKitSearchPath searchPath = new TestKitSearchPath(environmentName, mesaTestSession);
		session.xt = new Xdstest2(Installation.instance().toolkitxFile(), searchPath, session, testInstance.getTestSession());
		new TestRunner(this).run(session, mesaTestSession, siteSpec, testInstance, sections, params, params2, stopOnFirstFailure);
		return getTestOverview(mesaTestSession, testInstance);
	}

	/**
	 * Wrapper to TestRunner#run
	 * Expects an instance of XdsTestServiceManager
	 * @param xdsTestServiceManager
	 * @param environment
	 * @param testSession
	 * @param siteSpec
	 * @param testId
	 * @param sections
	 * @param params
	 * @param stopOnFirstError
	 * @param persistResult
	 * @return
	 */
	static public List<Result> runTestInstance(XdsTestServiceManager xdsTestServiceManager, String environment, TestSession testSession, SiteSpec siteSpec, TestInstance testId, List<String> sections, Map<String, String> params, boolean stopOnFirstError, boolean persistResult) {

		List<Result> results; // This wrapper does two important things of interest: 1) Set patient id if it is available in the Params map and 2) Eventually calls the UtilityRunner
		try {
			results = xdsTestServiceManager.runMesaTest(environment, testSession, siteSpec, testId, sections, params, null, stopOnFirstError);
		} catch (Exception e) {
			results = new ArrayList<>();
			Result result = new Result();
			result.pass = false;
			result.assertions.add(ExceptionUtil.exception_details(e), false);
			results.add(result);
			return results;
		}

		// Save results to external_cache.
		// Supports getTestResults tookit api call
		if (persistResult) {
			ResultPersistence rPer = new ResultPersistence();

			for (Result result : results) {
				try {
					rPer.write(result, testSession);
				} catch (Exception e) {
					result.assertions.add(ExceptionUtil.exception_details(e), false);
				}
			}
		}

		return results;
	}

	public void setGazelleTruststore() {
		String tsSysProp =
				System.getProperty("javax.net.ssl.trustStore");

		if (tsSysProp == null) {
			String tsFileName = "gazelle/gazelle_sts_cert_truststore.jks";
			try {
				File tsFile = null;
				tsFile = Paths.get(getClass().getResource("/").toURI()).resolve(tsFileName).toFile(); // Should this be a toolkit system property variable?
				if (tsFile != null) {
					tsFile = new File(tsFile.toString().replaceAll("%20", " "));
					System.setProperty("javax.net.ssl.trustStore", tsFile.toString());
					System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
					System.setProperty("javax.net.ssl.trustStoreType", "JKS");
				} else {
					throw new ToolkitRuntimeException("Cannot find truststore by URL: " + tsFileName);
				}
			} catch (URISyntaxException urise) {
				throw new ToolkitRuntimeException(urise.toString());
			}

		}
	}

	public List<Result> querySts(SiteSpec siteSpec, String query, Map<String, String> params, boolean persistResult, TestSession testSession) {
		setGazelleTruststore();

		String environmentName = "default";
		Session mySession = new Session(Installation.instance().warHome(), testSession.toString());
		mySession.setEnvironment(environmentName);

		// Site must exist
		if (mySession.getTestSession() == null)
			mySession.setTestSession(testSession);
		mySession.setSiteSpec(siteSpec);
		mySession.setTls(true); // Required for STS

		String stsTpName = Installation.instance().propertyServiceManager().getStsTpName();

		TestInstance testInstance = new TestInstance(stsTpName, TestSession.DEFAULT_TEST_SESSION);

		List<String> sections = new ArrayList<String>();
		sections.add(query);

		XdsTestServiceManager xtsm = new XdsTestServiceManager(mySession);
		List<Result> results =  runTestInstance(xtsm, environmentName,testSession,siteSpec,testInstance,sections,params,true, persistResult);

		return results;
	}

	/**
	 * Original Xdstools2 function to retrieve test results based on the current Session by providing a list of
	 * TestInstance numbers / Test Ids.
	 * @param testInstances
	 * @param testSession
	 * @return
	 */
	public Map<String, Result> getTestResults(List<TestInstance> testInstances, String environmentName, TestSession testSession) {
		if (session != null)
			logger.debug(session.id() + ": " + "getTestResults() ids=" + testInstances + " testSession=" + testSession);

		Map<String, Result> map = new HashMap<String, Result>();

		ResultPersistence rp = new ResultPersistence();

		TestKitSearchPath testKitSearchPath = new TestKitSearchPath(environmentName, testSession);
		for (TestInstance testInstance : testInstances) {
			try {
				TestDefinition testDefinition = testKitSearchPath.getTestDefinition(testInstance.getId());
				List<String> sectionNames = testDefinition.getSectionIndex();

				Result result = rp.read(testInstance, sectionNames, testSession);
				map.put(testInstance.getId(), result);
			}
			catch (Exception e) {}
		}
		return map;
	}

	/**
	 * For every successful testInstance log delete, an empty TestOverviewDTO for it is returned.
	 * @param testInstances
	 * @param environmentName
	 * @param testSession
	 * @return
	 */
	public List<TestOverviewDTO> delTestResults(List<TestInstance> testInstances, String environmentName, TestSession testSession) {
		List<TestOverviewDTO> testOverviewDTOs = new ArrayList<>();
		if (session != null)
			logger.debug(session.id() + ": " + "delTestResults() ids=" + testInstances + " testSession=" + testSession);
		TestKitSearchPath testKitSearchPath = new TestKitSearchPath(environmentName, testSession);
		ResultPersistence rp = new ResultPersistence();
		for (TestInstance testInstance : testInstances) {
			try {
				TestDefinition testDefinition = testKitSearchPath.getTestDefinition(testInstance.getId());
				List<String> sectionNames = testDefinition.getSectionIndex();
				rp.delete(testInstance, testSession, sectionNames);
				testOverviewDTOs.add(getTestOverview(testInstance.getTestSession(), testInstance));
			}
			catch (Exception e) {}
		}
		return testOverviewDTOs;
	}

	// this translates from the xds-common version of AssertionResults
	// to the xdstools2.client version which is serializable and can be passed
	// to the gui front end
	void scanLogs(Xdstest2 xt, AssertionResults res, Collection<String> sections) throws Exception {

		gov.nist.toolkit.testengine.errormgr.AssertionResults car = xt.scanLogs(sections);

		for (gov.nist.toolkit.testengine.errormgr.AssertionResult cara : car.assertions) {
			res.add(cara.assertion, cara.info, cara.status);
		}
	}

	/**
	 * Fetch the log for a particular testId. The testId comes from a Result class
	 * instance. It is an identifier that is generated when the result is
	 * created so that the log details can be cached in the server and the
	 * client can ask for them later.
	 *
	 * This call only works for test logs created as part of the session
	 * that correspond to utilities based on the test engine. Raw
	 * test engine output cannot be accessed this way as they are stored
	 * separate from the current GUI session.
	 */
	public TestLogs getRawLogs(TestInstance testInstance) {
		if (testInstance == null) {
			logger.error("XdsTestServiceManager:getRawLogs() for testInstance null");
			return new TestLogs();
		}
		if (session != null)
			logger.debug(session.id() + ": " + "getRawLogs for " + testInstance.describe());

		LogMapDTO logMapDTO;
		try {
			logMapDTO = LogRepository.logIn(testInstance);
		} catch (Exception e) {
			logger.error(ExceptionUtil.exception_details(e, "Logs not available for " + testInstance));
			TestLogs testLogs = new TestLogs();
			testLogs.testInstance = testInstance;
			testLogs.assertionResult = new AssertionResult(
					String.format("Internal Server Error: Cannot find logs for Test %s", testInstance),
					false);
			return testLogs;
		}

		try {
			TestLogs testLogs = TestLogsBuilder.build(logMapDTO);
			testLogs.testInstance = testInstance;
			return testLogs;
		} catch (Exception e) {
			TestLogs testLogs = new TestLogs();
			String details = ExceptionUtil.exception_details(e);
			logger.error(details);
			testLogs.assertionResult = new AssertionResult(details,false);
			return testLogs;
		}
	}

	/**
	 * build structured display content.  Also returns non-FHIR content.  Each test step
	 * returns a pair of Messages, one for the request and one for the response.  If there is no
	 * request/response content for a step then an empty Message pair is returned.
	 * @param testInstance
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public List<Message> getFhirResult(TestInstance testInstance) throws ParserConfigurationException, SAXException, IOException {
		TestLogs testLogs = getRawLogs(testInstance);
		List<Message> result = new ArrayList<>();

		if (testLogs.size() == 0) {
			result.add(new Message("---"));
			result.add(new Message("---"));
			return result;
		}

		for (int step=0; step<testLogs.size(); step++) {
			TestLog testLog = testLogs.getTestLog(step);
			// inputMetadata
			String input = deformat(testLog.inputMetadata);
			if (!input.equals("")) {
//				try {
					// FHIR content
//					boolean isJson = input.trim().startsWith("{");
//					IBaseResource resource = FhirSupport.parse(input);
//					result.add(new FhirMessageBuilder(isJson).build("", resource).setName(testLog.stepName + " Request"));
//				} catch (Exception e) {
					// non-FHIR content (failed parse)
					result.add(new MessageBuilder().build("", input).setName(testLog.stepName + " Request"));
//				}
			} else {
				result.add(new MessageBuilder().build("", testLog.outHeader).setName(testLog.stepName + " Request"));
			}
			// result
			String output = deformat(testLog.result);
//			try {
				// FHIR content
//				IBaseResource resource = FhirSupport.parse(output);
//				boolean isJson = output.trim().startsWith("{");
//				result.add(new FhirMessageBuilder(isJson).build("", resource).setName(testLog.stepName + " Response"));
//			} catch (Exception e) {
				// non-FHIR content (failed parse)
				result.add(new MessageBuilder().build("", output).setName(testLog.stepName + " Response"));
//			}
		}
		return result;
	}

	String deformat(String xml) {
		if (xml == null) return xml;
		if (xml.equals("")) return xml;
		boolean isXml = xml.trim().startsWith("<");
//		if (!isXml) return xml;
		xml = xml.replaceAll("<br />", "\n");
		xml = xml.replace("&nbsp;", " ");
		xml = xml.replace("&lt;", "<");
		return xml;
	}

	public Map<String, String> getCollection(String collectionSetName, TestCollectionCode testCollectionId) throws Exception  {
		if (session != null)
			logger.debug(session.id() + ": " + "getCollection " + collectionSetName + ":" + testCollectionId);
		try {
			System.out.println("ENVIRONMENT: "+session.getCurrentEnvName()+", SESSION: "+session.getTestSession());
			Map<String,String> collection=new HashMap<String,String>();
			for (File testkitFile:Installation.instance().testkitFiles(session.getCurrentEnvName(),session.getTestSession())) {
				try {
					TestKit tk = new TestKit(testkitFile);
					Map<String, String> c = tk.getCollection(collectionSetName, testCollectionId);
					for (String key : c.keySet()) {
						if (!collection.containsKey(key)) {
							collection.put(key, c.get(key));
						}
					}
					return collection;
				} catch (Exception e) {
					// not a problem until the list is exhausted
				}
			}
		} catch (Exception e) {
			logger.error("getCollection", e);
			throw new Exception(e.getMessage());
		}
		// collection was not found -- oops
		throw new Exception("Collection " + collectionSetName + "/" + testCollectionId + "  was not found");
	}

	/**
	 * For test collections like collections and actorcollections, return the TestInstances.
	 * TestInstances will be loaded with sutInitiates to indicate whether any sections
	 * are to be initiated by the SUT
	 * @param collectionSetName - collections or actorcollections
	 * @param testCollectionId - name of specific collection
	 * @return
	 * @throws Exception
	 */
	public List<TestInstance> getCollectionMembers(String collectionSetName, TestCollectionCode testCollectionId) throws Exception {
		try {
			if (session != null)
				logger.debug(session.id() + ": " + "getCollectionMembers " + collectionSetName + ":" + testCollectionId);
			TestKitSearchPath searchPath = session.getTestkitSearchPath();
			Collection<String> collec =  searchPath.getCollectionMembers(collectionSetName, testCollectionId);
			if (session != null)
				logger.debug("Return " + collec.size() + " tests");

			List<TestInstance> tis = new ArrayList<>();
			for (String testId : collec) {
				TestInstance ti = new TestInstance(testId, TestSessionServiceManager.INSTANCE.getTestSession(session));
				tis.add(ti);
			}

			for (TestInstance ti : tis) {
				TestDefinition def = null;
				try {
					def = session.getTestkitSearchPath().getTestDefinition(ti.getId());
				} catch (Exception e) {
					throw new XdsInternalException("Unable to load test definition  for " + ti.getId(), e);
				}
				if (def == null)
					throw new XdsInternalException("Cannot find test definition for " + ti);
				List<SectionDefinitionDAO> sectionDAOs;
				try {
					sectionDAOs = def.getSections();
				} catch (XdsInternalException e) {
					throw new XdsInternalException(e.getMessage() + " of test " + ti.getId());
				} catch (Exception e) {
					throw new XdsInternalException("Unable to load test definition  for " + ti.getId(), e);
				}
				boolean sutInitiated = false;
				for (SectionDefinitionDAO dao : sectionDAOs) {
					if (dao.isSutInitiated()) sutInitiated = true;
				}
				ti.setSutInitiated(sutInitiated);
			}
			return tis;
		} catch (Exception e) {
			logger.error(ExceptionUtil.exception_details(e, "getCollectionsMembers error: "));
			throw e;
		}
	}

	public List<SectionDefinitionDAO> getTestSectionsDAOs(TestInstance testInstance) throws Exception {
		TestDefinition def = session.getTestkitSearchPath().getTestDefinition(testInstance.getId());
		return def.getSections();
	}

	public String getTestReadme(String test) throws Exception {
		logger.debug(session.id() + ": " + "getTestReadme " + test);
		try {
			TestDefinition tt = session.getTestkitSearchPath().getTestDefinition(test);
			return tt.getFullTestReadme();
		} catch (Exception e) {
			logger.error("getTestReadme", e);
			throw new Exception(e.getMessage());
		}
	}

	public List<String> getTestSections(String test) throws Exception   {
//		if (session != null)
//			logger.debug(session.id() + ": " + "getTestSectionsReferencedInUseReports " + test);
		TestKitSearchPath searchPath = session.getTestkitSearchPath();
		TestDefinition def = session.getTestkitSearchPath().getTestDefinition(test);
		return def.getSectionIndex();
	}

	/**
	 * Collect ID and title for each test collection
	 * @param collectionSetName
	 * @return List<TestCollectionDefinitionDAO>
	 * @throws Exception if cannot scan testkit
	 */
	public List<TestCollectionDefinitionDAO> getTestCollections(String collectionSetName) throws Exception {
		List<TestCollectionDefinitionDAO> daos = new ArrayList<>();
		TestKitSearchPath searchPath = session.getTestkitSearchPath();
		for (TestKit testkit : searchPath.getTestkits()) {
			for (TestCollectionDefinitionDAO dao : testkit.getTestCollections(collectionSetName)) {
				if (!hasTestCollection(daos, dao.getCollectionID()))
					daos.add(dao);
			}
		}
		return daos;
	}

	private boolean hasTestCollection(List<TestCollectionDefinitionDAO> daos, TestCollectionCode tcId) {
		for (TestCollectionDefinitionDAO dao : daos) {
			if (dao.getCollectionID().equals(tcId))
				return true;
		}
		return false;
	}

	public boolean isPrivateMesaTesting() {
		if (session != null)
			logger.debug(session.id() + ": " + "isPrivateMesaTesting");
		return Installation.instance().propertyServiceManager().isTestLogCachePrivate();
	}

	/**
	 *
	 * @param testInstance
	 * @param section
	 * @return
	 * @throws Exception
	 */
	public String getTestplanAsText(TestInstance testInstance, String section) throws Exception {
		TestKitSearchPath searchPath = session.getTestkitSearchPath();
		TestDefinition testDefinition = searchPath.getTestDefinition(testInstance.getId());
		return testDefinition.getTestPlanText(section);
	}

	/**
	 *
	 * @param testInstance
	 * @param section
	 * @return
	 * @throws Exception
	 */
	public TestPartFileDTO getTestplanDTO(TestInstance testInstance, String section) throws Exception {
		try {
			if (session != null)
				logger.debug(session.id() + ": " + "getTestplanAsText");

			File tsFile;
			OMElement testPlanEle;

			try {
				TestDefinition testDefinition = session.getTestkitSearchPath().getTestDefinition(testInstance.getId());
				tsFile = testDefinition.getTestplanFile(section);
				testPlanEle = Util.parse_xml(tsFile);
			} catch (Exception e) {
				throw new Exception("Cannot load test plan " + testInstance + "#" + section);
			}
			TestPartFileDTO testplanDTO = new TestPartFileDTO(TestPartFileDTO.TestPartFileType.SECTION_TESTPLAN_FILE);
			String content = new OMFormatter(tsFile).toString();
			testplanDTO.setPartName(section);
			testplanDTO.setFile(tsFile.toString());
			testplanDTO.setContent(content);
			testplanDTO.setHtlmizedContent(XmlFormatter.htmlize(content));

			List<Gather> gathers = null;

			List<OMElement> gatherEles = XmlUtil.decendentsWithLocalName(testPlanEle, "Gather");
			if (gatherEles.size() > 0) {
				gathers = new ArrayList<>();
				for (OMElement gatherEle : gatherEles) {
					gathers.add(new Gather(gatherEle.getAttributeValue(new QName("prompt")), null));
				}
				testplanDTO.setGathers(gathers);
			}


			return testplanDTO;
		} catch (Throwable t) {
			throw new Exception(t.getMessage() + "\n" + ExceptionUtil.exception_details(t));
		}
	}

	public TestPartFileDTO popStepMetadataFile(TestPartFileDTO sectionTpf) throws Exception{
		String testplanFileString = sectionTpf.getFile();
		File testplanFile = new File(testplanFileString);
		String testplanXmlString = sectionTpf.getContent();

		try {
			OMElement testplanEle = Parse.parse_xml_string(testplanXmlString);
			List<OMElement> testSteps = XmlUtil.decendentsWithLocalName(testplanEle, "TestStep");
			for (OMElement testStep : testSteps) {
				String stepName = testStep.getAttributeValue(MetadataSupport.id_qname);
				OMElement metadataFileEle = XmlUtil.firstDecendentWithLocalName(testStep, "MetadataFile");
				if (metadataFileEle == null) continue;
				String metadataFileName = metadataFileEle.getText();
				if (metadataFileName == null || metadataFileName.equals("")) continue;
				File metadataFile = new File(testplanFile.getParent(), metadataFileName);
				if (!(metadataFile.exists())) continue;
				TestPartFileDTO stepTpf = new TestPartFileDTO(TestPartFileDTO.TestPartFileType.STEP_METADATA_FILE);
				stepTpf.setPartName(stepName);
				stepTpf.setFile(metadataFile.toString());
				sectionTpf.getStepList().add(stepName);
				sectionTpf.getStepTpfMap().put(stepName,stepTpf);
			}
		} catch (Throwable t) {
			throw new Exception("Error traversing metadataFile elements:" + t.toString() + " testplan file: " + testplanFileString + " xmlString: " + testplanXmlString);
		}
		return sectionTpf;
	}

	public TestPartFileDTO getSectionTestPartFile(TestInstance testInstance, String section) throws Exception {

		TestPartFileDTO sectionTpf = getTestplanDTO(testInstance, section);

		if (sectionTpf!=null) {
			popStepMetadataFile(sectionTpf);

			// See if this section has a ContentBundle
			File contentBundle = new File(new File(sectionTpf.getFile()).getParentFile(),"ContentBundle");
			if (contentBundle.exists() && contentBundle.isDirectory()) {
				List<TestPartFileDTO> cbSections = new ArrayList<>();
				List<String> contentBundleSections = getTestSections(contentBundle.toString());
				if (contentBundleSections.size()>0) {
					for (String cbSectionName : contentBundleSections) {
						TestPartFileDTO cbSection = getTestplanDTO(testInstance, section + File.separator + "ContentBundle" + File.separator + cbSectionName);
						popStepMetadataFile(cbSection);
						cbSections.add(cbSection);
					}
					sectionTpf.setContentBundle(cbSections);
				}

			}
		}
		return sectionTpf;
	}

	public static TestPartFileDTO loadTestPartContent(TestPartFileDTO testPartFileDTO) throws Exception {
		File f = new File(testPartFileDTO.getFile());
		if (f.exists()) {
			String content = new OMFormatter(f).toString();
			testPartFileDTO.setContent(content);
			testPartFileDTO.setHtlmizedContent(XmlFormatter.htmlize(content));
		}
		return testPartFileDTO;
	}

	public List<TestInstance> getTestlogListing(String sessionName) throws Exception {
		if (session != null)
			logger.debug(session.id() + ": " + "getTestlogListing(" + sessionName + ")");

		if (Installation.instance().propertyServiceManager().isMultiuserMode())
			throw new ToolkitRuntimeException("Function getTestlogListing() not available in MulitUserMode");

		List<String> sessionNames = TestSessionServiceManager.INSTANCE.getNames();

		if (!sessionNames.contains(sessionName))
			throw new Exception("Don't understand session name " + sessionName);

		File sessionDir = new File(Installation.instance().propertyServiceManager().getTestLogCache() +
				File.separator + sessionName);

		List<String> names = new ArrayList<>();

		for (File test : sessionDir.listFiles()) {
			if (!test.isDirectory() || test.getName().equals("Results"))
				continue;
			names.add(test.getName());
		}

		List<TestInstance> testInstances = new ArrayList<TestInstance>();
		for (String name : names) testInstances.add(new TestInstance(name, TestSessionServiceManager.INSTANCE.getTestSession(session)));

		return testInstances;
	}

	public List<TestOverviewDTO> getTestsOverview(TestSession testSession, List<TestInstance> testInstances) throws Exception {
		List<TestOverviewDTO> results = new ArrayList<>();
		try {
			for (TestInstance testInstance : testInstances) {
				try {
					results.add(getTestOverview(testSession, testInstance));
				} catch (Exception e) {
					logger.error("Test " + testInstance + " does not exist");
				}
			}
		} catch (Exception e) {
			throw e;
		}
		return results;
	}



	/**
	 * Return the contents of all the log.xml files found under external_cache/TestLogCache/&lt;sessionName&gt;.  If there
	 * are multiple SECTIONS to the test then load them all. Each element of the
	 * returned list (Result model) represents the output of all steps in a single section of the test.
	 * @param testSession - not the servlet session but instead the dir name
	 * under external_cache/TestLogCache identifying the user of the service
	 * @param testInstance like 12355
	 * @return
	 * @throws Exception
	 */
	public TestOverviewDTO getTestOverview(TestSession testSession, TestInstance testInstance) throws Exception {
		try {
			//if (session != null)
			//logger.debug(session.id() + ": " + "getTestOverview(" + testInstance + ")");

			testInstance.setTestSession(testSession);

			File testDir = getTestLogCache().getTestDir(testSession, testInstance);

			LogMapDTO lm = null;
			if (testDir != null)
				lm = buildLogMap(testDir, testInstance);

			TestLogDetails testLogDetails = new TestLogDetails(session.getTestkitSearchPath().getTestDefinition(testInstance.getId()), testInstance);
			if (testLogDetails == null)
				throw new XdsInternalException(testInstance + " no longer exists");
			List<TestLogDetails> testLogDetailsList = new ArrayList<TestLogDetails>();
			testLogDetailsList.add(testLogDetails);

			if (testDir != null) {
				for (String section : lm.getLogFileContentMap().keySet()) {
					LogFileContentDTO ll = lm.getLogFileContentMap().get(section);
					testLogDetails.addTestPlanLog(section, ll);
				}

				// Save the created logs in the SessionCache (or testLogCache if this is a conformance test)
				TestInstance logid = newTestLogId();
				logid.setTestSession(testSession);

				//  -  why is a method named getTestOverview doing a WRITE???????
				if (session.transactionSettings.logRepository != null)
					session.transactionSettings.logRepository.logOut(logid, lm);
			}

			TestOverviewDTO dto = new TestOverviewBuilder(session, testLogDetails).build();
			if (testDir == null)
				dto.setRun(false);
			else
				dto.setLogMapDTO(lm);
			return dto;
		} catch (Exception e) {
			if (e.getMessage() != null && !e.getMessage().equals("")) throw e;
			throw new Exception(ExceptionUtil.exception_details(e));
		}
	}

	// testInstance.user must be set or null will be returned
//	private File getTestLogDir(TestInstance testInstance) throws IOException {
//		return getTestLogCache().getTestDir(testInstance);
//	}

	public LogFileContentDTO getTestLogDetails(TestSession testSession, TestInstance testInstance) throws Exception {
		try {
			if (session != null)
				logger.debug(session.id() + ": " + "getTestOverview(" + testInstance + ")");

			testInstance.setTestSession(testSession);

			File testDir = getTestLogCache().getTestDir(testSession, testInstance);

			String sectionName = testInstance.getSection();
			File logFile;

			if (sectionName == null) {
				logFile = new File(testDir, "log.xml");
			} else {
				logFile = new File(new File(testDir, sectionName), "log.xml");
			}

			return new LogFileContentBuilder().build(logFile);
		} catch (Exception e) {
			if (e.getMessage() != null) throw e;
			throw new Exception(ExceptionUtil.exception_details(e));
		}

	}

	private LogMapDTO buildLogMap(File testDir, TestInstance testInstance) throws Exception {
		LogMapDTO lm = new LogMapDTO();

		TestDefinition testDefinition = session.getTestkitSearchPath().getTestDefinition(testInstance.getId());
		if (testDefinition == null)
			return lm;
		List<String> sectionNames = testDefinition.getSectionIndex();

		if (sectionNames.size() == 0) {   // now illegal
			File[] files = testDir.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.isFile() && f.getName().equals("log.xml")) {
						LogFileContentDTO ll = new LogFileContentBuilder().build(f);
						lm.add(f.getName(), ll);
					} else if (f.isDirectory()) {
						File logfile = new File(f, "log.xml");
						if (logfile.exists()) {
							LogFileContentDTO ll = new LogFileContentBuilder().build(logfile);
							lm.add(f.getName(), ll);
						}
					}
				}
			}

		} else {
			for (String sectionName : sectionNames ) {
				File lfx = new File(testDir + File.separator + sectionName + File.separator + "log.xml");
				try {
					LogFileContentDTO ll = new LogFileContentBuilder().build(lfx);
					lm.add(sectionName, ll);
				} catch (Exception e)
				{
					continue;
				}
			}
		}

		return lm;
	}


	private List<File> testLogDirsInTestSession(TestSession testSession) throws IOException {
		List<File> testLogDirs = new ArrayList<>();
		TestLogCache testLogCache = getTestLogCache();
		File sessionDir = testLogCache.getSessionDir(testSession);
		if (!sessionDir.exists())
			return testLogDirs;
		File[] files = sessionDir.listFiles();
		for (File file : files) {
			if (!file.isDirectory()) continue;
			testLogDirs.add(file);
		}
		return testLogDirs;
	}

	private List<LogMapDTO> getLogsForTestSession(TestSession testSession) throws Exception {
		List<LogMapDTO> logs = new ArrayList<>();

		for (File testLogDir : testLogDirsInTestSession(testSession)) {
			String testId = testLogDir.getName();
			LogMapDTO logMapDTO = buildLogMap(testLogDir, new TestInstance(testId, testSession));
			logs.add(logMapDTO);
		}

		return logs;
	}

	/**
	 * Validate testSession and site exist and that either the testSession is empty or it contains
	 * only test results for that site.
	 * @param testSession
	 * @param siteName
	 * @return status
	 */
	public ConformanceSessionValidationStatus validateConformanceSession(TestSession testSession, String siteName) throws Exception {
		List<LogMapDTO> logMapDTOs = getLogsForTestSession(testSession);
		if (siteName == null || siteName.equals("")) return new ConformanceSessionValidationStatus();
		Set<String> badSites = new HashSet<>();
		for (LogMapDTO logMapDTO : logMapDTOs) {
			Map<String, LogFileContentDTO> map = logMapDTO.getLogFileContentMap();
			for (LogFileContentDTO logFileContentDTO : map.values()) {
				String site = logFileContentDTO.getSiteName();
				if (site == null || site.equals("")) continue;
				if (!site.equals(siteName)) {
					badSites.add(site);
				}
			}
		}
		if (badSites.size() == 0) return new ConformanceSessionValidationStatus();
		StringBuilder buf = new StringBuilder();
		buf.append("Test Session ").append(testSession).append(" already has results for these sites: ").append(badSites.toString() +
				" you cannot use it to test " + siteName);
		return new ConformanceSessionValidationStatus(false, buf.toString());
	}

	public Collection<String> getSitesForTestSession(TestSession testSession) throws Exception {
		Set<String> sites = new HashSet<>();
		List<LogMapDTO> logMapDTOs = getLogsForTestSession(testSession);
		for (LogMapDTO logMapDTO : logMapDTOs) {
			Map<String, LogFileContentDTO> map = logMapDTO.getLogFileContentMap();
			for (LogFileContentDTO logFileContentDTO : map.values()) {
				String site = logFileContentDTO.getSiteName();
				if (site != null && !site.equals(""))
					sites.add(site);
			}
		}
		return sites;
	}

	public CodesResult getCodesConfiguration() {
		if (session != null)
			logger.debug(session.id() + ": " + "currentCodesConfiguration");

		CodesResult codesResult = new CodesResult();

		try {
			codesResult.codesConfiguration = codesConfiguration();
		} catch (Exception e) {
			codesResult.result = buildResult(e);
		} finally {
			if (codesResult.result == null)
				codesResult.result = buildResult();
		}
		return codesResult;
	}

	CodesConfiguration codesConfiguration() throws XdsInternalException,
			FactoryConfigurationError, EnvironmentNotSelectedException {
		if (codesConfiguration == null)
			codesConfiguration = new CodesConfigurationBuilder(
					//					new File(warHome + "/toolkitx/codes/codes.xml")
					session.getCodesFile()
			)
					.get();
		return codesConfiguration;
	}



	void cleanupParams(Map<String, String> params) {
		for (String key : params.keySet()) {
			if (params.get(key) == null)
				params.put(key, "");
		}
	}

	//	Result mkResult(Throwable t) {
	//		Result r = mkResult();
	//		r.addAssertion(ExceptionUtil.exception_details(t), false);
	//		return r;
	//	}

	//	Result mkResult() {
	//		Result r = new Result();
	//		Calendar calendar = Calendar.getInstance();
	//		r.timestamp = calendar.getTimestamp().toString();
	//
	//		return r;
	//	}

	//	Result mkResult(XdstestLogId id) {
	//		Result result = mkResult();
	//		result.testId = id;
	//		return result;
	//	}

	TestInstance newTestLogId() {
		return new TestInstance(UuidAllocator.allocate().replaceAll(":", "_"), TestSessionServiceManager.INSTANCE.getTestSession(session));
	}

	Result buildResult(List<TestLogDetails> testLogDetailses, TestInstance logId) throws Exception {
		TestInstance testInstance;
		if (testLogDetailses.size() == 1) {
			testInstance = testLogDetailses.get(0).getTestInstance();
		} else {
			testInstance = new TestInstance("Combined_Test", logId.getTestSession());
		}
		Result result = ResultBuilder.RESULT(testInstance);
		result.logId = logId;
		//		Result result = mkResult(testId);

		//		// Also save the log file organized by sessionID, siteName, testNumber
		//		// This allows xdstest2 to use sessionID/siteNae dir as the LOGDIR
		//		// for referencing old log file. May also lead to downloadable log files
		//		// for Pre-Connectathon test results
		//
		//		SessionCache sc = new SessionCache(s, getTestLogCache());
		//		for (LogMapItemDTO item : lm.items) {
		//			sc.addLogFile(item.log);
		//
		//		}

		// load metadata results into Result
		//		List<TestSpec> testLogDetailses = s.xt.getTestSpecs();
		for (TestLogDetails testLogDetails : testLogDetailses) {
			for (String section : testLogDetails.sectionLogMapDTO.keySet()) {
				if (section.equals("THIS"))
					continue;
				LogFileContentDTO logFileContentDTO = testLogDetails.sectionLogMapDTO.get(section);
				for (int i = 0; i < logFileContentDTO.size(); i++) {
					StepResult stepResult = new StepResult();

					boolean stepPass = false;
					result.stepResults.add(stepResult);
					try {
						TestStepLogContentDTO testStepLogContentDTO = logFileContentDTO.getTestStepLog(i);
						stepResult.section = section;
						stepResult.stepName = testStepLogContentDTO.getId();
						stepResult.status = testStepLogContentDTO.getStatus();
						stepResult.transaction = testStepLogContentDTO.getTransaction();
						stepResult.setSoapFaults(testStepLogContentDTO.getSoapFaults());

						stepPass = stepResult.status;

						logger.info("test section " + section + " has status " + stepPass);

						// a transaction can have metadata in the request OR
						// the response
						// look in both places and save
						// If this is a retrieve then no metadata will be
						// found
						boolean inRequest = false;
						try {
							String input = testStepLogContentDTO.getInputMetadata();
							stepResult.setRawResults(input);
//								try {
//									IBaseResource resource = FhirSupport.parse(input);
//									ResourceToMetadataCollectionParser parser = new ResourceToMetadataCollectionParser();
//									parser.add(resource, null);
//									stepResult.setMetadata(parser.get());
//									inRequest = true;
//								} catch (Throwable e) {
									Metadata m = MetadataParser
											.parseNonSubmission(input);
									if (m.getAllObjects().size() > 0) {
										MetadataToMetadataCollectionParser mcp = new MetadataToMetadataCollectionParser(
												m, stepResult.stepName);
										stepResult.setMetadata(mcp.get());
										inRequest = true;
									}
//								}
						} catch (Exception e) {
							logger.warn("Cannot convert logs for section " + section + " for UI");
						}

						boolean inResponse = false;
						if (!inRequest) {
							try {
								String reslt = testStepLogContentDTO.getResult();
//								try {
//									IBaseResource resource = FhirSupport.parse(reslt);
//									ResourceToMetadataCollectionParser parser = new ResourceToMetadataCollectionParser();
//									parser.add(resource, null);
//									stepResult.setMetadata(parser.get());
//								} catch (Throwable e) {
									Metadata m = MetadataParser
											.parseNonSubmission(reslt);
									MetadataToMetadataCollectionParser mcp = new MetadataToMetadataCollectionParser(
											m, stepResult.stepName);
									stepResult.setMetadata(mcp.get());
//								}
								inResponse = true;
							} catch (Exception e) {
								logger.warn("Cannot convert logs for section " + section + " for UI");
							}
						}

						if (inRequest || inResponse)
							result.includesMetadata = true;

						// look for document contents
						if (stepPass) {
							String response = null;
							try {
								response = testStepLogContentDTO.getResult();  // throws exception on Direct messages (no response)
								if (response != null && response.trim().startsWith("<")) {
									OMElement rdsr = Util.parse_xml(response);
									if (!rdsr.getLocalName().equals(
											"RetrieveDocumentSetResponse"))
										rdsr = XmlUtil
												.firstDecendentWithLocalName(
														rdsr,
														"RetrieveDocumentSetResponse");
									if (rdsr != null) {

										// Issue 103: We need to propagate the response status since the interpretation of a StepResult of "Pass" to "Success" is not detailed enough with the additional status of PartialSuccess. This fixes the issue of RetrieveDocs tool, displaying a "Success" when it is actually a PartialSuccess.
										try {
											String rrStatusValue = XmlUtil.firstDecendentWithLocalName(rdsr, "RegistryResponse").getAttributeValue(new QName("status"));
											stepResult.setRegistryResponseStatus(rrStatusValue);
										} catch (Throwable t) {
											logger.error(t.toString());
										}

										RetrievedDocumentsModel rdm = new RetrieveResponseParser(rdsr).get();

										Map<String, RetrievedDocumentModel> resMap = rdm.getMap();
										for (String docUid : resMap.keySet()) {
											RetrievedDocumentModel ri = resMap.get(docUid);
											Document doc = new Document();
											doc.uid = ri.getDocUid();
											doc.repositoryUniqueId = ri
													.getRepUid();
											doc.newUid = ri.getNewDoc_uid();
											doc.newRepositoryUniqueId = ri.getNewRep_uid();
											doc.mimeType = ri.getContent_type();
											doc.homeCommunityId = ri.getHome();
											doc.cacheURL = getRepositoryCacheWebPrefix()
													+ doc.uid
													+ LogFileContentBuilder.getRepositoryCacheFileExtension(doc.mimeType);

											if (stepResult.documents == null)
												stepResult.documents = new ArrayList<Document>();
											stepResult.documents.add(doc);
										}
									}
								}
							} catch (Exception e) {

							}

						}
					} catch (Exception e) {
						result.assertions.add(
								ExceptionUtil.exception_details(e), false);
					}
				}

			}
		}

		return result;

	}

	private String getRepositoryCacheWebPrefix() {
		String toolkitHost = session.getServerIP();
		// context.getInitParameter("toolkit-host").trim();
		String toolkitPort = session.getServerPort();
		// context.getInitParameter("toolkit-port").trim();
//		return "http://" + toolkitHost + ":" + toolkitPort
//				+ Session.servletContextName + "/DocumentCache/";
		return  "DocumentCache/";
	}


	/******************************************************************
	 *
	 * Expose these methods to the ToolkitService
	 *
	 ******************************************************************/


	public List<String> getTestdataSetListing(String environmentName,TestSession testSession,String testdataSetName) {
		logger.debug(session.id() + ": " + "getTestdataSetListing:" + testdataSetName);
		TestKitSearchPath searchPath = new TestKitSearchPath(environmentName, testSession);
		Collection<String> listing = searchPath.getTestdataSetListing(testdataSetName);
		return new ArrayList<String>(listing);
	}

	public Map<String, String> getCollectionNames(String collectionSetName) throws Exception  {
		logger.debug(session.id() + ": " + "getCollectionNames(" + collectionSetName + ")");
		Map<String,String> collectionNames=new HashMap<String,String>();
		List<File> testkitsFiles=Installation.instance().testkitFiles(session.getCurrentEnvName(),session.getTestSession());
		for (File testkitFile:testkitsFiles){
			TestKit tk=new TestKit(testkitFile);
			Map<String, String> tmpCollectionNames=tk.getCollectionNames(collectionSetName);
			for (String key:tmpCollectionNames.keySet()) {
				if (!collectionNames.containsKey(key)) {
					collectionNames.put(key, tmpCollectionNames.get(key));
				}
			}
		}
		return collectionNames;
	}

	public List<Result> sendPidToRegistry(SiteSpec site, List<Pid> pid, String environmentName, TestSession testSession) throws Exception {
		if (session != null)
			logger.debug(session.id() + ": " + "sendPidToRegistry(" + pid + ")");

		List<Result> results = new ArrayList<>();
		session.setSiteSpec(site);
		Map<String, String> params = new HashMap<>();
		TestInstance testInstance = new TestInstance("PidFeed", testSession);
		for (Pid aPid : pid) {
			if (session.xt == null) {
				TestKitSearchPath searchPath = new TestKitSearchPath(environmentName, testSession);
				session.xt = new Xdstest2(Installation.instance().toolkitxFile(), searchPath, session, testSession);
			}

			params.put("$pid$", aPid.asString());
			results.add(new UtilityRunner(this, TestRunType.UTILITY).run(session, params, null, null, testInstance, null, true));
		}
		return results;
	}


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Tests Overview Tab
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// TODO To complete

	/**
	 * A Test model includes a Test Id or Instance Number, a Short Description, a Time and a Status. This model is used
	 * for display purposes only. Build similar objects using the package Results, or replace Test with a similar
	 * existing model.
	 */
	public List<Test> reloadAllTestResults(String sessionName) throws Exception {
//		List<TestInstance> testList = null;
//		Map<String, Result> results = null;
//		List<Test> display = new ArrayList<Test>();
//
//		System.out.println("test session name: "+sessionName);
//
//		// ----- Retrieve list of test instance numbers -----
//		// TODO is there a case where sessionName might not be found in the system (bug?)
//		if (sessionName == null) {
//			logger.error("Could not retrieve the list of test instance numbers because the user session is null");
//			// TODO throw new TestRetrievalException
//		}
//		else { testList = getTestlogListing(sessionName); }
//
//		// ----- Retrieve test log results for each test instance -----
//		if (testList == null){
//			logger.error("Could not retrieve the log results");
//			// TODO throw new TestRetrievalException
//			}
//		else {
//			results = getTestResults(testList, sessionName);
//			String testId;
//			Result res;
//			List<StepResult> sectionList;
//			boolean hasSections = false;
//
//			System.out.println("building data for display");
//			// Use the set of Results to build the data for display
//			for (Map.Entry<String, Result> entry: results.entrySet()){
//				testId = entry.getKey();
//				res = entry.getValue();
//				sectionList = res.getStepResults();
//
//				// Check whether the test has SECTIONS
//				if (sectionList == null || (sectionList.size() == 0)) { hasSections = true; }
//
//				// TODO not sure what the test status is
//				display.add(new Test(10500, false, z"", "", res.getText(), res.getTimestamp(), "pass"));
//			}
//		}
//		return display;

		// Test data
		return Arrays.asList(
				new Test(10891, false, "10891", "10891", "test 1", "04:10 PM EST", "failed"),
				new Test(10891, true, "10891", "section a", "test 1", "04:10 PM EST", "failed"),
				new Test(10891, true, "10891", "section b", "test 1", "04:12 PM EST", "pass"),
				new Test(17685, false, "17685", "17685", "test 2", "04:10 PM EST", "not run"),
				new Test(17688, false, "17688", "17688", "test 3", "04:15 PM EST", "run with warnings")
		);
	}

	public List<Test> runAllTests(String sessionName, Site site){
		// Test data
		return Arrays.asList(
				new Test(10891, false, "10891", "10891", "re-run test 1", "04:10 PM EST", "pass"),
				new Test(10891, true, "10891a", "section a", "re-run test 1", "04:10 PM EST", "pass"),
				new Test(10891, true, "10891b", "section b", "re-run test 1", "04:12 PM EST", "pass"),
				new Test(17685, false, "17685", "17685", "re-run test 2", "04:10 PM EST", "failed")
		);
		//    public Test(int _id, boolean _isSection, String _idWithSection, String _name, String _description, String _timestamp, String _status){
	}

	public List<Test> deleteAllTestResults(String sessionName, Site site){
		// Test data
		return Arrays.asList(
				new Test(10891, false, "10891", "10891", "test 1", "--", "not run"),
				new Test(10891, true, "10891a", "section a", "test 1", "--", "not run"),
				new Test(10891, true, "10891b", "section b", "test 1", "--", "not run"),
				new Test(17685, false, "17685", "17685", "test 2", "--", "not run")
		);
	}

	public Test runSingleTest(String sessionName, Site site, int testId) {
		// Test data
		return new Test(testId, false, "test#", "test name", "returned result test", "05:23 PM EST", "failed");
	}

	public TestOverviewDTO deleteSingleTestResult(String environmentName, TestSession testSession, TestInstance testInstance) throws Exception {
		try {
			TestKitSearchPath searchPath = new TestKitSearchPath(environmentName, testSession);
			TestDefinition testDef = searchPath.getTestDefinition(testInstance.getId());
			List<String> sectionNames = testDef.getSectionIndex();
			new ResultPersistence().delete(testInstance, testSession, sectionNames);
		} catch (Exception e) {
			logger.info("Cannot delete test " + testInstance + e.getMessage());
			// oh well
		}
		return getTestOverview(testInstance.getTestSession(), testInstance);

	}

	private static final String SITEFILE = "site.txt";

	public String getAssignedSiteForTestSession(TestSession testSession) throws IOException {
		TestLogCache testLogCache = getTestLogCache();
		File testSessionDir = testLogCache.getSessionDir(testSession);
		if (!testSessionDir.exists() || !testSessionDir.isDirectory()) return null;
		try {
			return Io.stringFromFile(new File(testSessionDir, SITEFILE)).trim();
		} catch (IOException e) {
			// none assigned
			return null;
		}
	}

	public void setAssignedSiteForTestSession(TestSession testSession, String siteName) throws IOException {
		if (!Installation.instance().testSessionExists(testSession))
			throw new IOException("Test Session " + testSession + " does not exist");
		TestLogCache testLogCache = getTestLogCache();
		File testSessionDir = testLogCache.getSessionDir(testSession);
		testSessionDir.mkdirs();
//		if (!testSessionDir.exists() || !testSessionDir.isDirectory())
//			throw new IOException("Test Session " + testSession + " does not exist");
		if (siteName == null) {
			Io.delete(new File(testSessionDir, SITEFILE));
		} else {
			Io.stringToFile(new File(testSessionDir, SITEFILE), siteName);
		}
	}

	private void clearAssignedSiteForTestSession(TestSession testSession) throws IOException {
		TestLogCache testLogCache = getTestLogCache();
		File testSessionDir = testLogCache.getSessionDir(testSession);
		if (!testSessionDir.exists() || !testSessionDir.isDirectory())
			return;
		Io.delete(new File(testSessionDir, SITEFILE));
	}

	public String clearTestSession(TestSession testSession) throws IOException {
		TestLogCache testLogCache = getTestLogCache();
		File testSessionDir = testLogCache.getSessionDir(testSession);
		Io.deleteContents(testSessionDir);
		return null;
	}


}
