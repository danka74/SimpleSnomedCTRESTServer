package se.liu.imt.mi.snomedct.simpleserver;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;

import org.apache.log4j.Logger;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import se.liu.imt.mi.snomedct.expression.tools.ExpressionSyntaxError;
import se.liu.imt.mi.snomedct.expression.tools.SNOMEDCTParserUtil;

public class ExpressionResource extends ServerResource {

	private static Logger log = Logger.getLogger(ExpressionResource.class);

	@Post()
	public String add_expression(Representation entity) {

		OWLOntology ontology = (OWLOntology) getContext().getAttributes().get(
				"ontology");
		
		Form form = new Form(entity); 
		String expression = form.getFirstValue("expression");

		OWLAxiom axiom = null;
		try {
			axiom = SNOMEDCTParserUtil.parseExpressionToOWLAxiom(expression,
					ontology);
		} catch (ExpressionSyntaxError e) {
			return e.toString();
		}

		IRI iri = null;
		if (axiom.getAxiomType() == AxiomType.SUBCLASS_OF) {
			iri = ((OWLSubClassOfAxiom) axiom).getSuperClass().asOWLClass()
					.getIRI();
		} else if (axiom.getAxiomType() == AxiomType.EQUIVALENT_CLASSES) {
			List<OWLClassExpression> classExpressionList = ((OWLEquivalentClassesAxiom) axiom)
					.getClassExpressionsAsList();
			for(OWLClassExpression classExpression : classExpressionList) {
				if(classExpression.getClassExpressionType() == ClassExpressionType.OWL_CLASS)
					iri = classExpression.asOWLClass().getIRI();
			}
		}
		return iri.toString();
	}

	@Delete()
	public String delete_expression(Representation entity) {

		OWLOntology ontology = (OWLOntology) getContext().getAttributes().get(
				"ontology");
		OWLOntologyManager manager = ontology.getOWLOntologyManager();

		Form form = new Form(entity); 
		String expression = form.getFirstValue("iri");

		IRI iri = IRI.create(expression);
		OWLClass owlClass = manager.getOWLDataFactory().getOWLClass(iri);

		for(OWLAxiom axiom : ontology.getAxioms()) {
			if(axiom.containsEntityInSignature(owlClass)) {
				ontology.getOWLOntologyManager().removeAxiom(ontology, axiom);
			}
		}

		return expression;
	}

}
