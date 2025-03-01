package gov.nist.toolkit.actortransaction.client;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

import gov.nist.toolkit.actortransaction.shared.ActorType;
import gov.nist.toolkit.configDatatypes.client.TransactionType;


/**
 * This factory defines the available Actors and Transactions and the relationships between Actors and Transactions.
 * @author bill
 *
 */
public class ATFactory implements IsSerializable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Map from transaction code (used in actors file) and transaction definition
	 */
	static Map<String, TransactionType> transactionMapByCode = new HashMap<String, TransactionType>();
	
	
	// Create definitions of transactions
	static {
		
		for (TransactionType tr : TransactionType.values()) {
			transactionMapByCode.put(tr.getCode(), tr);
		}
	}


	static public List<ActorType> RetrieveActorTypes = Arrays.asList(
			ActorType.REPOSITORY,
			ActorType.ONDEMAND_DOCUMENT_SOURCE,
			ActorType.ISR
//			ActorType.IMAGING_DOC_SOURCE
	);


	static List<TransactionType> gatewayTransactions = new ArrayList<TransactionType>();
	
	static {
		gatewayTransactions.add(TransactionType.XC_QUERY);
		gatewayTransactions.add(TransactionType.XC_RETRIEVE);
		gatewayTransactions.add(TransactionType.XC_PATIENT_DISCOVERY);
		gatewayTransactions.add(TransactionType.IG_QUERY);
		gatewayTransactions.add(TransactionType.IG_RETRIEVE);
		gatewayTransactions.add(TransactionType.XC_RET_IMG_DOC_SET);
	}
	
	
	///////////////////////////////////////////////////////////////////////
	// END OF CONFIGURATIONS
	///////////////////////////////////////////////////////////////////////
	
	
	static public boolean isGatewayTransaction(TransactionType tt) {
		return gatewayTransactions.contains(tt);
	}
	
	static public TransactionType getTransactionFromCode(String code) {
		return transactionMapByCode.get(code);
	}	
	
	static public TransactionType findTransactionByShortName(String name) {
		if (name == null)
			return null;
		
		for (TransactionType trans : TransactionType.values()) {
			if (trans.getShortName().equals(name))
				return trans;
		}
		return null;
	}
	
	
	public ATFactory() {
		
	}
	
	static public List<TransactionType> getAllTransactionTypes() {
		return TransactionType.asList();
	}
	
}
