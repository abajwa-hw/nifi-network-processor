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



- Download to local laptop (not sandbox) xml template for flow that uses ExecuteProcess/EL to parse tcpdump flow from https://www.dropbox.com/s/w13t1e9mruy6atj/TCPDump_example.xml?dl=0
  - TODO: add updated link
  
- Launch Nifi by opening http://sandbox.hortonworks.com:9090/nifi

- Import flow template info Nifi:
  - Import template by clicking on Templates (third icon from right) which will launch the 'Nifi Flow templates' popup 
  - Browse and navigate to where ever you downloaded Twitter_Dashboard.xml on your local machine
  - Click Import. Now the template should appear in 'Nifi Flow templates' popup window
  - Close the popup window

- Instantiate the Twitter dashboard template:
  - Drag/drop the Template icon (7th icon form left) onto the canvas so that a picklist popup appears 
  - Select 'TCP dump' and click Add

- Run the flow and note what each component is doing
  - TODO: add details

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
- Connect to VNC from local laptop using a VNC viewer software (e.g. Tight VNC viewer or Chicken of the VNC or just your browser). Detailed steps [here](https://github.com/hortonworks-gallery/ambari-vnc-service)
- (Optional): To install maven manually instead:
```
curl -o /etc/yum.repos.d/epel-apache-maven.repo https://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo
yum -y install apache-maven-3.2*
```

- In general, when starting a new project you would use the mvn archetype to create a custom processor. Details here: https://cwiki.apache.org/confluence/display/NIFI/Maven+Projects+for+Extensions

- In this case we will download sample code
```
cd
sudo git clone https://github.com/abajwa-hw/nifi-network-processor.git
```
- Import to Eclipse 
  - File > Import > Maven > Existing Maven projects
  - Browse > root > nifi-network-processor > OK > Finish
  
- code walk through
  
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
    ![Image](../master/screenshots/maven-run-configuration.png?raw=true)
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

- import template for flow that uses custom processor to parse tcpdump
- run the flow

