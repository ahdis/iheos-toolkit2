package gov.nist.toolkit.simcommon.server.factories;

import gov.nist.toolkit.actortransaction.shared.ActorType;
import gov.nist.toolkit.actortransaction.client.ParamType;
import gov.nist.toolkit.configDatatypes.server.SimulatorProperties;
import gov.nist.toolkit.configDatatypes.client.TransactionType;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.simcommon.client.SimId;
import gov.nist.toolkit.simcommon.client.Simulator;
import gov.nist.toolkit.simcommon.client.SimulatorConfig;
import gov.nist.toolkit.simcommon.server.AbstractActorFactory;
import gov.nist.toolkit.simcommon.server.IActorFactory;
import gov.nist.toolkit.simcommon.server.SimCache;
import gov.nist.toolkit.simcommon.server.SimManager;
import gov.nist.toolkit.sitemanagement.client.Site;
import gov.nist.toolkit.sitemanagement.client.TransactionBean;
import gov.nist.toolkit.sitemanagement.client.TransactionBean.RepositoryType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class RepositoryActorFactory extends AbstractActorFactory implements IActorFactory {

	static final String repositoryUniqueIdBase = "1.1.4567332.1.";
	static int repositoryUniqueIdIncr = 1;
	boolean isRecipient = false;

	static final List<TransactionType> incomingTransactions = 
		Arrays.asList(
				TransactionType.PROVIDE_AND_REGISTER,
				TransactionType.RETRIEVE);

	public RepositoryActorFactory(){ super(); }

	// Label as a DocumentRecipient
	public void asRecipient() {
		isRecipient = true;
	}

	public Simulator buildNew(SimManager simm, SimId simId, String environment, boolean configureBase) throws Exception {
		ActorType actorType = ActorType.REPOSITORY;
//		logger.fine("Creating " + actorType.getName() + " with id " + simId);
		SimulatorConfig sc;
		if (configureBase)
			sc = configureBaseElements(actorType, simId, simId.getTestSession(), environment);
		else
			sc = new SimulatorConfig();

		if (isRecipient) {
			addFixedEndpoint(sc, SimulatorProperties.pnrEndpoint, actorType, TransactionType.XDR_PROVIDE_AND_REGISTER, false);
			addFixedEndpoint(sc, SimulatorProperties.pnrTlsEndpoint, actorType, TransactionType.XDR_PROVIDE_AND_REGISTER, true);
		} else {   // Repository
			addEditableConfig(sc, SimulatorProperties.repositoryUniqueId, ParamType.TEXT, getNewRepositoryUniqueId(simId.getTestSession()));
			addFixedEndpoint(sc, SimulatorProperties.pnrEndpoint, actorType, TransactionType.PROVIDE_AND_REGISTER, false);
			addFixedEndpoint(sc, SimulatorProperties.pnrTlsEndpoint, actorType, TransactionType.PROVIDE_AND_REGISTER, true);
			addFixedEndpoint(sc, SimulatorProperties.retrieveEndpoint, actorType, TransactionType.RETRIEVE, false);
			addFixedEndpoint(sc, SimulatorProperties.retrieveTlsEndpoint, actorType, TransactionType.RETRIEVE, true);
			addFixedEndpoint(sc, SimulatorProperties.removeDocumentsEndpoint, actorType, TransactionType.REMOVE_DOCUMENTS, false);
			addFixedEndpoint(sc, SimulatorProperties.removeDocumentsTlsEndpoint, actorType, TransactionType.REMOVE_DOCUMENTS, true);
			addEditableNullEndpoint(sc, SimulatorProperties.registerEndpoint, actorType, TransactionType.REGISTER, false);
			addEditableNullEndpoint(sc, SimulatorProperties.registerTlsEndpoint, actorType, TransactionType.REGISTER, true);
			addEditableConfig(sc, SimulatorProperties.REMOVE_DOCUMENTS, ParamType.BOOLEAN, false);

		}

		return new Simulator(sc);
	}
	
	static synchronized String getNewRepositoryUniqueId(TestSession testSession) {
		Collection<String> existingIds;
		try {
			existingIds = SimCache.getAllRepositoryUniqueIds(testSession);
		} catch (Throwable t) {
			existingIds = new ArrayList<>();
		}
		String value = newValue();
		while (existingIds.contains(value)) {
			value = newValue();
		}
		return value;
	}

	private static String newValue() {
		return repositoryUniqueIdBase + repositoryUniqueIdIncr++;
	}

	protected void verifyActorConfigurationOptions(SimulatorConfig sc) {

	}

	@Override
	public Site buildActorSite(SimulatorConfig asc, Site site) {
		String siteName = asc.getDefaultName();

		if (site == null)
			site = new Site(siteName, asc.getId().getTestSession());

		site.setTestSession(asc.getId().getTestSession());  // labels this site as coming from a sim

		boolean isAsync = false;

		site.addTransaction(new TransactionBean(
				TransactionType.PROVIDE_AND_REGISTER.getCode(),
				RepositoryType.NONE,
				asc.get(SimulatorProperties.pnrEndpoint).asString(),
				false, 
				isAsync));
		site.addTransaction(new TransactionBean(
				TransactionType.PROVIDE_AND_REGISTER.getCode(),
				RepositoryType.NONE,
				asc.get(SimulatorProperties.pnrTlsEndpoint).asString(),
				true, 
				isAsync));

		site.addTransaction(new TransactionBean(
				TransactionType.REMOVE_DOCUMENTS.getCode(),
				RepositoryType.NONE,
				asc.get(SimulatorProperties.removeDocumentsEndpoint).asString(),
				false,
				isAsync));
		site.addTransaction(new TransactionBean(
				TransactionType.REMOVE_DOCUMENTS.getCode(),
				RepositoryType.NONE,
				asc.get(SimulatorProperties.removeDocumentsTlsEndpoint).asString(),
				true,
				isAsync));

		site.addRepository(new TransactionBean(
				asc.get(SimulatorProperties.repositoryUniqueId).asString(),
				RepositoryType.REPOSITORY,
				asc.get(SimulatorProperties.retrieveEndpoint).asString(),
				false, 
				isAsync));
		site.addRepository(new TransactionBean(
				asc.get(SimulatorProperties.repositoryUniqueId).asString(),
				RepositoryType.REPOSITORY,
				asc.get(SimulatorProperties.retrieveTlsEndpoint).asString(),
				true, 
				isAsync));

		return site;
	}

	public List<TransactionType> getIncomingTransactions() {
		return incomingTransactions;
	}


	@Override
	public ActorType getActorType() {
		return ActorType.REPOSITORY;
	}
}
