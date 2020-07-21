package glove;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLClass;

public class myOWLClass {
	OWLClass name;
	String id;
	String label;
	List<myOWLClass> subClasses = new ArrayList<myOWLClass>();
	boolean isSubClass = false;
}

