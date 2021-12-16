package gov.nist.toolkit.registrymetadata;

import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.utilities.xml.Util;
import gov.nist.toolkit.utilities.xml.XmlFileStream;
import gov.nist.toolkit.xdsexception.client.MetadataException;
import gov.nist.toolkit.xdsexception.client.MetadataValidationException;
import gov.nist.toolkit.xdsexception.client.ToolkitRuntimeException;
import gov.nist.toolkit.xdsexception.client.XdsInternalException;
import org.apache.axiom.om.OMElement;
import java.util.logging.Logger;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;

public class MetadataParser {
	static final Logger logger = Logger.getLogger(MetadataParser.class.getName());

	public MetadataParser() {
	}

	static public Metadata parseNonSubmission(OMElement e) throws MetadataException, MetadataValidationException {
		return parseNonSubmission(e, false);
	}

	static public Metadata parseNonSubmission(String s) throws MetadataException, MetadataValidationException, XdsInternalException, FactoryConfigurationError {
		return parseNonSubmission(Util.parse_xml(s), false);
	}

	static public Metadata parseNonSubmission(OMElement e, boolean rm_duplicates) throws MetadataException, MetadataValidationException {
		Metadata m = new Metadata();

		m.setGrokMetadata(false);

		if (e != null) {
			m.setMetadata(e);

			m.runParser(rm_duplicates);
		}

		return m;
	}

	static public Metadata parseObject(OMElement e) throws MetadataException {
		Metadata m = new Metadata();

		m.setGrokMetadata(false);

		m.parseObject(e);

		return m;
	}


	static public Metadata parseNonSubmission(File metadata_file) throws MetadataException, MetadataValidationException, XdsInternalException {
//		return parseNonSubmission(Util.parse_xml(metadata_file));
		XmlFileStream xmlFs = null;
		try {
			xmlFs = XmlFileStream.parse_xml(metadata_file);
			return parseNonSubmission(xmlFs.getOmElement());
		} finally {
			if (xmlFs!=null) {
				if (xmlFs.getParser()!=null)
						try {xmlFs.getParser().close();} catch (XMLStreamException xmlse) {}
				if (xmlFs.getFr()!=null)
						try {xmlFs.getFr().close();} catch (IOException ioe){}
			}

		}

	}

	// may be hidden in a multi-part or worse - make best effort
	static public Metadata parseContent(File metadata_file) throws IOException, XdsInternalException, MetadataException {
		String data = Io.stringFromFile(metadata_file);
		return parseContent(null, data);
	}

	static public Metadata parseContent(File metadata_file, String data) throws XdsInternalException, MetadataException {
		int index = 0;
		OMElement ele = null;
		for (int i=0; i<10; i++) { // try to skip past header junk to find real xml
			try {
				String x = data.substring(index);
				logger.fine("Trying to parse - " + x.substring(0, 30));
				ele = Util.parse_xml(x);
			} catch (XdsInternalException e) {
				logger.fine("Parse Failed");
				index = data.indexOf('<', index+1);
				if (index == -1)
					throw new XdsInternalException("Cannot XML parse metadata " + ((metadata_file == null) ? "" : metadata_file));
				continue;
			}
			logger.fine("Parse succeeded");
			return parseNonSubmission(ele);
		}
		throw new XdsInternalException("Cannot XML parse file " + metadata_file);
	}

	static public Metadata noParse(OMElement e) {
		Metadata m = new Metadata();

		m.setGrokMetadata(false);

		if (e != null) {
			m.setMetadata(e);

		}
		return m;
	}

	static public Metadata noParse(File metadata_file) throws MetadataException,XdsInternalException  {
		return noParse(Util.parse_xml(metadata_file));
	}

	static public Metadata parse(OMElement e)  throws MetadataException,XdsInternalException, MetadataValidationException {
		return new Metadata(e);
	}

	static public Metadata parse(String metadata) throws XdsInternalException, MetadataException {
		OMElement ele = Util.parse_xml(metadata);
		return parse(ele);
	}
}
