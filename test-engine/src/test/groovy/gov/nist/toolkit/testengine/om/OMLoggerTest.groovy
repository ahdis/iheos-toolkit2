package gov.nist.toolkit.testengine.om


import gov.nist.toolkit.utilities.xml.OMFormatter
import gov.nist.toolkit.utilities.xml.Util
import org.apache.axiom.om.OMElement
import org.apache.axiom.om.OMException
import spock.lang.Specification

class OMLoggerTest extends Specification {

    def 'test original'() {
        given:
        def msg = '''
<SOAP-ENV:Envelope xmlns:SOAP-ENV='http://www.w3.org/2003/05/soap-envelope' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:s='http://www.w3.org/2001/XMLSchema' xmlns:wsa='http://www.w3.org/2005/08/addressing' xmlns:wsse='http://docs.oasis-open.org/wss/2004/01/o0asis-200401-wss-wssecurity-secext-1.0.xsd' xmlns:wsu='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd'>
    <SOAP-ENV:Header>
        <wsa:Action>urn:hl7-org:v3:PRPA_IN201306U0V02:CrossGatewayPatientDiscovery</wsa:Action>
        <wsa:MessagelD>urn:uuid:E2E5DD4A-2FFE-11E9-A926-005056A6P8F7</wsa:MessagelD>>
        <wsa:RelatesTo>e70ba0ff7-4075-4eca-bec8-444b7da25446</wsa:RelatesTo>
        <wsa:To>http://www.w3.org/2005/08/addressing/anonymous</wsa:To>
        <Security xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
            <Timestamp xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" wsu:Id="Timestamp-E2E552P8-2FFE-11E9-A926-O05056A6F8F7">
                <Created>2019-02-14T02:18 : 58Z</Created>
                <Expires>2019-02-14T02:23:58Z</Expires>
            </Timestamp>
            <SignatureConfirmation xmlns="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsqa" wsu:Id="Id-E2E5562C-2PPE-11E9-A926-O05056A6F8F7" Value="gOv6FWOOVUT6r3JtC2JbJ2kPmws/TPOtGwEnH6cZMpqPjc6/jBBGdY4vzveyjbLgtG92azhxDNVZojR0eX60Bwb1GVM/LdShhvwkahUBCrN9/bzTXLdoWwQX24y3pvykj+0Ae6w2yDYV5ACSvks1E+0M4Y1H2BDUEbg8+ELbvDhUjoErLemJaNGCPZ9LCXZ/1qBDR0itelSMOHCUnAc4rTsKRiP04/2S80KA2aElvugh8J8tBRMS5WArDgmQTzc8Dof4UCsf4FfABCU7TKhozA04vM5wGHh1 Prcw6MQ/ZvDqr5EDSi9XOPAdX00C1zWq3jvUpTz2QvVdSQEP2jDWHV4EW="/>
        </Security>
    </SOAP-ENV:Header>
    <SOAP-ENV:Body>
        <PRPA_IN201306UV02 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="urn:hl7-org:v3" xmlns:hl7="urn:hl7-org.v3" ITSVersion="XML_1.0" />
    </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
    '''
        when:
        OMElement orig = Util.parse_xml(msg)
        println orig.toString()


        def log = new OMFormatter(orig).toString()
        println log

        then:
        true
    }

