Summary:

Lets you interatively:
- List your EC2 instances
- Select an EC2 instance
- List attached EBS volumes
- Detach an EBS volume from an EC2 instance
- List unattached EBS volumes
- Attach an EBS volume to an EC2 instance

Instructions:
To install/run from a running Amazon Red Hat Linux Image:
  1.   Install git:
	     sudo yum install git
  2. 	Update environment variables:
      vi ~/.bash_profile
   	  Add these lines with your keys in the placeholders:
         export AWS_ACCESS_KEY_ID=<YOUR-ACCESS-KEY>
         export AWS_SECRET_KEY=<YOUR-SECRET-KEY>     
   	  Exit and save (esc, shift :wq, enter)
   	  source ~/.bash_profile (to refresh environment variables)      
  3. 	Clone this git repository:
	    git clone https://github.com/samuel-henry/EC2Manager
  4. 	Enter the program directory
	    cd EC2Manager
  5. 	Run the included executable jar:
	    java -jar EC2Manager.jar
