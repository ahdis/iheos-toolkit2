package gov.nist.toolkit.fhir.server.resourceProvider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.primitive.IdDt
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.method.RequestDetails
import ca.uhn.fhir.rest.server.IResourceProvider
import ca.uhn.fhir.rest.server.IRestfulServerDefaults
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException
import gov.nist.toolkit.fhir.context.ToolkitFhirContext
import gov.nist.toolkit.fhir.search.SearchByTypeAndId
import gov.nist.toolkit.fhir.server.servlet.Attributes
import gov.nist.toolkit.fhir.server.servlet.HttpRequestParser
import gov.nist.toolkit.fhir.support.ResourceIndex
import gov.nist.toolkit.fhir.support.SimContext
import gov.nist.toolkit.fhir.support.SimIndexManager
import gov.nist.toolkit.simcommon.client.SimId
import gov.nist.toolkit.simcommon.server.SimDb
import gov.nist.toolkit.utilities.id.UuidAllocator
import java.util.logging.*
import org.hl7.fhir.dstu3.model.DomainResource
import org.hl7.fhir.dstu3.model.IdType

import java.nio.charset.Charset

/**
 * Collection of utilities for linking HAPI ResourceProvider
 * classes to toolkit ResDb funtions.
 */
class ToolkitResourceProvider {
    static Logger logger = Logger.getLogger(ToolkitResourceProvider.class.getName());
    FhirContext fhirContext  // expensive to create so it is built once
    Class<?> resourceType
    RequestDetails requestDetails
    SimContext simContext

    ToolkitResourceProvider(Class<?> resourceType, RequestDetails requestDetails) {
        this.resourceType = resourceType
        this.requestDetails = requestDetails

        // get singleton copy - expensive to build
        fhirContext = ToolkitFhirContext.get()

        // linkage to ResDb simulator environment
        Attributes a = new Attributes(requestDetails)
        SimDb simDb = a.simDb
        assert simDb, 'SimDb not set by logging interceptor'
        simContext = new SimContext(simDb)
    }

    static MethodOutcome create(DomainResource resource, SimId simId) {
        IResourceProvider provider = findProvider(resource)
        def requestDetails = getRequestDetails()
        Attributes attributes = new Attributes(requestDetails)
        SimDb simDb = new SimDb(simId, SimDb.BASE_TYPE, SimDb.ANY_TRANSACTION, false)
        attributes.setSimId(simId)
        attributes.setSimDb(simDb)
        return provider.create(resource, requestDetails)
    }

    static IToolkitResourceProvider findProvider(DomainResource resource) {
        def type = resource.getClass().getSimpleName()
        def providerClassName = "gov.nist.toolkit.fhir.server.resourceProvider.${type}ResourceProvider"
        Class clas = providerClassName as Class
        return clas.newInstance()
    }

    static RequestDetails getRequestDetails() {
        new RequestDetails() {
            @Override
            protected byte[] getByteStreamRequestContents() {
                return new byte[0]
            }

            @Override
            Charset getCharset() {
                return null
            }

            @Override
            String getHeader(String s) {
                return null
            }

            @Override
            List<String> getHeaders(String s) {
                return null
            }
            @Override
            InputStream getInputStream() throws IOException {
                return null
            }

            @Override
            Reader getReader() throws IOException {
                return null
            }

            @Override
            IRestfulServerDefaults getServer() {
                return null
            }

            @Override
            String getServerBaseForRequest() {
                return null
            }
        }
    }

    String resourceTypeAsString() {
        getResourceType().simpleName
    }

    /**
     * simId is encoded in URL
     * @return
     */
//    SimId getSimId() { HttpRequestParser.simIdFromRequest(request) }
    SimId getSimId() { new Attributes(requestDetails).simId }
    SimDb getSimDb() { new Attributes(requestDetails).simDb }

    /**
     * this implements most of the requirements for the CREATE
     * operation on a Resource.  Before calling this the
     * Resource should be validated.
     * @param theResource
     * @return
     */
    MethodOutcome createOperation(DomainResource theResource) {
        // store the resource
        IdDt newId = addResource(theResource)

        // flush and close the index
        flushIndex()

        // Let the caller know the ID of the newly created resource
        return new MethodOutcome(newId);
    }

    /**
     * this is most of the read operation for any Resource.
     * @param theId of the Resource
     * @return File containing the Resource
     */
    File readOperation(IdType theId) {
        String id = theId.getIdPart();

        List<String> paths = new SearchByTypeAndId(simContext).run(resourceTypeAsString(), id);

        if (paths.size() == 0)
            return null;

        if (paths.size() > 1)
            throw new InternalErrorException("Multiple results found");

        return new File(paths.get(0));
    }

    /**
     * add a resource instance to the store. Allocate a resource id.  We use UUIDs
     * because we don't yet have a way to allocate sequential integers.  UUIDs are
     * legal but unconventional in FHIR.
     *
     * @param theResource - resource to be stored
     * @return - id assigned to resource
     */
    IdDt addResource(DomainResource theResource) {
        String resourceType = theResource.getResourceType().name()

        // Generate the id
        String id = UuidAllocator.allocateNaked()
        IdDt idDt = new IdDt(resourceType, id, "1")
        theResource.setId(idDt)

        if (!simContext) simContext = new SimContext(simDb)

        // save resource
        File resourceFile = simContext.store(resourceType, theResource, id)

        // add it to the index
        ResourceIndex resourceIndex = simContext.index(resourceType, theResource, id)
        resourceIndex.path = resourceFile.path

        return idDt
    }

    /**
     * addResource builds index in memory.  This gets called
     * once to flush index to disk.
     * @return
     */
    def flushIndex() {
        simContext.flushIndex()
    }

    /**
     * display Lucene index
     * @return
     */
    def displayIndex() {
        SimIndexManager.getIndexer(HttpRequestParser.simIdFromRequest(requestDetails)).dump();
    }

    IParser getJsonParser() {
        fhirContext.newJsonParser()
    }

    /**
     * Lucene query returns list of paths for matching resources.
     * HAPI search method requires return of List resources from
     * search methods.  This translates.
     *
     * The linkage between IBaseResource and DomainResource is
     * complicated.  So instead of having conflicts show up in
     * every ResourceProvider, I munge it here.
     *
     * @param paths - list of ResDb paths to matching resources
     * @return - list of resource bodies
     */
    List<DomainResource> searchResults(List<String> paths) {
        List<DomainResource> items

        items = paths.collect { String path ->
            File f = new File(path)
            FileReader fr = null
            try {
                fr = new FileReader(f)
                (DomainResource) jsonParser.parseResource(resourceType, fr)
            } finally {
                if (fr)
                    fr.close()
            }
        }

        return (List<DomainResource>) items
    }
}
