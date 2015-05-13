/**
 * 
 */
package se.liu.imt.mi.snomedct.simpleserver;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class SimpleSnomedCTServerApplication extends Application {

	private static final Logger log = Logger
			.getLogger(SimpleSnomedCTServerApplication.class);
	private static Configuration config = null;

	public SimpleSnomedCTServerApplication(Context context) {
		super(context);
	}
	public static void main(String[] args) throws Exception {

		// initialize configuration
		try {
			config = new XMLConfiguration("config.xml");
			log.debug("Configuration in 'config.xml' loaded");
		} catch (Exception e) {
			log.debug("Exception", e);
			throw e;
		}

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.createOntology();

		// Create a new Restlet component and add a HTTP server connector to it
		Component component = new Component();
		component.getServers().add(Protocol.HTTP, config.getInt("server.port"));

		// Create a new context and attach it to the component
		Context sharedContext = component.getContext().createChildContext();
		sharedContext.getAttributes().put("ontology", ontology);

		// Then attach it to the local host
		component.getDefaultHost().attach(new SimpleSnomedCTServerApplication(sharedContext));

		// Now, let's start the component!
		// Note that the HTTP server connector is also automatically started.
		component.start();

	}

	@Override
	public Restlet createRoot() {
		Router router = new Router(getContext());
		router.attach("/expression", ExpressionResource.class);
		router.attach("/ontology", OntologyResource.class);
		router.attach("/inferredontology", InferredOntologyResource.class);


		return router;
	}

}
