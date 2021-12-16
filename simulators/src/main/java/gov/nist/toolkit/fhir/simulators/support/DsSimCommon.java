package gov.nist.toolkit.fhir.simulators.support;

import gov.nist.toolkit.commondatatypes.MetadataSupport;
import gov.nist.toolkit.configDatatypes.server.SimulatorProperties;
import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.errorrecording.GwtErrorRecorder;
import gov.nist.toolkit.errorrecording.GwtErrorRecorderBuilder;
import gov.nist.toolkit.errorrecording.client.ValidationStepResult;
import gov.nist.toolkit.errorrecording.client.ValidatorErrorItem;
import gov.nist.toolkit.errorrecording.client.XdsErrorCode;
import gov.nist.toolkit.errorrecording.factories.ErrorRecorderBuilder;
import gov.nist.toolkit.fhir.simulators.sim.reg.AdhocQueryResponseGenerator;
import gov.nist.toolkit.fhir.simulators.sim.reg.RegistryResponseSendingSim;
import gov.nist.toolkit.fhir.simulators.sim.reg.SoapWrapperRegistryResponseSim;
import gov.nist.toolkit.fhir.simulators.sim.reg.store.RegIndex;
import gov.nist.toolkit.fhir.simulators.sim.rep.RepIndex;
import gov.nist.toolkit.http.HttpParserBa;
import gov.nist.toolkit.installation.server.Installation;
import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.registrymsg.registry.RegistryResponse;
import gov.nist.toolkit.registrymsg.registry.Response;
import gov.nist.toolkit.registrysupport.RegistryErrorListGenerator;
import gov.nist.toolkit.simcommon.client.SimulatorConfig;
import gov.nist.toolkit.simcommon.server.SimCommon;
import gov.nist.toolkit.simcommon.server.SimDb;
import gov.nist.toolkit.soap.http.SoapFault;
import gov.nist.toolkit.soap.http.SoapUtil;
import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.utilities.xml.OMFormatter;
import gov.nist.toolkit.utilities.xml.XmlUtil;
import gov.nist.toolkit.validatorsSoapMessage.engine.ValidateMessageService;
import gov.nist.toolkit.validatorsSoapMessage.message.*;
import gov.nist.toolkit.valregmetadata.top.AbstractCustomMetadataValidator;
import gov.nist.toolkit.valregmsg.message.StoredDocumentInt;
import gov.nist.toolkit.valregmsg.service.SoapActionFactory;
import gov.nist.toolkit.valsupport.client.MessageValidationResults;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import gov.nist.toolkit.valsupport.engine.ValidationStep;
import gov.nist.toolkit.valsupport.message.AbstractMessageValidator;
import gov.nist.toolkit.xdsexception.ExceptionUtil;
import gov.nist.toolkit.xdsexception.client.XdsException;
import gov.nist.toolkit.xdsexception.client.XdsInternalException;
import org.apache.axiom.om.OMElement;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Support class for SOAP based simulators
 * More generic stuff comes from SimCommon
 */
public class DsSimCommon {
    SimulatorConfig simulatorConfig = null;
    public RegIndex regIndex = null;
    public RepIndex repIndex = null;
    public SimCommon simCommon;
    ErrorRecorder er = null;
    MessageValidationResults mvr = null;
    ValidateMessageService vms = new ValidateMessageService(null);


    Map<String, StoredDocument> documentsToAttach = null;  // cid => document
    RegistryErrorListGenerator registryErrorListGenerator = null;

    static Logger logger = Logger.getLogger(DsSimCommon.class.getName());

    public DsSimCommon(SimCommon simCommon, RegIndex regIndex, RepIndex repIndex, MessageValidatorEngine mvc) throws IOException, XdsException {
        this.simCommon = simCommon;
        this.er = simCommon.getCommonErrorRecorder();
        this.regIndex = regIndex;
        this.repIndex = repIndex;
        simCommon.mvc = mvc;

        if (regIndex != null) {
            regIndex.setSimDb(simCommon.db);
            regIndex.mc.regIndex = regIndex;
            regIndex.mc.vc = simCommon.vc;
        }

        vms = new ValidateMessageService(regIndex);
    }

    // only used to issue Soap Faults
    public DsSimCommon(SimCommon simCommon, MessageValidatorEngine mvc) {
        this.simCommon = simCommon;
        if (mvc == null)
            mvc = new MessageValidatorEngine();
        simCommon.mvc = mvc;
        this.er = simCommon.getCommonErrorRecorder();
    }

