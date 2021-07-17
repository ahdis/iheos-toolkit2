package gov.nist.toolkit.fhir.simulators.servlet;

import gov.nist.toolkit.actorfactory.PatientIdentityFeedServlet;
import gov.nist.toolkit.actortransaction.client.ATFactory;
import gov.nist.toolkit.actortransaction.shared.ActorType;
import gov.nist.toolkit.configDatatypes.client.TransactionType;
import gov.nist.toolkit.configDatatypes.server.SimulatorProperties;
import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.errorrecording.GwtErrorRecorderBuilder;
import gov.nist.toolkit.fhir.simulators.proxy.service.ElementalReverseProxy;
import gov.nist.toolkit.fhir.simulators.proxy.service.RequestListenerThread;
import gov.nist.toolkit.fhir.simulators.sim.reg.store.MetadataCollection;
import gov.nist.toolkit.fhir.simulators.sim.reg.store.RegIndex;
import gov.nist.toolkit.fhir.simulators.sim.rep.RepIndex;
import gov.nist.toolkit.fhir.simulators.support.BaseDsActorSimulator;
import gov.nist.toolkit.fhir.simulators.support.DsSimCommon;
import gov.nist.toolkit.http.HttpHeader;
import gov.nist.toolkit.http.HttpHeader.HttpHeaderParseException;
import gov.nist.toolkit.http.ParseException;
import gov.nist.toolkit.installation.server.Installation;
import gov.nist.toolkit.installation.server.TestSessionFactory;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.simcommon.client.NoSimException;
import gov.nist.toolkit.simcommon.client.SimId;
import gov.nist.toolkit.simcommon.client.SimulatorConfig;
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement;
import gov.nist.toolkit.simcommon.server.*;
import gov.nist.toolkit.sitemanagement.SeparateSiteLoader;
import gov.nist.toolkit.sitemanagement.client.Site;
import gov.nist.toolkit.soap.http.SoapFault;
import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.utilities.xml.OMFormatter;
import gov.nist.toolkit.valsupport.client.MessageValidationResults;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.valsupport.engine.DefaultValidationContextFactory;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import gov.nist.toolkit.xdsexception.ExceptionUtil;
import gov.nist.toolkit.xdsexception.client.ToolkitRuntimeException;
import org.apache.axiom.om.OMElement;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class SimServlet  extends HttpServlet {
	static Logger logger = Logger.getLogger(SimServlet.class);

	private static final long serialVersionUID = 1L;

	static ServletConfig config = null;
	Map<String, String> headers = new HashMap<String, String>();
	String contentType;
	HttpHeader contentTypeHeader;
	String bodyCharset;
	//	File simDbDir;
	MessageValidationResults mvr;
	PatientIdentityFeedServlet patientIdentityFeedServlet;
	boolean isProxy;
	boolean isFhir;
	private RequestListenerThread proxyThread = null;

	@Override
	public void init(ServletConfig sConfig) throws ServletException {
		super.init(sConfig);
		if (config != null)
			return;
		config = sConfig;
		logger.info("Initializing toolkit in SimServlet");
		File warHome = new File(config.getServletContext().getRealPath("/"));
		logger.info("...warHome is " + warHome);
		Installation.instance().warHome(warHome);
		logger.info("...warHome initialized to " + Installation.instance().warHome());

		Installation.instance().setServletContextName(getServletContext().getContextPath());

		Installation.instance().setServletContextName(getServletContext().getContextPath());

		patientIdentityFeedServlet = new PatientIdentityFeedServlet();
		patientIdentityFeedServlet.init(config);



		onServiceStart();

		logger.info("SimServlet initialized");


		// Initialize SimProxy
		try {
			long id = Thread.currentThread().getId();
			Object it = this;
			logger.info("Proxy Operation: start proxy from SimServlet on thread " + id + " port " + Installation.instance().propertyServiceManager().getProxyPort());
			proxyThread = ElementalReverseProxy.start(Installation.instance().propertyServiceManager().getProxyPort());
		} catch (Exception e) {
			logger.fatal("Proxy startup on port " +  Thread.currentThread().getId()  + " failed - " + ExceptionUtil.exception_details(e));
			throw new ServletException(e);
		}
	}

	@Override
	public void destroy() {
		// stop SimProxy
		if (proxyThread != null) {
			Object it = this;
			long id = Thread.currentThread().getId();
			logger.info("Proxy Operation: shutdown started from SimServlet {"+ id + ")");
			try {
				proxyThread.getServersocket().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			id = Thread.currentThread().getId();
			logger.info("Proxy Operation: shutdown completed from SimServlet {"+ id + ")");
			proxyThread = null;
		}
		onServiceStop();
	}


	public MessageValidationResults getMessageValidationResults() {
		return mvr;
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		String uri = request.getRequestURI();
		logger.info("SIMSERVLET GET " + uri);
		String[] parts;
		try {
			int in = uri.indexOf("/del/");
			if (in != -1) {
				parts = uri.substring(in + "/del/".length()).split("\\/");
				handleDelete(response, parts);
				return;
			}
			in = uri.indexOf("/index/");
			if (in != -1) {
				parts = uri.substring(in + "/index/".length()).split("\\/");
				handleIndex(response, parts);
				return;
			}
			in = uri.indexOf("/message/");
			if (in != -1) {
				parts = uri.substring(in + "/message/".length()).split("\\/");
				handleMsgDownload(response, parts);
				return;
			}
			in = uri.indexOf("/siteconfig/");
			logger.info("siteconfig in is " + in);
			if (in != -1) {
				logger.info("working on siteconfig");
				parts = uri.substring(in + "/siteconfig/".length()).split("\\/");
				handleSiteDownload(response, parts);
				return;
			}
			in = uri.indexOf("/codes/");
			logger.info("codes in is " + in);
			if (in != -1) {
				logger.info("working on codes");
				parts = uri.substring(in + "/codes/".length()).split("\\/");
				if (parts.length < 1) return;
				handleCodesDownload(response, parts);
				return;
			}
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		} catch (Exception e) {
			logger.error(ExceptionUtil.exception_details(e));
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}


	}

	private void handleDelete(HttpServletResponse response, String[] parts) {
		String simid;
		String actor;
		String transaction;
		String message;

		try {
			simid = parts[0];
			actor = parts[1];
			transaction = parts[2];
			message = parts[3];
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		if (actor == null || actor.equals("null")) {
			try {
				SimDb sdb = new SimDb(SimDb.simIdBuilder(simid));
				actor = sdb.getActorsForSimulator().get(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (actor == null || actor.equals("null")) {
			logger.debug("No actor name found");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		SimDb db;
		try {
			db = new SimDb(SimDb.simIdBuilder(simid), actor, transaction, true);
		}
		catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		List<String> registryFilenames = db.getRegistryIds(simid, actor, transaction, message);
		List<String> registryUUIDs = new ArrayList<String>();
		for (String filename : registryFilenames) {
			registryUUIDs.add("urn:uuid:" + filename);
		}


		RegIndex regIndex = null;
		try {
			regIndex = getRegIndex(SimDb.simIdBuilder(simid));
		}
		catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		MetadataCollection mc = regIndex.mc;
		List<String> docUids = new ArrayList<String>();
		for (String id : registryUUIDs) {
			String uid = mc.deleteRo(id);
			if (uid != null)
				docUids.add(uid);
		}

		if (docUids.size() > 0) {
			RepIndex repIndex = null;
			try {
				repIndex = getRepIndex(SimDb.simIdBuilder(simid));
			}
			catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			for (String uid : docUids) {
				logger.debug("Delete document from index " + uid);
				repIndex.dc.delete(uid);
			}

		}


		logger.debug("Delete event " + simid + "/" + actor + "/" + transaction + "/" + message);
		File transEventFile = db.getTransactionEvent(simid, actor, transaction, message);
		db.delete();


		response.setStatus(HttpServletResponse.SC_OK);
	}

	// handle simulator message download
	void handleMsgDownload(HttpServletResponse response, String[] parts) {
		String simid;
		String actor;
		String transaction;
		String message;


		try {
			simid       = parts[0];
			actor       = parts[1];
			transaction = parts[2];
			message     = parts[3];
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		if (actor == null || actor.equals("null")) {
			try {
				SimDb sdb = new SimDb(SimDb.simIdBuilder(simid));
				actor = sdb.getActorsForSimulator().get(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (actor == null || actor.equals("null")) {
			logger.debug("No actor name found");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		SimDb db;
		try {
			db = new SimDb(SimDb.simIdBuilder(simid), actor, transaction, true);
			response.setContentType("application/zip");
			db.getMessageLogZip(response.getOutputStream(), message);
			response.getOutputStream().close();
			response.addHeader("Content-Disposition", "attachment; filename=" + message + ".zip");
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	// handle simulator message download
	void handleSiteDownload(HttpServletResponse response, String[] parts) {
		String simid;

		try {
			simid       = parts[0];
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		logger.debug("site download of " + simid);

		if (simid == null || simid.trim().equals("")) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		SimDb db;
		try {
			SimId simId = SimDb.simIdBuilder(simid);
			db = new SimDb(simId);
			SimulatorConfig config = db.getSimulator(simId);
			Site site = SimManager.getSite(config);
			OMElement siteEle = new SeparateSiteLoader(simId.getTestSession()).siteToXML(site);
			String siteString = new OMFormatter(siteEle).toString();
			logger.debug(siteString);

			response.setContentType("text/xml");
			response.getOutputStream().print(siteString);
			response.getOutputStream().close();
			response.addHeader("Content-Disposition", "attachment; filename=" + site.getName() + ".xml");
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	// handle codes.xml download
	private void handleCodesDownload(HttpServletResponse response, String[] parts) {
		String envName;

		try {
			envName       = parts[0];
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		logger.debug("codes download of " + envName);

		if (envName == null || envName.trim().equals("")) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		try {
			File codesFile = new File(Installation.instance().environmentFile(envName), "codes.xml");
			if (!codesFile.exists()) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			String codesContent = Io.stringFromFile(codesFile);
			response.setContentType("text/xml");
			response.getOutputStream().print(codesContent);
			response.getOutputStream().close();
			response.addHeader("Content-Disposition", "attachment; filename=" + "codes.xml");
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}


	// clean up index of an actor
	void handleIndex(HttpServletResponse response, String[] parts) {
		String simid;
		String actor;
		String transaction;

		try {
			simid = parts[0];
			actor = parts[1];
			transaction = parts[2];
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		if (actor == null || actor.equals("null")) {
			try {
				SimDb sdb = new SimDb(SimDb.simIdBuilder(simid));
				actor = sdb.getActorsForSimulator().get(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (actor == null || actor.equals("null")) {
			logger.debug("No actor name found");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		SimDb db;
		try {
			db = new SimDb(SimDb.simIdBuilder(simid), actor, transaction, true);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		if (actor.equals("registry")) {
			RegIndex regIndex;
			try {
				regIndex = getRegIndex(SimDb.simIdBuilder(simid));
			}
			catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			MetadataCollection mc = regIndex.getMetadataCollection();

			// purge model in the index that are no longer present behind the index
			mc.purge();
		}

		response.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		String uri  = request.getRequestURI().toLowerCase();
		logger.info("+ + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + + ");
		logger.info("uri is " + uri);
		logger.info("warHome is " + Installation.instance().warHome());
		RegIndex regIndex = null;
		RepIndex repIndex = null;
		ServletContext servletContext = config.getServletContext();
		boolean responseSent = false;
		MessageValidatorEngine mvc = new MessageValidatorEngine();
		ValidationContext vc = DefaultValidationContextFactory.validationContext();

		Date now = new Date();

		//is client behind something?
		String ipAddress = request.getHeader("X-FORWARDED-FOR");
		if (ipAddress == null) {
			ipAddress = request.getRemoteAddr();
		}

		String[] uriParts = uri.split("\\/");
		String toolkitServletName = (uriParts.length < 2) ? "" : uriParts[1];

		String endpointFormat = " - Endpoint format is http://" + request.getLocalName() + ":" + request.getLocalPort() + "/" + toolkitServletName + "/sim/simid/actor/transaction[/validation] " +
				"where simid, actor and transaction are variables for simulators. "  +
				"If validation is included, then this validation must be performed successfully for the transaction to be successful. " +
				" Validations are documented as part of tests that use them.";

		// endpoint parsing
		//
		// endpoint looks like
		// http://host:port/xdstools2/sim/simid/actor/transaction[/validation]
		// where
		//   simid is a uniqueID for a simulator
		//   actor
		//   transaction
		//   validation - name of a validation to be performed


		//
		// Parse endpoint to see which simulator/actor/transaction is target
		//
		int simIndex;

		for (simIndex=0; simIndex<uriParts.length; simIndex++) {
			if ("sim".equals(uriParts[simIndex]))
				break;
		}
		if (simIndex >= uriParts.length) {
			sendSoapFault(response, "Simulator: Do not understand endpoint http://" + request.getLocalName() + ":" + request.getLocalPort()  + uri + endpointFormat, mvc, vc);
			return;
		}

		List<String> transIds = new ArrayList<String>();
		transIds.add("pnr");
		transIds.add("xcqr");

		SimId simid = null;
		String actor = null;
		String transaction = null;
		String validation = null;
		try {
			simid = SimDb.simIdBuilder(uriParts[simIndex + 1]);
			actor = uriParts[simIndex + 2];
			if (uriParts.length > simIndex + 3)
				transaction = uriParts[simIndex + 3];
			else
				transaction = "any";  // this happens with a FHIR transaction through the simproxy
		}
		catch (Exception e) {
			sendSoapFault(response, "Simulator: Do not understand endpoint http://" + request.getLocalName() + ":" + request.getLocalPort() + uri + endpointFormat + " - " + e.getClass().getName() + ": " + e.getMessage(), mvc, vc);
			return;
		}

		simid = SimDb.getFullSimId(simid);
		isProxy = ActorType.findActor(simid.getActorType()).equals(ActorType.SIM_PROXY);
		/*
		isFhir = simid.isFhir();
		if (isProxy && isFhir) {
			transaction = TransactionType.FHIR.getCode();
			actor = ActorType.FHIR_SERVER.getShortName();
		}

		 */

		try {
			validation = uriParts[simIndex+4];
		} catch (Exception e) {
			// ignore - null value will signal no validation
		}

		TransactionType transactionType = ATFactory.findTransactionByShortName(transaction);
		logger.debug("Incoming transaction is " + transaction);
		logger.debug("... which is " + transactionType);
		if (transactionType == null) {
			sendSoapFault(response, "Simulator: Do not understand the transaction requested by this endpoint (" + transaction + ") in http://" + request.getLocalName() + ":" + request.getLocalPort() + uri + endpointFormat, mvc, vc);
			return;
		}

		ActorType actorType = ActorType.findActor(actor);
		if (actorType == null) {
			sendSoapFault(response, "Simulator: Do not understand the actor requested by this endpoint (" + actor + ") in http://" + request.getLocalName() + ":" + request.getLocalPort() + uri + endpointFormat, mvc, vc);
			return;
		}
//		isFhir = actorType.isFhir();

		boolean transactionOk = true;

		try {

			// DB space for this simulator
			SimDb db = new SimDb(simid, actor, transaction, false);
			request.setAttribute("SimDb", db);
			db.setClientIpAddess(ipAddress);

			logRequest(request, db, actor, transaction);

			SimulatorConfig asc = GenericSimulatorFactory.getSimConfig(simid);
			request.setAttribute("SimulatorConfig", asc);

			regIndex = getRegIndex(simid);
			repIndex = getRepIndex(simid);

			vc.forceMtom = transactionType.isRequiresMtom();

			SimulatorConfigElement stsSce = asc.get(SimulatorProperties.requiresStsSaml);
			if (stsSce!=null && stsSce.hasBoolean() && stsSce.asBoolean()) {
				/*
				NOTE:
				The validate SAML flag is global to the entire combined simulator. SAML needs to be propagated to internally forwarded transactions.
				*/
				vc.requiresStsSaml = true;
			}

			SimulatorConfigElement asce = asc.get(SimulatorProperties.codesEnvironment);
			if (asce != null)
				vc.setCodesFilename(asce.asString());

			asce = asc.get(SimulatorProperties.VALIDATE_CODES);
			if (asce != null) {
				vc.isValidateCodes = asce.asBoolean();
			}

			SimCommon common= new SimCommon(db, request.isSecure(), vc, response, mvc);
			DsSimCommon dsSimCommon = new DsSimCommon(common, regIndex, repIndex, mvc);

			common.setActorType(actorType);
			common.setTransactionType(transactionType);

			ErrorRecorder er = new GwtErrorRecorderBuilder().buildNewErrorRecorder();
			er.sectionHeading("Endpoint");
			er.detail("Endpoint is " + uri);
			mvc.addErrorRecorder("**** Web Service: " + uri, er);

			/////////////////////////////////////////////////////////////
			//
			// run the simulator for the requested actor/transaction pair
			//
			//////////////////////////////////////////////////////////////

//			if (actorType == ActorType.FHIR_SERVER)
//				response.setContentType("application/fhir+xml");  // TODO - JSON
//			else
				response.setContentType("application/soap+xml");

//			BaseDsActorSimulator sim = getSimulatorRuntime(simid);
			BaseActorSimulator baseSim = RuntimeManager.getSimulatorRuntime(simid);
			if (baseSim instanceof BaseDsActorSimulator) {
				BaseDsActorSimulator sim = (BaseDsActorSimulator) baseSim;

				sim.init(dsSimCommon, asc);
				if (asc.getConfigEle(SimulatorProperties.FORCE_FAULT).asBoolean()) {
					sendSoapFault(dsSimCommon, "Forced Fault");
					responseSent = true;
				} else {
					sim.onTransactionBegin(asc);
					transactionOk = sim.run(transactionType, mvc, validation);
					sim.onTransactionEnd(asc);
				}
			} else if (baseSim != null) {
				if (asc.getConfigEle(SimulatorProperties.FORCE_FAULT).asBoolean()) {
					sendSoapFault(dsSimCommon, "Forced Fault");
					responseSent = true;
				} else {
					baseSim.onTransactionBegin(asc);
					transactionOk = baseSim.run(transactionType, mvc, validation);
					baseSim.onTransactionEnd(asc);
				}
			} else {
				throw new ToolkitRuntimeException("Simulator " + simid.toString() + " is not runnable");
			}

			// Archive logs
			if (Installation.instance().propertyServiceManager().getArchiveLogs()) {
				SimDb simDb = dsSimCommon.simDb();
				if (simDb != null) {
					File eventDir = simDb.getEventDir();
					if (eventDir.exists()) {
						FileUtils.copyDirectory(eventDir, new File(Installation.instance().archive(), simDb.getEvent()));
					}
				}
			}

			TestSessionFactory.updateTimestanp(dsSimCommon.simDb().getTestSession());


		}
		catch (Throwable e) {
			sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
			logger.error(ExceptionUtil.exception_details(e));
			responseSent = true;
		}
//		catch (InvocationTargetException e) {
//			sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
//			logger.error(ExceptionUtil.exception_details(e));
//			responseSent = true;
//		}
//		catch (IllegalAccessException e) {
//			sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
//			logger.error(ExceptionUtil.exception_details(e));
//			responseSent = true;
//		}
//		catch (InstantiationException e) {
//			sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
//			logger.error(ExceptionUtil.exception_details(e));
//			responseSent = true;
//		}
//		catch (RuntimeException e) {
//			sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
//			logger.error(ExceptionUtil.exception_details(e));
//			responseSent = true;
//		}
//		catch (IOException e) {
//			sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
//			logger.error(ExceptionUtil.exception_details(e));
//			responseSent = true;
//		}
//		catch (HttpHeaderParseException e) {
//			sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
//			logger.error(ExceptionUtil.exception_details(e));
//			responseSent = true;
//		} catch (ClassNotFoundException e) {
//			sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
//			logger.error(ExceptionUtil.exception_details(e));
//			responseSent = true;
//		} catch (XdsException e) {
//			sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
//			logger.error(ExceptionUtil.exception_details(e));
//			responseSent = true;
//		} catch (NoSimException e) {
//			sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
//			logger.error(ExceptionUtil.exception_details(e));
//			responseSent = true;
//		} catch (ParseException e) {
//			sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
//			logger.error(ExceptionUtil.exception_details(e));
//			responseSent = true;
//		}
//		catch (Exception e) {
//			sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
//			logger.error(ExceptionUtil.exception_details(e));
//			responseSent = true;
//		}
//		catch (AssertionError e) {
//			sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
//			logger.error(ExceptionUtil.exception_details(e));
//			responseSent = true;
//		}
		finally {
			mvc.run();
			closeOut(response);
			if (responseSent)
				return;
		}

		// Is mvc.run really required here again since it is already called in the Finally block?
		// Sunil.
		mvc.run();


		// this should go away after repository code made to use deltas
		if (!transactionOk) {
			synchronized(this) {
				// delete memory copy of indexes so they don't getRetrievedDocumentsModel written out
				servletContext.setAttribute("Rep_" + simid, null);
				repIndex = null;
			}
		}

		List<String> flushed = new ArrayList<String>();
		int regCacheCount = 0;
		int repCacheCount = 0;
		try {

			// Update disk copy of indexes
			if (repIndex != null) {
				repIndex.save();
			}

			logger.info("Starting Reg/Rep Cache cleanout");
			synchronized(this) {

				// check for indexes that are old enough they should be removed from cache
				for (@SuppressWarnings("unchecked")
					 Enumeration<String> en = (Enumeration<String>) servletContext.getAttributeNames(); en.hasMoreElements(); ) {
					String name = en.nextElement();
					if (name.startsWith("Reg_")) {
						RegIndex ri = (RegIndex) servletContext.getAttribute(name);
						if (ri.cacheExpires.before(now)) {
							logger.info("Unloading " + name);
							servletContext.removeAttribute(name);
							flushed.add(name);
						} else
							regCacheCount++;
					}
					if (name.startsWith("Rep_")) {
						RepIndex ri = (RepIndex) servletContext.getAttribute(name);
						if (ri.cacheExpires.before(now)) {
							logger.info("Unloading " + name);
							servletContext.removeAttribute(name);
							flushed.add(name);
						} else
							repCacheCount++;
					}
				}

			}
			logger.info("Done with Reg/Rep Cache cleanout");
		} catch (IOException e) {
			logger.info("Done with Reg/Rep Cache cleanout");
			if (!responseSent)
				sendSoapFault(response, ExceptionUtil.exception_details(e), mvc, vc);
			e.printStackTrace();
		}

		logger.debug(regCacheCount + " items left in the Registry Index cache");
		logger.debug(repCacheCount + " items left in the Repository Index cache");

	} // EO doPost method

	private static void onServiceStart()  {
		try {
			for (TestSession testSession : Installation.instance().getTestSessions()) {
				List<SimId> simIds = SimDb.getAllSimIds(testSession);
				for (SimId simId : simIds) {
					BaseActorSimulator baseSim = (BaseActorSimulator) RuntimeManager.getSimulatorRuntime(simId);
					if (baseSim == null) continue;
					if (baseSim instanceof BaseDsActorSimulator) {
						BaseDsActorSimulator sim = (BaseDsActorSimulator) baseSim;
						DsSimCommon dsSimCommon = null;
						SimulatorConfig asc = GenericSimulatorFactory.getSimConfig(simId);
						sim.init(dsSimCommon, asc);
						sim.onServiceStart(asc);
					}
				}
			}
		} catch (Exception e) {
			logger.fatal(ExceptionUtil.exception_details(e));
		}
	}

	private static void onServiceStop() {
		try {
			for (TestSession testSession : Installation.instance().getTestSessions()) {
				List<SimId> simIds = SimDb.getAllSimIds(testSession);
				for (SimId simId : simIds) {
					BaseActorSimulator baseSim = (BaseActorSimulator) RuntimeManager.getSimulatorRuntime(simId);
					if (baseSim == null) continue;
					if (baseSim instanceof BaseDsActorSimulator) {
						BaseDsActorSimulator sim = (BaseDsActorSimulator) baseSim;
						DsSimCommon dsSimCommon = null;
						SimulatorConfig asc = GenericSimulatorFactory.getSimConfig(simId);
						sim.init(dsSimCommon, asc);
						sim.onServiceStop(asc);
					}
				}
			}
		} catch (Exception e) {
			logger.fatal(ExceptionUtil.exception_details(e));
		}
	}

	static public RegIndex getRegIndex(SimId simid) throws IOException, NoSimException {
		SimDb db = new SimDb(simid);
		ServletContext servletContext = config.getServletContext();
		String registryIndexFile = db.getRegistryIndexFile().toString();
		RegIndex regIndex;

		logger.info("GetRegIndex");
		synchronized(config) {
			regIndex = (RegIndex) servletContext.getAttribute("Reg_" + simid);
			if (regIndex == null) {
				logger.debug("Creating new RegIndex for " + simid + " in in-memory cache");
				regIndex = new RegIndex(registryIndexFile, simid);
				regIndex.setSimDb(db);
				servletContext.setAttribute("Reg_" + simid, regIndex);
			} else
				logger.debug("Using cached RegIndex: " + simid + " db loc:" + regIndex.getSimDb().getRegistryIndexFile().toString());

			regIndex.cacheExpires = getNewExpiration();
		}

		return regIndex;
	}

	static public RepIndex getRepIndex(SimId simid) throws IOException, NoSimException {
		SimDb db = new SimDb(simid);
		ServletContext servletContext = config.getServletContext();
		String repositoryIndexFile = db.getRepositoryIndexFile().toString();
		RepIndex repIndex;

		logger.info("GetRepIndex");
		synchronized(config) {
			repIndex = (RepIndex) servletContext.getAttribute("Rep_" + simid);
			if (repIndex == null) {
				repIndex = new RepIndex(repositoryIndexFile, simid);
				servletContext.setAttribute("Rep_" + simid, repIndex);
			}

			repIndex.cacheExpires = getNewExpiration();
		}
		return repIndex;
	}

	// remove the index(s)
	static public void deleteSim(SimId simId) {
		if (config == null) return;
		ServletContext servletContext = config.getServletContext();
		servletContext.removeAttribute("Reg_" + simId);
		servletContext.removeAttribute("Rep_" + simId);
	}


	void closeOut(HttpServletResponse response) {
		try {
			response.getOutputStream().close();
		} catch (IOException e) {

		}
	}

	static Calendar getNewExpiration() {
		// establish expiration for newly touched cache elements
		Date now = new Date();
		Calendar newExpiration = Calendar.getInstance();
		newExpiration.setTime(now);
		newExpiration.add(Calendar.MINUTE, 15);
		return newExpiration;
	}


	private void sendSoapFault(HttpServletResponse response, String message, MessageValidatorEngine mvc, ValidationContext vc) {
		try {
			if (isFhir) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
				return;
			}
			SoapFault sf = new SoapFault(SoapFault.FaultCodes.Sender, message);
			SimCommon c = new SimCommon(response);
			c.vc = vc;
			DsSimCommon dsSimCommon = new DsSimCommon(c, mvc);
			OMElement faultEle = sf.getXML();
			logger.info("Sending SOAP Fault:\n" + new OMFormatter(faultEle).toString());
			OMElement soapEnv = dsSimCommon.wrapResponseInSoapEnvelope(faultEle);
			dsSimCommon.sendHttpResponse(soapEnv, SimCommon.getUnconnectedErrorRecorder(), false);
		} catch (Exception e) {
			logger.error(ExceptionUtil.exception_details(e));
		}
	}

	private void sendSoapFault(DsSimCommon dsSimCommon, String message) {
//        try {
		SoapFault sf = new SoapFault(SoapFault.FaultCodes.Sender, message);
		dsSimCommon.sendFault(sf);
//        } catch (Exception e) {
//            logger.error(ExceptionUtil.exception_details(e));
//        }
	}

	void logRequest(HttpServletRequest request, SimDb db, String actor, String transaction)
			throws FileNotFoundException, IOException, HttpHeaderParseException, ParseException {
		StringBuffer buf = new StringBuffer();

		buf.append(request.getMethod() + " " + request.getRequestURI() + " " + request.getProtocol() + "\r\n");
		for (Enumeration<String> en=request.getHeaderNames(); en.hasMoreElements(); ) {
			String name = en.nextElement();
			String value = request.getHeader(name);
			if (name.equals("Transfer-Encoding"))
				continue;  // log will not include transfer encoding so don't include this
			headers.put(name.toLowerCase(), value);
			buf.append(name).append(": ").append(value).append("\r\n");
		}
		//		bodyCharset = request.getCharacterEncoding();
		String ctype = headers.get("content-type");
		if (ctype == null || ctype.equals(""))
			throw new IOException("Content-Type header not found");
		contentTypeHeader = new HttpHeader("content-type: " + ctype);
		bodyCharset = contentTypeHeader.getParam("charset");
		contentType = contentTypeHeader.getValue();

		if (bodyCharset == null || bodyCharset.equals(""))
			bodyCharset = "UTF-8";

		buf.append("\r\n");


		db.putRequestHeaderFile(buf.toString().getBytes());

		db.putRequestBodyFile(Io.getBytesFromInputStream(request.getInputStream()));

	}
}
