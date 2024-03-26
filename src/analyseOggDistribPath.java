import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.Console;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONObject;


public class analyseOggDistribPath {

	
	   static JSONObject jObj = null;
	   static String json;
	   static String currentDeploy;
	   static String currentHostname;
       static String username;
	   static String password;
	   static String oggUrl ;
	   static String httpUrl ="";
	   static String httpUrlService="";
	   static String oggUrlPort ;
	   static String deployName="";
	   static String defaultDir;
	   static String outputPath;
	   static String suffixeFile;
	   static String pathStart=""; 
	   static String defaultProperties;
	   static Path outDistbrib;
	   
	   static int interval=60; 
	   static long refreshFrequency=10; 
	   static String refreshFrequencyStr;
	   static LocalDateTime currentDate ;
	   
	   static HttpURLConnection connectHttp = null ;  
	   static FileWriter fw;

	  
	   
	   public static String getPropertiesOgg(){
	       return pathStart ;
	   }
	   
	   
	   public static void main(String[]args) throws IOException, NumberFormatException, ParseException, SQLException, ClassNotFoundException
	   {
		   if (args.length == 0) {
			      Console console = System.console();
			   
				   System.out.println("Enter user and pwd for GoldenGate Service Manager:");
				   username = console.readLine("Username: ");
				   if (username.equals("")) {
			    	   System.out.println("Username is null, set default : oggadmin");
			    	   username="oggadmin";
			       }
				   
			       password = new String(console.readPassword("Password: "));
			       
			       if (password.equals("")) {
			    	   System.out.println("Password is null, set default : oggadmin");
			    	   password="oggadmin";
			       }
			       defaultProperties = console.readLine("Properties file: ");
			       
			       refreshFrequencyStr = console.readLine("Refresh frequency (in seconds): ");
			   }
			   else {
				   username=args[0];
				   password=args[1];
				   defaultProperties=args[2];
				   refreshFrequencyStr=args[3];
			   }
		       
		       
		       if (defaultProperties.equals("")) {
		    	   System.out.println("Properties file is null, set default : oggConnect.properties");
		    	   defaultProperties="oggConnect";
		       }
	       
	       
	      
	       if (refreshFrequencyStr.length()<2) {refreshFrequencyStr="10";}
	       refreshFrequency =  Long.parseLong(refreshFrequencyStr);
	       if (refreshFrequency<10) {
	    	   System.out.println("Setting refreshFrequency to 10 seconds");
	    	   refreshFrequency=10;
	       }
		   
		   defaultDir = oggProperties.getString("defaultDir"); 
		   oggUrl = oggProperties.getString("oggUrl"); 
		 
		   oggUrlPort=oggUrl.split(":")[2];
		   
		   File fileStructure = new File(defaultDir);
		   if(! fileStructure.exists()) {
			   if (fileStructure.mkdirs()) {
                   System.out.println(defaultDir + " created successfully.");
               } else {
                   System.out.println("Couldn't create " + fileStructure.getName());
                   System.exit(0);
               }
		   }
			   
		   
		  
	  
	   Runnable statsRunnable = new Runnable() {
		    public void run() {
		    	try {
					new analyseOggDistribPath().getOciOggInfo();
				} catch (NumberFormatException | ClassNotFoundException | IOException | ParseException
						| SQLException e) {
					e.printStackTrace();
				}
		    }
		};
		
		suffixeFile= new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss",   Locale.getDefault()).format(new Date());
		 
	    String pathDeploy = String.format( "%s%sOGG_DISTRIBUTION_PATH_%s.csv",defaultDir,File.separator,suffixeFile);
	    
		System.out.println("Output file : "+pathDeploy);
	    outDistbrib = Paths.get(pathDeploy);
		fw = new FileWriter(pathDeploy, true);
		fw.write("date;deploy_name;distribution_path;status;lag;inserts;updates;deletes;total;sendWaitTime;recvWaitTime;totalBytesSent;totalMsgsSent;inputSequence;inputOffset;outputSequence;outputOffset\r\n");
		fw.close();
		   
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(statsRunnable, 0, refreshFrequency, TimeUnit.SECONDS);
	  
   }

    
   