    public ValidationContext getValidationContext() {
        return simCommon.vc;
    }

    /**
     * Get the collection of error/statuses/messages for the validation steps
     * recorded so far.
     */

    public MessageValidationResults getMessageValidationResults() {
        buildMVR();
        return mvr;
    }

    /**
     * Return the collection of results/status/errors
     * @return
     */
    public List<ValidationStepResult> getErrors() {
        buildMVR();
        return mvr.getResults();
    }

    /**
     * Examine simulator stack - errors found?
     * @return
     */
    public boolean hasErrors() {
        buildMVR();
        return mvr.hasErrors();
    }

    public List<String> getValidatorNames() {
        return simCommon.mvc.getValidatorNames();
    }

    /**
     * Get MessageValidator off validation queue that is an instance of clas.
     * @param clas
     * @return Matching MessageValidator
     */
    public AbstractMessageValidator getMessageValidatorIfAvailable(@SuppressWarnings("rawtypes") Class clas) {
        return simCommon.mvc.findMessageValidatorIfAvailable(clas.getCanonicalName());
    }

    void generateLog() throws IOException {
        if (simCommon.mvc == null || simCommon.db == null)
            return;
        StringBuffer buf = new StringBuffer();

        //		buf.append(mvc.toString());

        Enumeration<ValidationStep> steps = simCommon.mvc.getValidationStepEnumeration();
        while (steps.hasMoreElements()) {
            ValidationStep step = steps.nextElement();
            buf.append(step).append("\n");
            ErrorRecorder er = step.getErrorRecorder();
            if (er instanceof GwtErrorRecorder) {
                GwtErrorRecorder ger = (GwtErrorRecorder) er;
                buf.append(ger);
            }
        }


        Io.stringToFile(simCommon.db.getLogFile(), buf.toString());
    }

    public void setSimulatorConfig(SimulatorConfig config) {
        this.simulatorConfig = config;
    }

    public SimulatorConfig getSimulatorConfig() {
        return simulatorConfig;
    }

    /**
     * Build the collection of error/statuses/messages for the validation steps
     * so far.
     */

    void buildMVR() {
        mvr = vms.getMessageValidationResults(simCommon.mvc);
    }


    /**
     * Starts the validation process by scheduling the HTTP parser. This is called
     * once per input message only.
     * Returns status indicating whether it is ok to continue.  If false then exit
     * immediately without returning a message.  A SOAPFault has already been returned;
     *
     * @return true if successful and false if fault sent
     * @throws IOException
     */
    public boolean runInitialValidationsAndFaultIfNecessary() throws IOException {
        runInitialValidations();
        return !returnFaultIfNeeded();
    }

    public void runInitialValidations() throws IOException {
        GwtErrorRecorderBuilder gerb = new GwtErrorRecorderBuilder();

        simCommon.mvc = runValidation(simCommon.vc, simCommon.db, simCommon.mvc, gerb);
        simCommon.mvc.run();
        buildMVR();

        int stepsWithErrors = simCommon.mvc.getErroredStepCount();
        ValidationStep lastValidationStep = simCommon.mvc.getLastValidationStep();
        if (lastValidationStep != null) {
            lastValidationStep.getErrorRecorder().detail
                    (stepsWithErrors + " steps with errors");
            logger.fine(stepsWithErrors + " steps with errors");
        } else {
            logger.fine("no steps with errors");
        }
    }

    /**
     * Starts the validation/simulator process by pulling the HTTP wrapper from
     * the db, creating a validation engine if necessary, and starting an HTTP
     * validator. It returns the validation engine. Remember that the basic
     * abstract Simulator class inherits directly from the abstract
     * MessageValidator class.
     * @param vc
     * @param db
     * @param mvc
     * @return
     * @throws IOException
     */
    public MessageValidatorEngine runValidation(ValidationContext vc, SimDb db,
       MessageValidatorEngine mvc, ErrorRecorderBuilder gerb) throws IOException {
       ValidateMessageService vms = new ValidateMessageService(regIndex);
       MessageValidatorEngine mve = vms.runValidation(vc,
           db.getRequestMessageHeader(), db.getRequestMessageBody(), mvc, gerb, db.getTestSession());
       hparser = vms.getHttpMessageValidator().getHttpParserBa();
       return mve;
    }
    private HttpParserBa hparser;
    public HttpParserBa getHttpParserBa() {
       return hparser;
    }


