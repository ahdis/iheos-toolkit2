package gov.nist.toolkit.fhir.simulators.support

import gov.nist.toolkit.simcommon.client.SimId
import gov.nist.toolkit.simcommon.server.SimDb
import gov.nist.toolkit.transactionNotificationService.TransactionLogBean
//import groovy.util.logging.Log4j

import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.Response
import java.util.logging.Logger

/**
 * Send toolkit simulator notify. This announces the arrival of a
 * message of interest from a SUT.
 */
//@Log4j
class Callback {
    def callback(SimDb db, SimId simId, String callbackURI, String callbackClassName) {
        Logger log = Logger.getLogger("Callback.groovy")
        if (callbackURI) {
            assert callbackURI.startsWith("http")
            TransactionLogBean bean = new TransactionReportBuilder().asBean(db, simId, callbackClassName);
            log.info("Transaction Request\n" + bean.requestMessageBody);
//            String payload = new TransactionReportBuilder().build(db, config);
            log.info("Callback to ${callbackURI}");
            try {
                Client client = ClientBuilder.newClient()
                WebTarget target = client.target(callbackURI)

                Response response = target
                        .request('text/xml')
                        .post(Entity.xml(bean))

                if (response.getStatus() != 200) {
                    throw new RuntimeException("Failed :\n...HTTP error code : "
                            + response.getStatus() + "\n..." +
                            response.getHeaderString("X-Toolkit-Error"));
                }
            } catch (Exception e) {
                log.error("Error on notify for simulator ${simId} to URI ${callbackURI} to class ${bean.callbackClassName}")
                throw e
            }

        }
    }
}
