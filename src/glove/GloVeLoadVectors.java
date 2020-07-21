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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.jblas.DoubleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import glove.objects.Vocabulary;
import glove.utils.Methods;
import glove.utils.Options;

/**
 * Read TXT files with patient data --> For each term find closest terms
 * @author jason
 *
 */
public class GloVeLoadVectors {

private static Logger log = LoggerFactory.getLogger(GloVeLoadVectors.class);
	
	public static void main(String[] args) throws Exception {
		InputStream input = new FileInputStream("infos.properties");
		Properties prop = new Properties();
		// load a properties file
		prop.load(input);
		//String[] criteria = prop.getProperty("criteria").split(" ");
		File dbDir = new File("chdb0"+prop.getProperty("dbid"));
		log.info("Checking for files...");
		File[] files = dbDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isFile();
			}
		});
		File newDir = new File("chdb0"+prop.getProperty("dbid")+"/Closest_words");
		newDir.mkdir();
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
	    for(int i=0; i<files.length; i++) {	
	    	log.info("Patient "+files[i].getName().split("\\.")[0]);
			File myObj1 = new File("chdb0"+prop.getProperty("dbid")+"/"+files[i].getName());
		    Scanner myReader1 = new Scanner(myObj1);
		    String data1 = "";
		    while (myReader1.hasNextLine()) {
		        data1 += myReader1.nextLine();
		    }
		    myReader1.close();
		    String[] words = data1.trim().replaceAll(" +", " ").split(" ");
		    List<String> myWords = new ArrayList<String>();
		    for(int j=0; j<words.length; j++) {
		    	myWords.add(words[j]);
		    }
		    Set<String> set = new HashSet<>(myWords);
		    myWords.clear();
		    myWords.addAll(set);
			Collection<String> wordList = null;
			String results = "";
			for(int j=0; j<myWords.size(); j++) {
				try {
					wordList = Methods.most_similar(W, vocab, myWords.get(j), Integer.valueOf(prop.getProperty("count")));
					while (wordList.remove(""));
					results += myWords.get(j)+": "+wordList.toString()+"\n";
				}
				catch(Exception e) {
					results += myWords.get(j)+": has no close words\n";
				}
				
			}
			
			log.info("Writing results in file...");
			final Path path = Paths.get("chdb0"+prop.getProperty("dbid")+"/Closest_words/closest_words_"+files[i].getName());
		    Files.write(path, Arrays.asList(results.replace("≥", "")), StandardCharsets.UTF_8,
		        Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
	    }
	}
}
