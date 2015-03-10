/*
 * MSB.java
 * 
 * This is a web-service used by the MSB to get targets' private
 * conversations from the databases. The conversations have been
 * encrypted, but I have heard rumors about the key being a part 
 * of the results retrieved from the database. 
 * 
 * 02/08/15 - I have replicated the database instances to make
 * the web service go faster.
 * 
 * To do (before 02/15/15): My team lead says that I can get a 
 * higher RPS by optimizing the retrieveDetails function. I 
 * stack overflowed "how to optimize retrieveDetails function", 
 * but could not find any helpful results. I need to get it done
 * before 02/15/15 or I will lose my job to that new junior systems
 * architect.
 * 
 * 02/15/15 - :'(
 * 
 * 
 */

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.platform.Verticle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class MSB extends Verticle {

	/*
	 * Use to cache. cache1 is used to store ID from 1-100 and 10000 to 850000 (only multiple of 10000).
	 * Size of cache1 is 185,and it is a hashmap.
	 * cache2 is used to store predicting range of ID responses according to the growing pattern.
	 * Size of cache2 is 815, and it is a linkedhashmap with hashtablecapacity as 815, which will automatically
	 * remove the eldest response.
	 * The range is got 50 records each time.
	 */
		
	private String[] databaseInstances = new String[2];
	private HashMap<Integer,String> cache1 = new HashMap<Integer,String>();
	int hashTableCapacity = (int) Math.ceil (810 / 0.75f) + 1;
	private LinkedHashMap cache2 = new LinkedHashMap<Integer, String>(hashTableCapacity, 0.75f,false){
		// (an anonymous inner class)
		private static final long serialVersionUID = 1;

		@Override
		protected boolean removeEldestEntry (Map.Entry<Integer, String> eldest)
		{
			return size () > 810;
		}
	};


	/* 
	 * init -initializes the variables which store the 
	 *	     DNS of your database instances
	 */
	private void init() {
		/* Add the DNS of your database instances here */
		databaseInstances[0] = "ec2-52-0-167-69.compute-1.amazonaws.com";
		databaseInstances[1] = "ec2-52-0-247-64.compute-1.amazonaws.com";
	}
	
	/*
	 * checkBackend - verifies that the DCI are running before starting this server
	 */	
    	private boolean checkBackend() {
        	try{
            		if(sendRequest(generateURL(0,"1")) == null ||
                	sendRequest(generateURL(1,"1")) == null)
                		return true;
        	} catch (Exception ex) {
            		System.out.println("Exception is " + ex);
			return true;
        	}

        	return false;
    	}

	/*
	 * sendRequest
	 * Input: URL
	 * Action: Send a HTTP GET request for that URL and get the response
	 * Returns: The response
	 */
	private String sendRequest(String requestUrl) throws Exception {
		 
		URL url = new URL(requestUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("GET");
		connection.setRequestProperty("User-Agent", "Mozilla/5.0");
 
		BufferedReader in = new BufferedReader(
					new InputStreamReader(connection.getInputStream(), "UTF-8"));
		
		String responseCode = Integer.toString(connection.getResponseCode());
		if(responseCode.startsWith("2")){
			String inputLine;
			StringBuffer response = new StringBuffer();
 
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			connection.disconnect();
			return response.toString();
        	} else {
            		System.out.println("Unable to connect to "+requestUrl+
            		". Please check whether the instance is up and also the security group settings"); 
			connection.disconnect();
            		return null;
	    	}   
	}
	/*
	 * generateURL
	 * Input: Instance ID of the Data Center
	 * 		  targetID
	 * Returns: URL which can be used to retrieve the target's details
	 * 			from the data center instance
	 * Additional info: the target's details are cached on backend instance
	 */
	private String generateURL(Integer instanceID, String key) {
		return "http://" + databaseInstances[instanceID] + "/target?targetID=" + key;
	}
	
	/*
	 * generateRangeURL
	 * Input: 	Instance ID of the Data Center
	 * 		  	startRange - starting range (targetID)
	 *			endRange - ending range (targetID)
	 * Returns: URL which can be used to retrieve the details of all
	 * 			targets in the range from the data center instance
	 * Additional info: the details of the last 10,000 targets are cached
	 * 					in the database instance
	 * 				
	 */
	private String generateRangeURL(Integer instanceID, Integer startRange, Integer endRange) {
		return "http://" + databaseInstances[instanceID] + "/range?start_range="
				+ Integer.toString(startRange) + "&end_range=" + Integer.toString(endRange);
	}

	/* 
	 * retrieveDetails - you have to modify this function to achieve a higher RPS value
	 * Input: the targetID
	 * Returns: The result from querying the database instance
	 */

	int lastID=0;
	int lastlastID=0;
	int startRange=50;
	int endRange = startRange+50;
	int judge = 0;
	private String retrieveDetails(String targetID) throws Exception {
		Random rand=new Random();
		int x = rand.nextInt(1);

		int ID = Integer.parseInt(targetID);
		String rangResult = "";
		String returnValue = "";

		if(judge == 0){
			try {
				//cache1 total has 185 records
				//get ID 1-100
				for(int m=1;m<101;m++){
					cache1.put(m,sendRequest(generateURL(0, String.valueOf(m))));
				}

				for(int m=853681;m<101;m--){
					cache1.put(m,sendRequest(generateURL(0, String.valueOf(m))));
				}

				//get ID 10000 to 850000
				for(int m=10000;m<850001;m=m+10000){
					cache1.put(m,sendRequest(generateURL(0, String.valueOf(m))));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			judge = 1;
		}

		try{
			System.out.print("ID = "+ID+";");
			if(cache1.containsKey(ID)){

				//in cache1
				returnValue = cache1.get(ID);
				System.out.println("Correct. Cache1.");

			}else if(cache2.containsKey(ID)){
			//in cache2
				System.out.println("Correct. Cache2.");
				if(ID > lastID && lastID > lastlastID && ID-lastID<50) {

					if (ID + 15 > endRange) {

						startRange = endRange + 1;
						endRange = startRange + 50;

						rangResult = sendRequest(generateRangeURL(x, startRange, endRange));
						String[] parts = rangResult.split(";");
						for (int m = startRange; m < endRange + 1; m++) {
							cache2.put(m, parts[m - startRange]);
						}

					}
				}else if(ID < lastID && lastID < lastlastID && lastID-ID<50) {
					if (ID - 15 < startRange) {

						endRange = startRange - 1;
						startRange = endRange -50;
						if(startRange<1||endRange<1){
							startRange=100;
							endRange=150;
						}

						rangResult = sendRequest(generateRangeURL(x, startRange, endRange));
						String[] parts = rangResult.split(";");
						for (int m = startRange; m < endRange + 1; m++) {
							cache2.put(m, parts[m - startRange]);
						}
					}
				}

				returnValue = (String) cache2.get(ID);

			}else{
				//if both cache1 and cache2 don't have the ID
				System.out.print("Not in cache1 and cache2.");
				if(ID > lastID && lastID > lastlastID && ID-lastID<50) {

					startRange = ID;
					endRange = startRange + 49;

					rangResult = sendRequest(generateRangeURL(x, startRange, endRange));
					String[] parts = rangResult.split(";");
					for (int m = startRange; m < endRange + 1; m++) {
						cache2.put(m, parts[m - startRange]);
					}
					System.out.println("Add increasing range.");

				}else if(ID < lastID && lastID < lastlastID && lastID-ID<50) {

					endRange = ID;
					startRange = endRange -49;
					if(startRange<1||endRange<1){
						startRange=101;
						endRange=150;
					}


					rangResult = sendRequest(generateRangeURL(x, startRange, endRange));
					String[] parts = rangResult.split(";");
					for (int m = startRange; m < endRange + 1; m++) {
						cache2.put(m, parts[m - startRange]);
					}
					System.out.println("Add decreasing range.");
				}

				if(cache2.containsKey(ID)){
					returnValue = (String) cache2.get(ID);
				}

			}

			if(returnValue.equals("")){
				returnValue = sendRequest(generateURL(x, targetID));
				cache2.put(ID,returnValue);

			}

			lastlastID = lastID;
			lastID = ID;

			return returnValue;



		} catch (Exception ex){
			System.out.println(ex);
			lastID=ID;
			return null;
		}


	}
	
	/* 
	 * processRequest - calls the retrieveDetails function with the targetID
	 */
	private void processRequest(String targetID, HttpServerRequest req) throws Exception {
		String result = retrieveDetails(targetID);
		if(result != null)
			req.response().end(result);	
		else
			req.response().end("No resopnse received");
	}

	/*
	 * processRequest - calls the retrieveDetails function with the targetID
	 */
	private void processRequestRange(String targetID, HttpServerRequest req) throws Exception {
		String result = retrieveDetails(targetID);
		if(result != null)
			req.response().end(result);
		else
			req.response().end("No resopnse received");
	}
	
	/*
	 * start - starts the server
	 */
  	public void start() {
  		init();
		if(!checkBackend()){
			vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
				public void handle(HttpServerRequest req) {
				    String query_type = req.path();		
				    req.response().headers().set("Content-Type", "text/plain");
				
				    if(query_type.equals("/target")){
					    String key = req.params().get("targetID");
						try {
							processRequest(key,req);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				    else {
					    String key = "1";
						try {
							processRequestRange(key,req);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
			    }               
			}).listen(80);
		} else {
			System.out.println("Please make sure that both your DCI are up and running");
			System.exit(0);
		}
	}
}


