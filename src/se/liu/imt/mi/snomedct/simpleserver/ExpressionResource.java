package se.liu.imt.mi.snomedct.simpleserver;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

import org.apache.log4j.Logger;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import se.liu.imt.mi.snomedct.expression.tools.ExpressionSyntaxError;
import se.liu.imt.mi.snomedct.expression.tools.SNOMEDCTParserUtil;

public class ExpressionResource extends ServerResource {
	
	private static Logger log = Logger.getLogger(ExpressionResource.class);

	@Post
	public String add_expression() {
		
		OWLOntology ontology = (OWLOntology) getContext().getAttributes().get("ontology");
		
		String expression = null;
		try {
			expression = URLDecoder.decode((String) getRequestAttributes().get("expression"), "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			;
		}
		
		OWLAxiom axiom = null;
		try {
			axiom = SNOMEDCTParserUtil.parseExpressionToOWLAxiom(expression, ontology);
		} catch (ExpressionSyntaxError e) {
			return e.toString();
		}
	
		IRI iri = null;
		if(axiom.getAxiomType() == AxiomType.SUBCLASS_OF) {
			iri = ((OWLSubClassOfAxiom)axiom).getSuperClass().asOWLClass().getIRI();
		} else if(axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
			iri = ((OWLEquivalentClassesAxiom)axiom).getClassExpressionsAsList().get(1).asOWLClass().getIRI();
		}
		return iri.toString();
	}
	
	@Get
	public String entire_ontology() {
		OWLOntology ontology = (OWLOntology) getContext().getAttributes().get("ontology");

		OWLOntologyFormat ontologyFormat = new TurtleOntologyFormat();
		
		StringDocumentTarget tgt = new StringDocumentTarget();
		
		// save the ontology in the selected format
		try {
			ontology.getOWLOntologyManager().saveOntology(ontology, ontologyFormat, tgt);
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return tgt.toString();
	}

}