    public void sendErrorsInRegistryResponse(ErrorRecorder er) {
        if (er == null)
            er = new GwtErrorRecorderBuilder().buildNewErrorRecorder();


        // this works when RegistryResponse is the return message
        // need other options for other messaging environments
        simCommon.mvc.addMessageValidator("Send RegistryResponse with errors", new RegistryResponseSendingSim(simCommon, this), er);

        simCommon.mvc.run();
    }

    public void sendErrorsInAdhocQueryResponse(ErrorRecorder er) {
        if (er == null)
            er = new GwtErrorRecorderBuilder().buildNewErrorRecorder();

        // this works when RegistryResponse is the return message
        // need other options for other messaging environments
        AdhocQueryResponseGenerator queryResponseGenerator = new AdhocQueryResponseGenerator(simCommon, this);
        simCommon.mvc.addMessageValidator("Send AdhocQueryResponse with errors", queryResponseGenerator, er);
//        common.mvc.addMessageValidator("Send AdhocQueryResponse with errors", new RegistryResponseSendingSim(common, this), er);
        simCommon.mvc.addMessageValidator("ResponseInSoapWrapper", new SoapWrapperRegistryResponseSim(simCommon, this, queryResponseGenerator), er);

        simCommon.mvc.run();
    }

    public SimDb simDb() { return simCommon.db;  }

    /**
     * Wrap response in a SOAP Envelope (and body), header is generated by
     * examining the request header
     *
     * @param body response to be wrapped
     * @return SOAP Envelope
     */
    public OMElement wrapResponseInSoapEnvelope(OMElement body) {
        OMElement env = SoapUtil.buildSoapEnvelope();
        SoapMessageValidator smv = null;

        try {
            smv = (SoapMessageValidator) getMessageValidatorIfAvailable(SoapMessageValidator.class);
        } catch (Exception e) {

        }

        String resp_wsaction = null;
        String messageId = null;

        if (smv != null) {
            String wsaction = smv.getWsAction();
            messageId = smv.getMessageId();

            resp_wsaction = SoapActionFactory.getResponseAction(wsaction);
        }

        SoapUtil.attachSoapHeader(null, resp_wsaction, null, messageId, env);
        SoapUtil.attachSoapBody(body, env);

        return env;
    }

    public OMElement wrapResponseInRetrieveDocumentSetResponse(OMElement regResp) {
        OMElement rdsr = MetadataSupport.om_factory.createOMElement(MetadataSupport.retrieve_document_set_response_qnamens);
        rdsr.addChild(regResp);
        return rdsr;
    }

    public RegistryResponse getRegistryResponse() throws XdsInternalException {
        buildMVR();
        return getRegistryResponse(mvr.getResults());
    }

    public RegistryErrorListGenerator getRegistryErrorList() throws XdsInternalException {
        return getRegistryErrorList(getErrors());
    }

    public void setRegistryErrorListGenerator(RegistryErrorListGenerator relg) {
        registryErrorListGenerator = relg;
    }

    public RegistryErrorListGenerator getRegistryErrorList(List<ValidationStepResult> results) throws XdsInternalException {
        try {
            RegistryErrorListGenerator rel = registryErrorListGenerator;
            if (rel == null)
                rel = new RegistryErrorListGenerator(Response.version_3, false);

            rel.setPartialSuccess(simCommon.mvc.isPartialSuccess());

            for (ValidationStepResult vsr : results) {
                for (ValidatorErrorItem vei : vsr.er) {
                   if (vei.soaped) continue; vei.soaped = true;
                    if (vei.level == ValidatorErrorItem.ReportingLevel.ERROR) {
                        String msg = vei.msg;
                        if (vei.resource != null && !vei.resource.equals(""))
                            msg = msg + " (" + vei.resource + ")";
                        rel.add_error(vei.getCodeString(), vei.msg, vei.location, null, null);
                    }
                    if (vei.level == ValidatorErrorItem.ReportingLevel.WARNING) {
                        String msg = vei.msg;
                        if (vei.resource != null && !vei.resource.equals(""))
                            msg = msg + " (" + vei.resource + ")";
                        rel.add_warning(vei.getCodeString(), vei.msg, vei.location, null, null);
                    }
                }
            }
            return rel;
        } finally {
            registryErrorListGenerator = null;
        }
    }

    public RegistryResponse getRegistryResponse(List<ValidationStepResult> results) throws XdsInternalException {
        RegistryErrorListGenerator rel = getRegistryErrorList(results);
        RegistryResponse rr = new RegistryResponse(Response.version_3, rel);
        return rr;
    }

