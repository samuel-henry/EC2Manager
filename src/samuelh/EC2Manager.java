package samuelh;

import java.util.Scanner;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Volume;

/**
 * Implementation of basic EC2 actions:
 * 		List EC2 instances
 * 		Select an EC2 instance
 * 		List EBS volumes attached to an EC2 instance
 * 		List unattached EBS volumes
 * 		Attach an EBS volume to an EC2 instance
 * 		Detach an EBS volume from an EC2 instance
 */
public class EC2Manager {
	private static final String LINE_SEPARATOR = "------------------";
	private static final String SPC_STR = " ";
	
	private static Scanner scn = new Scanner(System.in); //used to read user input
	private static AmazonEC2 ec2client = null; //an EC2 client to handle requests
	private static Instance currentInstance = null; //hold the user's selected EC2 instance
	
	public static void main(String[] args) {
		//welcome the user and give them a chance to edit environment variables before continuing
		System.out.println();
		System.out.println(LINE_SEPARATOR);
		System.out.println("Welcome to the EC2 Manager");
		System.out.println(LINE_SEPARATOR);
		System.out.println();
		System.out.println("Make sure you have edited your environment variables to include your AWS access keys before continuing***");
		System.out.println();
		System.out.println("Press enter to continue...");
		System.out.println(LINE_SEPARATOR);
		
		//wait for enter
		scn.nextLine();
		
		//get an ec2client
		ec2client = getEC2Client();
				
		//show options
		while (true) {
			System.out.println();
			System.out.println("Please select an option below by entering the corresponding number and pressing enter");
			System.out.println();
			System.out.println("0 Exit the program");
			System.out.println("1 List EC2 instances");
			System.out.println("2 Select an instance");
			System.out.println("3 List attached EBS volumes");
			System.out.println("4 Detach an EBS volume from the selected instance");
			System.out.println("5 List unattached EBS volumes");
			System.out.println("6 Attach a volume to the selected instance");
			System.out.println();
			
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
			
			//get an EC2 client instance from the user's credentials
			myEC2client = new AmazonEC2Client(myCredentials); 
		} catch (Exception ex) {
			System.out.println("There was a problem reading your credentials.");
			System.out.println("Please make sure you have updated your environment variables with your AWS credentials and restart.");
			System.exit(0);
		}
		
		return myEC2client;
	}

