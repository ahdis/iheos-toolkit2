package gov.nist.toolkit.http;

import java.util.logging.Logger;

import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.errorrecording.client.XdsErrorCode;
import gov.nist.toolkit.http.HttpHeader.HttpHeaderParseException;


public class PartParser extends HttpParser {
	Part part = new Part();
	static final Logger logger = Logger.getLogger(PartParser.class.getName());
	
	public PartParser(byte[] msg) throws HttpParseException, HttpHeaderParseException, ParseException {
//		super(msg);
		logger.fine("new PartParser(" + this.toString() + ")");
		init(msg, part, er);
		initPart();
	}
	
	public PartParser(byte[] msg, ErrorRecorder er, boolean appendixV) throws HttpParseException, HttpHeaderParseException, ParseException {
//		super(msg,er);
		logger.fine("new PartParser(" + this.toString() + ")");
		this.er = er;
		this.appendixV = appendixV;
		String msgAsString = new String(msg);
		init(msg, part, er);
		initPart();
	}
	
	void initPart() throws HttpParseException, ParseException {
		String contentIDHeaderString = message.getHeader("content-id");
		if (appendixV == false && (contentIDHeaderString == null || contentIDHeaderString.equals("")))
			return;
		try {
			HttpHeader contentIDHeader = new HttpHeader(contentIDHeaderString, er);
			part.contentID = contentIDHeader.getValue();
			logger.fine("new PartParser(" + this.toString() + ") - contentId = " + part.contentID);
			if (part.contentID == null || part.contentID.equals(""))
				throw new HttpParseException("Part has no Content-ID header");
			part.contentID = part.contentID.trim();
			if (!isWrappedIn(part.contentID, "<",">")) {
				if (er != null)
					er.err(XdsErrorCode.Code.NoCode, "Part Content-ID header value must be wrapped in <   >: Content-ID is " + part.contentID, this, "http://www.w3.org/TR/2005/REC-xop10-20050125/  Example 2");
				else
					throw new HttpParseException("Part Content-ID header value must be wrapped in <   >: Content-ID is " + part.contentID);
			} else {
				part.contentID = unWrap(part.contentID);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static public boolean isWrappedIn(String st, String start, String end) {
		if (st.length() < 3)
			return false;
		if (!st.startsWith(start))
			return false;
		if (!st.endsWith(end))
			return  false;
		return true;
	}
	
	static public String unWrap(String st) {
		return st.substring(1, st.length()-1);
	}
	
	public String getContentId() {
		return part.contentID;
	}
	
}