    public void addDocumentAttachments(Metadata m, ErrorRecorder er) {
        try {
            List<String> uids = new ArrayList<String>();
            for (OMElement eo : m.getExtrinsicObjects()) {
                String uid = m.getUniqueIdValue(eo);
                uids.add(uid);
            }
            addDocumentAttachments(uids, er);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot extract DocumentEntry.uniqueId from metadata stored in simulator", e);
            er.err(XdsErrorCode.Code.XDSRepositoryError, "SimCommon#addDocumentAttachment: Cannot extract DocumentEntry.uniqueId from metadata stored in simulator", this, "Internal Error");
        }
    }

    public void addDocumentAttachments(List<String> uids, ErrorRecorder er) throws XdsInternalException {
        int notFound = 0;
        for (String uid : uids) {
            StoredDocument sd = repIndex.getDocumentCollection().getStoredDocument(uid);
            // Fix Issue 70
            if (sd == null) {
                notFound++;
                er.err(XdsErrorCode.Code.XDSDocumentUniqueIdError, "DsSimCommon#addDocumentAttachments: Not found.", uid, "Error");
            } else
                addDocumentAttachment(sd);
        }

        int foundDocuments = 0;
        if (documentsToAttach != null)
            foundDocuments = documentsToAttach.size();

        if (notFound > 0 && foundDocuments > 0) {
            // Only some documents were found.
            simCommon.mvc.setPartialSuccess(true);
        }


    }


    /**
     * Attempts to retrieve DICOM documents from passed list of UIDs in one of
     * the passed list of transfer syntaxes.
     *
     * @param imagingDocumentUids List of composite UIDs (studyUid:SeriesUid:InstanceUID)
     *                            for DICOM documents requested by caller.
     * @param transferSyntaxUids  List of transfer syntax UIDs acceptable to caller
     * @param er                  ErrorRecorder to store errors found during processing.
     */
    public void addImagingDocumentAttachments(List<String> imagingDocumentUids, List<String> transferSyntaxUids, ErrorRecorder er) {
        logger.fine("DsSimComon#addImagingDocumentAttachments");
        for (String uid : imagingDocumentUids) {
            StoredDocument sd = this.getStoredImagingDocument(uid, transferSyntaxUids);
            logger.fine(" uid=" + uid);
            if (sd == null)
                continue;
            addDocumentAttachment(sd);
            logger.fine(" Added document for this uid");
        }
    }

    public Collection<StoredDocument> getAttachments() {
        if (documentsToAttach == null)
            return new ArrayList<StoredDocument>();
        return documentsToAttach.values();
    }

    String mkCid(int i) {
        return "doc" + i + "@ihexds.nist.gov";
    }

