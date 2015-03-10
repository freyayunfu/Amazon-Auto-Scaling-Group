import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

/**
 * Created by fuyun on 5/02/15.
 */
public class testHTTP {

    public static void main(String[] args) throws Exception {
        URL url2 = null;

        url2 = new URL("http://ec2-52-0-156-253.compute-1.amazonaws.com/log?name=test.1423288157042.log");



        HttpURLConnection con;
        InputStream is = null;
        BufferedReader br;
        ArrayList<Double> rpsList = new ArrayList<Double>();
        ArrayList<String> sectionList = new ArrayList<String>();

//        do {
//            con = (HttpURLConnection) url2.openConnection();
//
//
//            try {
//                is = con.getInputStream();
//            } catch (Exception e) {
//                is = con.getErrorStream();
//            }
//
//            br = new BufferedReader(new InputStreamReader(is));
//
//            int i = 0;
//            String input = "";
//            String allInput = "";
//
//            while ((input = br.readLine()) != null) {
//                allInput += input;
//            }
//
//            System.out.println(allInput);

        String allInput="; 2015-02-07T05:49:17+0000; Horizontal Scaling Test; Test launched. Please check every minute for update.; Your goal is too achieve rps=4000 in 30 min; Minimal interval of adding instances is 100 sec[Test]type=horizontaltestId=1423288157042testFile=test.1423288157042.log[Minute 1]ec2-52-0-17-182.compute-1.amazonaws.com=674.38[Minute 2]ec2-52-0-17-182.compute-1.amazonaws.com=1019.22[Minute 3]ec2-52-0-17-182.compute-1.amazonaws.com=1025.37[Minute 4]ec2-52-0-17-182.compute-1.amazonaws.com=1022.18[Minute 5]ec2-52-0-17-182.compute-1.amazonaws.com=1026.34[Minute 6]ec2-52-0-17-182.compute-1.amazonaws.com=1022.75[Minute 7]ec2-52-0-17-182.compute-1.amazonaws.com=1017.45[Minute 8]ec2-52-0-17-182.compute-1.amazonaws.com=1029.22[Minute 9]ec2-52-0-17-182.compute-1.amazonaws.com=990.01[Minute 10]ec2-52-0-17-182.compute-1.amazonaws.com=1027.36[Minute 11]ec2-52-0-17-182.compute-1.amazonaws.com=1019.20[Minute 12]ec2-52-0-17-182.compute-1.amazonaws.com=1023.62[Minute 13]ec2-52-0-17-182.compute-1.amazonaws.com=1015.65[Minute 14]ec2-52-0-17-182.compute-1.amazonaws.com=1006.63[Minute 15]ec2-52-0-17-182.compute-1.amazonaws.com=992.10[Minute 16]ec2-52-0-17-182.compute-1.amazonaws.com=1023.29[Minute 17]ec2-52-0-17-182.compute-1.amazonaws.com=1032.55[Minute 18]ec2-52-0-17-182.compute-1.amazonaws.com=1026.81[Minute 19]ec2-52-0-17-182.compute-1.amazonaws.com=1019.31[Minute 20]ec2-52-0-17-182.compute-1.amazonaws.com=1022.32[Minute 21]ec2-52-0-17-182.compute-1.amazonaws.com=1025.77[Minute 22]ec2-52-0-17-182.compute-1.amazonaws.com=1011.91[Minute 23]ec2-52-0-17-182.compute-1.amazonaws.com=1021.15[Minute 24]ec2-52-0-17-182.compute-1.amazonaws.com=1022.71[Minute 25]ec2-52-0-17-182.compute-1.amazonaws.com=1020.87[Minute 26]ec2-52-0-17-182.compute-1.amazonaws.com=1030.60[Minute 27]ec2-52-0-17-182.compute-1.amazonaws.com=1032.25[Minute 28]ec2-52-0-17-182.compute-1.amazonaws.com=1020.49[Minute 29]ec2-52-0-17-182.compute-1.amazonaws.com=1020.08ec2-52-0-17-182.compute-1.amazonaws.com=1031.21[Minute 30]ec2-52-0-17-182.compute-1.amazonaws.com=1031.21ec2-52-0-17-182.compute-1.amazonaws.com=1031.21ec2-52-0-17-182.compute-1.amazonaws.com=1031.21[Load Generator]awsId=272513073825andrewId=yunfuamiId=ami-4c4e0f24instanceId=i-725b9188instanceType=m3.mediumpublicHostname=ec2-52-0-156-253.compute-1.amazonaws.comavailabilityZone=us-east-1b[Data Center 0]instanceType=m3.mediumandrewId=yunfuavailabilityZone=us-east-1binstanceId=i-005b91faamiId=ami-b04106d8publicHostname=ec2-52-0-17-182.compute-1.amazonaws.comawsId=272513073825; MSB is validating...; You have not achieved the goal(rps = 4000).; Your rps is 1031.21; Keep trying.; Test finished";


            Pattern pattern = Pattern.compile("\\[Minute \\d{1,2}\\]([\\w\\.\\-=]*\\.\\d\\d)");
            Matcher matcher = pattern.matcher(allInput);

            while (matcher.find()) {
                sectionList.add(matcher.group(1));
              //  System.out.println(matcher.group(1));
            }


//        } while (sectionList.size() == 0);


        pattern = Pattern.compile("\\.com=(\\d{3,4}\\.\\d\\d)");
       matcher = pattern.matcher(sectionList.get(sectionList.size() - 1));
        System.out.println(sectionList.get(sectionList.size() - 1));


            while (matcher.find()) {
                rpsList.add(Double.parseDouble(matcher.group(1)));
                System.out.println(matcher.group(1));


            }


        double rps = 0;
        for (int m = 0; m < rpsList.size(); m++) {
            rps += rpsList.get(m);

            System.out.println(rps);
        }

    }
}

