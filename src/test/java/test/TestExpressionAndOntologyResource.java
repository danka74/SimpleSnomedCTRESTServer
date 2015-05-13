/**
 * 
 */
package test;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import se.liu.imt.mi.snomedct.simpleserver.SimpleSnomedCTServerApplication;


/**
 * @author Daniel Karlsson, daniel.karlsson@liu.se
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestExpressionAndOntologyResource {
	
	private static final Logger log = Logger
			.getLogger(SimpleSnomedCTServerApplication.class);
	private static Configuration config = null;
	
	private static String iri = null;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		// initialize configuration
		try {
			config = new XMLConfiguration("config.xml");
			log.debug("Configuration in 'config.xml' loaded");
		} catch (Exception e) {
			log.debug("Exception", e);
			throw e;
		}
		
		SimpleSnomedCTServerApplication.main(new String[] {});
		
		log.info("Started server");
	}

	@Test
	public void test1AddExpression() throws IOException {
		String url = "http://localhost:" + config.getString("server.port", "8184") +  "/expression";
		ClientResource clientResource = new ClientResource(url);
		Request request = new Request(Method.POST, url);
		Form form = new Form();
		form.set("expression", "125605004|fracture of bone|:{363698007|finding site|=71341001|bone structure of femur|}");
		
		Representation representation = clientResource.post(form);
		
		iri = representation.getText();
		
		assertTrue(iri.startsWith("http://snomed.org/postcoord/"));
	}
	
	@Test
	public void test2DeleteExpression() throws IOException {
		String url = "http://localhost:" + config.getString("server.port", "8184") +  "/expression?"+iri;
		ClientResource clientResource = new ClientResource(url);

		Representation representation = clientResource.delete();
		
		String result = representation.getText();
		
		assertTrue(result.startsWith("http://snomed.org/postcoord/"));
	}
	
	@Test
	public void test3DeleteExpression() throws IOException {
		String url = "http://localhost:" + config.getString("server.port", "8184") +  "/expression?foo";
		ClientResource clientResource = new ClientResource(url);

		
		try {
			clientResource.delete();
		} catch (ResourceException e) {
			;
		}
		Status status = clientResource.getStatus();
		
		assertTrue(status.equals(Status.CLIENT_ERROR_NOT_FOUND));
	}
	
	@Test
	public void test4GetEntireOntology() throws IOException {
		String url = "http://localhost:" + config.getString("server.port", "8184") +  "/ontology";
		ClientResource clientResource = new ClientResource(url);

		Representation representation = clientResource.get(new MediaType("text/owl-manchester"));
		
		long size = representation.getSize();
		
		assertTrue(size == 339);
	}

}