    public void addDocumentAttachment(StoredDocument sd) {
        if (documentsToAttach == null)
            documentsToAttach = new HashMap<String, StoredDocument>();

        sd.cid = mkCid(documentsToAttach.size() + 1);


        documentsToAttach.put(sd.cid, sd);
    }

//    /**
//     * Insert document Includes into DocumentResponse cluster
//     *
//     * @param env
//     * @throws MetadataException
//     */
//    void insertDocumentIncludes(OMElement env, ErrorRecorder er) throws MetadataException {
//        if (documentsToAttach == null)
//            return;
//
//        List<OMElement> docResponses = XmlUtil.decendentsWithLocalName(env, "DocumentResponse");
//        Map<String, OMElement> uidToDocResp = new HashMap<String, OMElement>();
//
//        for (OMElement docResp : docResponses) {
//            OMElement docUidEle = XmlUtil.firstChildWithLocalName(docResp, "DocumentUniqueId");
//            if (docUidEle == null) {
//                er.err(XdsErrorCode.Code.XDSRepositoryError, "Internal Error: response does not have DocumentUniqueId element", "SimCommon#insertDocumentIncludes", null);
//                continue;
//            }
//            String docUid = docUidEle.getText();
//            uidToDocResp.put(docUid, docResp);
//        }
//
//        for (String cid : documentsToAttach.keySet()) {
//            String uid = documentsToAttach.get(cid).getUid();
//            OMElement docResp = uidToDocResp.get(uid);
//            if (docResp == null) {
//                er.err(XdsErrorCode.Code.XDSRepositoryError, "Internal Error: response does not have Document for " + uid, "SimCommon#insertDocumentIncludes", null);
//                continue;
//            }
//            OMElement doc = MetadataSupport.om_factory.createOMElement(MetadataSupport.document_qnamens);
//            OMElement incl = MetadataSupport.om_factory.createOMElement(MetadataSupport.xop_include_qnamens);
//            incl.addAttribute("href", "cid:" + cid, null);
//            doc.addChild(incl);
//            docResp.addChild(doc);
//        }
//    }

//    /**
//     * Used to build RetrieveDocumentSetRespoinse
//     *
//     * @param env
//     * @param er
//     * @return
//     */
//    public StringBuffer wrapSoapEnvelopeInMultipartResponse(OMElement env, ErrorRecorder er) {
//        logger.fine("DsSimCommon#wrapSoapEnvelopeInMultipartResponse");
//
//        er.detail("Wrapping in Multipart");
//
//        // build body
//        String boundary = "MIMEBoundary112233445566778899";
//        StringBuffer contentTypeBuffer = new StringBuffer();
//        String rn = "\r\n";
//
//        contentTypeBuffer
//                .append("multipart/related")
//                .append("; boundary=")
//                .append(boundary)
//                .append(";  type=\"application/xop+xml\"")
//                .append("; start=\"<" + mkCid(0) + ">\"")
//                .append("; start-info=\"application/soap+xml\"");
//
//        simCommon.response.setHeader("Content-Type", contentTypeBuffer.toString());
//
//        StringBuffer body = new StringBuffer();
//
//        body.append("--").append(boundary).append(rn);
//        body.append("Content-Type: application/xop+xml; charset=UTF-8; type=\"application/soap+xml\"").append(rn);
//        body.append("Content-Transfer-Encoding: binary").append(rn);
//        body.append("Content-ID: <" + mkCid(0) + ">").append(rn);
//        body.append(rn);
//
//        body.append(env.toString());
//
//        body.append(rn);
//        body.append(rn);
//
//        if (documentsToAttach != null) {
//            er.detail("Attaching " + documentsToAttach.size() + " documents as separate Parts in the Multipart");
//            for (String cid : documentsToAttach.keySet()) {
//                StoredDocument sd = documentsToAttach.get(cid);
//
//                body.append("--").append(boundary).append(rn);
//                body.append("Content-Type: ").append(sd.getMimeType()).append(rn);
//                body.append("Content-Transfer-Encoding: binary").append(rn);
//                body.append("Content-ID: <" + cid + ">").append(rn);
//                body.append(rn);
//                try {
//                    String contents;
//                    if (sd.getCharset() != null) {
//                        contents = new String(sd.getContent(), sd.getCharset());
//                    } else {
//                        contents = new String(sd.getContent());
//                    }
//                    logger.fine("Attaching " + cid + " length " + contents.length());
//                    body.append(contents);
//                } catch (Exception e) {
//                    er.err(XdsErrorCode.Code.XDSRepositoryError, e);
//                }
//                body.append(rn);
//            }
//        }
//
//
//        body.append("--").append(boundary).append("--").append(rn);
//
//        return body;
//    }

    /**
     * Used to build RetrieveDocumentSetResponse
     *
     * @param env
     * @param er
     * @return
     */
    public StringBuffer wrapSoapEnvelopeInMultipartResponseBinary(OMElement env, ErrorRecorder er) {
        logger.fine("DsSimCommon#wrapSoapEnvelopeInMultipartResponseBinary");

        er.detail("Wrapping in Multipart");

        // build body
        String boundary = "MIMEBoundary112233445566778899";
        StringBuffer contentTypeBuffer = new StringBuffer();
        String rn = "\r\n";

        contentTypeBuffer
                .append("multipart/related")
                .append("; boundary=")
                .append(boundary)
                .append(";  type=\"application/xop+xml\"")
                .append("; start=\"<" + mkCid(0) + ">\"")
                .append("; start-info=\"application/soap+xml\"");

        simCommon.response.setHeader("Content-Type", contentTypeBuffer.toString());

        StringBuffer body = new StringBuffer();

        body.append("--").append(boundary).append(rn);
        body.append("Content-Type: application/xop+xml; charset=UTF-8; type=\"application/soap+xml\"").append(rn);
        body.append("Content-Transfer-Encoding: binary").append(rn);
        body.append("Content-ID: <" + mkCid(0) + ">").append(rn);
        body.append(rn);

        body.append(env.toString());

        body.append(rn);
        body.append(rn);

/*
        if (documentsToAttach != null) {
            er.detail("Attaching " + documentsToAttach.size() + " documents as separate Parts in the Multipart");
            for (String cid : documentsToAttach.keySet()) {
                StoredDocument sd = documentsToAttach.get(cid);
                body.append("--").append(boundary).append(rn);
                body.append("Content-Type: ").append(sd.getMimeType()).append(rn);
                body.append("Content-Transfer-Encoding: binary").append(rn);
                body.append("Content-ID: <" + cid + ">").append(rn);
                body.append(rn);
                try {
                    String contents;
                    if (sd.getCharset() != null) {
                        contents = new String(sd.getContent(), sd.getCharset());
                    } else {
                        contents = new String(sd.getContent());
                    }
		    logger.fine("Attaching " + cid + " length " + contents.length());
                    body.append(contents);
                } catch (Exception e) {
                    er.err(XdsErrorCode.Code.XDSRepositoryError, e);
                }
                body.append(rn);
            }
        }
*/


//        body.append("--").append(boundary).append("--").append(rn);

        return body;
    }

