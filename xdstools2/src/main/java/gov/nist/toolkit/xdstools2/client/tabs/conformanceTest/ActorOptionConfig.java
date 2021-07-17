package gov.nist.toolkit.xdstools2.client.tabs.conformanceTest;

import com.google.gwt.user.client.rpc.AsyncCallback;
import gov.nist.toolkit.actortransaction.shared.ActorType;
import gov.nist.toolkit.actortransaction.shared.IheItiProfile;
import gov.nist.toolkit.results.client.TestInstance;
import gov.nist.toolkit.actortransaction.shared.ActorOption;
import gov.nist.toolkit.xdstools2.client.command.command.GetCollectionMembersCommand;
import gov.nist.toolkit.xdstools2.client.tabs.GatewayTestsTabs.BuildRGTestOrchestrationButton;
import gov.nist.toolkit.xdstools2.client.util.ClientUtils;
import gov.nist.toolkit.xdstools2.shared.command.request.GetCollectionRequest;

import java.util.List;

/**
 * A type that takes into account both the actor type and option selected
 */
public class ActorOptionConfig extends ActorOption {
    /**
     * Contains the actor-root level tab config not the option level tab config.
     */
    private TabConfig tabConfig;
    private boolean launchedFromMenu = false;

    @Override
    public String toString() {
        return "ActorOption=[" + super.toString() + "] " + ((tabConfig != null) ? "tabConfig=[" + tabConfig.toString() + "]" : " tabConfig=null");
    }

    public ActorOptionConfig() {
    }

    public ActorOptionConfig(String actorTypeId) {
        this.actorTypeId = actorTypeId;
    }

    public ActorOptionConfig(String actorTypeId, String optionId) {
        this(actorTypeId);
        this.optionId = optionId;
    }

    public ActorOptionConfig(String actorTypeId, IheItiProfile profileId, String optionId) {
        this(actorTypeId, optionId);
        this.profileId = profileId;
    }

    public ActorOptionConfig(TabConfig actorTabConfig) {
        TabConfig lastVisibleProfileTabConfig = null;
        TabConfig lastVisibleOptionTabConfig = null;
        if (actorTabConfig!=null && actorTabConfig.hasChildren() ) {
            TabConfig profiles = actorTabConfig.getFirstChildTabConfig();
            if ("Profiles".equals(profiles.getLabel())) {
                for (TabConfig tc : profiles.getChildTabConfigs()) {
                    if (!tc.isVisible()) {
                        continue;
                    }
                    lastVisibleProfileTabConfig = tc;
                }
            }
        }
        if (lastVisibleProfileTabConfig!=null && lastVisibleProfileTabConfig.hasChildren()) {
            TabConfig options = lastVisibleProfileTabConfig.getFirstChildTabConfig();
            if ("Options".equals(options.getLabel())) {
                for (TabConfig tc : options.getChildTabConfigs()) {
                    if (!tc.isVisible()) {
                        continue;
                    }
                    lastVisibleOptionTabConfig = tc;
                }
            }
        }
        if (lastVisibleOptionTabConfig!=null) {
            this.actorTypeId = actorTabConfig.getTcCode();
            this.profileId = IheItiProfile.find(lastVisibleProfileTabConfig.getTcCode());
            this.optionId = lastVisibleOptionTabConfig.getTcCode();
        }
    }

    /**
     * Tests for options are listed in collections as
     * actor
     * actor_option
     * actor(profile)
     * actor(profile)_option
     * @param callback with list of testIds
     */
    void loadTests(final AsyncCallback<List<TestInstance>> callback) {
        GetCollectionRequest request;

        request = new GetCollectionRequest(ClientUtils.INSTANCE.getCommandContext(), "collections", getTestCollectionCode());

        new GetCollectionMembersCommand() {
            @Override
            public void onComplete(List<TestInstance> result) {
                callback.onSuccess(result);
                launchedFromMenu = false;
            }
        }.run(request);
    }