   private void getOciOggInfo() throws IOException, NumberFormatException, ParseException, SQLException, ClassNotFoundException{
	  
	   currentDate = LocalDateTime.now();
	   suffixeFile= new SimpleDateFormat("yyyy_MM_dd",Locale.getDefault()).format(new Date());
	     
	   System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
      
	   httpUrl=oggUrl+"/services/v2/deployments";
	  
	   JSONObject result = httpOgg(httpUrl,username,password,"GET","");
       Map<String, String> deployList = getDeployment(result);
       for (Map.Entry<String, String> entry : deployList.entrySet()) {
    	 String depName=entry.getKey();
    	 httpUrl=oggUrl+"/services/v2/deployments/"+depName;
  	     result = httpOgg(httpUrl,username,password,"GET","");
  	   }
      connectHttp.disconnect();
   }
   

   private static void getRetrieveServices(String depName, JSONObject ojb) throws IOException, ParseException 
   {
	   String host=null;
	   JSONObject result = (JSONObject) jObj.get("response");
	   
	   JSONArray getInput = (JSONArray) result.get("items");
	   for (int i = 0; i < getInput.length(); i++) {
    	   	String link=null;
    	   	ArrayList<String> res=   new ArrayList<String>();
    	    JSONObject getService = getInput.getJSONObject(i);
    	    String name = (String) getService.get("name");
    	    
    	    
    	    JSONArray getLinks = (JSONArray) getService.get("links");
  	    	for (int j = 0; j < getLinks.length(); j++) {
  	    		JSONObject currentLink = getLinks.getJSONObject(j);
  	    		String depHref=currentLink.get("href").toString();
  	
  	    		if (depHref.contains(depName)) {
  	    			String urlServices=currentLink.get("href").toString()+"/services";
  	    			res.add(urlServices);
  	    			link = urlServices=currentLink.get("href").toString();
  	    		}
  	    	}
    	    
    	    
    	    httpUrl=oggUrl+"/services/v2/deployments/"+depName+"/services/"+name;
	   	    JSONObject resultServices = httpOgg(httpUrl,username,password,"GET","");
	   	    JSONObject getresultService = (JSONObject) resultServices.get("response");
	   	    JSONObject getConfig = (JSONObject) getresultService.get("config");
	   	    JSONObject getNetwork= (JSONObject) getConfig.get("network");
	   	    String port = getNetwork.get("serviceListeningPort").toString();
	   	    getresultService.get("status").toString();
	   	    host =link.split("//")[1].split(":")[0];
	   	    currentHostname=host; 
	 	 
	   	    if (name.contains("distsrvr")) {
	 		  httpUrlService="http://"+host+":"+port+"/services/v2/sources";
	 		  result = httpOgg(httpUrlService,username,password,"GET","");
	 		  getDistribution(host,port,depName, result);
	   	    }
       }    
    	    
	
   }
   
   private static void getDistribution(String host, String port, String depName, JSONObject ojb) throws IOException 
   {
	   
		  JSONObject resultDist = (JSONObject) jObj.get("response");
		  JSONArray getInputDist = (JSONArray) resultDist.get("items");
		   for (int i = 0; i < getInputDist.length(); i++) {
	    	   JSONObject listDist = getInputDist.getJSONObject(i);
	    	   try {getDetailDistribution(host, port, depName, (String) listDist.get("name"));
	    	   }
	    	   catch(Exception e){}
	   	   }	  
   }	   
		   
	 
	   
