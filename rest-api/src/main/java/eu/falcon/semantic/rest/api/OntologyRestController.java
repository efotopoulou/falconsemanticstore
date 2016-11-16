package eu.falcon.semantic.rest.api;

import eu.falcon.semantic.rest.response.RestResponse;
import eu.falcon.semantic.rest.response.BasicResponseCode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contains all the rest endpoints regarding with authentication actions.
 */
@RestController
@RequestMapping("/api/v1/ontology")
public class OntologyRestController {

    @Value("${fuseki.triplestore}")
    private String triplestorURL;

    @Value("${fuseki.dataset}")
    private String triplestoreDataset;

    ///////////////////////////////////////////////
    //POST CALLS
    ///////////////////////////////////////////////
    @RequestMapping(value = "/query/run", method = RequestMethod.POST, headers = "Accept=application/xml, application/json")
    public RestResponse executeQueryToTriplestorePOST(@RequestBody String tobject) {

        String serviceURI = triplestorURL + "/" + triplestoreDataset + "/query";

        QueryExecution q = QueryExecutionFactory.sparqlService(serviceURI, tobject);
        ResultSet results = q.execSelect();
        //ResultSetFormatter.out(System.out, results);

        // write to a ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ResultSetFormatter.outputAsJSON(outputStream, results);

        // and turn that into a String
        String json = new String(outputStream.toByteArray());
        JSONObject jsonObject = new JSONObject(json);
        //System.out.println("Results as json" + json);
        //System.out.println("Results as string " + ResultSetFormatter.asText(results));

        q.close();

        return new RestResponse(BasicResponseCode.SUCCESS, Message.QUERY_EXECUTED, jsonObject.toString());
    }

