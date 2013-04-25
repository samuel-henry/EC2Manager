package samuelh;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeAttachment;
import com.amazonaws.services.ec2.model.VolumeAttachmentState;

public class EC2Manager {
	private static final String LINE_SEPARATOR = "------------------";
	private static final String SPC_STR = " ";
	
	private static Scanner scn = new Scanner(System.in);
	private static AmazonEC2 ec2client = null;
	private static Instance currentInstance = null;
	
	public static void main(String[] args) {
		//welcome the user and give them a chance to edit environment variables before continuing
		System.out.println("Welcome to the EC2 Manager");
		System.out.println(LINE_SEPARATOR);
		System.out.println("***Make sure you have edited your environment variables to include your AWS access keys before continuing***");
		System.out.println(LINE_SEPARATOR);
		System.out.println("Press enter to continue...");
		
		//wait for enter
		scn.nextLine();
		
		//get an ec2client
		ec2client = getEC2Client();
		
		//formatting
		System.out.println(LINE_SEPARATOR);
		System.out.println();
				
		//show options
		while (true) {
			System.out.println("Please select an option below by entering the corresponding number and pressing enter");
			System.out.println();
			System.out.println("0 Exit the program");
			System.out.println("1 List EC2 instances");
			System.out.println("2 Select an instance");
			System.out.println("3 List attached EBS volumes");
			System.out.println("4 Detach an EBS volume from the selected instance");
			System.out.println("5 List unattached EBS volumes");
			System.out.println("6 Attach a volume to the selected instance");
			
			//call the operation corresponding to the user's choice
			handleUserChoice(scn.nextLine());
		}
		
	}

	/********************************************************************
	* Get an EC2 client using the user's credentials
	*********************************************************************/
	private static AmazonEC2 getEC2Client() {
		AWSCredentials myCredentials;
		AmazonEC2 myEC2client = null;
		
		try {
			//get credentials from environment variables
			myCredentials = new EnvironmentVariableCredentialsProvider().getCredentials();
			myEC2client = new AmazonEC2Client(myCredentials); 
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			System.out.println("There was a problem reading your credentials.");
			System.out.println("Please make sure you have updated your environment variables with your AWS credentials and restart.");
			System.exit(0);
		}
		
		return myEC2client;
	}

	/********************************************************************
	* Handle the user's choice of options from main screen
	* 		System.out.println("0 Exit the program");
			System.out.println("1 List EC2 instances");
			System.out.println("2 Select an instance");
			System.out.println("3 List attached EBS volumes");
			System.out.println("4 Detach an EBS volume from the selected instance");
			System.out.println("5 List unattached EBS volumes");
			System.out.println("6 Attach a volume to the selected instance");
	*********************************************************************/
	private static void handleUserChoice(String userInput) {
		//initialize choice to an invalid option
		int choice = -1;
		
		try {
			//get the user's choice from the console
			choice = Integer.parseInt(userInput);
		} catch (NumberFormatException ex) {
			System.out.println(userInput + " is invalid. Please enter only the number of the option you want");
		}
		
		//handle the user's choice
		switch(choice) {
		case 0:
			//terminate the program
			System.out.println();
			System.out.println("Thank you for using EC2 Manager. Goodbye.");
			System.exit(0);
			break;
		case 1:
			//list EC2 instances
			listEC2Instances();
			break;
		case 2:
			//select an EC2 instance
			selectEC2Instance();
			break;
		case 3:
			//list attached EBS volumes
			listAttachedEBSVolumes();
			break;
		case 4:
			//detach an EBS volume from the selected instance
			detachEBSVolumeFromSelectedInstance();
			break;
		case 5:
			//list unattached EBS volumes
			listUnattachedEBSVolumes();
			break;
		case 6:
			//attach an EBS volume to the selected instance
			attachEBSVolumeToSelectedInstance();
			break;
		default:
			System.out.println("You did not enter a valid option. Please enter one of the numbers given");
		}
	}

