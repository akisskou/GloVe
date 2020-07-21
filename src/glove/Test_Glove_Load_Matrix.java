package glove;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.jblas.DoubleMatrix;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import glove.objects.Vocabulary;
import glove.utils.Methods;
import glove.utils.Options;

/**
 * Load Ontology Terms and find 10 closest terms (for testing purposes)
 * @author jason
 *
 */
public class Test_Glove_Load_Matrix {

	private static Logger log = LoggerFactory.getLogger(Test_Glove_Load_Matrix.class);
	private static OWLOntology ontology;
	private static OWLOntologyManager manager;
	private static IRI documentIRI;
	private static List<myOWLClass> allClasses;
	
	private static String stopwords1 = "<=,>=,_,=,<,>,+,%, -,- , - ,—,•,…,/,#,$,&,*,\\,^,{,},~,£,§,®,°,±,³,·,½,™";

	private static String stopwords2 = "i,me,my,myself,we,our,ours,ourselves,you,your,yours,yourself,yourselves,he,him,his,himself,she,her,hers,herself,it,its,itself,they,them,their,theirs,themselves,what,which,who,whom,never,this,that,these,those,am,is,are,was,were,be,been,being,have,has,had,having,do,does,did,doing,a,an,the,and,but,if,kung,or,because,as,until,while,of,at,by,for,with,about,against,between,into,through,during,before,after,above,below,to,from,up,down,in,out,on,off,over,under,again,further,then,once,here,there,when,where,why,how,long,all,any,both,each,few,more,delivering,most,other,some,such,no,nor,not,only,own,same,so,than,too,cry,very,s,t,can,lite,will,just,don,should,now";
	
	@SuppressWarnings("deprecation")
	private static void findClasses(){
		Set<OWLClass> ontClasses = new HashSet<OWLClass>(); 
        ontClasses = ontology.getClassesInSignature();
        allClasses = new ArrayList<myOWLClass>();
        
        for (Iterator<OWLClass> it = ontClasses.iterator(); it.hasNext(); ) {
        	myOWLClass f = new myOWLClass();
        	f.name = it.next();
        	f.id = f.name.getIRI().getFragment();
        	for(OWLAnnotationAssertionAxiom a : ontology.getAnnotationAssertionAxioms(f.name.getIRI())) {
        		if(a.getProperty().isLabel()) {
                    if(a.getValue() instanceof OWLLiteral) {
                        OWLLiteral val = (OWLLiteral) a.getValue();
                        f.label = val.getLiteral();
                    }
                }
        		
            }
        	allClasses.add(f);
        }
	}
	
	@SuppressWarnings("deprecation")
	private static void findSubclasses(){
		for (final org.semanticweb.owlapi.model.OWLSubClassOfAxiom subClasse : ontology.getAxioms(AxiomType.SUBCLASS_OF))
        {
			OWLClassExpression sup = subClasse.getSuperClass();
        	OWLClass sub = (OWLClass) subClasse.getSubClass();
        	
            if (sup instanceof OWLClass && sub instanceof OWLClass)
            {
            	int i;
            	for(i=0; i<allClasses.size(); i++){
            		if (sup.equals(allClasses.get(i).name)) break;
            	}
            	int j;
            	for(j=0; j<allClasses.size(); j++){
            		if (sub.equals(allClasses.get(j).name)){
            			allClasses.get(i).subClasses.add(allClasses.get(j));
            			allClasses.get(j).isSubClass = true;
            			break;
            		}
            	}
            }
        }
	}
	
	public static String getTermsWithNarrowMeaning(String myTerm) {
    	String narrowTerms = "";
    	for(int i=0; i<allClasses.size(); i++){
			if(myTerm.equals(allClasses.get(i).id)){
				try{
					narrowTerms = getSubKeywords(narrowTerms, allClasses.get(i));
				}
				catch (Exception e) {
		   			System.out.println(e);
		   		}
				
				break;	
			}
		}	
    	return narrowTerms;
    }
	
	private static String getSubKeywords(String keywords, myOWLClass checked){
		if(keywords.equals("")) keywords += checked.label;			
		else keywords += ","+checked.label;
		for(int i=0; i<checked.subClasses.size(); i++){
			keywords = getSubKeywords(keywords, checked.subClasses.get(i));
		}
		return keywords;
	}
	
