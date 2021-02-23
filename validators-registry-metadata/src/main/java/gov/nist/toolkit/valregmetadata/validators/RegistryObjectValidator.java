package gov.nist.toolkit.valregmetadata.validators;

import gov.nist.toolkit.commondatatypes.MetadataSupport;
import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.errorrecording.client.XdsErrorCode;
import gov.nist.toolkit.utilities.xml.XmlUtil;
import gov.nist.toolkit.valregmetadata.datatype.CxFormat;
import gov.nist.toolkit.valregmetadata.datatype.FormatValidator;
import gov.nist.toolkit.valregmetadata.datatype.OidFormat;
import gov.nist.toolkit.valregmetadata.datatype.UuidFormat;
import gov.nist.toolkit.valregmetadata.model.*;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import org.apache.axiom.om.OMElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class RegistryObjectValidator {
    AbstractRegistryObject mo;
    ObjectValidator ov;

    public RegistryObjectValidator(AbstractRegistryObject mo, ObjectValidator ov) {
        this.mo = mo;
        this.ov = ov;
    }

    public void validateRequiredClassificationsPresent(ErrorRecorder er, ValidationContext vc, ClassAndIdDescription desc, String resource) {
        if (!(vc.isXDM || vc.isXDRLimited)) {
            for (String cScheme : desc.getRequiredSchemes()) {
                List<Classification> cs = mo.getClassificationsByClassificationScheme(cScheme);
                if (cs.size() == 0)
                    er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": " + mo.classificationDescription(desc, cScheme) + " is required but missing", this, resource);
            }
        }
    }

    public void validateClassificationsLegal(ErrorRecorder er, ClassAndIdDescription desc, String resource) {
        List<String> cSchemes = new ArrayList<String>();

        for (Classification c : mo.getClassifications()) {
            String cScheme = c.getClassificationScheme();
            if (cScheme == null || cScheme.equals("") || !desc.getDefinedSchemes().contains(cScheme)) {
                er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": " + c.identifyingString() + " has an unknown classificationScheme attribute value: " + cScheme, this, resource);
            } else {
                cSchemes.add(cScheme);
            }
        }

        Set<String> cSchemeSet = new HashSet<String>();
        cSchemeSet.addAll(cSchemes);
        for (String cScheme : cSchemeSet) {
            if (count(cSchemes, cScheme) > 1 && !desc.getMultipleSchemes().contains(cScheme))
                er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": " + mo.classificationDescription(desc, cScheme) + " is specified multiple times, only one allowed", this, resource);
        }
    }

    public void validateClassificationsCodedCorrectly(ErrorRecorder er, ValidationContext vc) {
        for (Classification c : mo.getClassifications())
            new ClassificationValidator(c).validateStructure(er, vc);

        for (Author a : mo.getAuthors())
            new AuthorValidator(a).validateStructure(er, vc);
    }

    public void validateClassifications(ErrorRecorder er, ValidationContext vc, ClassAndIdDescription desc, String resource)  {
        er.challenge("Validating Classifications present are legal");
        validateClassificationsLegal(er, desc, resource);
        er.challenge("Validating Required Classifications present");
        validateRequiredClassificationsPresent(er, vc, desc, resource);
        er.challenge("Validating Classifications coded correctly");
        validateClassificationsCodedCorrectly(er, vc);
    }

    public void validateExternalIdentifiers(ErrorRecorder er, ValidationContext vc, ClassAndIdDescription desc, String resource) {
        er.challenge("Validating ExternalIdentifiers present are legal");
        validateExternalIdentifiersLegal(er, desc, resource);
        er.challenge("Validating Required ExternalIdentifiers present");
        validateRequiredExternalIdentifiersPresent(er, vc, desc, resource);
        er.challenge("Validating ExternalIdentifiers coded correctly");
        validateExternalIdentifiersCodedCorrectly(er, vc, desc, resource);
    }

    public void validateExternalIdentifiersCodedCorrectly(ErrorRecorder er, ValidationContext vc, ClassAndIdDescription desc, String resource) {
        for (ExternalIdentifier ei : mo.getExternalIdentifiers()) {
            new ExternalIdentifierValidator(ei).validateStructure(er, vc);
            if (MetadataSupport.XDSDocumentEntry_uniqueid_uuid.equals(ei.getIdentificationScheme())) {
                String[] parts = ei.getValue().split("\\^");
                // Expecting an uniqueId to be in the format: OID^extension
                // where OID = root.suffix (a length of 64 characters)
                // where extension is 16 characters (CDA Document ID)
                // In other words, a full-form of an uniqueId is: root.suffix^extension
                new OidFormat(er, mo.identifyingString() + ": " + ei.identifyingString(), externalIdentifierDescription(desc, ei.getIdentificationScheme()))
                        .validate(parts[0]);
                /*
                ITI TF 3
                4.2.3.2.26 DocumentEntry.uniqueId
                For documents using URIs, the uniqueId should be the URI, *except* for URNs with the “urn:oid:” and “urn:uuid:” namespaces.
                -- This is why only the plain value is stored without the URI scheme.
                ITI TF 3
                Table 4.2.3.1.7-2: Data Types (previously Table 4.1-3)
                Identifier data type:
                when an OID format is specified, it shall follow the assignment and format rules defined for unique IDs in ITI TF-2x: Appendix B.
                ITI TF 2x
                Appendix B
                B.3 UID Encoding Rules
                UIDs shall not exceed 64 total characters, including the digits of each component, and separators between components.
                 */
                if (parts[0].length() > 64)
                    er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": " + ei.identifyingString() + " OID part of DocumentEntry uniqueID is limited to 64 digits", this, resource);
                if (parts.length > 1 && parts[1].length() > 16) {
                    er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": " + ei.identifyingString() + " extension part of DocumentEntry uniqueID is limited to 16 characters", this, resource);
                }

            } else if (MetadataSupport.XDSDocumentEntry_patientid_uuid.equals(ei.getIdentificationScheme())){
                new CxFormat(er, mo.identifyingString() + ": " + ei.identifyingString(), "ITI TF-3: Table 4.1.7")
                        .validate(ei.getValue());
            } else if (MetadataSupport.XDSSubmissionSet_uniqueid_uuid.equals(ei.getIdentificationScheme())) {
                new OidFormat(er, mo.identifyingString() + ": " + ei.identifyingString(), externalIdentifierDescription(desc, ei.getIdentificationScheme()))
                        .validate(ei.getValue());
            }
        }
    }

    public void validateRequiredExternalIdentifiersPresent(ErrorRecorder er, ValidationContext vc, ClassAndIdDescription desc, String resource)  {
        for (String idScheme : desc.getRequiredSchemes()) {
            List<ExternalIdentifier> eis = mo.getExternalIdentifiers(idScheme);
            if (eis.size() == 0)
                er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": " + externalIdentifierDescription(desc, idScheme) + " is required but missing", this, resource);
            if (eis.size() > 1)
                er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": " + externalIdentifierDescription(desc, idScheme) + " is specified multiple times, only one allowed", this, resource);
        }
    }

    public void validateExternalIdentifiersLegal(ErrorRecorder er, ClassAndIdDescription desc, String resource) {
        for (ExternalIdentifier ei : mo.getExternalIdentifiers()) {
            String idScheme = ei.getIdentificationScheme();
            if (idScheme == null || idScheme.equals("") || !desc.getDefinedSchemes().contains(idScheme))
                er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": " + ei.identifyingString() + " has an unknown identificationScheme attribute value: " + idScheme, this, resource);
        }
    }

    public void validateSlot(ErrorRecorder er, String slotName, boolean multivalue, FormatValidator validator, String resource) {
        Slot slot = mo.getSlot(slotName);
        if (slot == null) {
            return;
        }

        new SlotValidator(slot).validate(er, multivalue, validator, resource);
    }

    public boolean verifySlotsUnique(ErrorRecorder er) {
        boolean ok = true;
        List<String> names = new ArrayList<String>();
        for (Slot slot : mo.getSlots()) {
            if (names.contains(slot.getName()))
                if (er != null) {
                    er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": Slot " + slot.getName() + " is multiply defined", this, "ebRIM 3.0 section 2.8.2");
                    ok = false;
                }
                else
                    names.add(slot.getName());
        }
        return ok;
    }

    public void validateHome(ErrorRecorder er, String resource) {
        if (mo.getHome() == null)
            er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": homeCommunityId attribute must be present", this, resource);
        else {
            if (mo.getHome().length() > 64)
                er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": homeCommunityId is limited to 64 characters, found " + mo.getHome().length(), this, resource);

            String[] parts = mo.getHome().split(":");
            if (parts.length < 3 || !parts[0].equals("urn") || !parts[1].equals("oid"))
                er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": homeCommunityId must begin with urn:oid: prefix, found [" + mo.getHome() + "]", this, resource);
            new OidFormat(er, mo.identifyingString() + " homeCommunityId", resource).validate(parts[parts.length-1]);
        }
    }

    public void validateTopAtts(ErrorRecorder er, ValidationContext vc, String tableRef, List<String> statusValues) {
        validateId(er, vc, "entryUUID", mo.getId(), null);

        if (vc.isSQ && vc.isResponse) {
            if (mo.getStatus() == null)
                er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": availabilityStatus attribute (status attribute in XML) must be present", this, tableRef);
            else {
                if (!statusValues.contains(mo.getStatus()))
                    er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": availabilityStatus attribute must take on one of these values: " + statusValues + ", found " + mo.getStatus(), this, "ITI TF-2a: 3.18.4.1.2.3.6");
            }

            validateId(er, vc, "lid", mo.getLid(), null);

            List<OMElement> versionInfos = XmlUtil.childrenWithLocalName(mo.getRo(), "VersionInfo");
            if (versionInfos.size() == 0) {
                er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": VersionInfo attribute missing", this, "ebRIM Section 2.5.1");
            }
        }

        if (vc.isSQ && vc.isXC && vc.isResponse) {
            validateHome(er, tableRef);
        }
    }

    public void validateId(ErrorRecorder er, ValidationContext vc, String attName, String attValue, String resource) {
        String defaultResource = "ITI TF-3: 4.1.12.3";
        if (attValue == null || attValue.equals("")) {
            er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": " + attName + " attribute empty or missing", this, (resource!=null) ? resource : defaultResource);
        } else {
            if (vc.isSQ && vc.isResponse) {
                new UuidFormat(er, mo.identifyingString() + " " + attName + " attribute must be a UUID (all lower case)", (resource!=null) ? resource : defaultResource).validate(mo.getId());
            } else if(mo.getId().startsWith("urn:uuid:")) {
                new UuidFormat(er, mo.identifyingString() + " " + attName + " attribute", (resource!=null) ? resource : defaultResource).validate(mo.getId());
            }
        }

        for (Classification c : mo.getClassifications())
            new RegistryObjectValidator(c, ov).validateId(er, vc, "entryUUID", c.getId(), resource);

        for (Author a : mo.getAuthors())
            new RegistryObjectValidator(a, ov).validateId(er, vc, "entryUUID", a.getId(), resource);

        for (ExternalIdentifier ei : mo.getExternalIdentifiers())
            new RegistryObjectValidator(ei, ov).validateId(er, vc, "entryUUID", ei.getId(), resource);

    }

    public void validateSlots(ErrorRecorder er, ValidationContext vc) {
        er.challenge("Validating that Slots present are legal");
        ov.validateSlotsLegal(er);
        er.challenge("Validating required Slots present");
        ov.validateRequiredSlotsPresent(er, vc);
        er.challenge("Validating Slots are coded correctly");
        ov.validateSlotsCodedCorrectly(er, vc);
    }

    public void verifyIdsUnique(ErrorRecorder er, Set<String> knownIds) {
        if (mo.getId() != null) {
            if (knownIds.contains(mo.getId()))
                er.err(XdsErrorCode.Code.XDSRegistryMetadataError, mo.identifyingString() + ": entryUUID " + mo.getId() + "  identifies multiple objects", this, "ITI TF-3: 4.1.12.3 and ebRS 5.1.2");
            knownIds.add(mo.getId());
        }

        for (Classification c : mo.getClassifications())
            new RegistryObjectValidator(c, ov).verifyIdsUnique(er, knownIds);

        for (Author a : mo.getAuthors())
            new RegistryObjectValidator(a, ov).verifyIdsUnique(er, knownIds);

        for (ExternalIdentifier ei : mo.getExternalIdentifiers())
            new RegistryObjectValidator(ei, ov).verifyIdsUnique(er, knownIds);
    }


    private String externalIdentifierDescription(ClassAndIdDescription desc, String eiScheme) {
        return "ExternalIdentifier(" + eiScheme + ")(" + desc.getNames().get(eiScheme) + ")";
    }

    private int count(List<String> strings, String target) {
        int i=0;

        for (String s : strings)
            if (s.equals(target))
                i++;

        return i;
    }


}