	private static void attachEBSVolumeToSelectedInstance() {
		if (currentInstance == null) {
			System.out.println("You have not selected an instance. Please select an instance and try again");
			return;
		}
		System.out.println("Enter the ID of the EBS volume to attach to EC2 instance " + currentInstance.getInstanceId() + ":");
		String volumeId = scn.nextLine();
		try {
			Volume volumeToAttach = new Volume().withVolumeId(volumeId);
			Collection<VolumeAttachment> volumeAttachments = new ArrayList<VolumeAttachment>();
			volumeAttachments.add(new VolumeAttachment().withInstanceId(currentInstance.getInstanceId()));
			volumeToAttach.setAttachments(volumeAttachments);
		} catch (Exception ex) {
			System.out.println("There was a problem attaching " + volumeId + " to EC2 instance " + currentInstance.getInstanceId());
			System.out.println("Please verify the volume name and try again.");
		}
	}

	private static void listUnattachedEBSVolumes() {
		System.out.println("Listing unattached volumes...");
		try {
			for (Volume vol : ec2client.describeVolumes().getVolumes()) {
				if (vol.getAttachments().size() == 0) {
					System.out.println(vol.getVolumeId());
				}
			}
		} catch (Exception ex) {
			System.out.println("There was a problem listing unattached volumes. Please try again.");
		}
	}

	private static void detachEBSVolumeFromSelectedInstance() {
		if (currentInstance == null) {
			System.out.println("You have not selected an instance. Please select an instance and try again");
			return;
		}
		try {
			System.out.println("Enter the volume ID to detach from instance " + currentInstance.getInstanceId());
			String volumeId = scn.nextLine();
			VolumeAttachment volAttachment = new VolumeAttachment().withInstanceId(currentInstance.getInstanceId()).withVolumeId(volumeId);
			volAttachment.setState(VolumeAttachmentState.Detached);
		} catch (Exception ex) {
			System.out.println("There was a problem. Please verify that the volume ID is correct and try again.");
		}
	}

	private static void listAttachedEBSVolumes() {
		if (currentInstance == null) {
			System.out.println("You have not selected an instance. Please select an instance and try again");
			return;
		}
		System.out.println("Listing EBS Volumes attached to EC2 instance " + currentInstance.getInstanceId() + "...");
		try {
			for (InstanceBlockDeviceMapping blockDevice : currentInstance.getBlockDeviceMappings()) {
				if (blockDevice.getEbs() != null) {
					System.out.println(blockDevice.getEbs().getVolumeId());
				}
			}
		} catch (Exception ex) {
			System.out.println("There was a problem listing the EBS volumes attached to EC2 instance " + currentInstance.getInstanceId() + " . Please try again.");
		}
	}

	private static void selectEC2Instance() {
		System.out.println("Please enter the ID of the instance you would like to select:");
		String instanceID = scn.nextLine();
		try {
			currentInstance = new Instance().withInstanceId(instanceID);
			System.out.println("Successfully selected instance: " + getBriefInstanceDescription(currentInstance));
		} catch (Exception ex) {
			System.out.println("There was a problem selecting instance " + instanceID + 
					". Please verify that this instance ID is correct and try again.");
		}
	}

	private static String getBriefInstanceDescription(Instance anInstance) {
		System.out.print("ID: " + anInstance.getInstanceId() + SPC_STR);
		System.out.print("NAME: " + anInstance.getKeyName() + SPC_STR);
		System.out.print("TYPE: " + anInstance.getInstanceType() + SPC_STR);
		System.out.println("ZONE: " + anInstance.getPlacement().getAvailabilityZone() + SPC_STR);
		return null;
	}

	private static void listEC2Instances() {
		try {
			DescribeInstancesResult results = ec2client.describeInstances();
			System.out.println();
			System.out.println("Your instances:");
			for (Reservation rslt : results.getReservations()) {
				for (Instance inst : rslt.getInstances()) {
					System.out.println(getBriefInstanceDescription(inst));
				}
			}
			System.out.println();
		} catch (Exception ex) {
			System.out.println("There was a problem retrieving your list of EC2 instances. Please try again.");
		}
	}
}
