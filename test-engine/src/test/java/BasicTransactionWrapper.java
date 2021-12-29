
import gov.nist.toolkit.testengine.engine.StepContext;
import gov.nist.toolkit.testengine.transactions.BasicTransaction;
import gov.nist.toolkit.xdsexception.client.XdsException;
import gov.nist.toolkit.xdsexception.client.XdsInternalException;

import org.apache.axiom.om.OMElement;

public class BasicTransactionWrapper extends BasicTransaction {

	public void run() throws XdsException {

	}

	protected BasicTransactionWrapper(StepContext s_ctx, OMElement instruction, OMElement instruction_output) {
		super(s_ctx, instruction, instruction_output);
		
	}

	@Override
	protected String getRequestAction() {
		return null;
	}

	@Override
	protected String getBasicTransactionName() {
		return null;
	}

	@Override
	protected void parseInstruction(OMElement part) throws XdsInternalException {

	}

	@Override
	protected void run(OMElement request) throws XdsException {

	}
}
