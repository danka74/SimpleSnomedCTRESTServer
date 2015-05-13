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
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
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
			ontology.getOWLOntologyManager().saveOntology(ontology,
					ontologyFormat, tgt);
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}

		return tgt.toString();
	}

	// TODO: To PUT or POST?
	// http://roy.gbiv.com/untangled/2009/it-is-okay-to-use-post
	// TODO: determine requirements
	
	/**
	 * PUT, will classify ontology and add inferred axioms to the inferred ontology
	 */
	@Put
	public void classify_ontology() {
		OWLOntology ontology = (OWLOntology) getContext().getAttributes().get(
				"ontology");
		OWLOntology inferredOntology = (OWLOntology) getContext()
				.getAttributes().get("inferredOntology");

		/*
		if (inferredOntology != null)
			inferredOntology.getOWLOntologyManager().removeOntology(
					inferredOntology);
		OWLOntologyManager inferredManager = OWLManager
				.createOWLOntologyManager();
		try {
			inferredOntology = inferredManager.createOntology();
		} catch (OWLOntologyCreationException e) {
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return;
		}
		*/
		OWLOntologyManager inferredManager;
		if(inferredOntology == null) {
			inferredManager = OWLManager
					.createOWLOntologyManager();
			try {
				inferredOntology = inferredManager.createOntology();
			} catch (OWLOntologyCreationException e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				return;
			}
		} else {
			inferredManager = inferredOntology.getOWLOntologyManager();
		}
			
		// Create reasoner and classify the ontology including SNOMED CT
		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
		reasoner.flush();
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

		List<InferredAxiomGenerator<? extends OWLAxiom>> gens = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
		gens.add(new InferredSubClassAxiomGenerator());
		gens.add(new InferredEquivalentClassAxiomGenerator());
		InferredOntologyGenerator generator = new InferredOntologyGenerator(
				reasoner, gens);

		try {
			generator.fillOntology(inferredManager, inferredOntology);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();

		for (OWLAxiom axiom : ontology.getAxioms()) {
			changes.add(new AddAxiom(inferredOntology, axiom));
		}

		for (OWLAnnotation annot : ontology.getAnnotations()) {
			changes.add(new AddOntologyAnnotation(inferredOntology, annot));
		}

		inferredOntology.getOWLOntologyManager().applyChanges(changes);

		getContext().getAttributes().put("inferredOntology", inferredOntology);
	}

}
