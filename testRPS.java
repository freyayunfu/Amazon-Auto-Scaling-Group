import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by fuyun on 14/02/15.
 */
public class testRPS {

    public static void main(String[] args) throws Exception {
//        boolean judge = false;
//        URL url3 = null;
//        InputStream is = null;
//        HttpURLConnection con;
//        BufferedReader br = null;
//        double averageRPS = 0;
//
//        String input = "";
//        br = new BufferedReader(new InputStreamReader(new FileInputStream("/Users/fuyun/Desktop/log111.txt")));
//        double rps = 0;
//        double m = 0;
//        while ((input = br.readLine()) != null) {
//            Pattern pattern = Pattern.compile("rps=([\\d\\.]{6,7})");
//            Matcher matcher = pattern.matcher(input);
//
//            while (matcher.find()) {
//                rps += Double.parseDouble(matcher.group(1));
//                System.out.println(rps);
//                System.out.println(matcher.group(0));
//                m++;
//            }
//
//
//
//
//
//        }
//        averageRPS = rps / m;
//        System.out.println("average RPS = " + averageRPS);
//
//        br.close();
//        double averageRPS1 = 0;
//
//        double rps1 = 0;
//        double m1 = 0;
//        averageRPS1 = rps1 / m1;
//        System.out.println(averageRPS1);
//
//        if(Double.isInfinite(averageRPS1)){
//            System.out.println("111");
//        }
//
//     //  System.out.println(averageRPS1+Double.NaN);

//        boolean judge = false;
//        URL url3 = null;
//        InputStream is = null;
//        HttpURLConnection con;
//        BufferedReader br;
//        double averageRPS = 0;
//
//
//        try {
//
//            url3 = new URL("http://ec2-52-1-113-102.compute-1.amazonaws.com/log?name=test.1423851335680.log");
//         //   System.out.println("http://" + LG.getPublicDnsName() + "/log?name=test." + testID + ".log");
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
//
//    //    Thread.sleep(400000);
//
//        while (judge == false) {
//
//            con = (HttpURLConnection) url3.openConnection();
//
//            try {
//                is = con.getInputStream();
//            } catch (Exception e) {
//                is = con.getErrorStream();
//            }
//
//            br = new BufferedReader(new InputStreamReader(is));
//
//
//            int i = 0;
//            String input = "";
//            while ((input = br.readLine()) != null) {
//                Pattern pattern = Pattern.compile(";\\sTest\\sfinished");
//                Matcher matcher = pattern.matcher(input);
//
//                if (matcher.find()) {
//                    judge = true;
//                }
//            }
//            input = "";
//            double rps = 0;
//            double m = 0;
//
//            if (judge == true) {
//                //注意要不要重新readline
//                con = (HttpURLConnection) url3.openConnection();
//
//                try {
//                    is = con.getInputStream();
//                } catch (Exception e) {
//                    is = con.getErrorStream();
//                }
//
//                 br = new BufferedReader(new InputStreamReader(is));
//
//
//                while ((input = br.readLine()) != null) {
//                    Pattern pattern = Pattern.compile("rps=([\\d\\.]{6,7})");
//                    Matcher matcher = pattern.matcher(input);
//
//                    if (matcher.find()) {
//                        m++;
//                        System.out.println("rps "+m+"= " + matcher.group(1) );
//                        rps += Double.parseDouble(matcher.group(1));
//
//                    }
//                }
//
//            }
//
//            averageRPS = rps / m;
//            if (judge == false){
//                averageRPS =0;
//            }
//            System.out.println(averageRPS);
//
//            is.close();
//            br.close();
//            con.disconnect();
//
//        }

        URL url = null;
        try {
            url = new URL("http://ec2-52-1-130-221.compute-1.amazonaws.com/warmup?dns=loader-1275008100.us-east-1.elb.amazonaws.com");
       //     System.out.println("http://" + LG.getPublicDnsName() + "/warmup?dns=" + elbDNS);
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

    }
}



