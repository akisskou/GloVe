package glove;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read DB (based on properties) --> Read Patient Data and save in separate TXT Files
 * @author jason
 *
 */

public class ExtractFromDB {

private static Logger log = LoggerFactory.getLogger(ExtractFromDB.class);
	
	private static String stopwords1 = "(,),[,],<=,>=,_,=,<,>,+,%, -,- , - ,—,•,…,/,#,$,&,*,\\,^,{,},~,£,§,®,°,±,³,·,½,™";

	private static String stopwords2 = "i,me,my,myself,we,our,ours,ourselves,you,your,yours,yourself,yourselves,he,him,his,himself,she,her,hers,herself,it,its,itself,they,them,their,theirs,themselves,what,which,who,whom,never,this,that,these,those,am,is,are,was,were,be,been,being,have,has,had,having,do,does,did,doing,a,an,the,and,but,if,kung,or,because,as,until,while,of,at,by,for,with,about,against,between,into,through,during,before,after,above,below,to,from,up,down,in,out,on,off,over,under,again,further,then,once,here,there,when,where,why,how,long,all,any,both,each,few,more,delivering,most,other,some,such,no,nor,not,only,own,same,so,than,too,cry,very,s,t,can,lite,will,just,don,should,now";
	
	private static JSONObject getCredentials(int cohortID) {
    	JSONObject credentials = new JSONObject();
    	try {
	        String webPage = "https://private.harmonicss.eu/index.php/apps/coh/api/1.0/cohortid?id="+cohortID;

	        String authString = "test1:1test12!";
	        System.out.println("auth string: " + authString);
	        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
	        String authStringEnc = new String(authEncBytes);
	        System.out.println("Base64 encoded auth string: " + authStringEnc);

	        URL url = new URL(webPage);
	        URLConnection urlConnection = url.openConnection();
	        urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
	        InputStream is = urlConnection.getInputStream();
	        InputStreamReader isr = new InputStreamReader(is);

	        int numCharsRead;
	        char[] charArray = new char[1024];
	        StringBuffer sb = new StringBuffer();
	        while ((numCharsRead = isr.read(charArray)) > 0) {
	            sb.append(charArray, 0, numCharsRead);
	        }
	        String result = sb.toString();
	        JSONArray jsonarray = new JSONArray(result);
	        credentials = jsonarray.getJSONObject(0);
	        /*System.out.println("*** BEGIN ***");
	        System.out.println(result);
	        System.out.println("*** END ***");*/
	    } catch (MalformedURLException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
    	return credentials;
    }
	
	private static void extractTerms(String dbid) throws SQLException, IOException {
		File myDir = new File(dbid);
		myDir.mkdir();
		String[] sw1 = stopwords1.split(",");	
		String[] sw2 = stopwords2.split(",");

		String query;
		String tables = "patient, cond_symptom,voc_symptom_sign";
		String where_clause = "patient.ID = cond_symptom.PATIENT_ID AND cond_symptom.CONDITION_ID = voc_symptom_sign.ID AND cond_symptom.STMT_ID=1";
		query = "SELECT patient.ID,voc_symptom_sign.NAME FROM " + tables + " WHERE " + where_clause + " ORDER BY patient.ID";
		//System.out.println("We are ready to execute the query: "+query);
		String mydata = DBServiceCRUD.getDataFromDB(query);
		//System.out.println(mydata);
		String[] data = mydata.split("\\|");
		for(int i=0; i<data.length; i++) {
			//System.out.println(data[i]);
			String[] patient = data[i].split("!");
			//System.out.println(patient[0]);
			log.info("Replacing stop words...");
			
			for(int j=0; j<sw1.length; j++) patient[1] = patient[1].replace(sw1[j], " ");
			for(int j=0; j<sw2.length; j++) patient[1] = patient[1].replace(" "+sw2[j]+" ", " ");
			final Path path = Paths.get(dbid+"/"+patient[0]+".txt");
		    Files.write(path, Arrays.asList(patient[1].replace(",", " ").replace(" aka", "").toLowerCase()), StandardCharsets.UTF_8,
		        Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
		}
		tables = "patient, cond_diagnosis,voc_medical_condition";
		where_clause = "patient.ID = cond_diagnosis.PATIENT_ID AND cond_diagnosis.CONDITION_ID = voc_medical_condition.ID AND cond_diagnosis.STMT_ID=1";
		query = "SELECT patient.ID,voc_medical_condition.NAME FROM " + tables + " WHERE " + where_clause + " ORDER BY patient.ID";
		mydata = DBServiceCRUD.getDataFromDB(query);
		data = mydata.split("\\|");
		for(int i=0; i<data.length; i++) {
			String[] patient = data[i].split("!");
			log.info("Replacing stop words...");
			
			for(int j=0; j<sw1.length; j++) patient[1] = patient[1].replace(sw1[j], " ");
			for(int j=0; j<sw2.length; j++) patient[1] = patient[1].replace(" "+sw2[j]+" ", " ");
			final Path path = Paths.get(dbid+"/"+patient[0]+".txt");
		    Files.write(path, Arrays.asList(patient[1].replace(",", " ").replace(" aka", "").toLowerCase()), StandardCharsets.UTF_8,
		        Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
		}
		tables = "patient, interv_medication,voc_pharm_drug";
		where_clause = "patient.ID = interv_medication.PATIENT_ID AND interv_medication.MEDICATION_ID = voc_pharm_drug.ID AND interv_medication.STMT_ID=1";
		query = "SELECT patient.ID,voc_pharm_drug.NAME FROM " + tables + " WHERE " + where_clause + " ORDER BY patient.ID";
		mydata = DBServiceCRUD.getDataFromDB(query);
		data = mydata.split("\\|");
		for(int i=0; i<data.length; i++) {
			String[] patient = data[i].split("!");
			log.info("Replacing stop words...");
			for(int j=0; j<sw1.length; j++) patient[1] = patient[1].replace(sw1[j], " ");
			for(int j=0; j<sw2.length; j++) patient[1] = patient[1].replace(" "+sw2[j]+" ", " ");
			final Path path = Paths.get(dbid+"/"+patient[0]+".txt");
		    Files.write(path, Arrays.asList(patient[1].replace(",", " ").replace(" aka", "").toLowerCase()), StandardCharsets.UTF_8,
		        Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
		}
		tables = "patient, exam_lab_test,voc_lab_test";
		where_clause = "patient.ID = exam_lab_test.PATIENT_ID AND exam_lab_test.TEST_ID = voc_lab_test.ID";
		query = "SELECT patient.ID,voc_lab_test.NAME FROM " + tables + " WHERE " + where_clause + " ORDER BY patient.ID";
		mydata = DBServiceCRUD.getDataFromDB(query);
		data = mydata.split("\\|");
		for(int i=0; i<data.length; i++) {
			String[] patient = data[i].split("!");
			log.info("Replacing stop words...");
			
			for(int j=0; j<sw1.length; j++) patient[1] = patient[1].replace(sw1[j], " ");
			for(int j=0; j<sw2.length; j++) patient[1] = patient[1].replace(" "+sw2[j]+" ", " ");
			final Path path = Paths.get(dbid+"/"+patient[0]+".txt");
		    Files.write(path, Arrays.asList(patient[1].replace(",", " ").replace(" aka", "").toLowerCase()), StandardCharsets.UTF_8,
		        Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
		}
	}
	
	private static void accessCohort(String mycohort) throws SQLException, JSONException, IOException {
    	int mycohortid = Integer.valueOf(mycohort);
    	ConfigureFile obj;
    	if(mycohortid<10) mycohort = "chdb00"+mycohortid;
    	else mycohort = "chdb0"+mycohortid;
		JSONObject credentials = getCredentials(mycohortid);
		obj = new ConfigureFile("jdbc:mysql://"+credentials.getString("dbserver")+":"+credentials.getString("dbport")+"/"+credentials.getString("dbarea")+"?autoReconnect=true&useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC",credentials.getString("dbuname"),credentials.getString("dbupass"));
		
    	if(!DBServiceCRUD.makeJDBCConnection(obj)) {
    		System.out.println("Connection with the Database failed. Check the Credentials and the DB URL.");
    	}
    	else{
    	System.out.println("everything's gooooooood");
    	extractTerms(mycohort);
		DBServiceCRUD.closeJDBCConnection();
		System.out.println("End");
    	}
    }
	
	public static void main(String[] args) throws Exception {
		InputStream input = new FileInputStream("infos.properties");
		Properties prop = new Properties();
		// load a properties file
		prop.load(input);
		accessCohort(prop.getProperty("dbid"));
	}
}
