import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * A Launch Configuration template should be defined, which includes the AMI ID,
 * instance types, key pairs and security group information, among others.
 * As Auto Scaling is meant to scale automatically based on application demands;
 * i.e. the instance should be configured to automatically start the required
 * application services and work seamlessly on launch.
 *
 * Created by fuyun on 11/02/15.
 */
public class ELB {




    /**
     * @para AmazonElasticLoadBalancingClient elb the elastic load balance client we use
     * This method creates a security group.
     */
    public static void createSecurityGroup(AmazonEC2Client ec2) {
        CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest();
        csgr.withGroupName("yunSecurityGroup").withDescription("My security group");
        ec2.createSecurityGroup(csgr);
        IpPermission ipPermission = new IpPermission();
        ipPermission.withIpRanges(new String[]{"0.0.0.0/0"}).withIpProtocol("tcp").withFromPort(Integer.valueOf(80)).withToPort(Integer.valueOf(80));
        AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest();
        authorizeSecurityGroupIngressRequest.withGroupName("yunSecurityGroup").withIpPermissions(new IpPermission[]{ipPermission});
        ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
    }



    /**
     * @para AmazonElasticLoadBalancingClient elb the elastic load balance client we use
     * @return instance elastic load balance that this method launched
     * This method launch a elastic load balance
     */

    public String launchLB(AmazonElasticLoadBalancingClient elb) throws InterruptedException {
        //create load balancer
        CreateLoadBalancerRequest lbRequest = new CreateLoadBalancerRequest();
        lbRequest.setLoadBalancerName("loader");
        List<Listener> listeners = new ArrayList<Listener>(1);
        listeners.add(new Listener("HTTP", 80, 80));
        lbRequest.withAvailabilityZones("us-east-1b");
        lbRequest.setListeners(listeners);
        lbRequest.withSecurityGroups("sg-0b7ad86f");
        lbRequest.withTags(new com.amazonaws.services.elasticloadbalancing.model.Tag().withKey("Project").withValue("2.2"));

        CreateLoadBalancerResult lbResult = elb.createLoadBalancer(lbRequest);

        Thread.sleep(10000);

        String elasticLoadBalancingDNS = lbResult.getDNSName();
        System.out.println("Created load balancer.");


        return elasticLoadBalancingDNS;



    }





    /**
     * @return instance load generator that this method launched
     * This method launch a load generator
     * @para AmazonEC2Client ec2 the client we use
     */
    public static com.amazonaws.services.ec2.model.Instance launchLG(AmazonEC2Client ec2) throws InterruptedException {

        //Create Instance Request
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        //Configure Instance Request
        runInstancesRequest.withImageId("ami-ae0a46c6")
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

        while (!instance.getState().getName().equals("running")) {
            Thread.sleep(10000);
            instance = RunInstance.getInstanceFromInstanceId(ec2, instance.getInstanceId());
        }


        //Add a Tag to the instance:
        CreateTagsRequest createTagsRequest = new CreateTagsRequest();
        createTagsRequest.withResources(instance.getInstanceId())
                .withTags(new Tag("Project", "2.2"));
        ec2.createTags(createTagsRequest);


        instance = RunInstance.getInstanceFromInstanceId(ec2, instance.getInstanceId());

        return instance;
    }



}