    public StringBuffer getTrailer() {
        String rn = "\r\n";
        String boundary = "MIMEBoundary112233445566778899";
        StringBuffer body = new StringBuffer();
        body.append(rn).append("--").append(boundary).append("--").append(rn);
        return body;
    }

    public void sendFault(SoapFault fault) {
        OMElement env = wrapResponseInSoapEnvelope(fault.getXML());
        sendHttpResponse(env, simCommon.getUnconnectedErrorRecorder(), true);
    }

    /**
     * Generate HTTP wrapper in correct format (SIMPLE or MTOM)
     *
     * @param env         SOAPEnvelope
     * @param er
     * @param multipartOk
     * @throws IOException
     */
    public void sendHttpResponse(OMElement env, ErrorRecorder er, boolean multipartOk) {
        if (simCommon.responseSent) {
            // this should never happen
            logger.severe(ExceptionUtil.here("Attempted to send second response"));
            return;
        }
        simCommon.responseSent = true;
        String respStr;
        logger.info("vc is " + simCommon.vc);
        logger.info("multipartOk is " + multipartOk);
        if (simCommon.vc != null && simCommon.vc.requiresMtom() && multipartOk) {
            StringBuffer body = wrapSoapEnvelopeInMultipartResponseBinary(env, er);

            respStr = body.toString();
        } else {
            respStr = new OMFormatter(env).toString();
        }
        try {
            if (simCommon.db != null)
                simCommon.db.putResponseBody(respStr);
//                Io.stringToFile(common.db.getResponseBodyFile(), respStr);
            if (simCommon.os != null)
                simCommon.os.write(respStr.getBytes());
            if (simCommon.vc.requiresMtom()) {
                this.writeAttachments(simCommon.os, er);
                if (multipartOk) {
                    simCommon.os.write(getTrailer().toString().getBytes());
                }
            }
            generateLog();
//            SimulatorConfigElement callbackElement = getSimulatorConfig().getRetrievedDocumentsModel(SimulatorConfig.TRANSACTION_NOTIFICATION_URI);
//            if (callbackElement != null) {
//                String callbackURI = callbackElement.asString();
//                if (callbackURI != null && !callbackURI.equals("")) {
//                    new Callback().notify(common.db, getSimulatorConfig(), callbackURI);
//                }
//            }
        } catch (IOException e) {
            logger.severe(ExceptionUtil.exception_details(e));
        }
    }

    private void writeAttachments(OutputStream os, ErrorRecorder er) {
        String boundary = "MIMEBoundary112233445566778899";
        String rn = "\r\n";
        try {

            if (documentsToAttach != null) {
                er.detail("Attaching " + documentsToAttach.size() + " documents as separate Parts in the Multipart");
                for (String cid : documentsToAttach.keySet()) {
                    StringBuffer body = new StringBuffer();
                    StoredDocument sd = documentsToAttach.get(cid);
                    sd.setRepIndex(repIndex);

                    body.append(rn).append("--").append(boundary).append(rn);
                    body.append("Content-Type: ").append(sd.getMimeType()).append(rn);
                    body.append("Content-Transfer-Encoding: binary").append(rn);
                    body.append("Content-ID: <" + cid + ">").append(rn);
                    body.append(rn);
                    os.write(body.toString().getBytes());
                    os.write(sd.getContent());
//                    os.write(rn.getBytes());
                }
            }

        } catch (Exception e) {
            logger.severe(ExceptionUtil.exception_details(e));
        }
    }

