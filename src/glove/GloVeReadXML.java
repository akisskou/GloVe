package glove;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Clinical Trials GOV XML --> Text Processing --> Save in one Doc --> raw_text.txt
 * @author jason
 *
 */
public class GloVeReadXML {

private static Logger log = LoggerFactory.getLogger(GloVeReadXML.class);
	
	private static String stopwords1 = "(,),[,],<=,>=,_,=,<,>,+,%, -,- , - ,—,•,…,/,#,$,&,*,\\,^,{,},~,£,§,®,°,±,³,·,½,™";

	private static String stopwords2 = "i,me,my,myself,we,our,ours,ourselves,you,your,yours,yourself,yourselves,he,him,his,himself,she,her,hers,herself,it,its,itself,they,them,their,theirs,themselves,what,which,who,whom,never,this,that,these,those,am,is,are,was,were,be,been,being,have,has,had,having,do,does,did,doing,a,an,the,and,but,if,kung,or,because,as,until,while,of,at,by,for,with,about,against,between,into,through,during,before,after,above,below,to,from,up,down,in,out,on,off,over,under,again,further,then,once,here,there,when,where,why,how,long,all,any,both,each,few,more,delivering,most,other,some,such,no,nor,not,only,own,same,so,than,too,cry,very,s,t,can,lite,will,just,don,should,now";
	
	public static void main(String[] args) throws Exception {
		try {
			InputStream input = new FileInputStream("infos.properties");
			Properties prop = new Properties();
			// load a properties file
			prop.load(input);
			//System.out.println(prop.getProperty("pathToXML"));
			File folder;
			folder = new File(new URI("file:///"+prop.getProperty("pathToXML")));
			log.info("Checking for files...");
			File[] files = folder.listFiles(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isFile();
				}
			});
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			String data = "";
			String[] sw1 = stopwords1.split(",");	
			String[] sw2 = stopwords2.split(",");
				String field="";
				log.info("Reading all files...");
				for (int i = 0; i < files.length; i++) {
					Document doc = dBuilder.parse(files[i]);
					doc.getDocumentElement().normalize();
					for(int k=0; k<3; k++) {
						if(k==0) field="brief_summary";
						else if(k==1) field="detailed_description";
						else field="criteria";
						//System.out.println(field);
						NodeList nList = doc.getElementsByTagName(field);
						if(nList.getLength()>0) {
							Node nNode = nList.item(0);
							if (nNode.getNodeType() == Node.ELEMENT_NODE) {
								Element eElement = (Element) nNode;
								data = eElement
										.getElementsByTagName("textblock")
										.item(0)
										.getTextContent().toLowerCase();
								log.info("Replacing stop words...");
								for(int j=0; j<sw1.length; j++) data = data.replace(sw1[j], " ");
								for(int j=0; j<sw2.length; j++) data = data.replace(" "+sw2[j]+" ", " ");
								final Path path = Paths.get("raw_text.txt");
							    Files.write(path, Arrays.asList(data.replace(",", " ").replace("\\.", "")), StandardCharsets.UTF_8,
							        Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
							}
						}
					}
				}
				
	        
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