   private static void getDetailDistribution(String host, String port, String depName, String name) throws IOException 
   {
	   		
			DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");  
	  	    httpUrlService="http://"+host+":"+port+"/services/v2/sources/"+name;
	  		JSONObject result = httpOgg(httpUrlService,username,password,"GET","");
	   		JSONObject resultDist = (JSONObject) jObj.get("response");
	   		((JSONObject)  resultDist.get("source")).get("uri").toString(); 
	   		String status = resultDist.get("status").toString();
	   		JSONObject target = (JSONObject) resultDist.get("target");
	   		String targetInitiated = "";
	   		try {

	   			targetInitiated = resultDist.get("targetInitiated").toString();
	   			if (targetInitiated.contentEquals("true")){
	   				targetInitiated ="TARGET-INITIATED";
	   				};
   			}
	   		catch(Exception e){}
	   		
	   		target.get("uri").toString();
	   		
			JSONObject trailsTarget = (JSONObject) target.get("details");
	   		Long.parseLong( ((JSONObject)  trailsTarget.get("trail")).get("sizeMB").toString());
	   		
	   		httpUrlService="http://"+host+":"+port+"/services/v2/sources/"+name+"/info";
	   		result = httpOgg(httpUrlService,username,password,"GET","");
	   		((JSONObject)  result.get("response")).get("sourceDatabaseName").toString(); 
	   		((JSONObject)  result.get("response")).get("sourceDatabaseInstance").toString(); 
	   		((JSONObject)  result.get("response")).get("sourceExtractName").toString(); 
	   		Long.parseLong( ((JSONObject)  result.get("response")).get("sinceLagReported").toString()); 
	   		
	   		
	   		String reslastStarted = ((JSONObject)  result.get("response")).get("lastStarted").toString(); 
	   		try {
	    		LocalDateTime.parse(reslastStarted,dateFmt);
	    	} catch(Exception e) {};
	    	long lag =  Long.parseLong( ((JSONObject)  result.get("response")).get("lag").toString()); 
	   		
	   		httpUrlService="http://"+host+":"+port+"/services/v2/sources/"+name+"/checkpoints";
	   		result = httpOgg(httpUrlService,username,password,"GET","");
	   		JSONObject  response = (JSONObject) result.get("response");
	   		JSONObject current = (JSONObject) response.get("current");
	   		
	   		long inputSequence= 0;
	   		long inputOffset = 0;
	   		long outputSequence= 0;
	   		long outputOffset = 0;
	   		JSONArray input = (JSONArray) current.get("input");
	   		   for (int i = 0; i < input.length(); i++) {
		    	   JSONObject listDist = input.getJSONObject(i);
		    	   ((JSONObject) listDist.get("current")).get("path").toString();
		    	   ( (JSONObject) listDist.get("current")).get("name").toString();
		    	   inputSequence=Long.parseLong( ( (JSONObject) listDist.get("current")).get("sequence").toString());
		    	   inputOffset = Long.parseLong( ( (JSONObject) listDist.get("current")).get("offset").toString());
		    	   String reinputTimeStamp= ( (JSONObject) listDist.get("current")).get("timestamp").toString();
		    	   try {
		    		   LocalDateTime.parse(reinputTimeStamp,dateFmt);
			    	} catch(Exception e) {};
			    	
			    	
			   }
	   		   
	   		JSONArray output = (JSONArray) current.get("output");
	   		   for (int i = 0; i < output.length(); i++) {
		    	   JSONObject listDist = output.getJSONObject(i);
		    	   ( (JSONObject) listDist.get("current")).get("path").toString();
		    	   ( (JSONObject) listDist.get("current")).get("name").toString();
		    	   outputSequence= Long.parseLong( ( (JSONObject) listDist.get("current")).get("sequence").toString());
		    	   outputOffset= Long.parseLong( ( (JSONObject) listDist.get("current")).get("offset").toString());
		    	   String reoutputTimeStamp= ( (JSONObject) listDist.get("current")).get("timestamp").toString();
		    	   try {
		    		   LocalDateTime.parse(reoutputTimeStamp,dateFmt);
			    	} catch(Exception e) {}
		    	   
	   		   }
	   		   
	   		httpUrlService="http://"+host+":"+port+"/services/v2/sources/"+name+"/stats";
	   		result = httpOgg(httpUrlService,username,password,"GET","");
	   		
	   		
	   		response = (JSONObject) result.get("response");
	   		long sendWaitTime = Long.parseLong( ((JSONObject) response.get("netStats")).get("sendWaitTime").toString());
	   		long  recvWaitTime = Long.parseLong( ((JSONObject) response.get("netStats")).get("recvWaitTime").toString());
	   		long totalBytesSent =Long.parseLong( ( (JSONObject) response.get("netStats")).get("totalBytesSent").toString());
	   		long totalMsgsSent = Long.parseLong( ( (JSONObject) response.get("netStats")).get("totalMsgsSent").toString());
	   		
	   		JSONObject localAddress = (JSONObject) ((JSONObject) response.get("netStats")).get("localAddress");
	   		Long.parseLong(  localAddress.get("port").toString());
	   		localAddress.get("host").toString();
	   		JSONObject remoteAddress = (JSONObject) ((JSONObject) response.get("netStats")).get("remoteAddress");
	   		Long.parseLong(   remoteAddress.get("port").toString());
	   		remoteAddress.get("host").toString();
	   	 
		   	Long.parseLong( response.get("ddlReceived").toString());
		   	Long.parseLong( response.get("ddlSent").toString());
		   	long inserts = Long.parseLong( response.get("inserts").toString());
		   	long updates = Long.parseLong( response.get("updates").toString());
		   	long upserts = Long.parseLong( response.get("upserts").toString());
		   	long deletes = Long.parseLong( response.get("deletes").toString());
			Long.parseLong( response.get("unsupported").toString());
			Long.parseLong( response.get("other").toString());
			long total = inserts+updates+upserts+deletes;
	   		
	   		System.out.println(currentDate+ " Deploy :" + depName+ " -- Distrib :"+name +  " " +status+ " -- Lag:"+lag + " -- Dml inserts:"+ inserts
	   				+ " -- Dml updates:"+ updates+ " -- Dml deletes:"+ deletes+ " -- Dml total:"+ total + " -- totalBytesSent:"+totalBytesSent);
	   		
	   		String res= currentDate+ ";" + depName+ ";"+name +  ";" +status+ ";"+lag + ";"+ inserts+ ";"+ updates+ ";"+ deletes+ ";"+ total+
	   				";"+ sendWaitTime+";"+recvWaitTime+";"+ totalBytesSent+";"+ totalMsgsSent+";"+inputSequence+ ";"+inputOffset+ ";"+outputSequence+ ";"+outputOffset+"\r\n";
	   		
	   		Files.write(outDistbrib, res.getBytes(), StandardOpenOption.APPEND); 
   }
   
   

  
   private static Map<String, String> getDeployment(JSONObject ojb) throws IOException, ParseException 
   {
	   Map<String, String> dictionary = new HashMap<String, String>();
	   JSONObject result = (JSONObject) jObj.get("response");
	   JSONArray getInput = (JSONArray) result.get("items");
       for (int i = 0; i < getInput.length(); i++) {
    	    ArrayList<String> res=   new ArrayList<String>();
    	    JSONObject currentChk = getInput.getJSONObject(i);
    		String depName= currentChk.get("name").toString();
    		if (!depName.contains("ServiceManager")) {
  	    		deployName=depName;
  	    		currentDeploy=depName;
  	    		outputPath=String.format("%s%s%s",defaultDir,File.separator,deployName);
  	    		Paths.get(outputPath);
  	    		httpUrl=oggUrl+"/services/v2/deployments/"+depName+"/services";
  	   	     	result = httpOgg(httpUrl,username,password,"GET","");
  	   	     	getRetrieveServices(depName,result);
  	   	    	
    		}
    		
  	    	JSONArray getLinks = (JSONArray) currentChk.get("links");
  	    	for (int j = 0; j < getLinks.length(); j++) {
  	    		JSONObject currentLink = getLinks.getJSONObject(j);
  	    		String depHref=currentLink.get("href").toString();
  	    		if (depHref.contains(depName)) {
  	    			String urlServices=currentLink.get("href").toString()+"/services";
  	    			res.add(urlServices);
  	    		}
  	    	}
  	    	dictionary.put(depName, String.join(";", res));
  	   }
    return dictionary;
    
   }
  
   
   private static JSONObject httpOgg(String httpUrl, String username, String password,String method,String params) throws IOException {
	   URL url;   
	   
	   jObj = null;
	   url = new URL(httpUrl); 
	   String userpass = username + ":" + password;
       String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
       connectHttp = (HttpURLConnection) url.openConnection();
       connectHttp.setRequestProperty ("Authorization", basicAuth);
       connectHttp.setRequestMethod(method);
       connectHttp.setRequestProperty("accept", "application/json");
       connectHttp.setConnectTimeout(10000);
      
       try{
    	   jObj=gethttpOgg(method,params);
    	  
       }
       catch (Exception e) {
       }
       connectHttp.disconnect();
      
       return jObj;
       
   }
       
	private static JSONObject gethttpOgg (String method,String params) throws IOException {  
			JSONObject jObj = null;
			   try {
		          if(connectHttp!=null){
		 		    try {
		 		       BufferedReader br =new BufferedReader(new InputStreamReader(connectHttp.getInputStream()));
		 		       String input;
		 		       while ((input = br.readLine()) != null){
		 		           jObj = new JSONObject(input);
		 		           }
		 		       br.close();
		 		    } catch (Exception e) {
		 		    }
		         }
		        } catch (Exception e) {
		         e.printStackTrace();}
		   
		   return jObj; 
   }
}