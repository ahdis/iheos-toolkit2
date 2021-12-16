package gov.nist.toolkit.installation.server;

import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.xdsexception.client.TkNotFoundException;
import gov.nist.toolkit.xdsexception.client.ToolkitRuntimeException;
import java.util.logging.Logger;

import java.io.*;
import java.util.*;

public class PropertyManager {

	static private Logger logger = Logger.getLogger(PropertyManager.class.getName());

	static private final String ADMIN_PASSWORD      = "Admin_password";
	static private final String TOOLKIT_HOST        = "Toolkit_Host";
	static private final String TOOLKIT_PORT        = "Toolkit_Port";
	static private final String TOOLKIT_TLS_PORT    = "Toolkit_TLS_Port";
	static private final String GAZELLE_CONFIG_URL  = "Gazelle_Config_URL";
	static private final String EXTERNAL_CACHE      = "External_Cache";
	static private final String USE_ACTORS_FILE     = "Use_Actors_File";
	static public  final String ENABLE_SAML			= "Enable_SAML";
	static public  final String BYPASS_SECURITYHDR_MU_ON_INCOMING_RESPONSE = "Bypass_SecurityHeaderMuOnIncomingResponse";
	/**
	 * Same name is used for both the sts actor and its related sts test plan
	 */
	static public  final String STS_ACTOR_NAME = "Sts_ActorName";
	static public  final String STS_TESTPLAN_NAME = "Sts_TpName";
	static private final String TESTKIT             = "Testkit";
	static private final String LISTENER_PORT_RANGE = "Listener_Port_Range";
	static private final String AUTO_INIT_CONFORMANCE_TOOL = "Auto_init_conformance_tool";
	static private final String MSH_3 = "MSH_3";
	static private final String MSH_4 = "MSH_4";
	static private final String MSH_5 = "MSH_5";
	static private final String MSH_6 = "MSH_6";
	static private final String ARCHIVE_LOGS = "Archive_Logs";
	static private final String IGNORE_INTERNAL_TESTKIT = "Ignore_internal_testkit";
	static public final String MULTIUSER_MODE = "Multiuser_mode";
	static public final String CAS_MODE = "Cas_mode";
	static private final String NONCE_SIZE = "Nonce_size";
	static private final String GAZELLE_TESTING_SESSION = "Gazelle_testing_session";
	static private final String USING_SSL = "Using_SSL";
	static private final String SSL_PORT = "SSL_Port";
	static private final String DEFAULT_TEST_SESSION = "Default_Test_Session";
	static private final String DEFAULT_TEST_SESSION_IS_PROTECTED = "Default_Test_Session_is_Protected";
	static private final String CLIENT_CIPHER_SUITES = "Client_Cipher_Suites";
	static private final String CLIENT_SSL_PROTOCOLS = "Client_SSL_Protocols";

	private String propFile;
	private Properties toolkitProperties = null;

	public PropertyManager(String propFile) {
		this.propFile = propFile;
	}

	public void update(Map<String, String> props) throws Exception {
		if (toolkitProperties == null)
			toolkitProperties = new Properties();
		for (String key : props.keySet()) {
			String value = props.get(key);
			validateProperty(key, value);
			toolkitProperties.put(key, value);
		}
		save(props);
	}

	private void validateProperty(String name, String value) throws Exception {
		if (name == null)
			throw new Exception("Property with name null not allowed");
		if (name.equals(ADMIN_PASSWORD)) {
			if (value == null || value.equals(""))
				throw new Exception("Empty password not allowed");
		}
		else if (name.equals("Actors_file")) {
			File f = new File(value);
			if (f.exists() && f.canWrite())
				return;
			File dir = f.getParentFile();
			dir.mkdirs();
			if (!dir.exists())
				throw new Exception("Cannot create directory for actors.xml file: " + dir.toString());
			if (!f.exists()) {
				f.createNewFile();
				if (f.exists() && f.canWrite()) {
					f.delete();
					return;
				}
				f.delete();
			}
			throw new Exception("Cannot create actors.xml file at: " + f);
		}
		else if (name.equals("Simulator_database_directory")) {
			File f = new File(value);
			f.mkdirs();
			if (!f.exists())
				throw new Exception("Cannot create Message_database_directory " + value);
		}
	}

	public boolean ignoreInternalTestkit() {
		loadProperties();
		String value = (String) toolkitProperties.get(IGNORE_INTERNAL_TESTKIT);
		if (value == null) return false;
		return value.toLowerCase().equals("true");
	}

	public boolean archiveLogs() {
		loadProperties();
		String value = (String) toolkitProperties.get(ARCHIVE_LOGS);
		if (value == null) return false;
		return value.toLowerCase().equals("true");
	}

	public String getMSH3() {
		loadProperties();
		String value = (String) toolkitProperties.get(MSH_3);
		if (value == null || value.equals("")) value = "SRCADT";
		return value;
	}

	public String getMSH4() {
		loadProperties();
		String value = (String) toolkitProperties.get(MSH_4);
		if (value == null || value.equals("")) value = "DH";
		return value;
	}

