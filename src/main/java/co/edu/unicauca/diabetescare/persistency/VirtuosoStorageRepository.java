package co.edu.unicauca.diabetescare.persistency;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.xml.resolver.apps.resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestOperations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

@Repository
public class VirtuosoStorageRepository {
	
	@Autowired
	private RestOperations virtuosoRestTemplate;
	
	@Value("${diabetescare.virtuoso.homeurl}")
	private String virtuosoHomeUrl;
	
	@Value("${diabetescare.virtuoso.user}")
	private String virtuosoUser;
	
	@Value("${diabetescare.virtuoso.pass}")
	private String virtuosoPassword;
	
	@Value("${diabetescare.virtuoso.sparqlEndpoint}")
	private String virtuosoSparql;
	
	private static final Logger log = LoggerFactory.getLogger(VirtuosoStorageRepository.class);
	
	public void insertTriples(String triples)
	{
		String body="INSERT IN GRAPH <"+ virtuosoHomeUrl+"> { "+triples+"}";
		
		try {
			body=new String(body.getBytes("UTF-8"), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		log.debug("Sending query to virtuoso:"+body);
		
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Content-Type", "application/sparql-query");
		String basicCredentials = Base64.getEncoder().encodeToString ((virtuosoUser+":"+virtuosoPassword).getBytes());
		requestHeaders.set("Authorization", "Basic " + basicCredentials);
											
		HttpEntity<String> httpEntity = new HttpEntity<String>(body, requestHeaders);
		virtuosoRestTemplate.exchange(virtuosoHomeUrl+"new", HttpMethod.POST, httpEntity,String.class);
	}
	
	public ObjectNode select(String variablesToSelect, String whereTriples)
	{
		String selectQuery="select "+variablesToSelect+" FROM <"+virtuosoHomeUrl+"> where { "+whereTriples+" }";
		
		try {
			selectQuery=new String(selectQuery.getBytes("UTF-8"), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		HttpHeaders requestHeaders = new HttpHeaders();
		String bodyBoundary=UUID.randomUUID().getLeastSignificantBits()+"";
		requestHeaders.set("Content-Type", "multipart/form-data; boundary="+bodyBoundary);
		
		String body="--"+bodyBoundary+"\n"+
		"Content-Disposition: form-data; name=\"query\" \n\n"+
				selectQuery+"\n"+
				"--"+bodyBoundary+"--\n";
		
		HttpEntity<String> httpEntity = new HttpEntity<String>(body, requestHeaders);
		
		log.trace("Select query body:"+body);
		ResponseEntity<ObjectNode> result = virtuosoRestTemplate.exchange(virtuosoSparql, HttpMethod.POST,httpEntity, ObjectNode.class);
		log.trace("Virtuoso response:"+result);
		
		
		return result.getBody();
	}

	public Boolean ask(String triples) {
		String askQuery="ASK  FROM <"+virtuosoHomeUrl+"> { "+triples+" }";
		try {
			askQuery=new String(askQuery.getBytes("UTF-8"), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HttpHeaders requestHeaders = new HttpHeaders();
		String bodyBoundary=UUID.randomUUID().getLeastSignificantBits()+"";
		requestHeaders.set("Content-Type", "multipart/form-data; boundary="+bodyBoundary);
		
		String body="--"+bodyBoundary+"\n"+
		"Content-Disposition: form-data; name=\"query\" \n\n"+
				askQuery+"\n"+
				"--"+bodyBoundary+"--\n";
		
		HttpEntity<String> httpEntity = new HttpEntity<String>(body, requestHeaders);
		
		log.trace("Ask query body:"+body);
		ResponseEntity<String> result = virtuosoRestTemplate.exchange(virtuosoSparql, HttpMethod.POST,httpEntity, String.class);
		log.trace("Virtuoso response:"+result);
		if(result.getBody().contains("true"))
			return true;
		return false;
	}
	
	public InputStream getDomainOntologyInputStream()
	{
		String query="construct {?s ?p ?o } from <"+virtuosoHomeUrl+"> where { ?s ?p ?o }";
		HttpHeaders requestHeaders = new HttpHeaders();
		String bodyBoundary=UUID.randomUUID().getLeastSignificantBits()+"";
		requestHeaders.set("Content-Type", "multipart/form-data; boundary="+bodyBoundary);
		
		String body="--"+bodyBoundary+"\n"+
		"Content-Disposition: form-data; name=\"query\" \n\n"+
				query+"\n"+
				"--"+bodyBoundary+"\n"+
				"Content-Disposition: form-data; name=\"format\" \n\n"+
				"application/rdf+xml"+"\n"+
				"--"+bodyBoundary+"--\n";
		
		HttpEntity<String> httpEntity = new HttpEntity<String>(body, requestHeaders);
		
		log.trace("Get domain ontology query body:"+body);
		ResponseEntity<String> result = virtuosoRestTemplate.exchange(virtuosoSparql, HttpMethod.POST,httpEntity, String.class);
		log.trace("Virtuoso response:"+result.getBody());
		return new ByteArrayInputStream(result.getBody().getBytes(StandardCharsets.UTF_8));
	}

	public void deleteTriples(String triples) {
		String body="DELETE FROM GRAPH <"+ virtuosoHomeUrl+"> { "+triples+"}";
		
		try {
			body=new String(body.getBytes("UTF-8"), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		log.debug("Sending query to virtuoso:"+body);
		
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("Content-Type", "application/sparql-query");
		String basicCredentials = Base64.getEncoder().encodeToString ((virtuosoUser+":"+virtuosoPassword).getBytes());
		requestHeaders.set("Authorization", "Basic " + basicCredentials);
											
		HttpEntity<String> httpEntity = new HttpEntity<String>(body, requestHeaders);
		virtuosoRestTemplate.exchange(virtuosoHomeUrl+"new", HttpMethod.POST, httpEntity,String.class);
		
	}

	

}
