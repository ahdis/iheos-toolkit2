/**
 * 
 */
package gov.nist.toolkit.fhir.simulators.sim.ids;

import gov.nist.toolkit.configDatatypes.client.TransactionType;
import gov.nist.toolkit.errorrecording.GwtErrorRecorderBuilder;
import gov.nist.toolkit.errorrecording.client.XdsErrorCode.Code;
import gov.nist.toolkit.http.HttpMessageBa;
import gov.nist.toolkit.http.HttpParserBa;
import gov.nist.toolkit.fhir.simulators.support.BaseHttpActorSimulator;
import gov.nist.toolkit.fhir.simulators.support.DsSimCommon;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import org.apache.commons.lang3.StringUtils;
import java.util.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulator for Image Document Source (IDS) receiving WADO (RAD-55)
 * transactions.
 * 
 * @author Ralph Moulton / MIR WUSTL IHE Development Project
 * <a href="mailto:moultonr@mir.wustl.edu">moultonr@mir.wustl.edu</a>
 */
public class IdsHttpActorSimulator extends BaseHttpActorSimulator {

   static Logger logger = Logger.getLogger(IdsHttpActorSimulator.class.getName());

   static List <TransactionType> transactions = new ArrayList <>();

   static {
      transactions.add(TransactionType.WADO_RETRIEVE);
   }

   public boolean supports(TransactionType transactionType) {
      return transactions.contains(transactionType);
   }
   
   /*
    * Valid MIME types for Accept header in WADO (RAD-55) Http Requests. One of
    * these must be present, others may be present. per RAD TF-3 Table 4.55-1
    */
   private static String[] validTypes = new String[] {
      "application/dicom",
      "image/jpeg",
      "application/text",
      "application/html",
      "*/*" 
   };

   private DsSimCommon dsSimCommon = null;
   public void setDsSimCommon(DsSimCommon ds) {dsSimCommon = ds;}

   @Override
   public boolean run(TransactionType transactionType, MessageValidatorEngine mvc) throws IOException {
      return run(transactionType, mvc, null);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * gov.nist.toolkit.fhir.simulators.support.BaseHttpActorSimulator#run(gov.nist.
    * toolkit.configDatatypes.client.TransactionType,
    * gov.nist.toolkit.valsupport.engine.MessageValidatorEngine)
    */
   @Override
   public boolean run(TransactionType transactionType, MessageValidatorEngine mvc, String validation) throws IOException {

      logger.info("IdsHttpActorSimulator: run - transactionType = " + transactionType);
      simCommon.setLogger(logger);
      GwtErrorRecorderBuilder gerb = new GwtErrorRecorderBuilder();
      if (dsSimCommon == null) dsSimCommon = new DsSimCommon(simCommon, mvc);

      logger.fine(transactionType.toString());
      switch (transactionType) {
         case WADO_RETRIEVE:
            simCommon.vc.isRad55 = true;
            simCommon.vc.isRequest = true;
            String httpResponseString = "";
            
            logger.fine("dsSimCommon.runInitialValidationsAndFaultIfNecessary()");
            if (!dsSimCommon.runInitialValidationsAndFaultIfNecessary()) {
               return false;
            }

            logger.fine("mvc.hasErrors()");
            if (mvc.hasErrors()) {
               return false;
            }
            
            HttpParserBa hparser = dsSimCommon.getHttpParserBa();
            HttpMessageBa httpMsg = hparser.getHttpMessage();

            String accept = httpMsg.getHeaderValue("Accept");
            if (StringUtils.isBlank(accept)) {
               String headerAcceptError = "Required header 'Accept' absent or empty";
               err(headerAcceptError);
               httpResponseString += "(" + headerAcceptError + ") ";
            }
            String [] types = accept.split(",");
            boolean foundOne = false;

            for (String type : types) {
               if (StringUtils.startsWithAny(type, validTypes)) {
                  foundOne = true;
                  break;
               }
            }

            if (!foundOne) {
               String unrecognizedAccept = "Unrecognized Accept Header (" + accept + "); must be one of: " + validTypesAsOneString();
               err(unrecognizedAccept);
               httpResponseString += "(" + unrecognizedAccept + ")";
            }
            
            if (!"WADO".equals(httpMsg.getQueryParameterValue("requestType"))) {
               String wadoNotFound = "Required Request parameter 'requestType=WADO' not found.";
               err(wadoNotFound);
               httpResponseString += "(" + wadoNotFound + ")";
            }

            String studyUID = httpMsg.getQueryParameterValue("studyUID");
            if (!isOid(studyUID, false)) {
               String missingStudyUID = "Required Request parameter 'studyUID' not found.";
               err(missingStudyUID);
               httpResponseString += "(" + missingStudyUID + ")";
            }

            String seriesUID = httpMsg.getQueryParameterValue("seriesUID");
            if (!isOid(seriesUID, false)) {
               String missingSeriesUID = "Required Request parameter 'seriesUID' not found.";
               err(missingSeriesUID);
               httpResponseString += "(" + missingSeriesUID + ")";
            }

            String objectUID = httpMsg.getQueryParameterValue("objectUID");
            if (!isOid(objectUID, false)) {
               String missingObjectUID = "Required Request parameter 'objectUID' not found.";
               err(missingObjectUID);
               httpResponseString += "(" + missingObjectUID + ")";
            }
            
            String contentType = httpMsg.getQueryParameterValue("contentType");
            if (StringUtils.isBlank(contentType)) {
               String missingContentType = "Required Request parameter 'contentType' not found.";
               err(missingContentType);
               httpResponseString += "(" + missingContentType + ")";
            }
            
            if (mvc.hasErrors()) {
               returnError(mvc, 400, "Invalid WADO (RAD-55) transaction. " + httpResponseString);
               return false;
            }
            
            WadoRetrieveResponseSim wrr = new WadoRetrieveResponseSim(simCommon, httpMsg, dsSimCommon);
            mvc.addMessageValidator("Generated RAD-55 Response", wrr, gerb.buildNewErrorRecorder());
            mvc.run();
            

            return false;

         default:
            er.err(Code.XDSRegistryError, "Don't understand transaction " + transactionType,
               "ImagingDocSourceActorSimulator", "");
            simCommon.sendHttpFault("Don't understand transaction " + transactionType);
            return true;

      }  // EO switch (transactionType)

   } // EO run()
   
   private void returnError(MessageValidatorEngine mvc, int status, String msg) {
      mvc.run();
      String em = "Returning http response status " + status + " " + msg;
      logger.info(em);
      er.detail(em);
      try {
         simCommon.response.sendError(status, msg);
      } catch (IOException ioe){
         logger.warning("I/O error attempting to send error response " + ioe.getMessage());
         return;
      }      
   }
   
   private void err(String msg) {
      er.err(Code.XDSIRequestError, msg, "", "");
   }
   

   /**
    * Is the passed string properly formatted OID? Example would be a home
    * community id.
    * @param value String to be validated.
    * @param blankOk boolean, return true for a null/empty string?
    * @return boolean true if value is properly formatted, false otherwise.
    */
   private boolean isOid(String value, boolean blankOk) {
      if (value == null || value.length() == 0) return blankOk;
      return value.matches("\\d(?=\\d*\\.)(?:\\.(?=\\d)|\\d){0,255}");
   }

   private String validTypesAsOneString() {
      String rtnString = "";
      String delimiter = "";
      for (String s: validTypes) {
         rtnString += (delimiter + s);
         delimiter = ", ";
      }
      return rtnString;
   }

} // EO class