	public String getMSH5() {
		loadProperties();
		String value = (String) toolkitProperties.get(MSH_5);
		if (value == null || value.equals("")) value = "LABADT";
		return value;
	}

	public String getMSH6() {
		loadProperties();
		String value = (String) toolkitProperties.get(MSH_6);
		if (value == null || value.equals("")) value = "DH";
		return value;
	}

	public String getCacheDisabled() {
		loadProperties();
		return (String) toolkitProperties.get("Cache_Disabled");
	}

	public String getPassword() {
		loadProperties();
		return (String) toolkitProperties.get(ADMIN_PASSWORD);
	}

	public boolean isUsingSSL() {
		loadProperties();
		String value = (String) toolkitProperties.get(USING_SSL);
		if (value == null || value.equals("")) return false;
		return value.equalsIgnoreCase("true");
	}

	public String getSSLPort() {
		loadProperties();
		return (String) toolkitProperties.get(SSL_PORT);
	}

	public String getToolkitHost() {
		loadProperties();
		return (String) toolkitProperties.get(TOOLKIT_HOST);
	}

	public String getToolkitPort() {
		loadProperties();
		return (String) toolkitProperties.get(TOOLKIT_PORT);
	}

	public String getToolkitTlsPort() {
		loadProperties();
		return (String) toolkitProperties.get(TOOLKIT_TLS_PORT);
	}

	public List<String> getListenerPortRange() {
		loadProperties();
		String rangeString = (String) toolkitProperties.get(LISTENER_PORT_RANGE);
		if (rangeString == null) throw new ToolkitRuntimeException(LISTENER_PORT_RANGE + " missing from toolkit.properties file");
		String[] parts = rangeString.split(",");
		if (parts.length != 2) throw new ToolkitRuntimeException(LISTENER_PORT_RANGE + " from toolkit.properties is badly formtted - it must be port_number, port_number");
		List<String> range = new ArrayList<>();
		range.add(parts[0].trim());
		range.add(parts[1].trim());
		return range;
	}

	public boolean getAutoInitializeConformanceTool() {
		loadProperties();
		String value = (String) toolkitProperties.get(AUTO_INIT_CONFORMANCE_TOOL);
		if (value == null) return false;
		if (value.trim().equalsIgnoreCase("true")) return true;
		return false;
	}

	public String getToolkitGazelleConfigURL() {
		loadProperties();
		return (String) toolkitProperties.get(GAZELLE_CONFIG_URL);
	}

	public String getStsActorName() throws TkNotFoundException {
		loadProperties();
		String value = (String) toolkitProperties.get(STS_ACTOR_NAME);
		if (value!=null && !"".equals(value)) {
			return value;
		}
		throw new TkNotFoundException(STS_ACTOR_NAME + " is not configured.", "toolkit.properties");
	}

	public String getStsTpName() throws TkNotFoundException {
		loadProperties();
		String value = (String) toolkitProperties.get(STS_TESTPLAN_NAME);
		if (value!=null && !"".equals(value)) {
			return value;
		}
		throw new TkNotFoundException(STS_TESTPLAN_NAME + " is not configured.", "toolkit.properties");
	}

	public boolean isBypassSecurityHeaderMuOnResponse() {
		loadProperties();
		String use = (String) toolkitProperties.get(BYPASS_SECURITYHDR_MU_ON_INCOMING_RESPONSE);
		if (use == null)
			return false;
		use = use.trim().toLowerCase();
		return "true".compareToIgnoreCase(use) == 0;
	}

	public boolean isEnableSaml() {
		loadProperties();
		String use = (String) toolkitProperties.get(ENABLE_SAML);
		if (use == null)
			return true;
		use = use.trim().toLowerCase();
		return "true".compareToIgnoreCase(use) == 0;
	}

	public String getExternalCache() {
		loadProperties();
		String cache = (String) toolkitProperties.get(EXTERNAL_CACHE);
		// may have %20 instead of space characters on Windows.  Clean them up
		if (cache != null)
			cache = cache.replaceAll("%20", " ");
//		System.setProperty("External_Cache", cache);
		return cache;
	}

	public boolean isUseActorsFile() {
		loadProperties();
		String use = (String) toolkitProperties.get(USE_ACTORS_FILE);
		if (use == null)
			return true;
		return "true".compareToIgnoreCase(use) == 0;
	}

	public String getDefaultAssigningAuthority() {
		loadProperties();
		return (String) toolkitProperties.get("PatientID_Assigning_Authority");
	}

	public String getDefaultTestSession() {
		loadProperties();
		return (String) toolkitProperties.get(DEFAULT_TEST_SESSION);
	}

	public boolean getDefaultTestSessionIsProtected() {
		loadProperties();
		return "true".equalsIgnoreCase((String)toolkitProperties.get(DEFAULT_TEST_SESSION_IS_PROTECTED));
	}