    def 'test undeclared namespace prefix'() {
        /* Caused by: com.ctc.wstx.exc.WstxParsingException: Undeclared namespace prefix "wsu" (for attribute "Id") */
        given:
        def msg = '''
<SOAP-ENV:Envelope xmlns:SOAP-ENV='http://www.w3.org/2003/05/soap-envelope' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:s='http://www.w3.org/2001/XMLSchema' xmlns:wsa='http://www.w3.org/2005/08/addressing' xmlns:wsse='http://docs.oasis-open.org/wss/2004/01/o0asis-200401-wss-wssecurity-secext-1.0.xsd'>
    <SOAP-ENV:Header>
        <wsa:Action>urn:hl7-org:v3:PRPA_IN201306U0V02:CrossGatewayPatientDiscovery</wsa:Action>
        <wsa:MessagelD>urn:uuid:E2E5DD4A-2FFE-11E9-A926-005056A6P8F7</wsa:MessagelD>>
        <wsa:RelatesTo>e70ba0ff7-4075-4eca-bec8-444b7da25446</wsa:RelatesTo>
        <wsa:To>http://www.w3.org/2005/08/addressing/anonymous</wsa:To>
        <Security xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
            <Timestamp xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" wsu:Id="Timestamp-E2E552P8-2FFE-11E9-A926-O05056A6F8F7">
                <Created>2019-02-14T02:18 : 58Z</Created>
                <Expires>2019-02-14T02:23:58Z</Expires>
            </Timestamp>
            <SignatureConfirmation xmlns="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsqa" wsu:Id="Id-E2E5562C-2PPE-11E9-A926-O05056A6F8F7" Value="gOv6FWOOVUT6r3JtC2JbJ2kPmws/TPOtGwEnH6cZMpqPjc6/jBBGdY4vzveyjbLgtG92azhxDNVZojR0eX60Bwb1GVM/LdShhvwkahUBCrN9/bzTXLdoWwQX24y3pvykj+0Ae6w2yDYV5ACSvks1E+0M4Y1H2BDUEbg8+ELbvDhUjoErLemJaNGCPZ9LCXZ/1qBDR0itelSMOHCUnAc4rTsKRiP04/2S80KA2aElvugh8J8tBRMS5WArDgmQTzc8Dof4UCsf4FfABCU7TKhozA04vM5wGHh1 Prcw6MQ/ZvDqr5EDSi9XOPAdX00C1zWq3jvUpTz2QvVdSQEP2jDWHV4EW="/>
        </Security>
    </SOAP-ENV:Header>
    <SOAP-ENV:Body>
        <PRPA_IN201306UV02 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="urn:hl7-org:v3" xmlns:hl7="urn:hl7-org.v3" ITSVersion="XML_1.0" />
    </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
    '''
        and:
        OMElement o = Util.parse_xml(msg)
        when:
        println 'initial stage ' + new OMFormatter(msg).toString()
//        OMElement header = o.getChildrenWithLocalName("Header").next()
//        String headerString = header.toString() //.replace('xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"')
//        println 'orig header extract: ' + new OMFormatter(headerString).toString()
        /*
        OMElement headerCopy = Util.deep_copy(header)
        then:
        */
        then:
        def e = thrown(OMException)
        println e.cause
        e.cause.toString().contains('Undeclared namespace prefix')
//        println 'copied header: ' + Util.deep_copy(headerCopy).toString()

    }

