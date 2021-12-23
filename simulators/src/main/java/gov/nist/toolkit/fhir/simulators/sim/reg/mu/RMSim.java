package gov.nist.toolkit.fhir.simulators.sim.reg.mu;

import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.errorrecording.client.XdsErrorCode;
import gov.nist.toolkit.fhir.simulators.sim.reg.store.*;
import gov.nist.toolkit.fhir.simulators.support.DsSimCommon;
import gov.nist.toolkit.fhir.simulators.support.TransactionSimulator;
import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.simcommon.client.SimulatorConfig;
import gov.nist.toolkit.valregmsg.message.MetadataContainer;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import java.util.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RMSim extends TransactionSimulator {
    static Logger log = Logger.getLogger(RMSim.class.getName());
    protected MessageValidatorEngine mvc;
    protected DsSimCommon dsSimCommon;
    protected Metadata m = null;
    public MetadataCollection mc;
    public MetadataCollection delta;

    public RMSim(DsSimCommon dsSimCommon, SimulatorConfig simulatorConfig) {
        super(dsSimCommon.simCommon, simulatorConfig);
        this.dsSimCommon = dsSimCommon;
    }

    private boolean isRMU() {
        return dsSimCommon.getValidationContext().isRMU;
    }

    private boolean isMU() {
        return dsSimCommon.getValidationContext().isMU;
    }

    private boolean isRMD() {
        return dsSimCommon.getValidationContext().isRMD;
    }

    @Override
    public void run(ErrorRecorder er, MessageValidatorEngine mvc) {
        this.er = er;
        this.mvc = mvc;
        delta = dsSimCommon.regIndex.mc.mkDelta();

        setup();

        if (er.hasErrors())
            return;

        List<String> toDelete = new ArrayList<>(m.getObjectRefIds());

        // good references?
        for (String id : m.getObjectRefIds()) {
            XdsErrorCode.Code code = XdsErrorCode.Code.XDSMetadataUpdateError;
            if (getCommon().vc.isRMU || getCommon().vc.isRM)
                code = XdsErrorCode.Code.UnresolvedReferenceException;
            if (!delta.hasObject(id)) {
                    er.err(code, id, null, null);
            }
        }

        if(er.hasErrors())
            return;

        delta.setDeleting(toDelete);

//        if (delta.getObjectById(toDelete.get(0)) != null)
//            er.err(XdsErrorCode.Code.XDSReferencesExistException, "Oh crap + ", null, null);


        Map<String, Ro> typeMap = delta.buildTypeMap();

        // for each object type verify that no dangling references will be left behind
        for (String id : toDelete) {
            if (typeMap.get(id) instanceof DocEntry) {
                List<Assoc> deAssocs = delta.assocCollection.allThatReference(id);
                if (!deAssocs.isEmpty())
                    er.err(XdsErrorCode.Code.ReferencesExistException, "Cannot delete DocumentEntry " + typeMap.get(id).toString() + " because " + deAssocs.size() + " Associations reference it", null, null);

            }
            if (typeMap.get(id) instanceof SubSet) {
                List<Assoc> deAssocs = delta.assocCollection.allThatReference(id);
                if (!deAssocs.isEmpty())
                    er.err(XdsErrorCode.Code.ReferencesExistException, "Cannot delete SubmissionSet " + typeMap.get(id).toString() + " because " + deAssocs.size() + " Associations reference it", null, null);

            }
            if (typeMap.get(id) instanceof Fol) {
                List<Assoc> deAssocs = delta.assocCollection.allThatReference(id);
                if (!deAssocs.isEmpty())
                    er.err(XdsErrorCode.Code.ReferencesExistException, "Cannot delete Folder " + typeMap.get(id).toString() + " because " + deAssocs.size() + " Associations reference it", null, null);
            }
            if (typeMap.get(id) instanceof Assoc) {
                List<Assoc> aAssocs = delta.assocCollection.allThatReference(id);
                if (!aAssocs.isEmpty())
                    er.err(XdsErrorCode.Code.ReferencesExistException, "Cannot delete Association " + typeMap.get(id).toString() + " because " + aAssocs.size() + " Associations reference it", null, null);
            }
        }

        handleDanglingObjects(er, delta, typeMap, toDelete);

        if(er.hasErrors())
            return;

        for (String id : toDelete) {
            delta.deleteRo(id);
        }
    }

    public static void handleDanglingObjects(ErrorRecorder er, MetadataCollection delta, Map<String, Ro> typeMap, List<String> toDelete) {
        for (String id : toDelete) {
            if (typeMap.get(id) instanceof Assoc) {
                Assoc assoc = delta.assocCollection.getById(id);
                String source = assoc.from;
                String target = assoc.to;

                if (!delta.assocCollection.idsBeingDeleted().contains(source))
                    if (delta.assocCollection.allThatReference(source).isEmpty())
                        er.err(XdsErrorCode.Code.XDSUnreferencedObjectException, "The object " + delta.getObjectById(source) + " would be left without any refereces to it", null, null);
                if (!delta.assocCollection.idsBeingDeleted().contains(target))
                    if (delta.assocCollection.allThatReference(target).isEmpty())
                        er.err(XdsErrorCode.Code.XDSUnreferencedObjectException, "The object " + delta.getObjectById(target) + " would be left without any refereces to it", null, null);
            }
        }
    }

    protected void setup() {

        // Pull metadata container off validation stack
        try {
            MetadataContainer metaCon = (MetadataContainer) dsSimCommon.getMessageValidatorIfAvailable(MetadataContainer.class);
            m = metaCon.getMetadata();
            if (m == null) throw new Exception("");
        } catch (Exception e) {
            er.err(XdsErrorCode.Code.XDSRegistryError, "Internal Error: cannot access input metadata", this, null);
        }

        if (m.getSubmissionSets().size() != 0) {
            er.err(XdsErrorCode.Code.XDSRegistryError, "Remove Metadata request cannot contain SubmissionSet objects", null, null);
            return;
        }
        if (m.getExtrinsicObjects().size() != 0) {
            er.err(XdsErrorCode.Code.XDSRegistryError, "Remove Metadata request cannot contain DocumentEntry objects", null, null);
            return;
        }
        if (m.getAssociations().size() != 0) {
            er.err(XdsErrorCode.Code.XDSRegistryError, "Remove Metadata request cannot contain Association objects", null, null);
            return;
        }
        if (m.getFolders().size() != 0) {
            er.err(XdsErrorCode.Code.XDSRegistryError, "Remove Metadata request cannot contain Folder objects", null, null);
            return;
        }
        mc = dsSimCommon.regIndex.mc;

    }


    }