	public static void main(String[] args) throws IOException {
		InputStream input = new FileInputStream("infos.properties");
		Properties prop = new Properties();
		// load a properties file
		prop.load(input);
		manager = OWLManager.createOWLOntologyManager();
		//documentIRI = IRI.create(prop.getProperty("pathToOWLFile"));
		documentIRI = IRI.create("file:///", prop.getProperty("owlFile"));
		try{
	        ontology = manager.loadOntologyFromOntologyDocument(documentIRI);
            findClasses();
            findSubclasses();
		}
		catch (OWLOntologyCreationException e) {
	        e.printStackTrace();
		}
		
		String file = "raw_text.txt";
        
        Options options = new Options(); 
        options.debug = true;
        
        Vocabulary vocab = GloVe.build_vocabulary(file, options);
        //vocab.addOrUpdate("rheumatoid arthritis");
        
        String[] targetWords = prop.getProperty("targetWords").split(",");
        String[] sw1 = stopwords1.split(",");	
		String[] sw2 = stopwords2.split(",");
        String[] tempTerms = new String[targetWords.length];
		for(int j=0; j<targetWords.length; j++) {
			tempTerms[j] = getTermsWithNarrowMeaning(targetWords[j].trim()).toLowerCase();
			String[] narrowTerms = tempTerms[j].split(",");
			for(int k=0; k<narrowTerms.length; k++) {
				String myWord = "";
				for(int i=0; i<sw1.length; i++) myWord = narrowTerms[k].replace(sw1[i], " ");
				for(int i=0; i<sw2.length; i++) myWord = myWord.replace(" "+sw2[i]+" ", " ");
				myWord = myWord.trim().replaceAll(" +", " ");
				vocab.addOrUpdate(myWord);
			}
		}
        
        file = "test_vectors_matrix.txt";
		log.info("Loading word vectors from GloVe test txt file....");
		File myObj = new File(file);
	    Scanner myReader = new Scanner(myObj);
	    String data = "";
	    while (myReader.hasNextLine()) {
	        data += myReader.nextLine();
	    }
	    myReader.close();
	    DoubleMatrix W = DoubleMatrix.valueOf(data);
	    /*String[] row = data.split("; ");
	    String[] column = row[0].split(", ");
	    double[][] temp = new double[row.length][column.length];
	    for(int j=0; j<column.length; j++) {
    		temp[0][j] = Double.parseDouble(column[j]);
    	}
	    for(int i=1; i<row.length; i++) {
	    	column = row[i].split(", ");
	    	for(int j=0; j<column.length; j++) {
	    		temp[i][j] = Double.parseDouble(column[j]);
	    	}
	    }
	    DoubleMatrix W = new DoubleMatrix(temp);*/
	    PrintWriter out = new PrintWriter("reference model closest words GloVe.txt");
	    data = "";
	    for(int j=0; j<targetWords.length; j++) {
	    	String[] narrowTerms = tempTerms[j].split(",");
			for(int k=0; k<narrowTerms.length; k++) {
				log.info("Replacing stop words...");
				System.out.println(narrowTerms[k]);
				String myWord = "";
				for(int i=0; i<sw1.length; i++) myWord = narrowTerms[k].replace(sw1[i], " ");
				for(int i=0; i<sw2.length; i++) myWord = myWord.replace(" "+sw2[i]+" ", " ");
				myWord = myWord.trim().replaceAll(" +", " ");
				String[] narrowTermWords = myWord.split(" ");
				Collection<String> wordList = null;
				boolean flag=false;
				for(int i=0; i<narrowTermWords.length; i++) {
					try {
						if(!flag) wordList = Methods.most_similar(W, vocab, narrowTermWords[i], Integer.valueOf(prop.getProperty("count")));
						else {
							Collection<String> myList = Methods.most_similar(W, vocab, narrowTermWords[i], Integer.valueOf(prop.getProperty("count")));
							Iterable<String> combinedIterables = Iterables.unmodifiableIterable(
									Iterables.concat(wordList, myList));
							wordList = Lists.newArrayList(combinedIterables);
						}
						flag=true;
					}
					catch(Exception e) {
						continue;
					}
				}
				if(flag) {
					Set<String> set = new HashSet<>(wordList);
					wordList.clear();
					wordList.addAll(set);
					while (wordList.remove(""));
					log.info("Words closest to '"+narrowTerms[k]+"': {}", wordList);
				       
			        data += narrowTerms[k]+": "+wordList.toString()+"\n";
				}
				else {
					log.info(narrowTerms[k]+" has no close words");
				       
			        data += narrowTerms[k]+" has no close words\n";
				}
				
				
			}
		}
	   /* PrintWriter out = new PrintWriter("reference model closest words GloVe-1.txt");
	    data = "";
	    for(int j=0; j<targetWords.length; j++) {
			String[] narrowTerms = tempTerms[j].split(",");
			for(int k=0; k<narrowTerms.length; k++) {
				String myWord = "";
				for(int i=0; i<sw1.length; i++) myWord = narrowTerms[k].replace(sw1[i], " ");
				for(int i=0; i<sw2.length; i++) myWord = myWord.replace(" "+sw2[i]+" ", " ");
				myWord = myWord.trim().replaceAll(" +", " ");
				Collection<String> wordList = Methods.most_similar(W, vocab, myWord, Integer.valueOf(prop.getProperty("count")));
				Set<String> set = new HashSet<>(wordList);
				wordList.clear();
				wordList.addAll(set);
				while (wordList.remove(""));
				log.info("Words closest to '"+narrowTerms[k]+"': {}", wordList);
			       
		        data += narrowTerms[k]+": "+wordList.toString()+"\n";
			}
		}*/
	    
	    out.println(data);
		out.close();
	}
}
