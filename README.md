## Getting started with ELs and building a custom Nifi processor on HDP

This tutorial is part of a webinar on Hortonworks DataFlow.
- http://hortonworks.com/partners/learn/


#### Background

- For a primer on HDF, you can refer to the materials [here](http://hortonworks.com/products/dataflow/) to get a basic background
- A basic tutorial on using Nifi on HDP sandbox is also available [here](http://community.hortonworks.com/articles/1282/sample-hdfnifi-flow-to-push-tweets-into-solrbanana.html)

#### Goals 

- Build Nifi flow to analyze Nifi's network traffic using tcpdump. Use Expression Language to extract out source/target IPs/ports
- Build and use custom tcpdump processor to filter Nifi's source/target IPs/ports


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

- Once installed, launch Nifi by opening http://sandbox.hortonworks.com:9090/nifi

#### Steps

#####  Explore tcpdump

- Tcpdump is a common packet analyzer that runs under the command line. It allows the user to display TCP/IP and other packets being transmitted or received over a network to which the computer is attached.  Full details can be found [here](http://www.tcpdump.org/tcpdump_man.html)

- To install tcdump on sandbox:
```
yum install -y tcpdump
```

- Here is a common usage:
```
tcpdump -n -nn
```

- On sandbox, this will output something like below for each network connection being made, showing:
  - which socket (i.e. IP/port) was the source (to the left of >) and 
  - which was the target (to the right of >)
```
08:16:15.878652 IP 192.168.191.1.49270 > 192.168.191.144.9090: Flags [.], ack 2255, win 8174, options [nop,nop,TS val 1176961367 ecr 32747195], length 0
```

- In the example above:
  - the source machine was 192.168.191.1 (port 49270) and 
  - the target machine was 192.168.191.144 (port 9090)

- Note that since Nifi is running on port 9090, by monitoring traffic to port 9090, we will be able to capture connections made by Nifi

#####  Build tcpdump flow using ExecuteProcess and EL

- Download to local laptop (not sandbox) xml template for flow that uses ExecuteProcess/EL to parse tcpdump flow from https://raw.githubusercontent.com/abajwa-hw/nifi-network-processor/master/templates/TCPDump_EL_Example.xml
  
- On the Nifi webui, import flow template:
  - Import template by clicking on Templates (third icon from right) which will launch the 'Nifi Flow templates' popup 
  - Browse and navigate to where ever you downloaded TCPDump_EL_Exmple.xml on your local machine
  - Click Import. Now the template should appear in 'Nifi Flow templates' popup window
  - Close the popup window

- Instantiate the 'TCPDump EL Example' dashboard template:
  - Drag/drop the Template icon (7th icon form left) onto the canvas so that a picklist popup appears 
  - Select 'TCPDump EL Example' and click Add
    ![Image](../master/screenshots/nifi-import-el-processor.png?raw=true)

- Run the flow.  After a few seconds you should see all the counters increase

 ![Image](../master/screenshots/nifi-tcpdump-el-flow.png?raw=true)
 
  - TODO: note what each component is doing

- For more details on Nifi Expression Language see [Nifi docs](https://nifi.apache.org/docs/nifi-docs/html/expression-language-guide.html)
- Stop the flow using the stop button

#####  Build custom processor for tcpdump

- Setup your sandbox for development by using [VNC Ambari service](https://github.com/hortonworks-gallery/ambari-vnc-service) to install VNC/eclipse/maven
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
 ![Image](../master/screenshots/vnc-connect.png?raw=true)
 
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
  - This will create an archetype maven project for a custom processor with the package name, artifactId, etc specified above.

- In this case we will download a previously built sample and walk through what changes you would need to make to the archetype to create a basic custom processor
```
cd
sudo git clone https://github.com/abajwa-hw/nifi-network-processor.git
```
- Open Eclipse using the shortcut on the Desktop
 ![Image](../master/screenshots/vnc-desktop.png?raw=true)
 
- Import to Eclipse 
  - File > Import > Maven > Existing Maven projects
   ![Image](../master/screenshots/eclipse-import.png?raw=true)
  - Browse > root > nifi-network-processor > OK > Finish
   ![Image](../master/screenshots/eclipse-import-maven.png?raw=true)
  
- Here is a summary of code changes made to the generated archetype to create the sample tcpdump processor:
  - pom.xml: add commons-io dependency (for utils) [here](https://github.com/abajwa-hw/nifi-network-processor/blob/master/nifi-network-processors/pom.xml#L47-L51)
  - In org.apache.nifi.processor.Processor, add the class name [here](https://github.com/abajwa-hw/nifi-network-processor/blob/master/nifi-network-processors/src/main/resources/META-INF/services/org.apache.nifi.processor.Processor#L15)
  - In [GetTcpDumpAttributes.java](https://github.com/abajwa-hw/nifi-network-processor/blob/master/nifi-network-processors/src/main/java/com/hortonworks/processors/network/GetTcpDumpAttributes.java):
    - Define the `tags` and `description` which will be displayed on the 'Add processor' screen of Nifi UI using `@Tags` and `@CapabilityDescription` [here](https://github.com/abajwa-hw/nifi-network-processor/blob/master/nifi-network-processors/src/main/java/com/hortonworks/processors/network/GetTcpDumpAttributes.java#L43-L45)
    ![Image](../master/screenshots/nifi-tcp-processor.png?raw=true)
    - Define `properties` for the processor [here](https://github.com/abajwa-hw/nifi-network-processor/blob/master/nifi-network-processors/src/main/java/com/hortonworks/processors/network/GetTcpDumpAttributes.java#L51-L57)
    ![Image](../master/screenshots/nifi-tcp-processor-properties.png?raw=true)    
    - Define `relationships` for the processor [here](https://github.com/abajwa-hw/nifi-network-processor/blob/master/nifi-network-processors/src/main/java/com/hortonworks/processors/network/GetTcpDumpAttributes.java#L59-L63)
    ![Image](../master/screenshots/nifi-tcp-processor-relationships.png?raw=true)    
    - Any initializations to be done when Nifi starts would be done in `init()` [here](https://github.com/abajwa-hw/nifi-network-processor/blob/master/nifi-network-processors/src/main/java/com/hortonworks/processors/network/GetTcpDumpAttributes.java#L73)
    - `onTrigger()` is the main method to override to define the logic when a flow file is passed to our processor. This is where we parse a line of tcpdump output and store the src and destination sockets [here](https://github.com/abajwa-hw/nifi-network-processor/blob/master/nifi-network-processors/src/main/java/com/hortonworks/processors/network/GetTcpDumpAttributes.java#L98-L128)
  - In [GetTcpDumpAttributesTest.java](https://github.com/abajwa-hw/nifi-network-processor/blob/master/nifi-network-processors/src/test/java/com/hortonworks/processors/network/GetTcpDumpAttributesTest.java), you can define a Junit to test that the processor is working correctly
    
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
        
- To run Junit to confirm processor is working correctly
  - In Eclipse, under 'Package Explorer' select 'nifi-network-processors' and then click: Run > Run as > JUnit test
    ![Image](../master/screenshots/eclipse-junit-correct.png?raw=true)
  - After a few seconds the test should pass and you should see below (in green):  
    ![Image](../master/screenshots/eclipse-junit-success.png?raw=true)
  - To see what happens if test does not pass, try changing the value of the dest.socket as highlighted below, save your changes and re-run JUnit  
    ![Image](../master/screenshots/eclipse-junit-incorrect.png?raw=true)
  - This time you will see the test fail (in red below)  
    ![Image](../master/screenshots/eclipse-junit-failure.png?raw=true)
  - Press Control-Z to undo your changes  
                
- Confirm the nar file (Nifi library file for your processor) file got built
```
ls -la ~/nifi-network-processor/nifi-network-nar/target/nifi-network-nar-1.0-SNAPSHOT.nar
```
- Deploy the nar into Nifi: copy the compiled nar file into Nifi lib dir and correct permissions
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

- Run the flow. After a few seconds you should see all the counters increase
  - TODO: note what each component is doing

- You have successfully created flows to analyze network traffic using both expression languages and a custom processor


##### Further reading

- [https://nifi.apache.org/developer-guide.html](https://nifi.apache.org/developer-guide.html)
- [https://nifi.apache.org/docs/nifi-docs/html/expression-language-guide.html](https://nifi.apache.org/docs/nifi-docs/html/expression-language-guide.html)
- [http://www.nifi.rocks/developing-a-custom-apache-nifi-processor-json/](http://www.nifi.rocks/developing-a-custom-apache-nifi-processor-json/)  
- [http://bryanbende.com/development/2015/02/04/custom-processors-for-apache-nifi/](http://bryanbende.com/development/2015/02/04/custom-processors-for-apache-nifi/)
