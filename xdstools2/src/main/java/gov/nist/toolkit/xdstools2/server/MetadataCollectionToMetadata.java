package gov.nist.toolkit.xdstools2.server;

import gov.nist.toolkit.commondatatypes.client.MetadataTypes;
import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.registrymetadata.MetadataParser;
import gov.nist.toolkit.registrymetadata.client.*;
import gov.nist.toolkit.commondatatypes.MetadataSupport;
import gov.nist.toolkit.results.MetadataToMetadataCollectionParser;
import gov.nist.toolkit.testengine.transactions.BasicTransaction;
import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.utilities.xml.OMFormatter;
import gov.nist.toolkit.utilities.xml.Util;
import gov.nist.toolkit.valregmsg.message.SchemaValidation;
import gov.nist.toolkit.xdsexception.client.XdsInternalException;
import org.apache.axiom.om.OMElement;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MetadataCollectionToMetadata {
	Metadata m = new Metadata();
	boolean allowSymbolicIds = false;
	
	static public Metadata buildMetadata(MetadataCollection mc) {
		return buildMetadata(mc, false);
	}

	static public Metadata buildMetadata(MetadataCollection mc, boolean allowSymbolicIds) {
		MetadataCollectionToMetadata mcm = new MetadataCollectionToMetadata(mc, allowSymbolicIds);
		mcm.allowSymbolicIds = allowSymbolicIds;
		return mcm.m;
	}
	
	MetadataCollectionToMetadata(MetadataCollection mc, boolean allowSymbolicIds) {
		this.allowSymbolicIds = allowSymbolicIds;
		for (DocumentEntry de : mc.docEntries) 
			buildDocumentEntry(de);
		for (SubmissionSet ss : mc.submissionSets) 
			buildSubmissionSet(ss);
		for (Folder fol : mc.folders) 
			buildFolder(fol);
		for (Association a : mc.assocs) 
			buildAssociation(a);
	}
	
	void buildDocumentEntry(DocumentEntry de) {
		OMElement eo = m.mkExtrinsicObject(de.id, de.mimeType);
		if (de.status != null && !de.status.equals(""))
			m.setStatus(eo, de.status);
		if (de.title != null && !de.title.equals(""))
			m.setTitleValue(eo, de.title);
		if (de.comments != null && !de.comments.equals(""))
			m.setDescriptionValue(eo, de.comments);
		if (de.patientId != null && !de.patientId.equals(""))
			m.addDocumentEntryPatientId(eo, de.patientId);
		if (de.uniqueId != null && !de.uniqueId.equals(""))
			m.addDocumentEntryUniqueId(eo, de.uniqueId);
		
		if(de.lid != null && !de.lid.equals("") && (de.lid.startsWith("urn:uuid:") || allowSymbolicIds) )
			m.addLid(eo, de.lid);
		if (de.version != null && !de.version.equals("") && de.id.startsWith("urn:uuid:"))
			m.setVersion(eo, de.version);
		if (de.hash != null && !de.hash.equals(""))
			m.addSlot(eo, "hash", de.hash);
		if (de.lang != null && !de.lang.equals(""))
			m.addSlot(eo, "languageCode", de.lang);
		else
			m.addSlot(eo, "languageCode", "en-us");
		if (de.legalAuth != null && !de.legalAuth.equals(""))
			m.addSlot(eo, "legalAuthenticator", de.legalAuth);
		if (de.serviceStartTime != null && !de.serviceStartTime.equals(""))
			m.addSlot(eo, "serviceStartTime", de.serviceStartTime);
		if (de.serviceStopTime != null && !de.serviceStopTime.equals(""))
			m.addSlot(eo, "serviceStopTime", de.serviceStopTime);
		if (de.repositoryUniqueId != null && !de.repositoryUniqueId.equals(""))
			m.addSlot(eo, "repositoryUniqueId", de.repositoryUniqueId);
		if (de.size != null && !de.size.equals(""))
			m.addSlot(eo, "size", de.size);
		if (de.sourcePatientId != null && !de.sourcePatientId.equals(""))
			m.addSlot(eo, "sourcePatientId", de.sourcePatientId);
		if (de.creationTime != null && !de.creationTime.equals(""))
			m.addSlot(eo, "creationTime", de.creationTime);
		
		addClassification(eo, de.classCode, "urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a");
		addClassification(eo, de.confCodes, "urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f");
		addClassification(eo, de.eventCodeList, "urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4");
		addClassification(eo, de.formatCode, "urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d");
		addClassification(eo, de.hcftc, "urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1");
		addClassification(eo, de.pracSetCode, "urn:uuid:cccf5598-8b07-4b77-a05e-ae952c785ead");
		addClassification(eo, de.typeCode, "urn:uuid:f0306f51-975f-434e-a61c-c59651d33983");
		
		for (Author a : de.authors) {
			OMElement aele = m.addExtClassification(eo, "urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d", null, null, "");
			m.addSlot(aele, "authorPerson", a.person);
			if (a.institutions.size() > 0) {
				OMElement iSlot = m.mkSlot("authorInstitution");
				aele.addChild(iSlot);
				for (String inst : a.institutions) {
					m.addSlotValue(iSlot, inst);
				}
			}
			if (a.roles.size() > 0) {
				OMElement iSlot = m.mkSlot("authorRole");
				aele.addChild(iSlot);
				for (String inst : a.roles) {
					m.addSlotValue(iSlot, inst);
				}
			}
			if (a.specialties.size() > 0) {
				OMElement iSlot = m.mkSlot("authorSpecialty");
				aele.addChild(iSlot);
				for (String inst : a.specialties) {
					m.addSlotValue(iSlot, inst);
				}
			}
			if (a.telecom.size() > 0) {
				OMElement iSlot = m.mkSlot("authorTelecommunication");
				aele.addChild(iSlot);
				for (String inst : a.telecom) {
					m.addSlotValue(iSlot, inst);
				}
			}
		}
		
		OMElement spiEle = m.mkSlot("sourcePatientInfo");
		eo.addChild(spiEle);
		for (String spi : de.sourcePatientInfo) {
			m.addSlotValue(spiEle, spi);
		}
		
	}
	
	void buildSubmissionSet(SubmissionSet ss) {
		OMElement ssEle = m.mkSubmissionSet(ss.id);
		m.addIntClassification(ssEle, "urn:uuid:a54d6aa5-d40d-43f9-88c5-b4633d873bdd");
		if (ss.status != null && !ss.status.equals(""))
			m.setStatus(ssEle, ss.status);
		if (ss.title != null && !ss.title.equals(""))
			m.setTitleValue(ssEle, ss.title);
		if (ss.comments != null && !ss.comments.equals(""))
			m.setDescriptionValue(ssEle, ss.comments);
		if (ss.patientId != null && !ss.patientId.equals(""))
			m.addSubmissionSetPatientId(ssEle, ss.patientId);
		if (ss.uniqueId != null && !ss.uniqueId.equals(""))
			m.addSubmissionSetUniqueId(ssEle, ss.uniqueId);
		
		if (ss.id.startsWith("urn:uuid:"))
			m.setVersion(ssEle, "1.1");
		
		if (ss.submissionTime != null && !ss.submissionTime.equals(""))
			m.addSlot(ssEle, "submissionTime", ss.submissionTime);
		
		if (ss.sourceId != null && !ss.sourceId.equals(""))
			m.addExternalId(ssEle, "urn:uuid:554ac39e-e3fe-47fe-b233-965d2a147832", ss.sourceId, MetadataSupport.XDSSubmissionSet_sourceid_name);
		
		addClassification(ssEle, ss.contentTypeCode, "urn:uuid:aa543740-bdda-424e-8c96-df4873be8500");
		
		if (ss.intendedRecipients.size() > 0) {
			OMElement iSlot = m.mkSlot("intendedRecipients");
			ssEle.addChild(iSlot);
			for (String inst : ss.intendedRecipients) {
				m.addSlotValue(iSlot, inst);
			}
		}
		
		for (Author a : ss.authors) {
			OMElement aele = m.addExtClassification(ssEle, "urn:uuid:a7058bb9-b4e4-4307-ba5b-e3f0ab85e12d", null, null, "");
			m.addSlot(aele, "authorPerson", a.person);
			if (a.institutions.size() > 0) {
				OMElement iSlot = m.mkSlot("authorInstitution");
				aele.addChild(iSlot);
				for (String inst : a.institutions) {
					m.addSlotValue(iSlot, inst);
				}
			}
			if (a.roles.size() > 0) {
				OMElement iSlot = m.mkSlot("authorRole");
				aele.addChild(iSlot);
				for (String inst : a.roles) {
					m.addSlotValue(iSlot, inst);
				}
			}
			if (a.specialties.size() > 0) {
				OMElement iSlot = m.mkSlot("authorSpecialty");
				aele.addChild(iSlot);
				for (String inst : a.specialties) {
					m.addSlotValue(iSlot, inst);
				}
			}
			if (a.telecom.size() > 0) {
				OMElement iSlot = m.mkSlot("authorTelecommunication");
				aele.addChild(iSlot);
				for (String inst : a.telecom) {
					m.addSlotValue(iSlot, inst);
				}
			}
		}
		
		
	}
	
	void buildFolder(Folder fol) {
		OMElement folEle = m.mkFolder(fol.id);
		if (fol.status != null && !fol.status.equals(""))
			m.setStatus(folEle, fol.status);
		if (fol.title != null && !fol.title.equals(""))
			m.setTitleValue(folEle, fol.title);
		if (fol.comments != null && !fol.comments.equals(""))
			m.setDescriptionValue(folEle, fol.comments);
		if (fol.patientId != null && !fol.patientId.equals(""))
			m.addFolderPatientId(folEle, fol.patientId);
		if (fol.uniqueId != null && !fol.uniqueId.equals(""))
			m.addFolderUniqueId(folEle, fol.uniqueId);
		
		if(fol.lid != null && !fol.lid.equals("") && fol.lid.startsWith("urn:uuid:"))
			m.addLid(folEle, fol.lid);

		
		m.setVersion(folEle, "1.1");
		
		if (fol.lastUpdateTime != null && !fol.lastUpdateTime.equals(""))
			m.addSlot(folEle, "lastUpdateTime", fol.lastUpdateTime);
				
		addClassification(folEle, fol.codeList, "urn:uuid:1ba97051-7806-41a8-a48b-8fce7af683c5");
				
	}
	
	void buildAssociation(Association a) {
		OMElement aEle = m.mkAssociation(a.type, a.source, a.target);
		aEle.addAttribute("id", a.id, null);
		if(a.lid != null && !a.lid.equals("") && a.lid.startsWith("urn:uuid:"))
			aEle.addAttribute("lid", a.lid, null);
		if (a.status != null && !a.status.equals(""))
			m.setStatus(aEle, a.status);

		if (a.previousVersion != null && !a.previousVersion.equals(""))
			m.addSlot(aEle, "PreviousVersion", a.previousVersion);
		
		if (a.ssStatus != null && !a.ssStatus.equals("")) 
			m.addSlot(aEle, "SubmissionSetStatus", a.ssStatus);

		
		m.setVersion(aEle, "1.1");
		
				
	}
	
	void addClassification(OMElement ele, List<String> values, String classificationScheme) {
		for (String value : values) {
			String[] parts = value.split("\\^");
			String codeValue   = (parts.length < 1) ? "" : parts[0];
			String codeName    = (parts.length < 2) ? "" : parts[1];
			String codeScheme  = (parts.length < 3) ? "" : parts[2];
			
			m.addExtClassification(ele, classificationScheme, codeScheme, codeName, codeValue);
		}
	}
	
	static public void main(String[] args) {
		File inFile = null;
		File outFile = null;
		boolean metadataV2 = false;
		int inMetadataType = MetadataTypes.METADATA_TYPE_Rb;
		int outMetadataType = MetadataTypes.METADATA_TYPE_Rb;
		boolean replaceInputFileIfNoConversionErrors = false;

		if (args.length == 0) {
			inFile = new File("/Users/bill/dev/testkit/tests/11966/submit/single_doc.xml");
			outFile = new File("/Users/bill/tmp/submission.xml");
			translateMetadata(inFile, outFile, metadataV2, inMetadataType, outMetadataType);
		} else if (args.length == 1) {
			// auto replace from text file

			try {
				String bulkParametersRecords = Io.stringFromFile(new File(args[0]));
				List<String> bulkParameterRecord = Arrays.asList(bulkParametersRecords.split("\n"));
				for (String r : bulkParameterRecord) {
					if ("".equals(r))
						continue;
					String[] parameters = r.split(" ");
					inFile = new File(parameters[0]);
					outFile = inFile;
					inMetadataType = Integer.parseInt(parameters[1]);
					outMetadataType = MetadataTypes.METADATA_TYPE_Rb;
					String out = convertMetadataFile(inFile, metadataV2, inMetadataType, outMetadataType, false, true);
					if (outFile != null) {
						out += "\r\n<!-- Issue 575 -->\r\n";
						Io.stringToFile(inFile, out);
					} else {
						System.out.println("Error: no output for file " + inFile);
					}
				}

			} catch (Exception ioex) {
				ioex.printStackTrace();
				System.exit(1);
			}
			return;

		} else if (args.length>2) {
			try {
				inFile = new File(args[0]);
				outFile = new File(args[1]);
//				metadataV2 = Boolean.parseBoolean(args[2]);
				if (args.length > 2) {
					inMetadataType = Integer.parseInt( args[2] );
					if (args.length > 3) {
						outMetadataType = Integer.parseInt(args[3]);
					}
				}
				translateMetadata(inFile, outFile, metadataV2, inMetadataType, outMetadataType);
			} catch (Exception ex) {
			    ex.printStackTrace();
			    System.exit(2);
			}
		}
	}

	private static void translateMetadata(File inFile, File outFile, boolean metadataV2, int inMetadataType, int outMetadataType) {
		System.out.println(String.format("In MetadataType=%d is set to %s." , inMetadataType, MetadataTypes.getMetadataTypeName(inMetadataType)));
		System.out.println(String.format("Out MetadataType=%d is set to %s." , outMetadataType, MetadataTypes.getMetadataTypeName(outMetadataType)));

//		Metadata m = null;

		try {
//			m = MetadataParser.parseNonSubmission(inFile);
			/*
			Part 1 of 2
			 * When parseNonSubmission is used to translate to a v3 metadata file,
			 * the v3 Association registry object structure is not preserved properly.
			 * Certain types of Association object structure are corrupted.
			 * Example  associationType="urn:ihe:iti:2010:AssociationType:UpdateAvailabilityStatus"
			 * Corruption happens when some test metadata input files have mixed content: v2 (detected as any namespace non-v3) rim XML namespace with v3 Association objects.
			 *
			 * When metadata files are attempted to translate into v3 using this main method or the parse_xml (unlike the parseNonSubmission) method,
			 * the first translate call changes all registry object namespaces to v3, and when the v3 Association structure is translated it is kept intact because the namespace is v3 by that time.
			 *
			 *
			 */


			String out = convertMetadataFile(inFile, metadataV2, inMetadataType, outMetadataType, true, true);
			if (outFile != null) {
				Io.stringToFile(outFile, out);
			}
			System.out.println("NOTE: XML metadata comments do not carry over when original metadata is being converted in to a separate file. XML comments must be added back manually.");

		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(2);
		}

		/*
		Part 2 of 2
		MetadataCollection mc = MetadataToMetadataCollectionParser.buildMetadataCollection(m, "test");

		Metadata m2 = MetadataCollectionToMetadata.buildMetadata(mc, true);

		List<OMElement> eles = null;

		OMElement x = null;
		try {
		    if (metadataV2) {
		    	eles = m2.getV2();
				x = MetadataSupport.om_factory.createOMElement("Metadata", null);
				for (OMElement e : eles)
					x.addChild(e);
			} else
//				eles = m2.getV3(); // Misses the SubmitObjectsRequest wrapper if it existed
				x = m2.getV3SubmitObjectsRequest(); // assume SubmitObjectsRequest was present
		} catch (XdsInternalException e1) {
			e1.printStackTrace();
			System.exit(1);
		}



		try {
			Io.stringToFile(outFile, new OMFormatter(x).toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		*/
	}

	private static String convertMetadataFile(File inFile, boolean metadataV2, int inMetadataType, int outMetadataType, boolean checkInput, boolean checkOutput) throws Exception {
		Metadata m;
		OMElement e = Util.parse_xml(inFile);
		if (checkInput) {
			validateMetadata("Input", e, inMetadataType);
		}

		m = MetadataParser.parse(e);
		OMElement x = null;
		if (metadataV2) {
			x = m.getV2SubmitObjectsRequest(); // v2 output is not really needed
		} else {
			 x =	m.getV3SubmitObjectsRequest();
			try {
				if (checkOutput) {
					if (! validateMetadata("Output", x, outMetadataType)) {
					    return null;
					}
				}
				return new OMFormatter(x).toString();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		throw new RuntimeException("Unexpected return");
	}

	private static boolean validateMetadata(String label, OMElement e, int metadata_type) throws Exception {
		String schemaLocation ="C:\\Users\\skb1\\myprojects\\iheos-toolkit2\\xdstools2\\src\\test\\resources\\war\\toolkitx\\schema";
		String errors = SchemaValidation.validate(schemaLocation, e, metadata_type);
		boolean isSuccess = "".equals(errors);
		System.out.println(
				String.format("%s XML Validation error(s) ? %s",
						label,
						(isSuccess ? "None." : errors)));
		return isSuccess;
	}

}
