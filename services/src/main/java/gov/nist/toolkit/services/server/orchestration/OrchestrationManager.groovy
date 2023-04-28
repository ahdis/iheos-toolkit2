package gov.nist.toolkit.services.server.orchestration

import gov.nist.toolkit.installation.server.Installation
import gov.nist.toolkit.services.client.*
import gov.nist.toolkit.services.server.RawResponseBuilder
import gov.nist.toolkit.services.server.ToolkitApi
import gov.nist.toolkit.session.server.Session
import groovy.transform.TypeChecked

/**
 *
 */
@TypeChecked
class OrchestrationManager {

    public RawResponse buildIgTestEnvironment(Session session, IgOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new IgOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    public RawResponse buildIgxTestEnvironment(Session session, IgxOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new IgxOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    public RawResponse buildIigTestEnvironment(Session session, IigOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new IigOrchestrationBuilder(api, session, request).buildTestEnvironment();
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    public RawResponse buildRgTestEnvironment(Session session, RgOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new RgOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    public RawResponse buildRgxTestEnvironment(Session session, RgxOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new RgxOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    public RawResponse buildRigTestEnvironment(Session session, RigOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new RigOrchestrationBuilder(api, session, request).buildTestEnvironment();
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    public RawResponse buildIdsTestEnvironment(Session session, IdsOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new IdsOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    public RawResponse buildIdcTestEnvironment(Session session, IdcOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new IdcOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    public RawResponse buildEstTestEnvironment(Session session, EsOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new EstOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    public RawResponse buildRepTestEnvironment(Session session, RepOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new RepOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    public RawResponse buildRegTestEnvironment(Session session, RegOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new RegOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    public RawResponse buildRecTestEnvironment(Session session, RecOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new RecOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }


    public RawResponse buildRSNAEdgeTestEnvironment(Session session, RSNAEdgeOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new RSNAEdgeOrchestrationBuilder(api, session, request).buildTestEnvironment();
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

    RawResponse buildDocAdminTestEnvironment(Session session, DocAdminOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new DocAdminOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e)
        }
    }

    RawResponse buildSrcTestEnvironment(Session session, SrcOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new SrcOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e)
        }
    }

    RawResponse buildIsrTestEnvironment(Session session, IsrOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new IsrOrchestrationBuilder(api, session, request).buildTestEnvironment()
        } catch (Exception e) {
            return RawResponseBuilder.build(e)
        }
    }

    /*
    RawResponse buildFhirSupportEnvironment(Session session, FhirSupportOrchestrationRequest request) {
        try {
            ToolkitApi api
            if(Installation.instance().warHome()) {
                api = ToolkitApi.forNormalUse(session)
            } else {
                api = ToolkitApi.forInternalUse()
            }
            return new FhirSupportOrchestrationBuilder(api, session, request).buildTestEnvironment();
        } catch (Exception e) {
            return RawResponseBuilder.build(e);
        }
    }

     */
}
