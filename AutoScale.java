/**
 * Created by fuyun on 11/02/15.
 */


import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoScale {

    public static void main(String[] args) throws Exception {

        //Load the Properties File with AWS Credentials
        Properties properties = new Properties();
        properties.load(RunInstance.class.getResourceAsStream("/AwsCredentials.properties"));
        BasicAWSCredentials bawsc = new BasicAWSCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"));
        AmazonEC2Client ec2 = new AmazonEC2Client(bawsc);
        AmazonElasticLoadBalancingClient elb = new AmazonElasticLoadBalancingClient(bawsc);
        AmazonAutoScalingClient asClient = new AmazonAutoScalingClient(bawsc);
        AmazonCloudWatchClient cwc = new AmazonCloudWatchClient(bawsc);

        ELB ELB = new ELB();

        double rps;
        Instance LG = ELB.launchLG(ec2);
      //  Instance LG = getInstanceFromInstanceId(ec2,"i-0b5eebf1");
        String elbDNS = ELB.launchLB(elb);
      //  String elbDNS = "loader-1275008100.us-east-1.elb.amazonaws.com";
        List<Runnable> cleanupTasks = AutoScallingGroup.createAutoScalingGroup(asClient, ec2, cwc);
        Thread.sleep(120000);
        String testID = "";
        String testID2 = "";
        int n=0;

        do {
            testID = getTestID(LG, elbDNS);
            rps = warmUp(LG, testID);
            n++;
            System.out.println("Warm up " + n +": rps average = "+rps);

        } while (rps < 980);



        testID2 = startTest(LG,elbDNS);
        Thread.sleep(3000000);

        if (testEnd(LG, testID2) == true) {
            terminateAll(cleanupTasks);
        } else {
            Thread.sleep(120000);
        }
    }


    public static String getTestID(Instance LG, String elbDNS) throws IOException {
        URL url = null;
        try {
            url = new URL("http://" + LG.getPublicDnsName() + "/warmup?dns=" + elbDNS);
            System.out.println("http://" + LG.getPublicDnsName() + "/warmup?dns=" + elbDNS);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        InputStream is = null;
        String input;
        String testID = "";

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

            while (matcher.find()) {
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
                System.out.println("testID=" + testID);

            }
        }
        is.close();
        br.close();

        con.disconnect();

        return testID;
    }

    public static double warmUp(Instance LG, String testID) throws IOException, InterruptedException {
        boolean judge = false;
        URL url3 = null;
        InputStream is = null;
        HttpURLConnection con;
        BufferedReader br;
        double averageRPS = 0;


        try {

            url3 = new URL("http://" + LG.getPublicDnsName() + "/log?name=test." + testID + ".log");
            System.out.println("http://" + LG.getPublicDnsName() + "/log?name=test." + testID + ".log");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Thread.sleep(400000);
        while (judge == false) {

            con = (HttpURLConnection) url3.openConnection();

            try {
                is = con.getInputStream();
            } catch (Exception e) {
                is = con.getErrorStream();
            }

            br = new BufferedReader(new InputStreamReader(is));


            int i = 0;
            String input = "";
            while ((input = br.readLine()) != null) {
                Pattern pattern = Pattern.compile(";\\sTest\\sfinished");
                Matcher matcher = pattern.matcher(input);

                if (matcher.find()) {
                    judge = true;
                }
            }
            input = "";
            double rps = 0;
            double m = 0;

            if (judge == true) {
                //注意要不要重新readline
                con = (HttpURLConnection) url3.openConnection();

                try {
                    is = con.getInputStream();
                } catch (Exception e) {
                    is = con.getErrorStream();
                }

                br = new BufferedReader(new InputStreamReader(is));


                while ((input = br.readLine()) != null) {
                    Pattern pattern = Pattern.compile("rps=([\\d\\.]{6,7})");
                    Matcher matcher = pattern.matcher(input);

                    if (matcher.find()) {
                        m++;
                        System.out.println("rps "+m+"= " + matcher.group(1) );
                        rps += Double.parseDouble(matcher.group(1));

                    }
                }

            }

            averageRPS = rps / m;
            if (judge == false){
                averageRPS =0;
            }
            System.out.println("Average RPS = " +averageRPS);

            is.close();
            br.close();
            con.disconnect();

        }

        return averageRPS;
    }

    public static String startTest(Instance LG, String elbDNS) throws IOException, InterruptedException {
        String testID2="";
        URL url = null;
        HttpURLConnection con = null;
        try {

            url = new URL("http://" + LG.getPublicDnsName() + "/junior?dns=" + elbDNS);


        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        System.out.println("Test started.");

        con = (HttpURLConnection) url.openConnection();
        InputStream is = null;
        String input;


        try {
            Thread.sleep(10000);
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

            while (matcher.find()) {
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
                testID2 = matcher.group(1);
                System.out.println("testID2=" + testID2);

            }
        }
        is.close();
        br.close();



        con.disconnect();

        return testID2;
    }

    /**
     * @return true the test ended
     * This method is to check whether the test is ended.
     * @para Instance LG load generator
     * @para testID the ID of the test
     */
    public static boolean testEnd(Instance LG, String testID) throws IOException {
        boolean judge = false;
        URL url3 = null;
        try {
            url3 = new URL("http://" + LG.getPublicDnsName() + "/log?name=test." + testID + ".log");
            System.out.println("http://" + LG.getPublicDnsName() + "/log?name=test." + testID + ".log");;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        HttpURLConnection con = (HttpURLConnection) url3.openConnection();

        try {
            Thread.sleep(360000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        InputStream is = null;
        try {
            is = con.getInputStream();
        } catch (Exception e) {
            is = con.getErrorStream();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(is));


        int i = 0;
        String input = "";
        while ((input = br.readLine()) != null) {
            Pattern pattern = Pattern.compile("; Test finished");
            Matcher matcher = pattern.matcher(input);

            if (matcher.find()) {
                judge = true;
            }
        }

        is.close();
        br.close();
        con.disconnect();
        return judge;
    }

    public static void terminateAll(List<Runnable> cleanupTasks) {
        // Attempt to clean up anything we created
        Collections.reverse(cleanupTasks);
        for (final Runnable cleanupTask : cleanupTasks) {
            try {
                cleanupTask.run();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        System.out.println("Terminated all the things.");

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
}



