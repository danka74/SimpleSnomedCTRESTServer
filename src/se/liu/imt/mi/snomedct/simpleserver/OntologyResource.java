package se.liu.imt.mi.snomedct.simpleserver;

import java.util.List;

import org.apache.log4j.Logger;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class OntologyResource extends ServerResource {

	private static Logger log = Logger.getLogger(OntologyResource.class);

	@Get()
	public String entire_ontology() {
		OWLOntology ontology = (OWLOntology) getContext().getAttributes().get(
				"ontology");

		OWLOntologyFormat ontologyFormat = null;

		List<Preference<MediaType>> formatList = getClientInfo()
				.getAcceptedMediaTypes();
		for (Preference<MediaType> pref : formatList) {
			String mediaType = pref.getMetadata().getName();
			if (mediaType.equals("text/turtle")) {
				ontologyFormat = new TurtleOntologyFormat();
				break;
			}
			else if (mediaType.equals("text/owl-functional")) {
				ontologyFormat = new OWLFunctionalSyntaxOntologyFormat();
				break; 
			}
			else if (mediaType.equals("text/owl-manchester")) {
				ontologyFormat = new ManchesterOWLSyntaxOntologyFormat();
				break;
			}
			else if (mediaType.equals("application/owl+xml")) {
				ontologyFormat = new OWLXMLOntologyFormat();
				break;
			}
		}
		if(ontologyFormat == null)
			ontologyFormat = new TurtleOntologyFormat();
		
		StringDocumentTarget tgt = new StringDocumentTarget();

		// save the ontology in the selected format
		try {
			ontology.getOWLOntologyManager().saveOntology(ontology,
					ontologyFormat, tgt);
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}

		return tgt.toString();
	}

}
