package gov.nist.toolkit.toolkitServices;

import gov.nist.toolkit.actortransaction.client.ParamType;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.simcommon.client.SimulatorConfig;
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement;
import gov.nist.toolkit.toolkitServicesCommon.SimConfig;
import gov.nist.toolkit.toolkitServicesCommon.SimId;
import gov.nist.toolkit.toolkitServicesCommon.resource.SimConfigResource;
import gov.nist.toolkit.toolkitServicesCommon.resource.SimIdResource;
import java.util.logging.Logger;

/**
 * Not for public use.
 */

public class ToolkitFactory {
    static Logger logger = Logger.getLogger(ToolkitFactory.class.getName());

    static public gov.nist.toolkit.simcommon.client.SimId asServerSimId(SimId simId) {
        return new gov.nist.toolkit.simcommon.client.SimId(new TestSession(simId.getUser()), simId.getId(), simId.getActorType(), simId.getEnvironmentName(), false /* simId.isFhir()*/);
    }

    static public SimIdResource asSimIdBean(gov.nist.toolkit.simcommon.client.SimId simId) {
        SimIdResource bean = new SimIdResource();
        bean.setId(simId.getId());
        bean.setUser(simId.getTestSession().getValue());
        bean.setActorType(simId.getActorType());
        bean.setEnvironmentName(simId.getEnvironmentName());
        return bean;
    }

    static public SimConfigResource asSimConfigBean(SimulatorConfig config) {
        SimConfigResource bean = new SimConfigResource();
        bean.setId(config.getId().getId());
        bean.setUser(config.getId().getTestSession().getValue());
        bean.setActorType(config.getActorType());

        for (SimulatorConfigElement ele : config.getElements()) {
            if (ele.hasBoolean()) {
                bean.setProperty(ele.getName(), ele.asBoolean());
            } else if (ele.hasString()) {
                bean.setProperty(ele.getName(), ele.asString());
            } else if (ele.hasList()) {
                bean.setProperty(ele.getName(), ele.asList());
            }
        }
        return bean;
    }

    static public SimulatorConfig asSimulatorConfig(SimConfig res) {
        SimulatorConfig config = new SimulatorConfig();
        config.setId(new gov.nist.toolkit.simcommon.client.SimId(TestSession.DEFAULT_TEST_SESSION, res.getFullId()));
        config.setActorType(res.getActorType());

        for (String propName : res.getPropertyNames()) {
            if (res.isString(propName)) {
                if (propName.endsWith("endpoint")) {
                    config.add(new SimulatorConfigElement(propName, ParamType.ENDPOINT, res.asString(propName)));
                } else {
                    config.add(new SimulatorConfigElement(propName, ParamType.TEXT, res.asString(propName)));
                }
            } else if (res.isBoolean(propName)){
                config.add(new SimulatorConfigElement(propName, ParamType.BOOLEAN, res.asBoolean(propName)));
            } else if (res.isList(propName)) {
                config.add(new SimulatorConfigElement(propName, ParamType.SELECTION, res.asList(propName), false));
            }
        }

        return config;
    }


}
