## Getting started with ELs and custom Nifi processors on HDP

This tutorial is part of a webinar on Hortonworks DataFlow.
- http://hortonworks.com/partners/learn/

#### Background

Tcpdump is a common packet analyzer that runs under the command line. It allows the user to display TCP/IP and other packets being transmitted or received over a network to which the computer is attached. 

#### Goals 
- Build Nifi flow to run tcpdump. Use Expression Language to filter out source/target IPs 
- Build and use custom tcpdump processor to filter source/target IPs



#### Pre-Requisites: Install Nifi on sandbox

- The lab is designed for the HDP Sandbox. Download the HDP Sandbox [here](http://hortonworks.com/products/hortonworks-sandbox/#install), import into VMWare Fusion and start the VM
- After it boots up, find the IP address of the VM and add an entry into your machines hosts file e.g.
```
192.168.191.241 sandbox.hortonworks.com sandbox    
```
- Connect to the VM via SSH (root/hadoop), correct the /etc/hosts entry
```
ssh root@sandbox.hortonworks.com
```

- Deploy Nifi Ambari service on sandbox by running below
```
VERSION=`hdp-select status hadoop-client | sed 's/hadoop-client - \([0-9]\.[0-9]\).*/\1/'`
sudo git clone https://github.com/abajwa-hw/ambari-nifi-service.git   /var/lib/ambari-server/resources/stacks/HDP/$VERSION/services/NIFI   
#sandbox
service ambari restart
#non sandbox
service ambari-server restart
```
- To install Nifi, start the 'Install Wizard': Open Ambari (http://sandbox.hortonworks.com:8080) then:
  - On bottom left -> Actions -> Add service -> check NiFi server -> Next -> Next -> Change any config you like (e.g. install dir, port, setup_prebuilt or values in nifi.properties) -> Next -> Deploy. This will kick off the install which will run for 5-10min.

#### Steps

#####  Explore tcpdump

- Tcpdump is a network traffic analyzer. Full details can be found [here](http://www.tcpdump.org/tcpdump_man.html)

- To install tcdump on sandbox:
```
yum install -y tcpdump
```

- Here is a common usage:
```
tcpdump -n -nn
```

- On sandbox, this will output something like
```
23:32:50.115230 IP 192.168.191.1.57564 > 192.168.191.144.22: Flags [.], ack 513312, win 8179, options [nop,nop,TS val 1145841629 ecr 1341432], length 0
23:32:50.116109 IP 192.168.191.144.22 > 192.168.191.1.57564: Flags [P.], seq 513312:513824, ack 433, win 385, options [nop,nop,TS val 1341433 ecr 1145841629], length 512
```


#####  Build tcpdump flow using ExecuteProcess and EL



- Download to local laptop (not sandbox) xml template for flow that uses ExecuteProcess/EL to parse tcpdump flow from https://raw.githubusercontent.com/abajwa-hw/nifi-network-processor/master/templates/TCPDump_EL_Example.xml
  
- Launch Nifi by opening http://sandbox.hortonworks.com:9090/nifi

- Import flow template info Nifi:
  - Import template by clicking on Templates (third icon from right) which will launch the 'Nifi Flow templates' popup 
  - Browse and navigate to where ever you downloaded TCPDump_EL_Exmple.xml on your local machine
  - Click Import. Now the template should appear in 'Nifi Flow templates' popup window
  - Close the popup window

- Instantiate the 'TCPDump EL Example' dashboard template:
  - Drag/drop the Template icon (7th icon form left) onto the canvas so that a picklist popup appears 
  - Select 'TCPDump EL Example' and click Add
    ![Image](../master/screenshots/nifi-import-el-processor.png?raw=true)

- Run the flow

 ![Image](../master/screenshots/nifi-tcpdump-el-flow.png?raw=true)
 
  - TODO: note what each component is doing

- For more details on Nifi Expression Language see [Nifi docs](https://nifi.apache.org/docs/nifi-docs/html/expression-language-guide.html)
- Stop the flow using the stop button

#####  Build custom processor for tcpdump

- setup sandbox for development by using [VNC Ambari service](https://github.com/hortonworks-gallery/ambari-vnc-service) to install VNC/eclipse/maven
  - Download Ambari service for VNC (details below)
```  
VERSION=`hdp-select status hadoop-client | sed 's/hadoop-client - \([0-9]\.[0-9]\).*/\1/'`
sudo git clone https://github.com/hortonworks-gallery/ambari-vnc-service.git   /var/lib/ambari-server/resources/stacks/HDP/$VERSION/services/VNCSERVER   
service ambari restart
```

- Once the status of HDFS/YARN has changed from a yellow question mark to a green check mark...
- Setup Eclipse on the sandbox VM and remote desktop into it using an Ambari service for VNC
- In Ambari open, Admin > Stacks and Services tab. You can access this via http://sandbox.hortonworks.com:8080/#/main/admin/stack/services
- Deploy the service by selecting:
  - VNC Server -> Add service -> Next -> Next -> Enter password (e.g. hadoop) -> Next -> Proceed Anyway -> Deploy
  - Make sure the password is at least 6 characters or install will fail
 ![Image](../master/screenshots/ambari-vnc-config.png?raw=true)  
  
- Connect to VNC from local laptop using a VNC viewer software (e.g. Tight VNC viewer or Chicken of the VNC or just your browser). Detailed steps [here](https://github.com/hortonworks-gallery/ambari-vnc-service)
- (Optional): To install maven manually instead:
```
curl -o /etc/yum.repos.d/epel-apache-maven.repo https://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo
yum -y install apache-maven-3.2*
```

- In general, when starting a new project you would use the mvn archetype to create a custom processor. Details here: https://cwiki.apache.org/confluence/display/NIFI/Maven+Projects+for+Extensions
  - Command to run the wizard:
  ```
  cd /tmp
  mvn archetype:generate -DarchetypeGroupId=org.apache.nifi -DarchetypeArtifactId=nifi-processor-bundle-archetype -DarchetypeVersion=0.2.1 -DnifiVersion=0.2.1
  ```
  - Sample inputs to generate a maven project archetype skeleton.
  ```
Define value for property 'groupId': : com.hortonworks
Define value for property 'artifactId': : nifi-network-processors
Define value for property 'version':  1.0-SNAPSHOT: :
Define value for property 'artifactBaseName': : network
Define value for property 'package':  com.hortonworks.processors.network: :
  ```
  - This will create an archetype maven project for a custom processor with the package name, artifactId... specified above.

- In this case we will download a previously built sample and walk through what changes you would need to make to the archetype to create a basic custom processor
```
cd
sudo git clone https://github.com/abajwa-hw/nifi-network-processor.git
```
- Open Eclipse using the shortcut on the Desktop
- Import to Eclipse 
  - File > Import > Maven > Existing Maven projects
  - Browse > root > nifi-network-processor > OK > Finish
  
- code walk through
  - pom.xml: add commons-io dependency for utils [here](https://github.com/abajwa-hw/nifi-network-processor/blob/master/nifi-network-processors/pom.xml#L47-L51)
  ```
        <dependency>
        	<groupId>commons-io</groupId>
        	<artifactId>commons-io</artifactId>
        	<version>2.4</version>
        </dependency>  
  ```
  - In org.apache.nifi.processor.Processor, add the class name [here](https://github.com/abajwa-hw/nifi-network-processor/blob/master/nifi-network-processors/src/main/resources/META-INF/services/org.apache.nifi.processor.Processor#L15)
  - In GetTcpDumpAttributes.java:
  
- To run maven compile: 
  - In Eclipse, under 'Package Explorer' select 'network-analysis' and then click:
    - Run
    - Run Configurations
    - Maven Build
  
  - The first time you do this, it will ask you for the configuration:
    - Name: nifi-network
    - Base dir: /root/nifi-network-processor
    - Under 'Goals': clean package
    - Under Maven Runtime: (scroll down to see this option) add your existing mvn install on the sandbox (its faster than using the embedded one)
    ![Image](../master/screenshots/configure-maven-install.png?raw=true)
    - Configure > Add > click ‘Directory’ and navigate to mvn install: /usr/share/apache-maven > OK > Finish > Select 'apache-maven' > Apply > OK
    - So now your maven run configuration should look as below
    ![Image](../master/screenshots/eclipse-mvn-runconfig.png?raw=true)
    - Click Apply > Run to start compile
        


- Confirm the nar got built
```
ls -la ~/nifi-network-processor/nifi-network-nar/target/nifi-network-nar-1.0-SNAPSHOT.nar
```
- Build nar and deploy: copy the compiled nar file into Nifi lib dir and restart Nifi
```
cp ~/nifi-network-processor/nifi-network-nar/target/nifi-network-nar-1.0-SNAPSHOT.nar /opt/nifi-1.0.0.0-7/lib/
chown nifi:hadoop /opt/nifi-1.0.0.0-7/lib/nifi-network-nar-1.0-SNAPSHOT.nar
```
- Restart Nifi from Ambari

- Download to local laptop (not sandbox) the xml template for flow that uses Custom processor to parse tcpdump flow from https://github.com/abajwa-hw/nifi-network-processor/raw/master/templates/TCPDump_Custom_Processor_Example.xml
  
- Open Nifi UI and delete the existing flow by:
  - Control-A to select all the components and right click on any processor and select Delete

- Import the custom processor flow template info Nifi:
  - Import template by clicking on Templates (third icon from right) which will launch the 'Nifi Flow templates' popup 
  - Browse and navigate to where ever you downloaded TCPDump_Custom_Processor_Exmple.xml on your local machine
  - Click Import. Now the template should appear in 'Nifi Flow templates' popup window
  - Close the popup window

- Instantiate the 'TCPDump_Custom_Processor_Exmple' dashboard template:
  - Drag/drop the Template icon (7th icon form left) onto the canvas so that a picklist popup appears 
  - Select 'TCPDump_Custom_Processor_Exmple' and click Add
    ![Image](../master/screenshots/nifi-import-custom-processor.png?raw=true)

 ![Image](../master/screenshots/nifi-tcpdump-customprocessor-flow.png?raw=true)


- Run the flow

##### More resources

- [https://nifi.apache.org/developer-guide.html](https://nifi.apache.org/developer-guide.html)
- [https://nifi.apache.org/docs/nifi-docs/html/expression-language-guide.html](https://nifi.apache.org/docs/nifi-docs/html/expression-language-guide.html)
- [http://www.nifi.rocks/developing-a-custom-apache-nifi-processor-json/](http://www.nifi.rocks/developing-a-custom-apache-nifi-processor-json/)  
- [http://bryanbende.com/development/2015/02/04/custom-processors-for-apache-nifi/](http://bryanbende.com/development/2015/02/04/custom-processors-for-apache-nifi/)