    /*
    public boolean isFhirSupport() {
        return actorTypeId != null && ActorType.FHIR_SUPPORT.getActorCode().equals(actorTypeId);
    }
     */

    public boolean isDocAdmin() {
        return actorTypeId != null && ActorType.DOC_ADMIN.getActorCode().equals(actorTypeId);
    }

    public boolean isSrc() {
        return actorTypeId != null && ActorType.DOC_SOURCE.getActorCode().equals(actorTypeId);
    }

    public boolean isIsr() {
       return actorTypeId != null && ActorType.ISR.getActorCode().equals(actorTypeId);
    }

    public boolean isXds() {
        return profileId != null && IheItiProfile.XDS.equals(profileId);
    }

    public boolean isMhd() {
        return profileId != null && IheItiProfile.MHD.equals(profileId);
    }

    public boolean isRep() {
        return actorTypeId != null && ActorType.REPOSITORY.getActorCode().equals(actorTypeId);
    }

    public boolean isRg() {
        return actorTypeId != null && ActorType.RESPONDING_GATEWAY.getActorCode().equals(actorTypeId);
    }

    public boolean isIg() {
        return actorTypeId != null && ActorType.INITIATING_GATEWAY.getActorCode().equals(actorTypeId);
    }

    public boolean isReg() {
        return actorTypeId != null && ActorType.REGISTRY.getActorCode().equals(actorTypeId);
    }

    public boolean isRec() {
        return actorTypeId != null && ActorType.DOCUMENT_RECIPIENT.getActorCode().equals(actorTypeId);
    }

    public boolean isInitiatingImagingGatewaySut() {
        return actorTypeId != null
                && ActorType.INITIATING_IMAGING_GATEWAY.getActorCode().equals(actorTypeId);
    }

    public boolean isRespondingingImagingGatewaySut() {
        return actorTypeId != null
                && ActorType.RESPONDING_IMAGING_GATEWAY.getActorCode().equals(actorTypeId);
    }

    public boolean isEs() {
        return actorTypeId != null && ActorType.RSNA_EDGE_DEVICE.getShortName().equals(actorTypeId);
    }

    public boolean isOnDemand() {
        return optionId.equals(BuildRGTestOrchestrationButton.ON_DEMAND_OPTION);
    }

    public boolean isImagingDocSourceSut() {
        return actorTypeId != null
                && ActorType.IMAGING_DOC_SOURCE.getActorCode().equals(actorTypeId);
    }
    
    public boolean isIDC() {
       return actorTypeId != null 
                && ActorType.IMAGING_DOC_CONSUMER.getActorCode().equals(actorTypeId);
    }

    public void setOptionId(String optionId) {
        this.optionId = optionId;
    }

    public String getActorTypeId() {
        return actorTypeId;
    }

    public void setActorTypeId(String actorTypeId) {
        this.actorTypeId = actorTypeId;
    }

    public String getOptionId() {
        return optionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActorOptionConfig that = (ActorOptionConfig) o;

        if (actorTypeId != null ? !actorTypeId.equals(that.actorTypeId) : that.actorTypeId != null) return false;
        return optionId != null ? optionId.equals(that.optionId) : that.optionId == null;

    }

    @Override
    public int hashCode() {
        int result = actorTypeId != null ? actorTypeId.hashCode() : 0;
        result = 31 * result + (optionId != null ? optionId.hashCode() : 0);
        return result;
    }

    /**
     * Gets the actor-root level tab config not the option level tab config.
     */
    public TabConfig getTabConfig() {
        return tabConfig;
    }

    public void setTabConfig(TabConfig tabConfig) {
        this.tabConfig = tabConfig;
    }

    public boolean isLaunchedFromMenu() {
        return launchedFromMenu;
    }

    public void setLaunchedFromMenu(boolean launchedFromMenu) {
        this.launchedFromMenu = launchedFromMenu;
    }
}