	/********************************************************************
	* Handle the user's choice of options from main screen
	*********************************************************************/
	private static void handleUserChoice(String userInput) {
		System.out.println();
		
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
	
	/********************************************************************
	* List the user's EC2 instances
	*********************************************************************/
	private static void listEC2Instances() {
		try {
			//get summary information for the user's EC2 instances
			DescribeInstancesResult results = ec2client.describeInstances();
			
			//display the summary information
			System.out.println();
			System.out.println("Your instances:");
			for (Reservation rslt : results.getReservations()) {
				for (Instance inst : rslt.getInstances()) {
					printBriefInstanceDescription(inst);
				}
			}
			System.out.println();
		} catch (Exception ex) {
			System.out.println("There was a problem retrieving your list of EC2 instances. Please try again.");
		}
	}
	
	/********************************************************************
	* Let the user select an EC2 instance
	*********************************************************************/
	private static void selectEC2Instance() {
		//request the id of an EC2 instance the user would like to work with
		System.out.println("Please enter the ID of the instance you would like to select:");
		String instanceId = scn.nextLine();
		try {
			//get the specified instance and set it to static variable
			DescribeInstancesResult results = ec2client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
			for (Reservation rslt : results.getReservations()) {
				for (Instance inst : rslt.getInstances()) {
					if (inst.getInstanceId().equals(instanceId)) {
						currentInstance = inst;
						System.out.print("Successfully selected instance: " + instanceId);
						System.out.println();
					}
				}
			}
		} catch (Exception ex) {
			System.out.println("There was a problem selecting instance " + instanceId + 
					". Please verify that this instance ID is correct and try again.");
		}
	}
	
	/********************************************************************
	* List the EBS volumes attached to the selected instance
	*********************************************************************/
	private static void listAttachedEBSVolumes() {
		//make sure user has selected an instance
		if (currentInstance == null) {
			System.out.println("You have not selected an instance. Please select an instance and try again");
			return;
		}
		
		//list the EBS volumes attached to the selected instance
		System.out.println("Listing EBS Volumes attached to EC2 instance " + currentInstance.getInstanceId() + "...");
		try {
			for (InstanceBlockDeviceMapping blockDevice : currentInstance.getBlockDeviceMappings()) {
				if (blockDevice.getEbs() != null) {
					System.out.print("ID: " + blockDevice.getEbs().getVolumeId() + SPC_STR);
					System.out.println("STATE: " + blockDevice.getEbs().getStatus());
				}
			}
		} catch (Exception ex) {
			System.out.println("There was a problem listing the EBS volumes attached to EC2 instance " + currentInstance.getInstanceId() + " . Please try again.");
		}
	}

	/********************************************************************
	* Detach a specified EBS volume from the selected instance
	*********************************************************************/
	private static void detachEBSVolumeFromSelectedInstance() {
		//make sure user has selected an instance
		if (currentInstance == null) {
			System.out.println("You have not selected an instance. Please select an instance and try again");
			return;
		}
		
		//let the user detach a volume from the selected instance
		try {
			//request the id of the volume to detach
			System.out.println("Enter the volume ID to detach from instance " + currentInstance.getInstanceId());
			String volumeId = scn.nextLine();
			
			//submit the detach volume request
			ec2client.detachVolume(new DetachVolumeRequest().withVolumeId(volumeId));
			
			//alert the user that the volume is being detached and they should re-run the program to see updated volume state
			System.out.println("Detaching volume " + volumeId + " from instance " + currentInstance.getInstanceId());
			System.out.println("Detaching may take a moment. Please monitor the state of this volume by listing the volumes attached to this instance.");
			System.out.println("Please monitor the state of this volume by listing the volumes attached/detached to this instance the next time you run EC2Manager.");
			System.out.println("Goodbye.");
			System.exit(0);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			System.out.println("There was a problem. Please verify that the volume ID is correct and try again.");
		}
	}

	/********************************************************************
	* List unattached EBS volumes
	*********************************************************************/
	private static void listUnattachedEBSVolumes() {
		//list the user's unattached EBS volumes
		System.out.println("Listing unattached volumes...");
		try {
			//print info for EBS volumes that are not currently attached
			for (Volume vol : ec2client.describeVolumes().getVolumes()) {
				if (vol.getAttachments().size() == 0) {
					System.out.print("ID: " + vol.getVolumeId() + SPC_STR);
					System.out.println("STATE: " + vol.getState());
				}
			}
		} catch (Exception ex) {
			System.out.println("There was a problem listing unattached volumes. Please try again.");
		}
	}
	
	/********************************************************************
	* Attach a specified EBS volume to the selected instance
	*********************************************************************/
	private static void attachEBSVolumeToSelectedInstance() {
		//make sure user has selected an instance
		if (currentInstance == null) {
			System.out.println("You have not selected an instance. Please select an instance and try again");
			return;
		}
		
		//request the id of an EBS volume to attach to the selected instance
		System.out.println("Enter the ID of the EBS volume to attach to EC2 instance " + currentInstance.getInstanceId() + ":");
		String volumeId = scn.nextLine();
		
		try {
			//attach the specified EBS volume to the selected instance
			ec2client.attachVolume(new AttachVolumeRequest().withInstanceId(currentInstance.getInstanceId()).withVolumeId(volumeId).withDevice("/dev/sdf"));
			
			//alert the user that we are attaching the volume and they should re-run the program to see updated volume state
			System.out.println("Attaching volume " + volumeId + " to instance " + currentInstance.getInstanceId());
			System.out.println("Attaching may take a few minutes.");
			System.out.println("Please monitor the state of this volume by listing the volumes attached/detached to this instance the next time you run EC2Manager.");
			System.out.println("Goodbye.");
			System.exit(0);
		} catch (Exception ex) {
			System.out.println("There was a problem attaching " + volumeId + " to EC2 instance " + currentInstance.getInstanceId());
			System.out.println("Please verify the volume name and try again.");
		}
	}

	/********************************************************************
	* Print desired summary information for an EC2 instance
	*********************************************************************/
	private static void printBriefInstanceDescription(Instance anInstance) {
		System.out.print("ID: " + anInstance.getInstanceId() + SPC_STR);
		System.out.print("NAME: " + anInstance.getKeyName() + SPC_STR);
		System.out.print("TYPE: " + anInstance.getInstanceType() + SPC_STR);
		System.out.println("ZONE: " + anInstance.getPlacement().getAvailabilityZone() + SPC_STR);
	}
	
}