    @RequestMapping(value = "/instances/publish", method = RequestMethod.POST, headers = "Accept=application/xml, application/json")
    public RestResponse insertInstancesToTriplestorePOST(@RequestParam("file") MultipartFile initialFile, @RequestParam("format") String format) {

        try {
            InputStream inputStream = initialFile.getInputStream();
            String serviceURI = triplestorURL + "/" + triplestoreDataset + "/data";

            DatasetAccessor accessor;
            accessor = DatasetAccessorFactory.createHTTP(serviceURI);
            Model m = ModelFactory.createDefaultModel();
            String base = "http://samle-project.com/";
            m.read(inputStream, base, format);
            accessor.add(m);
            inputStream.close();
            m.close();

        } catch (IOException ex) {
            Logger.getLogger(OntologyRestController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new RestResponse(BasicResponseCode.SUCCESS, Message.IMPORTED_INSTANCES, "imported instances" + initialFile.getName());
    }

    @RequestMapping(value = "/publish", method = RequestMethod.POST, headers = "Accept=application/xml, application/json")
    public RestResponse publishOntologyToTriplestorePOST(@RequestParam("file") MultipartFile initialFile, @RequestParam("format") String format) {

        try {

            InputStream inputStream = initialFile.getInputStream();
            String serviceURI = triplestorURL + "/" + triplestoreDataset + "/data";

            DatasetAccessor accessor;
            accessor = DatasetAccessorFactory.createHTTP(serviceURI);
            Model m = ModelFactory.createDefaultModel();
            String base = "http://samle-project.com/";
            m.read(inputStream, base, format);
            accessor.add(m);
            //accessor.putModel(m);
            inputStream.close();
            m.close();

        } catch (IOException ex) {
            Logger.getLogger(OntologyRestController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new RestResponse(BasicResponseCode.SUCCESS, Message.ONTOLOGY_CREATED, "A new Ontology is inserted" + initialFile.getName());
    }

    @RequestMapping(value = "/instance/attributes", method = RequestMethod.POST, headers = "Accept=application/xml, application/json")
    public RestResponse getInstanceAttributes(@RequestBody String instanceURI) {

//        String query = "select distinct  ?property  where {\n"
//                + "         ?instance a <" + instanceURI + "> . \n"
//                + "  ?instance ?property ?obj . }";
        
         String query = "select distinct  ?instanceproperties  where {\n" +
"        <"+instanceURI+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class  .\n" +
"        <"+instanceURI+"> ?instanceproperties ?obj .}";
        


        String serviceURI = triplestorURL + "/" + triplestoreDataset + "/query";

        QueryExecution q = QueryExecutionFactory.sparqlService(serviceURI, query);
        ResultSet results = q.execSelect();
        //ResultSetFormatter.out(System.out, results);

        // write to a ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ResultSetFormatter.outputAsJSON(outputStream, results);

        // and turn that into a String
        String json = new String(outputStream.toByteArray());

        q.close();

        return new RestResponse(BasicResponseCode.SUCCESS, Message.QUERY_EXECUTED, json);
    }

    @RequestMapping(value = "/class/subclasses", method = RequestMethod.POST, headers = "Accept=application/xml, application/json")
    public RestResponse getClassSubclasses(@RequestBody String classURI) {

        String query = "SELECT distinct ?subclasses WHERE {\n"
                + "  ?subclasses <http://www.w3.org/2000/01/rdf-schema#subClassOf> <" + classURI + ">\n"
                + "}";

        String serviceURI = triplestorURL + "/" + triplestoreDataset + "/query";

        QueryExecution q = QueryExecutionFactory.sparqlService(serviceURI, query);
        ResultSet results = q.execSelect();

        //ResultSetFormatter.out(System.out, results);

        // write to a ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ResultSetFormatter.outputAsJSON(outputStream, results);

        // and turn that into a String
        String json = new String(outputStream.toByteArray());

        q.close();

        return new RestResponse(BasicResponseCode.SUCCESS, Message.QUERY_EXECUTED, json);
    }

    @RequestMapping(value = "/class/attributes", method = RequestMethod.POST, headers = "Accept=application/xml, application/json")
    public RestResponse getClassAttributes(@RequestBody String classURI) {

        String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX owl:  <http://www.w3.org/2002/07/owl#>\n"
                + "\n"
                + "SELECT ?classproperties ?a\n"
                + "WHERE {\n"
                + "  values ?propertyType { owl:DatatypeProperty owl:ObjectProperty }\n"
                + "  ?classproperties a ?propertyType ;\n"
                + "            rdfs:domain <" + classURI + "> .\n"
                + "}";

        String serviceURI = triplestorURL + "/" + triplestoreDataset + "/query";

        QueryExecution q = QueryExecutionFactory.sparqlService(serviceURI, query);
        ResultSet results = q.execSelect();
        //ResultSetFormatter.out(System.out, results);

        // write to a ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ResultSetFormatter.outputAsJSON(outputStream, results);

        // and turn that into a String
        String json = new String(outputStream.toByteArray());

        q.close();

        return new RestResponse(BasicResponseCode.SUCCESS, Message.QUERY_EXECUTED, json);
    }

    ///////////////////////////////////////////////
    //GET CALLS
    ///////////////////////////////////////////////
    @RequestMapping(path = "/publish", method = RequestMethod.GET)
    public RestResponse insertOntologyToTriplestore() {
        File initialFile = null;
        try {
            InputStream inputStream;

            initialFile = new File("/home/eleni/Downloads/iot-energyldao.owl");
            inputStream = new FileInputStream(initialFile);

            //triplestore datasource
            String serviceURI = "http://192.168.3.15:3030/ds2/data";

            DatasetAccessor accessor;
            accessor = DatasetAccessorFactory.createHTTP(serviceURI);
            Model m = ModelFactory.createDefaultModel();
            String base = "http://samle-project.com/";
            m.read(inputStream, base, "RDF/XML");
            accessor.add(m);
            inputStream.close();

        } catch (IOException ex) {
            Logger.getLogger(OntologyRestController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new RestResponse(BasicResponseCode.SUCCESS, Message.ONTOLOGY_CREATED, "uploaded file name" + initialFile.getName());
    }

    @RequestMapping(path = "/instances/publish", method = RequestMethod.GET)
    public RestResponse insertInstancesToTriplestore() {
        File initialFile = null;
        try {
            InputStream inputStream;

            //inputStream = new ByteArrayInputStream(instances.getBytes(StandardCharsets.UTF_8));
            initialFile = new File("/home/eleni/Downloads/analyticsid128_version1_kmeans_resultdocumentnt.n3");
            inputStream = new FileInputStream(initialFile);

//triplestore datasource
            String serviceURI = "http://192.168.3.15:3030/ds2/data";

            DatasetAccessor accessor;
            accessor = DatasetAccessorFactory.createHTTP(serviceURI);
            Model m = ModelFactory.createDefaultModel();
            String base = "http://samle-project.com/";
            m.read(inputStream, base, "Turtle");
            accessor.add(m);
            inputStream.close();

        } catch (IOException ex) {
            Logger.getLogger(OntologyRestController.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new RestResponse(BasicResponseCode.SUCCESS, Message.IMPORTED_INSTANCES, "imported instances" + initialFile.getName());
    }

    @RequestMapping(path = "/query/run", method = RequestMethod.GET)
    public RestResponse executeQueryToTriplestore() {

        String serviceURI = "http://192.168.3.15:3030/ds2/query";

        String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "SELECT * WHERE {\n"
                + "  ?sub ?pred ?obj .\n"
                + "} \n"
                + "LIMIT 10";

        QueryExecution q = QueryExecutionFactory.sparqlService(serviceURI, query);
        ResultSet results = q.execSelect();

        ResultSetFormatter.out(System.out, results);

//        while (results.hasNext()) {
//            QuerySolution solution = results.nextSolution();
//            RDFNode averageEnergy = solution.get("AverageEnergy");
//            System.out.println(averageEnergy);
//        }
        return new RestResponse(BasicResponseCode.SUCCESS, null, "test");
    }

//    @RequestMapping(path = "/schema", method = RequestMethod.GET)
//    public RestResponse getInstanceAttributes() {
//        
//    
//
//        return new RestResponse(BasicResponseCode.SUCCESS, null, "test");
//    }
    /**
     * Inner class containing all the static messages which will be used in an
     * RestResponse.
     *
     */
    private final static class Message {

        final static String ONTOLOGY_CREATED = "Ontology has been created";
        final static String IMPORTED_INSTANCES = "New Instances have been imported";
        final static String QUERY_EXECUTED = "QUERY is succesfully executed";
    }
}