    public ErrorRecorder registryResponseAsErrorRecorder(OMElement regResp) {
        ErrorRecorder er = simCommon.getUnconnectedErrorRecorder();

        for (OMElement re : XmlUtil.decendentsWithLocalName(regResp, "RegistryError")) {
            String errorCode = re.getAttributeValue(MetadataSupport.error_code_qname);
            String codeContext = re.getAttributeValue(MetadataSupport.code_context_qname);
            String location = re.getAttributeValue(MetadataSupport.location_qname);
            String severity = re.getAttributeValue(MetadataSupport.severity_qname);
            String resource = "";
            er.err(errorCode, codeContext, location, severity, resource);
        }

        return er;
    }

    /**
     * Send a SOAP Fault if soap errors are present
     *
     * @return fault sent?
     * @throws IOException
     */
    public boolean returnFaultIfNeeded() throws IOException {
        if (simCommon.faultReturned) return false;
        SoapFault fault = getSoapErrors();
        if (fault != null) {
            sendFault(fault);
            simCommon.faultReturned = true;
            return true;
        }
        return false;
    }

    public boolean verifySubmissionAllowed() {
        if (simulatorConfig.get(SimulatorProperties.locked).asBoolean()) {
            SoapFault fault = new SoapFault(SoapFault.FaultCodes.Receiver, "This actor simulator is locked and will not accept submissions");
            sendFault(fault);
            simCommon.faultReturned = true;
            return false;
        }
        return true;
    }



    public boolean isFaultNeeded() {
        SoapFault fault = getSoapErrors();
        return fault != null;
    }

    /**
     * Send a SOAP Fault
     *
     * @param description description of problem
     * @param e           exception causing fault
     */
    public void sendFault(String description, Exception e) {
        logger.info("Sending SoapFault - " + description + " - " + ((e == null) ? "" : ExceptionUtil.exception_details(e)));
        SoapFault fault = new SoapFault(SoapFault.FaultCodes.Receiver, "InternalError: Exception building Response: " + description + " : " + ((e == null) ? "" : e.getMessage()));
        sendFault(fault);
    }


    /**
     * Generate HTTP wrapper in correct format (SIMPLE or MTOM)
     *
     * @param env SOAPEnvelope
     * @param er
     * @throws IOException
     */
    public void sendHttpResponse(OMElement env, ErrorRecorder er) throws IOException {
        sendHttpResponse(env, er, true);
    }

    public void intallDocumentsToAttach(StoredDocumentMap docmap) {
        if (documentsToAttach == null)
            documentsToAttach = new HashMap<>();
        for (StoredDocument stor : docmap.docs) {
            documentsToAttach.put(stor.cid, stor);
        }
    }

    /**
     * Return a SoapFault instance containing the errors logged so far.
     * Note that only errors from certain validators cause SOAP Faults.
     * Errors from other validators should be returned in a way that
     * is specific to that messaging architecture.
     * Returns null
     * if no errors found.
     *
     * @return
     */
    SoapFault getSoapErrors() {

        SoapFault sf;


        sf = getFaultFromMessageValidator(HttpMessageValidator.class);
        if (sf != null) return sf;

        sf = getFaultFromMessageValidator(SoapMessageValidator.class);
        if (sf != null) return sf;

        sf = getFaultFromMessageValidator(SimpleSoapHttpHeaderValidator.class);
        if (sf != null) return sf;

        sf = getFaultFromMessageValidator(MtomMessageValidator.class);
        if (sf != null) return sf;

        sf = getFaultFromMessageValidator(StsSamlValidator.class);
        if (sf != null) return sf;

        return null;

    }

    /**
     * Examine simulator/validator step defined by designated class
     * and if error(s) is found then generate SOAP fault message.
     *
     * @param clas Java class to look for on simulator stack
     * @return SoapFault instance
     */
    SoapFault getFaultFromMessageValidator(Class clas) {
        AbstractMessageValidator mv = getMessageValidatorIfAvailable(clas);
        if (mv == null) {
            logger.fine("MessageValidator for " + clas.getName() + " not found");
            return null;
        }

        ErrorRecorder er = mv.getErrorRecorder();
        if (!(er instanceof GwtErrorRecorder)) {
            SoapFault fault = new SoapFault(SoapFault.FaultCodes.Receiver, "InternalError: Simulator: ErrorRecorder not instance of GwtErrorRecorder");
            return fault;
        }

        logger.fine("Found error recorder for " + clas.getName());
        GwtErrorRecorder ger = (GwtErrorRecorder) er;
        if (ger.hasErrorsOrContext()) {
            logger.fine("has errors or context");
            SoapFault fault = new SoapFault(SoapFault.FaultCodes.Sender, "Header/Format Validation errors reported by " + clas.getSimpleName());
            for (ValidatorErrorItem vei : ger.getValidatorErrorItems()) {
                logger.fine(vei.toString());
                String resource = vei.resource;
                if (!vei.isErrorOrContext())
                    continue;
                String reportable = vei.getReportable();
                if (mv instanceof StsSamlValidator) {
                   // Extract the fault code
                   if (reportable.contains(": Receiver: ")) {
                    fault.setFaultCode(SoapFault.FaultCodes.Receiver);
                   }
                }
                if (resource == null || "".equals(resource))
                    fault.addDetail(reportable);
                else
                    fault.addDetail(reportable + "(" + resource + ")");
            }
            return fault;
        }
        return null;
    }


