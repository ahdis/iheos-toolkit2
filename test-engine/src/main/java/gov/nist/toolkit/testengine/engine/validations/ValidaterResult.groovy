package gov.nist.toolkit.testengine.engine.validations

import gov.nist.toolkit.testengine.engine.AbstractValidater
import groovy.transform.ToString



@ToString
class ValidaterResult {
    /**
     * FhirSimulatorTransaction or (Xds)SimulatorTransaction
     */
    def transaction
    /**
     * AbstractFhirValidater  or AbstractXdsValidator
     */
    def filter
    boolean match
    private StringBuilder log

    ValidaterResult(def transaction, /* PostValidater */ AbstractValidater filter, boolean match) {
        this.transaction = transaction
        this.filter = filter
        this.match = match
        if (filter.log) {
            this.log = new StringBuilder(filter.log)
        } else {
            this.log = new StringBuilder()
        }
    }

    def log(String msg) {
        log.append(msg).append('\n')
    }

    String getLog() {
        log.toString()
    }

}
