package glove;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.jblas.DoubleMatrix;
import org.nd4j.shade.guava.collect.Iterables;
import org.nd4j.shade.guava.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import glove.objects.Vocabulary;
import glove.utils.Methods;
import glove.utils.Options;

/**
 * Based on the given TXT criterion --> For each term find closest terms --> Search for them in the TXT with criteria closest terms --> Find patient IDs and save in file
 * @author jason
 *
 */
public class GloVeLoadCriterionVectors {

private static Logger log = LoggerFactory.getLogger(GloVeLoadCriterionVectors.class);
	
	private static String stopwords1 = "<=,>=,_,=,<,>,+,%, -,- , - ,—,•,…,/,#,$,&,*,\\,^,{,},~,£,§,®,°,±,³,·,½,™";

	private static String stopwords2 = "i,me,my,myself,we,our,ours,ourselves,you,your,yours,yourself,yourselves,he,him,his,himself,she,her,hers,herself,it,its,itself,they,them,their,theirs,themselves,what,which,who,whom,never,this,that,these,those,am,is,are,was,were,be,been,being,have,has,had,having,do,does,did,doing,a,an,the,and,but,if,kung,or,because,as,until,while,of,at,by,for,with,about,against,between,into,through,during,before,after,above,below,to,from,up,down,in,out,on,off,over,under,again,further,then,once,here,there,when,where,why,how,long,all,any,both,each,few,more,delivering,most,other,some,such,no,nor,not,only,own,same,so,than,too,cry,very,s,t,can,lite,will,just,don,should,now";

	public static void main(String[] args) throws Exception {
		InputStream input = new FileInputStream("infos.properties");
		Properties prop = new Properties();
		// load a properties file
		prop.load(input);
		String criterionData = prop.getProperty("criteria");
		String[] sw1 = stopwords1.split(",");	
		String[] sw2 = stopwords2.split(",");
		String editData = criterionData;
		for(int j=0; j<sw1.length; j++) editData = editData.replace(sw1[j], " ");
		for(int j=0; j<sw2.length; j++) editData = editData.replace(" "+sw2[j]+" ", " ");
		String[] criterionWords = editData.trim().replace(" +", " ").toLowerCase().split(" ");
		List<String> myWords = new ArrayList<String>();
	    for(int j=0; j<criterionWords.length; j++) {
	    	myWords.add(criterionWords[j]);
	    }
	    Set<String> set = new HashSet<>(myWords);
	    myWords.clear();
	    myWords.addAll(set);
		List<String> wordList = null;
		String file = "raw_text.txt";
        
        Options options = new Options(); 
        options.debug = true;
        
        Vocabulary vocab = GloVe.build_vocabulary(file, options);
        
        file = "vectors_matrix.txt";
		log.info("Loading word vectors from GloVe txt file....");
		File myObj = new File(file);
	    Scanner myReader = new Scanner(myObj);
	    String data = "";
	    while (myReader.hasNextLine()) {
	        data += myReader.nextLine();
	    }
	    myReader.close();
	    DoubleMatrix W = DoubleMatrix.valueOf(data);
	    boolean flag = false;
	    File criteriaDir = new File("chdb0"+prop.getProperty("dbid")+"/Criteria");
		criteriaDir.mkdir();
		for(int j=0; j<myWords.size(); j++) {
			try {
				if(j==0) wordList = Methods.most_similar(W, vocab, myWords.get(j), Integer.valueOf(prop.getProperty("count")));
				else {
					List<String> myList = Methods.most_similar(W, vocab, myWords.get(j), Integer.valueOf(prop.getProperty("count")));
					Iterable<String> combinedIterables = Iterables.unmodifiableIterable(
							Iterables.concat(wordList, myList));
					wordList = Lists.newArrayList(combinedIterables);
				}
				while (wordList.remove(""));
				flag=true;
			}
			catch(Exception e) {
				continue;
			}
		}
		if(flag) {
			Set<String> set1 = new HashSet<>(wordList);
			wordList.clear();
			wordList.addAll(set1);
			Collections.sort(wordList);
			log.info("Words closest to '"+prop.getProperty("criteria")+"': {}", wordList);
			File closeDir = new File("chdb0"+prop.getProperty("dbid")+"/Closest_words");
			File[] files = closeDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isFile();
				}
			});
			criterionData += ", Patient IDs: ";
			for(int i=0; i<files.length; i++) {
				//File myObj = new File("chdb0"+prop.getProperty("dbid")+"/Closest_words/"+files[i].getName());
			    Scanner myReader1 = new Scanner(files[i],"utf-8");
			    String data1 = "";
			    while (myReader1.hasNextLine()) {
			        data1 = myReader1.nextLine();
			        if(!data1.contains(":")) break;
			        else{
			        	data1 = data1.split(": ")[1].trim();
			        	if(data1.equals("has no close words")) continue;
				        else {
				        	data1 = data1.replace("[", "").replace("]", "");
				        	List<String> myList = new ArrayList<String>(Arrays.asList(data1.split(",")));
				        	int j;
				        	for(j=0; j<myList.size(); j++) {
				        		int k;
				        		for(k=0; k<wordList.size(); k++) {
				        			if(myList.get(j).trim().equals(wordList.get(k).trim())) break;
				        		}
				        		if(k==wordList.size()) break;
				        	}
				        	if(j==myList.size()) {
				        		criterionData += files[i].getName().split("_")[2].split("\\.")[0]+" ";
				        		break;
				        	}
				        }
			        }
			    }
			    myReader1.close();
			}
			log.info(criterionData);
			
			final Path path = Paths.get("chdb0"+prop.getProperty("dbid")+"/Criteria/criteria.txt");
		    Files.write(path, Arrays.asList(criterionData), StandardCharsets.UTF_8,
		        Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
		}
		else {
			criterionData = prop.getProperty("criteria")+" has no close words";
			log.info(criterionData);
			final Path path = Paths.get("chdb0"+prop.getProperty("dbid")+"/Criteria/criteria.txt");
		    Files.write(path, Arrays.asList(criterionData), StandardCharsets.UTF_8,
		        Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
		}
	}
}
