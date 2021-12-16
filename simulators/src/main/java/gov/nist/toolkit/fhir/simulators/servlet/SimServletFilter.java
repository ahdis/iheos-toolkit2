package gov.nist.toolkit.fhir.simulators.servlet;

import gov.nist.toolkit.configDatatypes.server.SimulatorProperties;
import gov.nist.toolkit.http.HttpMessage;
import gov.nist.toolkit.http.HttpParseException;
import gov.nist.toolkit.simcommon.client.SimulatorConfig;
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement;
import gov.nist.toolkit.simcommon.server.SimDb;
import gov.nist.toolkit.fhir.simulators.support.Callback;
import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.xdsexception.ExceptionUtil;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class SimServletFilter implements Filter {
	static Logger logger = Logger.getLogger(SimServletFilter.class.getName());

	@Override
   public void destroy() {

	}

	@Override
   public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		SimServletResponseWrapper wrapper = new SimServletResponseWrapper((HttpServletResponse) response);

        logger.fine("in SimServletFilter");
		if (request instanceof HttpServletRequest) {
		    HttpServletRequest r = (HttpServletRequest) request;
		    logger.info(r.getRequestURI());
        }

		chain.doFilter(request,wrapper);


		SimDb db = (SimDb) request.getAttribute("SimDb");
		if (db == null) {
			logger.severe("SimServletFilter - request.getAttribute(\"SimDb\") failed");
			return;
		}
        SimulatorConfig config = (SimulatorConfig) request.getAttribute("SimulatorConfig");
        if (config == null) {
            logger.severe("SimServletFilter - request.getAttribute(\"SimulatorConfig\") failed");
            return;
        }


//		Map<String, String> hdrs = wrapper.headers;
//		String contentType = wrapper.contentType;
        String hdrsx = response.toString();
		Io.stringToFile(db.getResponseHdrFile(), hdrsx);

//		HttpMessage hmsg = new HttpMessage();
//		hmsg.setHeaderMap(hdrs);
//		String messageHeader;
//		try {
//			messageHeader = hmsg.asMessage();
//		} catch (HttpParseException e) {
//			Io.stringToFile(db.getResponseHdrFile(), ExceptionUtil.exception_details(e));
//			return;
//		}
//
//		if (contentType != null) {
//			if (messageHeader == null || messageHeader.equals(""))
//				messageHeader = "Content-Type: " + contentType + "\r\n\r\n";
//			else
//				messageHeader = messageHeader + "Content-Type: " + contentType + "\r\n\r\n";
//		}
//
//		Io.stringToFile(db.getResponseHdrFile(), messageHeader);

        // This parameter is the base address of a webservice, for example
        // http://localhost:8080/xdstools2/rest/
        SimulatorConfigElement callbackBaseAddressEle = config.get(SimulatorProperties.TRANSACTION_NOTIFICATION_URI);
        SimulatorConfigElement callbackClassNameEle = config.get(SimulatorProperties.TRANSACTION_NOTIFICATION_CLASS);
        if (callbackBaseAddressEle == null) return;
        if (callbackClassNameEle == null) return;
        String callbackClassName = callbackClassNameEle.asString();
        String callbackBase = callbackBaseAddressEle.asString();
        logger.info("Callback...\n...base address is " + callbackBase);
        logger.info("...class name is " + callbackClassName);
        if (callbackBase == null) return;
        callbackBase = callbackBase.trim();
        if (callbackBase.equals("")) return;
        if (!callbackBase.endsWith("/")) callbackBase = callbackBase + "/";
        String callbackURI = callbackBase + "toolkitcallback";
        new Callback().callback(db, config.getId(), callbackURI, callbackClassName);
        logger.info("...callback successful");
    }

	@Override
   public void init(FilterConfig arg0) throws ServletException {

	}

}
