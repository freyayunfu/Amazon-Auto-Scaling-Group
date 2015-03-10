/**
 * This class launched a load generator and several data center in a test to reach 4000 rps in 30 minutes.
 * @auther fuyun
 * @date 4/02/15
 */

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RunInstance {

    public static void main(String[] args) throws Exception {
        //Load the Properties File with AWS Credentials
        Properties properties = new Properties();
        properties.load(RunInstance.class.getResourceAsStream("/AwsCredentials.properties"));
        BasicAWSCredentials bawsc = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));
        AmazonEC2Client ec2 = new AmazonEC2Client(bawsc);

       createSecurityGroup(ec2);
        long start_time=0;
        long end_time=0;

        Instance LG = launchLG(ec2);

        Instance DC1 = launchDC(ec2);
        start_time = System.currentTimeMillis();

        ArrayList<Instance> DCList = new ArrayList<Instance>();
        DCList.add(DC1);

        int count = 2;

        String testID = getTestID(LG, DC1);

        Instance newDC = DC1;

        if (!testID.equals("")) {
            Double rps=getRPS(testID, LG);
            System.out.println("RPS="+rps);
            while (rps< 4000) {

                if (count<9){
                    newDC = launchDC(ec2);
                    DCList.add(newDC);
                    end_time = System.currentTimeMillis();
                    count++;


                    while (!addDC(LG, newDC)){
                        continue;
                    }
                    if(end_time-start_time<100000){
                        Thread.sleep(100000-(end_time-start_time));
                    }
                    start_time=end_time;

                }else{
                    System.out.println("There are more than 8 instances but RPS is still smaller than 4000.");
                }

                rps=getRPS(testID, LG);
                System.out.println("RPS="+rps);

            }
            if(testEnd(LG, testID)==true){
                System.out.println("RPS>4000. Test end. There are "+DCList.size()+" instances used and RPS is "+getRPS(testID, LG)+" now.");
            }else{
                System.out.println("RPS>4000. Test still going on.");
            }
            System.out.println("There are"+DCList.size()+" instances used and RPS is "+getRPS(testID, LG)+" now.");
                        showAllInstance(ec2);

        } else {
            System.out.println("Didn't get testID");
        }


    }

    /**
     * @para AmazonEC2Client ec2 the client we use
     * @para instance that we want to terminate
     * This method terminate the instance.
     */
    public static void terminate(AmazonEC2Client ec2, Instance instance) {
        //Prompt User
        System.out.printf("\nTerminate Instance?(y/n):");

        BufferedReader bufferedRead = new BufferedReader(new InputStreamReader(System.in));
        String userResponse = null;
        try {
            userResponse = bufferedRead.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (userResponse.toLowerCase().equals("y")) {
            //Terminate Instance
            TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
            List<String> instances = new ArrayList<String>();
            instances.add(instance.getInstanceId());
            terminateInstancesRequest.setInstanceIds(instances);
            ec2.terminateInstances(terminateInstancesRequest);
        }

    }

    /**
     * @para AmazonEC2Client ec2 the client we use
     * @return instance load generator that this method launched
     * This method launch a load generator
     */
    public static Instance launchLG(AmazonEC2Client ec2) throws InterruptedException {

        //Create Instance Request
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        //Configure Instance Request
        runInstancesRequest.withImageId("ami-4c4e0f24")
                .withInstanceType("m3.medium")
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName("project11")
                .withSecurityGroups("yunSecurityGroup");

        Placement place = new Placement();
        place.setAvailabilityZone("us-east-1b");

        runInstancesRequest.setPlacement(place);


        //Launch Instance
        RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);
        System.out.println("LG launched");
        Thread.sleep(10000);

         //Get Instance ID of Instance:
         Instance instance = runInstancesResult.getReservation().getInstances().get(0);

        while(!instance.getState().getName().equals("running")){
            Thread.sleep(10000);
            instance = getInstanceFromInstanceId(ec2, instance.getInstanceId());
        }


            //Add a Tag to the instance:
            CreateTagsRequest createTagsRequest = new CreateTagsRequest();
            createTagsRequest.withResources(instance.getInstanceId())
                    .withTags(new Tag("Project", "2.1"));
            ec2.createTags(createTagsRequest);


        instance = getInstanceFromInstanceId(ec2, instance.getInstanceId());

        return instance;
    }

    /**
     * @para AmazonEC2Client ec2 the client we use
     * @return Instance the data center launched
     * This method launch a data center
     */

    public static Instance launchDC(AmazonEC2Client ec2) throws InterruptedException {
        //Create Instance Request
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        //Configure Instance Request
        runInstancesRequest.withImageId("ami-b04106d8")
                .withInstanceType("m3.medium")
                .withMinCount(1)
                .withMaxCount(1)
                .withKeyName("project11")
                .withSecurityGroups("yunSecurityGroup");
        Placement place = new Placement();
        place.setAvailabilityZone("us-east-1b");

        runInstancesRequest.setPlacement(place);


        //Launch Instance
        RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);
        System.out.println("DC launched.");
        Thread.sleep(10000);

        //Get Instance ID of Instance:
        Instance instance = runInstancesResult.getReservation().getInstances().get(0);
        while(!instance.getState().getName().equals("running")){
            Thread.sleep(10000);
            instance = getInstanceFromInstanceId(ec2, instance.getInstanceId());

        }

        //Add a Tag to the instance:

        CreateTagsRequest createTagsRequest = new CreateTagsRequest();
        createTagsRequest.withResources(instance.getInstanceId())
                .withTags(new Tag("Project", "2.1"));
        ec2.createTags(createTagsRequest);
            System.out.println("DC tagged");


        return instance;

    }

    /**
     * @para Instance LG load generator
     * @para Instance DC data center
     * @return String the value of testID
     * This method get the test ID from a web page
     */
    public static String getTestID(Instance LG, Instance DC) throws IOException {
        URL url = null;
        try {
            url = new URL("http://" + LG.getPublicDnsName() + "/test/horizontal?dns=" + DC.getPublicDnsName());
            System.out.println("http://" + LG.getPublicDnsName() + "/test/horizontal?dns=" + DC.getPublicDnsName());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        InputStream is = null;
        String input;
        String testID="";

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            is = con.getInputStream();
        } catch (Exception e) {
            is = con.getErrorStream();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        //check whether the page is still not valid
        while ((input = br.readLine()) != null) {

            Pattern pattern = Pattern.compile("Invalid");
            Matcher matcher = pattern.matcher(input);

            while (matcher.find()){
                try {
                    con.disconnect();
                    br.close();
                    is.close();
                    Thread.sleep(10000);

                    con = (HttpURLConnection) url.openConnection();

                    is = null;
                    try {
                        is = con.getInputStream();
                    } catch (Exception e) {
                        is = con.getErrorStream();
                    }
                    br = new BufferedReader(new InputStreamReader(is));
                    input = br.readLine();
                    pattern = Pattern.compile("Invalid");
                    matcher = pattern.matcher(input);


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            pattern = Pattern.compile("test\\.(\\d*)\\.log");
            matcher = pattern.matcher(input);

            if (matcher.find()) {
                testID = matcher.group(1);
                System.out.println("testID="+testID);

            }
        }
        is.close();
        br.close();

        con.disconnect();

        return testID;
    }


    /**
     * @para Instance LG load generator
     * @para Instance DC data center that you wan to add it into the test on the load generator
     * @return true the data center is already added into the test
     * This method add a data center into a load generator
     */

    public static boolean addDC(Instance LG, Instance newDC) throws IOException {

        boolean alreadyAdd = false;
        BufferedReader br=null;
        HttpURLConnection con=null;


        URL url4 = new URL("http://" + LG.getPublicDnsName() + "/test/horizontal/add?dns=" + newDC.getPublicDnsName());
        System.out.println("http://" + LG.getPublicDnsName() + "/test/horizontal/add?dns=" + newDC.getPublicDnsName());
        InputStream is = null;
        try {
            con = (HttpURLConnection) url4.openConnection();

            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            try {
                is = con.getInputStream();

            } catch (Exception e) {
                is = con.getErrorStream();
            }


            br = new BufferedReader(new InputStreamReader(is));

            String input = null;

            while ((input = br.readLine()) != null) {

                Pattern pattern = Pattern.compile("added");
                Matcher matcher = pattern.matcher(input);

                if (matcher.find()) {
                    alreadyAdd = true;
                }

            }


        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            is.close();
            br.close();
            con.disconnect();
        }

        return alreadyAdd;

    }


    /**
     * @para testID the ID of the test
     * @para Instance LG load generator hold the test
     * @return double the rps value
     * This method get the sum of RPS of all instance that are running on the test at a moment.
     */
    public static double getRPS(String testID, Instance LG) throws IOException {
        URL url2 = null;

        url2 = new URL("http://" + LG.getPublicDnsName() + "/log?name=test." + testID + ".log");
        System.out.println("http://" + LG.getPublicDnsName() + "/log?name=test." + testID + ".log");


        HttpURLConnection con;
        InputStream is = null;
        BufferedReader br;
        ArrayList<Double> rpsList = new ArrayList<Double>();
        ArrayList<String> sectionList = new ArrayList<String>();

        while (sectionList.size()==0) {

            con = (HttpURLConnection) url2.openConnection();


            try {
                is = con.getInputStream();
            } catch (Exception e) {
                is = con.getErrorStream();
            }

            br = new BufferedReader(new InputStreamReader(is));

            int i = 0;
            String input = "";
            String allInput = "";

            while ((input = br.readLine()) != null) {
                allInput += input;
            }


            Pattern pattern = Pattern.compile("\\[Minute \\d{1,2}\\]([\\w\\.\\-=]*\\.\\d\\d)");
            Matcher matcher = pattern.matcher(allInput);

            while (matcher.find()) {
                sectionList.add(matcher.group(1));

            }
            is.close();
            br.close();
            con.disconnect();

        }

            Pattern pattern = Pattern.compile("\\.com=(\\d{3,4}\\.\\d\\d)");
            Matcher matcher = pattern.matcher(sectionList.get(sectionList.size() - 1));

            while (matcher.find()) {
                rpsList.add(Double.parseDouble(matcher.group(1)));
            }

            double rps = 0;
            for (int m = 0; m < rpsList.size(); m++) {
                rps += rpsList.get(m);
            }

        return rps;
    }


    /**
     * @para ec2 the Amazon client we use
     * This method create a security group.
     */
    public static void createSecurityGroup(AmazonEC2Client ec2) {

        CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest();
        csgr.withGroupName("yunSecurityGroup").withDescription("My security group");
        CreateSecurityGroupResult createSecurityGroupResult = ec2.createSecurityGroup(csgr);
        IpPermission ipPermission = new IpPermission();

        ipPermission.withIpRanges("0.0.0.0/0")
                .withIpProtocol("tcp")
                .withFromPort(80)
                .withToPort(80);

        AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest =
                new AuthorizeSecurityGroupIngressRequest();

        authorizeSecurityGroupIngressRequest.withGroupName("yunSecurityGroup")
                .withIpPermissions(ipPermission);

        ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
    }



    /**
     * @para ec2 Amazon client
     * This method show all instance ID of the instances in the client
     */
    public static void showAllInstance(AmazonEC2Client ec2) {


        //Obtain a list of Reservations
        List<Reservation> reservations = ec2.describeInstances().getReservations();
        int reservationCount = reservations.size();
        for (int i = 0; i < reservationCount; i++) {
            List instances = reservations.get(i).getInstances();
            int instanceCount = instances.size();


            //Print the instance IDs of every instance in the reservation.
            for (int j = 0; j < instanceCount; j++) {
                Instance instance2 = (Instance) instances.get(j);
                if (instance2.getState().getName().equals("running")) {
                    System.out.println(instance2.getInstanceId());
                }
            }
        }

    }


    /**
     * @para ec2 the amazon client
     * @para instanceId the ID o the instance that you want to refresh
     * @return Instance the refreshed instance
     * This method refresh your instance with its ID.
     */
    static Instance getInstanceFromInstanceId(AmazonEC2Client ec2, String instanceId) {
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);
        describeInstancesRequest.setInstanceIds(instanceIds);
        Instance instance1 = ec2.describeInstances(describeInstancesRequest).getReservations().get(0).getInstances().get(0);
        return instance1;
    }



    /**
     * @para Instance LG load generator
     * @para testID the ID of the test
     * @return true the test ended
     * This method is to check whether the test is ended.
     */
    public static boolean testEnd(Instance LG, String testID)throws IOException{
        boolean judge=false;
        URL url3 = null;
        try {

                url3 = new URL("http://" + LG.getPublicDnsName() + "/log?name=test." + testID + ".log");


        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        HttpURLConnection con = (HttpURLConnection) url3.openConnection();

        InputStream is = null;
        try {
            is = con.getInputStream();
        } catch (Exception e) {
            is = con.getErrorStream();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(is));


        int i = 0;
        String input="";
        while ((input=br.readLine()) != null) {
            Pattern pattern = Pattern.compile("; Test finished");
            Matcher matcher = pattern.matcher(input);

            if (matcher.find()) {
                judge=true;
            }
        }

        is.close();
        br.close();
        con.disconnect();
        return judge;
    }
}
