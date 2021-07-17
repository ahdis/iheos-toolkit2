package gov.nist.toolkit.xdstools2.client.util;

import com.google.gwt.user.client.rpc.AsyncCallback;
import gov.nist.toolkit.actortransaction.client.TransactionInstance;
import gov.nist.toolkit.configDatatypes.client.Pid;
import gov.nist.toolkit.datasets.shared.DatasetModel;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.interactionmodel.client.InteractingEntity;
import gov.nist.toolkit.registrymetadata.client.MetadataCollection;
import gov.nist.toolkit.results.client.CodesResult;
import gov.nist.toolkit.results.client.DocumentEntryDetail;
import gov.nist.toolkit.results.client.Result;
import gov.nist.toolkit.results.client.Test;
import gov.nist.toolkit.results.client.TestInstance;
import gov.nist.toolkit.results.client.TestLogs;
import gov.nist.toolkit.services.client.IdcOrchestrationRequest;
import gov.nist.toolkit.services.client.PifType;
import gov.nist.toolkit.services.client.RawResponse;
import gov.nist.toolkit.session.client.ConformanceSessionValidationStatus;
import gov.nist.toolkit.session.client.TestSessionStats;
import gov.nist.toolkit.session.client.logtypes.TestOverviewDTO;
import gov.nist.toolkit.session.client.logtypes.TestPartFileDTO;
import gov.nist.toolkit.session.shared.Message;
import gov.nist.toolkit.simcommon.client.SimId;
import gov.nist.toolkit.simcommon.client.Simulator;
import gov.nist.toolkit.simcommon.client.SimulatorConfig;
import gov.nist.toolkit.simcommon.client.SimulatorStats;
import gov.nist.toolkit.sitemanagement.client.Site;
import gov.nist.toolkit.sitemanagement.client.TransactionOfferings;
import gov.nist.toolkit.testenginelogging.client.LogFileContentDTO;
import gov.nist.toolkit.testkitutilities.client.SectionDefinitionDAO;
import gov.nist.toolkit.testkitutilities.client.TestCollectionDefinitionDAO;
import gov.nist.toolkit.tk.client.TkProps;
import gov.nist.toolkit.valsupport.client.MessageValidationResults;
import gov.nist.toolkit.xdstools2.client.tabs.conformanceTest.TabConfig;
import gov.nist.toolkit.xdstools2.client.tabs.conformanceTest.UserTestCollection;
import gov.nist.toolkit.xdstools2.shared.RegistryStatus;
import gov.nist.toolkit.xdstools2.shared.RepositoryStatus;
import gov.nist.toolkit.xdstools2.shared.command.CommandContext;
import gov.nist.toolkit.xdstools2.shared.command.InitializationResponse;
import gov.nist.toolkit.xdstools2.shared.command.request.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ToolkitServiceAsync {

    void getAutoInitConformanceTesting(CommandContext context,AsyncCallback<Boolean> callback);
    void clearTestSession(CommandContext context, AsyncCallback<String> callback);
    void validateConformanceSession(String testSession, String siteName, AsyncCallback<ConformanceSessionValidationStatus> callback);
    void getSitesForTestSession(CommandContext context, AsyncCallback<Collection<String>> callback);
    void getInitialization(CommandContext context,AsyncCallback<InitializationResponse> callback);
    void getTkProps(AsyncCallback<TkProps> callback);
    void getSessionProperties(AsyncCallback<Map<String, String>> callback);
    void setSessionProperties(Map<String, String> props, AsyncCallback callback);

    void getDefaultAssigningAuthority(CommandContext context, AsyncCallback<String> callback) ;
    void getAttributeValue(String username, String attName, AsyncCallback<String> callback);
    void setAttributeValue(String username, String attName, String attValue, AsyncCallback callback);


    void getCurrentEnvironment(AsyncCallback<String> callback);
    void getDefaultEnvironment(CommandContext context, AsyncCallback<String> callback);
    void setEnvironment(CommandContext context, AsyncCallback<String> callback);
    void getEnvironmentNames(CommandContext context, AsyncCallback<List<String>> callback);
    void isGazelleConfigFeedEnabled(CommandContext context, AsyncCallback<Boolean> callback) ;
    void reloadSystemFromGazelle(ReloadSystemFromGazelleRequest request, AsyncCallback<String> callback);
    void getSiteNamesWithRG(CommandContext context,AsyncCallback<List<String>> callback);
    void getSiteNamesByTranType(GetSiteNamesByTranTypeRequest request, AsyncCallback<List<String>> callback);

    void getDashboardRegistryData(CommandContext context, AsyncCallback<List<RegistryStatus>> callback);
    void getDashboardRepositoryData(CommandContext context, AsyncCallback<List<RepositoryStatus>> callback);

    void getTestsOverview(GetTestsOverviewRequest request, AsyncCallback<List<TestOverviewDTO>> callback);
    void getActorTestProgress(GetTestsOverviewRequest request, AsyncCallback<List<TestOverviewDTO>> callback);
    void getTestSectionsDAOs(GetTestSectionsDAOsRequest request, AsyncCallback<List<SectionDefinitionDAO>> callback);
    void getUpdateNames(AsyncCallback<List<String>> callback);

    void getTransactionRequest(GetTransactionRequest request, AsyncCallback<Message> callback);
    void getTransactionResponse(GetTransactionRequest request, AsyncCallback<Message> callback);
    void getTransactionLog(GetTransactionRequest request, AsyncCallback<String> callback);
    void getTransactionsForSimulator(GetTransactionRequest request, AsyncCallback<List<String>> callback);
    void getTransactionLogDirectoryPath(GetTransactionLogDirectoryPathRequest request, AsyncCallback<TransactionInstance> callback);
    void setSutInitiatedTransactionInstance(SetSutInitiatedTransactionInstanceRequest request, AsyncCallback<List<InteractingEntity>> callback);

//	void getActorNames(AsyncCallback<List<String>> notify);

    void executeSimMessage(ExecuteSimMessageRequest request, AsyncCallback<MessageValidationResults> callback);

    void renameSimFile(RenameSimFileRequest request, AsyncCallback callback);

    void deleteSimFile(DeleteSimFileRequest request, AsyncCallback callback);

    void getSimulatorEndpoint(CommandContext context, AsyncCallback<String> callback);

    void getSelectedMessage(GetSelectedMessageRequest request, AsyncCallback<List<Result>> callback);
    void getSelectedMessageResponse(GetSelectedMessageRequest request, AsyncCallback<List<Result>> callback);
    @Deprecated
    void getClientIPAddress(AsyncCallback<String> callback);

//	void  validateMessage(ValidationContext vc, String simFileName, AsyncCallback<MessageValidationResults> notify);

    void  getTransInstances(GetTransactionRequest request, AsyncCallback<List<TransactionInstance>> callback);
    void  getTransInstancesLists(GetTransactionListsRequest request, AsyncCallback<List<List<TransactionInstance>>> callback);

    void getLastMetadata(CommandContext context,AsyncCallback<List<Result>> callback);
    void getLastFilename(CommandContext context,AsyncCallback<String> callback);
    void getTimeAndDate(CommandContext context,AsyncCallback<String> callback);

    void updateDocumentEntry(UpdateDocumentEntryRequest request, AsyncCallback<Result> callback);
    void validateDocumentEntry(ValidateDocumentEntryRequest request, AsyncCallback<MessageValidationResults> callback);

    void validateMessage(ValidateMessageRequest vrequest, AsyncCallback<MessageValidationResults> callback);

    void getSiteNames(GetSiteNamesRequest request, AsyncCallback<List<String>> callback) ;

    void getTransactionOfferings(CommandContext commandContext, AsyncCallback<TransactionOfferings> callback);
    void getRegistryNames(AsyncCallback<List<String>> callback);
    void getRepositoryNames(AsyncCallback<List<String>> callback);
    void getRGNames(AsyncCallback<List<String>> callback);
    void getIGNames(AsyncCallback<List<String>> callback);
    void getRawLogs(GetRawLogsRequest request, AsyncCallback<TestLogs> callback);
    void getTestdataSetListing(GetTestdataSetListingRequest request, AsyncCallback<List<String>> callback);
    void getCodesConfiguration(CommandContext context, AsyncCallback<CodesResult> callback);
    void getSite(GetSiteRequest request, AsyncCallback<Site> callback);
    void getAllSites(CommandContext commandContext, AsyncCallback<Collection<Site>> callback);
    void saveSite(SaveSiteRequest request, AsyncCallback<String> callback);
    void reloadSites(boolean simAlso, AsyncCallback<List<String>> callback);
    void reloadExternalSites(CommandContext context,AsyncCallback<List<String>> callback);
    void deleteSite(DeleteSiteRequest request, AsyncCallback<String> callback);

    void getSSandContents(GetSubmissionSetAndContentsRequest request, AsyncCallback<List<Result>> callback);
    void srcStoresDocVal(GetSrcStoresDocValRequest request, AsyncCallback<List<Result>> callback);
    void findDocuments(FindDocumentsRequest request, AsyncCallback<List<Result>> callback);
    void findDocumentsByRefId(FindDocumentsRequest request, AsyncCallback<List<Result>> callback) ;
    void findFolders(FoldersRequest request, AsyncCallback<List<Result>> callback);
    void getDocuments(GetDocumentsRequest request, AsyncCallback<List<Result>> callback);
    void getFolders(GetFoldersRequest request, AsyncCallback<List<Result>> callback);
    void getFoldersForDocument(GetFoldersRequest request, AsyncCallback<List<Result>> callback);
    void getFolderAndContents(GetFoldersRequest request, AsyncCallback<List<Result>> callback);
    void getAssociations(GetAssociationsRequest request, AsyncCallback<List<Result>> callback);
    void getObjects(GetObjectsRequest request, AsyncCallback<List<Result>> callback);
    void getSubmissionSets(GetSubmissionSetsRequest request, AsyncCallback<List<Result>> callback);
    void registerAndQuery(RegisterAndQueryRequest request, AsyncCallback<List<Result>> callback);
    void getRelated(GetRelatedRequest request, AsyncCallback<List<Result>> callback);
    void retrieveDocument(RetrieveDocumentRequest request, AsyncCallback<List<Result>> callback);
    void retrieveImagingDocSet(RetrieveImagingDocSetRequest request, AsyncCallback<List<Result>> callback);
    void submitRegistryTestdata(SubmitTestdataRequest request, AsyncCallback<List<Result>> callback);
    void submitRepositoryTestdata(SubmitTestdataRequest request, AsyncCallback<List<Result>> callback);
    void submitXDRTestdata(SubmitTestdataRequest request, AsyncCallback<List<Result>> callback);
    /////////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    // FoldersRequest has the right constructor??? //
    /////////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    void provideAndRetrieve(ProvideAndRetrieveRequest request, AsyncCallback<List<Result>> callback);
    void lifecycleValidation(LifecycleValidationRequest request, AsyncCallback<List<Result>> callback);
    void folderValidation(FoldersRequest request, AsyncCallback<List<Result>> callback);

    //	void mpqFindDocuments(SiteSpec site, String pid, List<String> classCodes, List<String> hcftCodes, List<String> eventCodes, AsyncCallback<List<Result>> notify);
    void mpqFindDocuments(MpqFindDocumentsRequest request, AsyncCallback<List<Result>> callback);
    void getAll(GetAllRequest request, AsyncCallback<List<Result>> callback);
    void findDocuments2(FindDocuments2Request request, AsyncCallback<List<Result>> callback);

    void getAdminPassword(CommandContext context,AsyncCallback<String> callback);

    void getImplementationVersion(CommandContext context,AsyncCallback<String> callback);

    void getToolkitProperties(CommandContext context,AsyncCallback<Map<String, String>> callback);
    void setToolkitProperties(SetToolkitPropertiesRequest request, AsyncCallback<String> callback);
    void reloadPropertyFile(AsyncCallback<Boolean> callback);

    void getOrchestrationProperties(GetOrchestrationPropertiesRequest request, AsyncCallback<Map<String, String>> callback);
    void getOrchestrationPifType(GetOrchestrationPifTypeRequest request, AsyncCallback<PifType> callback);

    void getActorTypeNames(CommandContext context,AsyncCallback<List<String>> callback);
    void getNewSimulator(GetNewSimulatorRequest request, AsyncCallback<Simulator> callback);
    void getSimConfigs(GetSimConfigsRequest request,AsyncCallback<List<SimulatorConfig>> callback);
    void getAllSimConfigs(GetAllSimConfigsRequest user, AsyncCallback<List<SimulatorConfig>> callback);
    void putSimConfig(SimConfigRequest request, AsyncCallback<String> callback);
    void deleteConfig(SimConfigRequest request, AsyncCallback<String> callback);
    void getSimIdsForUser(GetSimIdsForUserRequest context, AsyncCallback<List<SimId>> callback);
    //	void getSimulatorTransactionNames(String simid, AsyncCallback<List<String>> notify);
    void removeOldSimulators(CommandContext context,AsyncCallback<Integer> callback);
    void getSimulatorStats(GetSimulatorStatsRequest request, AsyncCallback<List<SimulatorStats>> callback);
    void getPatientIds(PatientIdsRequest request, AsyncCallback<List<Pid>> callback);
    void addPatientIds(PatientIdsRequest request, AsyncCallback<String> callback);
    void deletePatientIds(PatientIdsRequest request, AsyncCallback<Boolean> callback);

    void getCollectionNames(GetCollectionRequest request, AsyncCallback<Map<String, String>> callback);

    void getCollectionMembers(GetCollectionRequest request, AsyncCallback<List<TestInstance>> callback);

    void getTestCollections(GetCollectionRequest request, AsyncCallback<List<TestCollectionDefinitionDAO>> callback);

    void getCollection(GetCollectionRequest request, AsyncCallback<Map<String, String>> callback);

    void getTestReadme(GetTestDetailsRequest request, AsyncCallback<String> callback);

    void getTestIndex(GetTestDetailsRequest request, AsyncCallback<List<String>> callback);
    void runMesaTest(RunTestRequest request, AsyncCallback<List<Result>> callback);
    void runTest(RunTestRequest request, AsyncCallback<TestOverviewDTO> callback);
    void isPrivateMesaTesting(AsyncCallback<Boolean> callback);
    void addTestSession(CommandContext context, AsyncCallback<Boolean> callback);
    void deleteTestSession(CommandContext context, AsyncCallback<Boolean> callback);
    void createPid(GeneratePidRequest generatePidRequest, AsyncCallback<Pid> callback);
    void getAssigningAuthority(CommandContext commandContext, AsyncCallback<String> callback);
    void getAssigningAuthorities(CommandContext commandContext, AsyncCallback<List<String>> callback);
    void sendPidToRegistry(SendPidToRegistryRequest request, AsyncCallback<List<Result>> callback);
    void getSimulatorEventRequest(GetSimulatorEventRequest request, AsyncCallback<Result> callback) ;
    void getSimulatorEventResponse(GetSimulatorEventRequest request, AsyncCallback<Result> callback) ;
    void getTestLogDetails(GetTestLogDetailsRequest request, AsyncCallback<LogFileContentDTO> callback);

    void getTestplanAsText(GetTestplanAsTextRequest request, AsyncCallback<String> callback);
    void getSectionTestPartFile(GetSectionTestPartFileRequest request, AsyncCallback<TestPartFileDTO> callback);
    void loadTestPartContent(LoadTestPartContentRequest request, AsyncCallback<TestPartFileDTO> callback);
    void getHtmlizedString(String xml, AsyncCallback<String> callback);

    void configureTestkit(CommandContext context, AsyncCallback<String> callback);
    void doesTestkitExist(CommandContext context, AsyncCallback<Boolean> asyncCallback) ;
    void generateTestkitStructure(CommandContext request, AsyncCallback<Void> asyncCallback);
    void indexTestKits(CommandContext context,AsyncCallback<Boolean> callback);

//	void getToolkitEnableNwHIN(AsyncCallback<String> notify);

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	// Test Services
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	void reloadAllTestResults(CommandContext context, AsyncCallback<List<Test>> callback) ;
	void getTestlogListing(String sessionName, AsyncCallback<List<TestInstance>> callback);
	void getTestResults(GetTestResultsRequest request, AsyncCallback<Map<String, Result>> callback);
	void buildTestSession(AsyncCallback<TestSession> callback);
    void testSessionExists(CommandContext request, AsyncCallback<Boolean> callback);
	void setTestSession(String sessionName, AsyncCallback callback);
	void getTestSessionNames(CommandContext request, AsyncCallback<List<String>> callback);
	void deleteAllTestResults(AllTestRequest request, AsyncCallback<List<Test>> callback);
	void deleteSingleTestResult(DeleteSingleTestRequest request, AsyncCallback<TestOverviewDTO> callback);
	void deleteMultipleTestLogs(DeleteMultipleTestLogsRequest request, AsyncCallback<List<TestOverviewDTO>> callback);
	void runAllTests(AllTestRequest request, AsyncCallback<List<Test>> callback);
	void runSingleTest(RunSingleTestRequest request, AsyncCallback<Test> callback);
	void getTransactionErrorCodeRefs(GetTransactionErrorCodeRefsRequest request, AsyncCallback<List<String>> callback);
	void buildIgTestOrchestration(BuildIgTestOrchestrationRequest request, AsyncCallback<RawResponse> callback);
	void buildRgTestOrchestration(BuildRgTestOrchestrationRequest request, AsyncCallback<RawResponse> callback);
	void buildIigTestOrchestration(BuildIigTestOrchestrationRequest request, AsyncCallback<RawResponse> callback);
	void buildRigTestOrchestration(BuildRigTestOrchestrationRequest request, AsyncCallback<RawResponse> callback);
	void buildIdsTestOrchestration(BuildIdsTestOrchestrationRequest request, AsyncCallback<RawResponse> callback);
//    void buildIdcTestOrchestration(IdcOrchestrationRequest request, AsyncCallback<RawResponse> callback);
	void buildRepTestOrchestration(BuildRepTestOrchestrationRequest request, AsyncCallback<RawResponse> callback);
	void buildRegTestOrchestration(BuildRegTestOrchestrationRequest request, AsyncCallback<RawResponse> callback);
	void buildRecTestOrchestration(BuildRecTestOrchestrationRequest request, AsyncCallback<RawResponse> callback);
	void buildRSNAEdgeTestOrchestration(BuildRSNAEdgeTestOrchestrationRequest request, AsyncCallback<RawResponse> callback);
    void buildIdcTestOrchestration(IdcOrchestrationRequest request, AsyncCallback<RawResponse> callback);
    void buildEsTestOrchestration(BuildEsTestOrchestrationRequest request, AsyncCallback<RawResponse> callback);
	void getSiteNamesWithRIG(CommandContext context, AsyncCallback<List<String>> callback);
	void getSiteNamesWithIDS(CommandContext context, AsyncCallback<List<String>> callback);
    void getSiteNamesWithRepository(CommandContext context, AsyncCallback<List<String>> callback);
	void register(RegisterRequest request, AsyncCallback<Result> callback) throws Exception;
	void registerWithLocalizedTrackingInODDS(RegisterRequest registerRequest, AsyncCallback<Map<String, String>> callback);
	void getOnDemandDocumentEntryDetails(GetOnDemandDocumentEntryDetailsRequest request, AsyncCallback<List<DocumentEntryDetail>> callback);
	void setOdSupplyStateIndex(SetOdSupplyStateIndexRequest request, AsyncCallback<Boolean> callback);
	void getInteractionFromModel(GetInteractionFromModelRequest request, AsyncCallback<InteractingEntity> callback);
	void getStsSamlAssertion(GetStsSamlAssertionRequest request, AsyncCallback<String> callback);
    void getStsSamlAssertionsMap(GetStsSamlAssertionMapRequest request, AsyncCallback<Map<String,String>> callback);


    void getServletContextName(AsyncCallback<String> callback);
    void retrieveConfiguredFavoritesPid(CommandContext commandContext, AsyncCallback<List<Pid>> callback);

    void getAssignedSiteForTestSession(CommandContext context, AsyncCallback<String> async);

    void setAssignedSiteForTestSession(SetAssignedSiteForTestSessionRequest request, AsyncCallback<Void> async);

    void getAllDatasets(CommandContext context, AsyncCallback<List<DatasetModel>> callback);

//    void fhirCreate(FhirCreateRequest request, AsyncCallback<List<Result>> async);

//    void fhirTransaction(FhirTransactionRequest request, AsyncCallback<List<Result>> async);

    void getFullSimId(GetFullSimIdRequest request, AsyncCallback<SimId> async);

    // Tab configuration
    void getToolTabConfig(GetTabConfigRequest request, AsyncCallback<TabConfig> callback);
    void getPrunedToolTabConfig(GetTabConfigRequest request, AsyncCallback<UserTestCollection> callback);

    void getDatasetContent(GetDatasetElementContentRequest var1, AsyncCallback<String> callback);

    void buildDocAdminTestOrchestration(BuildDocAdminTestOrchestrationRequest request, AsyncCallback<RawResponse> callback);
    void buildSrcTestOrchestration(BuildSrcTestOrchestrationRequest request, AsyncCallback<RawResponse> callback);
    void buildIsrTestOrchestration(BuildIsrTestOrchestrationRequest request, AsyncCallback<RawResponse> callback);

//    void buildFhirSupportOrchestration(FhirSupportOrchestrationRequest var1, AsyncCallback<RawResponse> callback);

//    void fhirSearch(FhirSearchRequest var1, AsyncCallback<List<Result>> callback);

//    void fhirRead(FhirReadRequest request, AsyncCallback<List<Result>> async);

//    void getFhirResult(GetRawLogsRequest request, AsyncCallback<List<Message>> async);

    void promote(PromoteRequest request, AsyncCallback<String> async);

    void getTestSessionStats(CommandContext commandContext, AsyncCallback<List<TestSessionStats>> async);

    void getMetadataFromRegIndex(GetMetadataFromRegIndexRequest request, AsyncCallback<MetadataCollection> callback);
}
