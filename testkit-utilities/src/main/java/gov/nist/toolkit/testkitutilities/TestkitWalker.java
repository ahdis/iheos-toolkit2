package gov.nist.toolkit.testkitutilities;

import gov.nist.toolkit.utilities.xml.Util;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.xml.parsers.FactoryConfigurationError;

import gov.nist.toolkit.utilities.xml.XmlUtil;
import org.apache.axiom.om.OMElement;
import org.jaxen.JaxenException;

abstract public class TestkitWalker {
	protected   String testkitPathName;
	int testkitPathNameSize;
	protected   int testkitPathElementsToIgnore;
	static boolean debug = false;
	protected static int errors = 0;
	protected String area;
	String[] areas = { "examples", "testdata", "tests", "gov/nist/toolkit/installation/server"};


	abstract public void startSection(File section) throws Exception;
	abstract public void endSection(File section) throws Exception;
	abstract public void startTest(File test) throws Exception;
	abstract public void endTest(File test) throws Exception;

	abstract public void startPart(File part) throws Exception;
	abstract public void endPart(File part) throws Exception;
	// test plan may be inside test or part but not both
	abstract public void startTestPlan(File testplan) throws Exception;
	abstract public void endTestPlan(File testplan) throws Exception;

	abstract public void startServer(File test) throws Exception;
	abstract public void endServer(File test) throws Exception;

	protected int testPlanCount = 0;

	abstract public void doStep(String step) throws Exception;

	abstract public void begin() throws Exception;
	abstract public void end() throws Exception;

	protected File testkit;

	private final static Logger logger = Logger.getLogger(TestkitWalker.class.getName());

	public File getTestkit() {
		return testkit;
	}
	public void setTestkit(File testkit) {
		this.testkit = testkit;
	}

	protected String getCurrentArea() {
		return area;
	}

	protected void setAreas(String[] areas) {
		this.areas = areas;
	}

	public void walkTree(File testkit) throws FactoryConfigurationError, Exception {
		testkitPathName = testkit.toString();
		testkitPathNameSize = testkitPathName.split(Matcher.quoteReplacement(File.separator)).length;
		testkitPathElementsToIgnore = testkitPathNameSize + 1;


		logger.info("Scanning testkit at " + testkit);

		setTestkit(testkit);

		begin();

		if ( !new File(testkit + File.separator + "tests").exists())
			throw new Exception("Testkit " + testkit + " is not really the testkit");

		for (String area : areas) {
			this.area = area;
			logger.info("Scanning " + area);

			File areaDir = new File(testkit.toString() + File.separator + area);
			if (!areaDir.isDirectory() || !areaDir.exists()) {
				logger.warning("Scan area does not exist. Skipping.");
				continue;
			}

			if (debug)
				logger.fine("Area: " + areaDir);

			startSection(areaDir);

			for (File test : areaDir.listFiles()) {
				if (test.getName().equals(".svn"))
					continue;

				if (debug)
					logger.info("Test: " + test);

				if (!test.isDirectory())
					continue;

				if ("gov/nist/toolkit/installation/server".equals(area)) {
					startServer(test);
					endServer(test);
				}

				startTest(test);

				for (File part : test.listFiles()) {
					if (part.getName().equals(".svn"))
						continue;

					if (debug)
						logger.info("Part: " + part);

					if (part.isFile()) {
						if (part.getName().equals("testplan.xml")) {
							startTestPlan(part);
							walkTestPlan(part);
							endTestPlan(part);
						}
					}

					if (part.isDirectory()) {
						for (File subelement : part.listFiles()) {
							if (subelement.getName().equals(".svn"))
								continue;

							if (debug)
								logger.info("Subelement: " + subelement);

							startPart(part);

							if (subelement.isFile()) {
								if (subelement.getName().equals("testplan.xml")) {
									startTestPlan(subelement);
									walkTestPlan(subelement);
									endTestPlan(subelement);
								}
							}

							endPart(part);
						}
					}
				}

				endTest(test);
			}

			endSection(areaDir);
		}

		end();
	}

	void walkTestPlan(File testPlanFile) throws FactoryConfigurationError, Exception {
		OMElement testplanEle = Util.parse_xml(testPlanFile);

		String testStepElementName = "TestStep";
		List<OMElement> steps = XmlUtil.childrenWithLocalName(testplanEle, testStepElementName);

		for(int i=0; i<steps.size(); i++) {
			OMElement stepEle = steps.get(i);
			String stepElementName = stepEle.getLocalName();
			doStep(stepElementName);

			List<OMElement> testStepChildrenElements = XmlUtil.children(stepEle);
			for(int j=0; j<testStepChildrenElements.size(); j++) {
				OMElement testStepChildEle = testStepChildrenElements.get(j);
				String testStepChildEleLocalName = testStepChildEle.getLocalName();

				if (testStepChildEleLocalName.endsWith("Transaction")) {
					doTransaction(testPlanFile, testStepChildEle, testStepElementName, testStepChildEleLocalName);
				}
			}

		}

	}

	protected abstract void doTransaction(File testPlanFile, OMElement stepEle, String testStepElementName, String stepElementName) throws JaxenException;

	protected String join(String[] parts, int first, int last, String separator) {
		StringBuffer buf = new StringBuffer();

		for (int i=first; i<= last; i++) {
			if (i != first)
				buf.append(separator);
			buf.append(parts[i]);
		}

		return buf.toString();
	}


}
