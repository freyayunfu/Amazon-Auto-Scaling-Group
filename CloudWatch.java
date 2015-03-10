import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

import java.util.Date;

/**
 * Created by fuyun on 12/02/15.
 */
public class CloudWatch {

    AmazonCloudWatchClient cwc;

    public AmazonCloudWatchClient getCwc(BasicAWSCredentials bawsc) {
        AmazonCloudWatchClient cwc = new AmazonCloudWatchClient(bawsc);
        cwc.setEndpoint("http://monitoring.eu-west-1.amazonaws.com");
        return cwc;
    }

    public void CloudWatchFetchCpuUtil(AmazonCloudWatchClient cwc, String instanceId) {

        final GetMetricStatisticsRequest request = request(instanceId);
        final GetMetricStatisticsResult result = result(cwc, request);
        toStdOut(result, instanceId);
    }


    private static GetMetricStatisticsRequest request(final String instanceId) {
        final long twentyFourHrs =  60 * 48;
        final int oneHour = 60 * 2;
        return new GetMetricStatisticsRequest()
               // .withStartTime(new Date(new Date().getTime() - twentyFourHrs))
                .withStartTime(new Date(new Date().getTime()))
                        .withNamespace("AWS/EC2")
                        .withPeriod(oneHour)
                        .withDimensions(new Dimension().withName("InstanceId").withValue(instanceId))
                        .withMetricName("CPUUtilization")
                        .withStatistics("Average", "Maximum")
                        .withEndTime(new Date(new Date().getTime() + twentyFourHrs));
    }

    private static GetMetricStatisticsResult result(
            final AmazonCloudWatchClient client, final GetMetricStatisticsRequest request) {
        return client.getMetricStatistics(request);
    }

    private static void toStdOut(final GetMetricStatisticsResult result, final String instanceId) {
        System.out.println(result); // outputs empty result: {Label: CPUUtilization,Datapoints: []}
        for (final Datapoint dataPoint : result.getDatapoints()) {
            System.out.printf("%s instance's average CPU utilization : %s%n", instanceId, dataPoint.getAverage());
            System.out.printf("%s instance's max CPU utilization : %s%n", instanceId, dataPoint.getMaximum());
        }
    }
  }


