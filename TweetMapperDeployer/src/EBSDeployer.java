

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityRequest;
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityResult;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionResult;
import com.amazonaws.services.elasticbeanstalk.model.CreateConfigurationTemplateRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateConfigurationTemplateResult;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

public class EBSDeployer {
	
	private static final String BUCKET_NAME = "cloud.tweetmap";
	private static final String BUCKET_KEY = "artifact/TweetMapper.war";
	
	private static final String APP_NAME = "TweetMapperApp";
	private static final String VERSION = "1.0.0";
	private static final String ENV_NAME = "TweetMapperAppEnv";
	private static final String CONFIG_NAME = "TweetMapConfigTemplate";
	
	public static void main(String[] args) {
		EBSDeployer instance = new EBSDeployer();
		try {
			instance.deploy();
		}
		catch(Exception e) {
			System.err.println("could not deploy the TweetMapper instance on EBS");
		}
	}
	
	private void deploy() throws IOException {
		
		try {
			AWSCredentials credentials = new PropertiesCredentials(
					EBSDeployer.class.getResourceAsStream("credentials.properties"));
			
			AmazonS3Client s3Client = new AmazonS3Client(credentials);
			
			if(s3Client.doesBucketExist(BUCKET_NAME)) {
				s3Client.deleteObject(BUCKET_NAME, BUCKET_KEY);
				s3Client.deleteBucket(BUCKET_NAME);
			}
			
			
			s3Client.createBucket(BUCKET_NAME);
			PutObjectRequest req = new PutObjectRequest(
					BUCKET_NAME,
					BUCKET_KEY,
					new File("TweetMapper.war"));
			PutObjectResult res = s3Client.putObject(req);
			
			AWSElasticBeanstalkClient ebsClient = new AWSElasticBeanstalkClient(credentials);
			
			CreateApplicationVersionRequest createAppRequest = new CreateApplicationVersionRequest();
			createAppRequest.setApplicationName(APP_NAME);
			createAppRequest.setVersionLabel(VERSION);
			createAppRequest.setAutoCreateApplication(true);
			createAppRequest.setDescription("App to display tweets on heatmap");
			createAppRequest.setSourceBundle(new S3Location(BUCKET_NAME, BUCKET_KEY));
		
		
			CreateApplicationVersionResult result = ebsClient.createApplicationVersion(createAppRequest);
			if(result.getApplicationVersion().getApplicationName().equals(APP_NAME)) {
				System.out.println("App deployed successfully");
			}
			
			String cnamePrefix = APP_NAME;
			int i = 0;
			while(true) {
				CheckDNSAvailabilityRequest dnsRequest = new CheckDNSAvailabilityRequest();
				dnsRequest.setCNAMEPrefix(cnamePrefix + i);
				CheckDNSAvailabilityResult dnsResult = ebsClient.checkDNSAvailability(dnsRequest);
				if (dnsResult.isAvailable()) {
					System.out.println("DNS is available for prefix:"+ cnamePrefix + i);
					break;
				}
			}
			
			
			DescribeApplicationVersionsRequest descRequest = new DescribeApplicationVersionsRequest();
			descRequest.setApplicationName(APP_NAME);
			descRequest.setVersionLabels(Collections.singleton(VERSION));
			DescribeApplicationVersionsResult descResult = ebsClient
					.describeApplicationVersions(descRequest);
			
			//ListAvailableSolutionStacksResult listRes = ebsClient.listAvailableSolutionStacks();
			//for(String s: listRes.getSolutionStacks())
			//	System.out.println(s);
			
			CreateConfigurationTemplateRequest templateRequest = new CreateConfigurationTemplateRequest();
			templateRequest.setApplicationName(APP_NAME);
			templateRequest.setTemplateName(CONFIG_NAME);
			templateRequest.setSolutionStackName("64bit Amazon Linux 2014.09 v1.2.0 running Tomcat 8 Java 8");
			CreateConfigurationTemplateResult templateResult = ebsClient
					.createConfigurationTemplate(templateRequest);
					
			CreateEnvironmentRequest envRequest = new CreateEnvironmentRequest();
			envRequest.setEnvironmentName(ENV_NAME);
			envRequest.setVersionLabel(VERSION);
			envRequest.setDescription("Env for tweet map app");
			envRequest.setTemplateName(CONFIG_NAME);
			envRequest.setApplicationName(APP_NAME);
			envRequest.setCNAMEPrefix(cnamePrefix+i);
			
			ConfigurationOptionSetting setting = new ConfigurationOptionSetting();
			setting.setNamespace("aws:autoscaling:launchconfiguration");
			setting.setOptionName("EC2KeyName");
			setting.setValue("cloud1.pem");
			envRequest.setOptionSettings(Collections.singleton(setting));
			
			CreateEnvironmentResult envResult = ebsClient.createEnvironment(envRequest);
			/*while(true) {
				String status = envResult.getStatus();
				System.out.println("status:" + status);
				if(status.equals("Ready"))
					break;
				Thread.sleep(30000);
			}*/
			
			DescribeEnvironmentsRequest descEnvRequest = new DescribeEnvironmentsRequest();
			descEnvRequest.setEnvironmentNames(Collections.singleton(ENV_NAME));
			DescribeEnvironmentsResult descEnvResult = ebsClient.describeEnvironments(descEnvRequest);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	
}
