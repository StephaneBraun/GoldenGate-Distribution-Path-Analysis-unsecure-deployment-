# GoldenGate-Distribution-Path-Analysis-unsecure-deployment-
Automatic recovery of GoldenGate Microservices Distribution Paths statistics (unsecured deployment)

This utility allows you to monitor Oracle GoldenGate Microservices distribution paths in unsecure deployment mode.
The GoldenGate Distribution service presents the data transfer activity contained in the trails in real time, but it does not offer historical metrics.
Here, it will be possible to keep for analysis the following information from the distribution paths of all the deployments of a service manager:

- Date and time of measurement
- Deployment name
- Distribution Path Name
- Status
- Lag
- Number of inserts
- Number of updates
- Number of deletes
- Total number of rows
- sendWaitTime
- recvWaitTime
- totalBytesSent
- totalMsgsSent
- Trail number in: inputSequence
- Rba of trail in: inputOffset
- Trail out number: outputSequence
- Rba of trail out: outputOffset


This tool connects to a GoldenGate Service Manager and will go through all the deployments to analyze all the distribution paths of all these deployments.

Prerequisites:

    JDK 1.8 minimum
    Have the oggConnect.properties file in the same directory as the jar file
  
  
The oggConnect.properties file contains the following settings:

    oggUrl: http connection string to the GoldenGate Service Manager
    defaultDir: directory for generating the Distribution Path statistics file

Example :

    oggUrl=http://localhost:5000
    defaultDir=/u01/goldengate/temp


Two ways to launch the program:

- With prompt for mandatory paramters :

       java -jar goldenGateDistributionPath.jar

  And enter the user and password of the GoldenGate Service Manager, the name of the properties file and the statistics refresh frequency in seconds
  If the frequency is less than 10 seconds, it will automatically be repositioned to 10 seconds.

- With the parameters in the command line :

        java -jar goldenGateDistributionPath.jar OGG_user OGG_pwd Properties_file_without_extension_.properties Frequency_in_seconds

  Example :

        java -jar goldenGateDistributionPath.jar oggadmin oggadmin oggConnect 30

Result file: in the directory declared by the defaultDir variable, the file name will be in the form:

    OGG_DISTRIBUTION_PATH_YYYY_MM_DD_HH_MI_SS.csv


To stop running the utility, click Ctrl+C