    /*
                <Timestamp xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" wsu:Id="Timestamp-E2E552P8-2FFE-11E9-A926-O05056A6F8F7" xmlns:wsu="http://xxdocs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" xmlns:wsu='http://yydocs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd'>

 causes

org.apache.axiom.om.OMException: com.ctc.wstx.exc.WstxParsingException: Duplicate declaration for namespace prefix 'wsu'.
 at [row,col {unknown-source}]: [9,278]

	at org.apache.axiom.om.impl.builder.StAXOMBuilder.next(StAXOMBuilder.java:293)
	at org.apache.axiom.om.impl.llom.OMElementImpl.getNextOMSibling(OMElementImpl.java:337)
	at org.apache.axiom.om.impl.traverse.OMChildrenIterator.next(OMChildrenIterator.java:102)
	at org.apache.axiom.om.impl.llom.OMElementImpl.internalSerialize(OMElementImpl.java:952)
	at org.apache.axiom.om.impl.llom.OMSerializableImpl.serialize(OMSerializableImpl.java:125)
	at org.apache.axiom.om.impl.llom.OMSerializableImpl.serialize(OMSerializableImpl.java:113)
	at org.apache.axiom.om.impl.llom.OMElementImpl.toString(OMElementImpl.java:1088)
	at gov.nist.toolkit.testengine.om.OMLoggerTest.test original(OMLoggerTest.groovy:34)
Caused by: com.ctc.wstx.exc.WstxParsingException: Duplicate declaration for namespace prefix 'wsu'.
 at [row,col {unknown-source}]: [9,278]
	at com.ctc.wstx.sr.StreamScanner.constructWfcException(StreamScanner.java:630)
	at com.ctc.wstx.sr.StreamScanner.throwParseError(StreamScanner.java:461)
	at com.ctc.wstx.sr.BasicStreamReader.handleNsAttrs(BasicStreamReader.java:3022)
	at com.ctc.wstx.sr.BasicStreamReader.handleStartElem(BasicStreamReader.java:2936)
	at com.ctc.wstx.sr.BasicStreamReader.nextFromTree(BasicStreamReader.java:2848)
	at com.ctc.wstx.sr.BasicStreamReader.next(BasicStreamReader.java:1019)
	at org.apache.axiom.om.impl.builder.StAXOMBuilder.parserNext(StAXOMBuilder.java:672)
	at org.apache.axiom.om.impl.builder.StAXOMBuilder.next(StAXOMBuilder.java:214)
	... 7 more
     */
    def 'test duplication declaration exception' () {
       given:
        def msg = '''
<SOAP-ENV:Envelope xmlns:SOAP-ENV='http://www.w3.org/2003/05/soap-envelope' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:s='http://www.w3.org/2001/XMLSchema' xmlns:wsa='http://www.w3.org/2005/08/addressing' xmlns:wsse='http://docs.oasis-open.org/wss/2004/01/o0asis-200401-wss-wssecurity-secext-1.0.xsd' xmlns:wsu='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd'>
    <SOAP-ENV:Header>
        <wsa:Action>urn:hl7-org:v3:PRPA_IN201306U0V02:CrossGatewayPatientDiscovery</wsa:Action>
        <wsa:MessagelD>urn:uuid:E2E5DD4A-2FFE-11E9-A926-005056A6P8F7</wsa:MessagelD>>
        <wsa:RelatesTo>e70ba0ff7-4075-4eca-bec8-444b7da25446</wsa:RelatesTo>
        <wsa:To>http://www.w3.org/2005/08/addressing/anonymous</wsa:To>
        <Security xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
            <Timestamp xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" wsu:Id="Timestamp-E2E552P8-2FFE-11E9-A926-O05056A6F8F7" xmlns:wsu='http://xxxdocs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd' xmlns:wsu='http://yyydocs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd'>
                <Created>2019-02-14T02:18 : 58Z</Created>
                <Expires>2019-02-14T02:23:58Z</Expires>
            </Timestamp>
            <SignatureConfirmation xmlns="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsqa" wsu:Id="Id-E2E5562C-2PPE-11E9-A926-O05056A6F8F7" Value="gOv6FWOOVUT6r3JtC2JbJ2kPmws/TPOtGwEnH6cZMpqPjc6/jBBGdY4vzveyjbLgtG92azhxDNVZojR0eX60Bwb1GVM/LdShhvwkahUBCrN9/bzTXLdoWwQX24y3pvykj+0Ae6w2yDYV5ACSvks1E+0M4Y1H2BDUEbg8+ELbvDhUjoErLemJaNGCPZ9LCXZ/1qBDR0itelSMOHCUnAc4rTsKRiP04/2S80KA2aElvugh8J8tBRMS5WArDgmQTzc8Dof4UCsf4FfABCU7TKhozA04vM5wGHh1 Prcw6MQ/ZvDqr5EDSi9XOPAdX00C1zWq3jvUpTz2QvVdSQEP2jDWHV4EW="/>
        </Security>
    </SOAP-ENV:Header>
    <SOAP-ENV:Body>
        <PRPA_IN201306UV02 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="urn:hl7-org:v3" xmlns:hl7="urn:hl7-org.v3" ITSVersion="XML_1.0" />
    </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
    '''

        when:
        OMElement o = Util.parse_xml(msg)
        println 'initial stage ' + new OMFormatter(msg).toString()
        then:
        def e = thrown(OMException)
        println e.cause
        e.cause.toString().contains('Duplicate declaration for namespace prefix')

        /*
        OMElement header = o.getChildrenWithLocalName("Header").next()
        String headerString = header.toString() //.replace('xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"')
        println 'orig header extract: ' + new OMFormatter(headerString).toString()
        OMElement headerCopy = Util.deep_copy(header)
        println 'copied header: ' + Util.deep_copy(headerCopy).toString()

         */
    }
}
