package se.liu.imt.mi.snomedct.simpleserver;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;

import se.liu.imt.mi.snomedct.expression.tools.DistributionNormalFormConverter;

public class InferredOntologyResource extends ServerResource {

	private static Logger log = Logger.getLogger(InferredOntologyResource.class);

	@Get()
	public String entire_ontology() {
		OWLOntology inferredOntology = (OWLOntology) getContext().getAttributes().get(
				"inferredOntology");
		
		if(inferredOntology == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return "";
		}

		OWLOntologyFormat ontologyFormat = null;

		List<Preference<MediaType>> formatList = getClientInfo()
				.getAcceptedMediaTypes();
		for (Preference<MediaType> pref : formatList) {
			String mediaType = pref.getMetadata().getName();
			if (mediaType.equals("text/turtle")) {
				ontologyFormat = new TurtleOntologyFormat();
				break;
			} else if (mediaType.equals("text/owl-functional")) {
				ontologyFormat = new OWLFunctionalSyntaxOntologyFormat();
				break;
			} else if (mediaType.equals("text/owl-manchester")) {
				ontologyFormat = new ManchesterOWLSyntaxOntologyFormat();
				break;
			} else if (mediaType.equals("application/owl+xml")) {
				ontologyFormat = new OWLXMLOntologyFormat();
				break;
			}
		}
		if (ontologyFormat == null)
			ontologyFormat = new TurtleOntologyFormat();

		StringDocumentTarget tgt = new StringDocumentTarget();

		// save the ontology in the selected format
		try {
			inferredOntology.getOWLOntologyManager().saveOntology(inferredOntology,
					ontologyFormat, tgt);
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}

		return tgt.toString();
	}

}