	public String getDefaultEnvironmentName() {
		loadProperties();
		return (String) toolkitProperties.get("Default_Environment");
	}

	public String getTestkit() {
		loadProperties();
		String testkit = (String) toolkitProperties.get(TESTKIT);
		if (testkit != null) {
			testkit = testkit.trim();
			if ("".equals(testkit)) testkit = null;
		}
		return testkit;
	}

	@Deprecated
	public String getCurrentEnvironmentName() {
		String cache = getExternalCache();
		File currentFile = new File(cache + File.separator + "environment" + File.separator + "current");
		String currentName = null;
		try {
			currentName = Io.stringFromFile(currentFile).trim();
		} catch (IOException e) {}
		return currentName;
	}

	@Deprecated
	public void setCurrentEnvironmentName(String name) throws IOException {
		String cache = getExternalCache();

		File currentFile = new File(cache + File.separator + "environment" + File.separator + "current");
		Io.stringToFile(currentFile, name);
	}

	public String getToolkitEnableAllCiphers() {
		loadProperties();
		return (String) toolkitProperties.getProperty("Enable_all_ciphers");
	}

	public void save(Map<String, String> props) throws Exception {
		saveProperties();
	}

	public void loadProperties() {
		if (toolkitProperties != null)
			return;
		toolkitProperties = new Properties();
		logger.info("Loading toolkit properties from " + propFile);
		try {
			toolkitProperties.load(new FileInputStream(propFile));
		} catch (Exception e) {
			throw new ToolkitRuntimeException("Error loading toolkit.properties", e);
		}

		validateProperties();
	}

	private void validateProperties() {
		boolean multiUserMode = toolkitProperties.getProperty(MULTIUSER_MODE, "false").equalsIgnoreCase("true");
		boolean hasDefaultTestSession = !toolkitProperties.getProperty(DEFAULT_TEST_SESSION, "").equals("");

		if (multiUserMode && !hasDefaultTestSession)
			throw new ToolkitRuntimeException("In toolkit.properties: Multiuser_mode is true therefore Default_Test_Session must be set.");
	}

	public void saveProperties() throws Exception {
		validateProperties();
			FileOutputStream fos = new FileOutputStream(propFile);
			toolkitProperties.store(fos, "");
			fos.flush();
			fos.close();
	}

	public Map<String, String> getPropertyMap() {
		loadProperties();
		validateProperties();
		return xformProperties2Map(toolkitProperties);
	}

	public static Map<String, String> xformProperties2Map(final Properties properties) {
		Map<String, String> props = new TreeMap<>();
		for (Object keyObj : properties.keySet()) {
			String key = (String) keyObj;
			String value = properties.getProperty(key);
			props.put(key, value);
		}
		return props;
	}

	public String getWikiBaseAddress() {
		loadProperties();
		return (String) toolkitProperties.getProperty("Wiki_Base_URL");
	}

	public void setExternalCache(String externalCache){
		toolkitProperties.setProperty(EXTERNAL_CACHE,externalCache);
	}

    public String getProxyPort() {
		loadProperties();
		String value = (String) toolkitProperties.getProperty("Proxy_Port");
		if (value == null || value.equals(""))
			value = "7297";
		return value;
    }

    public boolean isSingleuserMode() {
		return !isMultiuserMode() && !isCasMode();
	}

    public boolean isMultiuserMode() {
		loadProperties();
		String mode = (String) toolkitProperties.getProperty(MULTIUSER_MODE);
		if (mode == null)
			mode = "false";
		mode = mode.trim();
		if (mode.equalsIgnoreCase("true"))
			return true;
		return false;
	}

	public boolean isCasMode() {
		loadProperties();
		String mode = (String) toolkitProperties.getProperty(CAS_MODE);
		if (mode == null)
			mode = "false";
		mode = mode.trim();
		if (mode.equalsIgnoreCase("true"))
			return true;
		return false;
	}

	public int getNonceSize() {
		loadProperties();
		String value = (String) toolkitProperties.getProperty(NONCE_SIZE);
		try {
			return Integer.parseInt(value.trim());
		} catch (Throwable e) {
			return 6;
		}
	}

	public String getGazelleTestingSession() {
		loadProperties();
		String value = (String) toolkitProperties.getProperty(GAZELLE_TESTING_SESSION);
		if (value != null) {
			return value.trim();
		}
		return null;
	}

	public String[] getClientCipherSuites() {
		loadProperties();
		return splitTrimProperty((String)toolkitProperties.get(CLIENT_CIPHER_SUITES), ",");
	}


	public String[] getClientSSLProtocols() {
		loadProperties();
		return splitTrimProperty((String)toolkitProperties.get(CLIENT_SSL_PROTOCOLS), ",");
	}

	private String[] splitTrimProperty(String text, String delimiter) {
		if (text == null  || text.trim().equals("") || text.trim().startsWith("${")) return null;

		String[] items = text.split(delimiter);
		for (int i = 0; i < items.length; i++)
		{
			items[i] = items[i].trim();
		}
		return items;
	}
}
