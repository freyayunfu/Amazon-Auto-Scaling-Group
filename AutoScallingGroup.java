import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * An Auto Scaling group defined the launch configuration, policy, alarm and the features of the auto scaling group.
 * Created by fuyun on 11/02/15.
 */
public class AutoScallingGroup {


    public static List<Runnable> createAutoScalingGroup(final AmazonAutoScalingClient asClient, final AmazonEC2Client ec2, final AmazonCloudWatchClient cwc){
        final List<Runnable> cleanupTasks = new ArrayList<Runnable>();

        //create launch configuration
        final CreateLaunchConfigurationRequest lcRequest = new CreateLaunchConfigurationRequest();

        lcRequest.setLaunchConfigurationName("yunLaunchConfiguration");
        lcRequest.setImageId("ami-7c0a4614");
        lcRequest.setInstanceType("m1.small");
        lcRequest.withKeyName("project11");
        lcRequest.withSecurityGroups("yunSecurityGroup");


        com.amazonaws.services.autoscaling.model.InstanceMonitoring monitoring = new com.amazonaws.services.autoscaling.model.InstanceMonitoring();
        monitoring.setEnabled(Boolean.TRUE);
        lcRequest.setInstanceMonitoring(monitoring);
        asClient.createLaunchConfiguration(lcRequest);

        cleanupTasks.add( new Runnable() {
            @Override
            public void run() {
                System.out.println("Deleting launch configuration: " + lcRequest.getLaunchConfigurationName());
                asClient.deleteLaunchConfiguration( new DeleteLaunchConfigurationRequest().withLaunchConfigurationName( lcRequest.getLaunchConfigurationName()) );
            }
        } );



        //create auto scaling group
        final CreateAutoScalingGroupRequest asgRequest = new CreateAutoScalingGroupRequest();
        asgRequest.setAutoScalingGroupName("ASG");
        asgRequest.setLaunchConfigurationName("yunLaunchConfiguration"); // as above

        asgRequest.setMinSize(3);  // disabling it for the moment
        asgRequest.setMaxSize(11); //  disabling it for the moment


        asgRequest.withLoadBalancerNames("loader")
                .withAvailabilityZones("us-east-1b")
                .withHealthCheckType("ELB")
                .withHealthCheckGracePeriod(120)
                .withDefaultCooldown(120)
                .withTags(new Tag().withKey("Project").withValue("2.2"));

        asClient.createAutoScalingGroup(asgRequest);


        cleanupTasks.add( new Runnable() {
            @Override
            public void run() {
                System.out.println("Deleting group: " + "ASG");
                asClient.deleteAutoScalingGroup( new DeleteAutoScalingGroupRequest().withAutoScalingGroupName("ASG").withForceDelete( true ) );
            }
        } );

        cleanupTasks.add( new Runnable() {
            @Override
            public void run() {
                final List<String> instanceIds = getInstancesForGroup( ec2, "ASG", null );
                System.out.println("Terminating instances: " + instanceIds);
                ec2.terminateInstances( new TerminateInstancesRequest().withInstanceIds( instanceIds ) );
            }
        } );

        // Create policy
        System.out.println("Creating auto scaling policy " + "UpBoundPolicy");
        final PutScalingPolicyResult putScalingPolicyResult1 =
                asClient.putScalingPolicy(new PutScalingPolicyRequest()
                        .withAutoScalingGroupName("ASG")
                        .withPolicyName("UpboundPolicy")
                        .withAdjustmentType("ChangeInCapacity")
                        .withScalingAdjustment(2));
        final String policyArn1 = putScalingPolicyResult1.getPolicyARN();
        System.out.println("Using policy ARN: " + policyArn1);

        // Register cleanup for auto scaling group
        final String policyName1 = "UpboundPolicy";
        cleanupTasks.add(new Runnable() {
            @Override
            public void run() {
                System.out.println("Deleting policy: " + policyName1);
                asClient.deletePolicy(new DeletePolicyRequest().withAutoScalingGroupName("ASG").withPolicyName(policyName1));
            }
        });

        // Create alarm
        System.out.println("Creating alarm " + "UpBoundAlarm");
        cwc.putMetricAlarm(new PutMetricAlarmRequest()
                .withAlarmName("UpBoundAlarm")
                .withNamespace("AWS/EC2")
                .withDimensions(new Dimension().withName("AutoScalingGroupName").withValue("ASG"))
                .withMetricName("CPUUtilization")
                .withComparisonOperator(ComparisonOperator.GreaterThanOrEqualToThreshold)
                .withEvaluationPeriods(2)
                .withStatistic(Statistic.Average)
                .withThreshold(77d)
                .withPeriod(60)
                .withAlarmActions(policyArn1));

        // Register cleanup for metric alarm
        final String alarmName1 ="UpAlarm";
        cleanupTasks.add(new Runnable() {
            @Override
            public void run() {
                System.out.println("Deleting alarm: " + policyName1);
                cwc.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(alarmName1));
            }
        });

        // Create policy
        System.out.println("Creating auto scaling policy " + "LowBoundPolicy");
        final PutScalingPolicyResult putScalingPolicyResult2 =
                asClient.putScalingPolicy(new PutScalingPolicyRequest()
                        .withAutoScalingGroupName("ASG")
                        .withPolicyName("LowBoundPolicy")
                        .withAdjustmentType("ChangeInCapacity")
                        .withScalingAdjustment(-2));
        final String policyArn2 = putScalingPolicyResult2.getPolicyARN();
        System.out.println("Using policy ARN: " + policyArn2);

        // Register cleanup for auto scaling group
        final String policyName2 = "LowBoundPolicy";
        cleanupTasks.add(new Runnable() {
            @Override
            public void run() {
                System.out.println("Deleting policy: " + policyName2);
                asClient.deletePolicy(new DeletePolicyRequest().withAutoScalingGroupName("ASG").withPolicyName(policyName2));
            }
        });

        // Create alarm
        System.out.println("Creating alarm " + "LowAlarm");
        cwc.putMetricAlarm(new PutMetricAlarmRequest()
                .withAlarmName("LowAlarm")
                .withAlarmActions(policyArn2)
                .withNamespace("AWS/EC2")
                .withMetricName("CPUUtilization")
                .withComparisonOperator(ComparisonOperator.LessThanOrEqualToThreshold)
                .withEvaluationPeriods(3)
                .withStatistic(Statistic.Minimum)
                .withThreshold(40d)
                .withPeriod(60)
                .withDimensions(new Dimension().withName("AutoScalingGroupName").withValue("ASG")));

        // Register cleanup for metric alarm
        final String alarmName2 ="LowAlarm";
        cleanupTasks.add(new Runnable() {
            @Override
            public void run() {
                System.out.println("Deleting alarm: " + policyName2);
                cwc.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(alarmName2));
            }
        });



        return cleanupTasks;
    }

    private static List<String> getInstancesForGroup(final AmazonEC2 ec2,
                                                     final String groupName,
                                                     final String status) {
        final DescribeInstancesResult instancesResult = ec2.describeInstances( new DescribeInstancesRequest().withFilters(
                new Filter().withName( "tag:aws:autoscaling:groupName" ).withValues( groupName )
        ) );
        final List<String> instanceIds = new ArrayList<String>();
        for ( final Reservation reservation : instancesResult.getReservations() ) {
            for ( final Instance instance : reservation.getInstances() ) {
                if ( status == null || instance.getState()==null || status.equals( instance.getState().getName() ) ) {
                    instanceIds.add( instance.getInstanceId() );
                }
            }
        }
        return instanceIds;
    }





}
