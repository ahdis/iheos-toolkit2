package gov.nist.toolkit.simProxy

import gov.nist.toolkit.fhir.simulators.proxy.util.BinaryPartSpec
import gov.nist.toolkit.fhir.simulators.proxy.util.MultipartParser2
import gov.nist.toolkit.fhir.simulators.proxy.util.PartSpec
import gov.nist.toolkit.fhir.simulators.proxy.util.SoapBuilder
import spock.lang.Specification

import java.nio.file.Paths

/**
 *
 */
class SoapBuilderTest extends Specification {

    def service = '/home/free'
    def host = 'localhost'
    def port = 'any'
    def action = 'now'
    def bodyXml = 'builder'

    def 'template test' () {
        when:
        def (hdr, body) = new SoapBuilder().simpleSoap(service, host, port, action, bodyXml)
        String header = hdr

        then:
        header.split('\n')[0].trim() == 'POST /home/free HTTP/1.1'
    }

    def 'mtom test' () {
        given:
        String referenceMsg = Paths.get(this.getClass().getResource('/').toURI()).resolve('sample_mtom_message.txt').toFile().text
        def part1 = correctCRLF(Paths.get(this.getClass().getResource('/').toURI()).resolve('sample_part_1.txt').toFile().text)   // start part
        def part2 = correctCRLF(Paths.get(this.getClass().getResource('/').toURI()).resolve('sample_part_2.txt').toFile().text)   // text attachment

        def part1Spec = new PartSpec(PartSpec.SOAPXOP, part1, '444')
        def part2Spec = new PartSpec(PartSpec.PLAINTEXT, part2, '555')
        SoapBuilder builder = new SoapBuilder()

        when:
        def (header, String body) = builder.mtomSoap(service, host, port, action, [part1Spec, part2Spec])

        and:
        List<BinaryPartSpec> bparts = MultipartParser2.parse(body)


        then:
        bparts[1].content == part2.bytes
    }

    def correctCRLF(String msg) {
        StringBuilder buf = new StringBuilder()

        String last = ''
        msg.each { String c ->
            if (last == '\r' && c != '\n') {
                buf.append(last)
                buf.append('\n')
            } else if (last != '')
                buf.append(last)
            last = c
        }
        if (last != '')
            buf.append(last)
        return buf.toString()
    }

}
