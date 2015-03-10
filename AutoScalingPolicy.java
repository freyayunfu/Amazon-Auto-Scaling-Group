import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyResult;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * An Auto Scaling policy should be created, which defines the set of actions to perform
 * when an event, such as a CloudWatch alarm, is triggered.
 * Created by fuyun on 11/02/15.
 */
public class AutoScalingPolicy {


    /**
     * This policy scales the number of instances up, to scale down make the adjustment
     * a negative number.
     *
     */
    public void definePolicy(AmazonAutoScalingClient asClient){
        PutScalingPolicyRequest request = new PutScalingPolicyRequest();
        request.setAutoScalingGroupName("ASG");
        request.setPolicyName("ASGPolicy"); // This scales up so I've put up at the end.
        request.setScalingAdjustment(1); // scale up by one
        request.setAdjustmentType("ChangeInCapacity");

        PutScalingPolicyResult result = asClient.putScalingPolicy(request);
        String arn = result.getPolicyARN(); // You need the policy ARN in the next step so make a note of it.
    }

    /**
     * This alarm will scale the number of instance up when the CPU metric goes above 60% for 5 minutes.
     * To scale your instances down change the Comparision Operator to be LessThanThreshold and set your
     * threshold to be 40 for example.
     * @param cwc the cloud watch client we use
     */
    public void alarmUpbound(AmazonCloudWatchClient cwc, String arn){
        String upArn = arn; // from the policy request

        // Scale Up
        PutMetricAlarmRequest upRequest = new PutMetricAlarmRequest();
        upRequest.setAlarmName("AlarmName-up");
        upRequest.setMetricName("CPUUtilization");

        List dimensions = new ArrayList();
        Dimension dimension = new Dimension();
        dimension.setName("AutoScalingGroupName");
        dimension.setValue("ASG");
        dimensions.add(dimension);
        upRequest.setDimensions(dimensions);

        upRequest.setNamespace("AWS/EC2");
        upRequest.setComparisonOperator(ComparisonOperator.GreaterThanOrEqualToThreshold);
        upRequest.setStatistic(Statistic.Average);
        upRequest.setUnit(StandardUnit.Percent);
        upRequest.setThreshold(85d);
        upRequest.setPeriod(60);
        upRequest.setEvaluationPeriods(2);

        List actions = new ArrayList();
        actions.add(upArn); // This is the value returned by the ScalingPolicy request
        upRequest.setAlarmActions(actions);

        cwc.putMetricAlarm(upRequest);
    }

    public void alarmLowbound(AmazonCloudWatchClient cwc, String arn){
        String upArn = arn; // from the policy request

        // Scale Up
        PutMetricAlarmRequest downRequest = new PutMetricAlarmRequest();
        downRequest.setAlarmName("AlarmName-down");
        downRequest.setMetricName("CPUUtilization");

        List dimensions = new ArrayList();
        Dimension dimension = new Dimension();
        dimension.setName("AutoScalingGroupName");
        dimension.setValue("ASG");
        dimensions.add(dimension);
        downRequest.setDimensions(dimensions);

        downRequest.setNamespace("AWS/EC2");
        downRequest.setComparisonOperator(ComparisonOperator.LessThanOrEqualToThreshold);
        downRequest.setStatistic(Statistic.Average);
        downRequest.setUnit(StandardUnit.Percent);
        downRequest.setThreshold(40d);
        downRequest.setPeriod(60);
        downRequest.setEvaluationPeriods(3);

        List actions = new ArrayList();
        actions.add(upArn); // This is the value returned by the ScalingPolicy request
        downRequest.setAlarmActions(actions);

        cwc.putMetricAlarm(downRequest);
    }

    /**
     * @para AmazonElasticLoadBalancingClient elb the elastic load balance client we use
     * @para AmazonEC2Client ec2 the EC2 Client that we use
     * @return instance elastic load balance that this method launched
     * This method launch a elastic load balance
     */
    public void registerInstances(AmazonElasticLoadBalancingClient elb, AmazonEC2Client ec2) {

        //get the running instances
        DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
        List<Reservation> reservations = describeInstancesRequest.getReservations();
        List<Instance> instances = new ArrayList<Instance>();

        for (Reservation reservation : reservations) {
            instances.addAll(reservation.getInstances());
        }


        //get instance id's
        String id;
        List instanceId=new ArrayList();
        List instanceIdString=new ArrayList();
        Iterator<Instance> iterator=instances.iterator();
        while (iterator.hasNext())
        {
            id=iterator.next().getInstanceId();
            instanceId.add(new com.amazonaws.services.elasticloadbalancing.model.Instance(id));
            instanceIdString.add(id);
        }


        //register the instances to the balancer
        RegisterInstancesWithLoadBalancerRequest register =new RegisterInstancesWithLoadBalancerRequest();
        register.setLoadBalancerName("loader");
        register.setInstances((Collection)instanceId);
        RegisterInstancesWithLoadBalancerResult registerWithLoadBalancerResult= elb.registerInstancesWithLoadBalancer(register);
    }

}