    /**
     * Attempts to retrieve the referenced DICOM document in one of the
     * referenced Transfer Syntaxes.
     *
     * @param compositeUid       composite UID of DICOM document desired by caller
     *                           (studyUid:SeriesUid:InstanceUid)
     * @param transferSyntaxUids List of Transfer Syntax UIDs for syntaxes
     *                           acceptable to caller
     * @return StoredDocument instance for the DICOM document referenced by the
     * compositeUid, or null if no document can be returned. A side effect is
     * that the current ErrorRecorder has an error added to it if no document can
     * be returned.
     */
    public StoredDocument getStoredImagingDocument(String compositeUid, List<String> transferSyntaxUids) {
        logger.fine("DsSimCommon#getStoredImagingDocument: " + compositeUid);
        String[] uids = compositeUid.split(":");
      /*
       * The image cache is in the IDS Simulator config, absolute, or relative
       * to the image cache in the toolkit properties.
       */
        String simCache = simulatorConfig.get(SimulatorProperties.idsImageCache).asString();
        File idsRepositoryDir = Installation.instance().imageCache("sim" + File.separator + simCache);
        Path idsRepositoryPath = idsRepositoryDir.toPath();
        if (!idsRepositoryDir.exists() || !idsRepositoryDir.isDirectory()) {
            logger.warning("Could not file image cache directory " + idsRepositoryDir);
            er.err(XdsErrorCode.Code.XDSRepositoryError,
                    "Could not find image cache [" + idsRepositoryPath + "] ",
                    uids[2], MetadataSupport.error_severity, "Internal error");
            return null;
        }
        Path folderPath = idsRepositoryPath.resolve(uids[0]).resolve(uids[1]).resolve(uids[2]);
        logger.fine(" " + folderPath);
        File folder = folderPath.toFile();
        if (!folder.exists()) {
            logger.fine("Could not find file folder for composite UID: " + compositeUid);
            er.err(XdsErrorCode.Code.XDSDocumentUniqueIdError,
                    "No document matching composite UID [" + compositeUid + "] ",
                    uids[2], MetadataSupport.error_severity, "ITI TF-3 Table 4.2.4.1-2");
            return null;
        }
        boolean found = false;
        Iterator<String> it = transferSyntaxUids.iterator();
        Path finalPath = Paths.get("");
        String xferSyntax = "";
        while (it.hasNext() && !found) {
            xferSyntax = it.next().trim();
            finalPath = folderPath.resolve(xferSyntax);
            if (finalPath.toFile().exists()) {
                found = true;
            } else {
		logger.severe("Was searching for file with this path, but the file is not present: " + finalPath.toString());
            }
        }
        StoredDocument sd = null;
        if (found) {
            logger.fine("Found path to file: " + finalPath);
            StoredDocumentInt sdi = new StoredDocumentInt();
            sdi.pathToDocument = finalPath.toString();
            sdi.uid = uids[2];
            logger.fine(" Instance UID: " + sdi.uid);
            sdi.mimeType = (xferSyntax.equals("export.jpg")) ? "image/jpeg" : "application/dicom";
            sdi.charset = "UTF-8";
            sdi.content = null;
            sd = new StoredDocument(repIndex, sdi);
        } else {
            logger.fine("Did not find an image file that matched transfer syntax");
            logger.fine(" Composite UID: " + compositeUid);
            it = transferSyntaxUids.iterator();
            while (it.hasNext()) {
                logger.fine("  Xfer syntax: " + it.next());
            }
            er.err(XdsErrorCode.Code.XDSRepositoryError,
                    "IDS cannot encode the pixel data using any of the requested transfer syntaxes",
                    uids[2], MetadataSupport.error_severity, "RAD TF-3 4.69.4.2.3");
        }
        return sd;
    }

}
