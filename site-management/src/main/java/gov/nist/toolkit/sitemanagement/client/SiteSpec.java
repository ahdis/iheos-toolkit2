package gov.nist.toolkit.sitemanagement.client;

import com.google.gwt.user.client.rpc.IsSerializable;
import gov.nist.toolkit.actortransaction.shared.ActorType;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.xdsexception.client.ToolkitRuntimeException;

import java.io.Serializable;

/**
 * A Site is the collection of endpoints and parameters for a single site or as Gazelle calls it a system.
 * A Site references multiple actor types but it can hold only one copy of an actor type:
 * one Registry, one Repository etc.
 * A SiteSpec is a reference to a Site and a selection of one actor type. Having a SiteSpec you know
 * exactly which transactions are possible. The actorType parameter is the actor type of interest (personality
 * to be used in an operation) and name is the site name.
 *
 * SiteSpec reference the Site through the name attribute.
 * @author bill
 *
 */
public class SiteSpec implements Serializable, IsSerializable {

	public String name = "";   // site name
	// For tests that depend on Orchestration, we sometimes need to configure supporting actors into the
	// Site. To do this and not alter the Vendor configured Site, a Orchestration Site is created with the following
	// rules.
	//   1. name refers to vendor site
	//   2. orchestrationSiteName refers to orchestration site
	//   3. When searching for endpoint or other facet, look in orchestration site first, vendor site second
	public String orchestrationSiteName = null;
	public ActorType actorType = null;
	public String homeId = "";
	public String homeName = "";
	public boolean isTls = false;
	public boolean isSaml = false;
	String gazelleXuaUsername;
	String stsAssertion;
	public boolean isAsync = false;
	public TestSession testSession;

    /**
     * Create a site spec. This is a data transfer model (DTO) used to manage Sites in the UI.
     * @param name name of the site
     * @param actorType actor type of interest within the site
     * @param toClone if set it is another SiteSpec to get the TLS, SAML, and ASYNC settings from.  If this
     *                parameter is null then default values are used.
     */
	public SiteSpec(String name, ActorType actorType, SiteSpec toClone, TestSession testSession) {
		this.name = name;
		this.actorType = actorType;
		this.testSession = testSession;

		if (toClone == null) {
			isTls = false;
			isSaml = false;
			isAsync = false;
		} else {
			isTls = toClone.isTls;
			isSaml = toClone.isSaml;
			isAsync = toClone.isAsync;
			this.testSession = toClone.testSession;
			this.setGazelleXuaUsername(toClone.getGazelleXuaUsername());
			this.setStsAssertion(toClone.getStsAssertion());
		}
	}

	public SiteSpec() {
	}

	public SiteSpec(String name, TestSession testSession) {
        this(name, null, null, testSession);
    }
	
	public SiteSpec(TestSession testSession) {
		this("", null, null, testSession);
	}

	public boolean isNullSite() { return name.equals(""); }

	public boolean isGW() {
		return (actorType != null) && actorType.isGW();
	}
	
	public boolean isIG() {
		return (actorType != null) && actorType.isIGActor();
	}

	public boolean isIGX() { return (actorType != null) && actorType.isIGXActor(); }

	public boolean isRG() {
		return (actorType != null) && actorType.isRGActor();
	}

	public boolean isRGX() {
		return (actorType != null) && actorType.isRGXActor();
	}

	public boolean isImagingDocumentSourceActor() {
		return (actorType != null) && actorType.isImagingDocumentSourceActor();
	}
	
	public String getTypeName() {
		if (actorType == null) return null;
		return actorType.getShortName();
	}

	public String getName() {
		return name;
	}

	public ActorType getActorType() {
		return actorType;
	}

	public boolean isTls() {
		return isTls;
	}

	public boolean isSaml() {
		return isSaml;
	}

	public void setTls(boolean isTls) {
		this.isTls = isTls;
	}

	public void setSaml(boolean isSaml) {
		this.isSaml = isSaml;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setActorType(ActorType actorType) {
		this.actorType = actorType;
	}

	public String getOrchestrationSiteName() {
		return orchestrationSiteName;
	}

	public void setOrchestrationSiteName(String orchestrationSiteName) {
		this.orchestrationSiteName = orchestrationSiteName;
	}

	public String getGazelleXuaUsername() {
		return gazelleXuaUsername;
	}

	public void setGazelleXuaUsername(String gazelleXuaUsername) {
		this.gazelleXuaUsername = gazelleXuaUsername;
	}

	public String getStsAssertion() {
		return stsAssertion;
	}

	public void setStsAssertion(String stsAssertion) {
		this.stsAssertion = stsAssertion;
	}

	public void validate() {
		if (name == null || name.equalsIgnoreCase(""))
			throw new ToolkitRuntimeException("SiteSpec does not validate - no name - " + toString());
//		if (name.contains("__")) {
//			String[] parts = name.split("__");
//			String theTestSession = parts[0];
//			if (!theTestSession.equals(testSession.getValue()))
//				throw new ToolkitRuntimeException("SiteSpec does not validate - name has embedded testsession which does not match supplied test session is illegal - name is " + name + " testsession is " + testSession);
//		}
		if (testSession == null)
			throw new ToolkitRuntimeException("SiteSpec does not validate - no TestSession - " + toString());
	}

	@Override
	public String toString() {
		return testSession.getValue() + "/" + name;
	}

	public SiteSpec copy() {
		SiteSpec siteSpec = new SiteSpec();
		siteSpec.setSaml(isSaml);
		siteSpec.setStsAssertion(stsAssertion);
		siteSpec.setGazelleXuaUsername(getGazelleXuaUsername());
		siteSpec.setName(name);
		siteSpec.setActorType(actorType);
		siteSpec.testSession = new TestSession(testSession.toString());
		return siteSpec;
	}

}
