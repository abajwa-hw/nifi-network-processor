/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.processors.network;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;


public class GetTcpDumpAttributesTest {

    private TestRunner testRunner;

    @Before
    public void init() {
        testRunner = TestRunners.newTestRunner(GetTcpDumpAttributes.class);
    }

    @Test
    public void testProcessor() {
    	final String testInput = "09:45:12.126164 IP 173.194.204.105.443 > 10.0.2.15.60536: Flags [P.]blahblah";
    	testRunner.setProperty(GetTcpDumpAttributes.MY_PROPERTY, "my value");
    	testRunner.enqueue(testInput.getBytes());
    	testRunner.run();
    	testRunner.assertAllFlowFilesTransferred(GetTcpDumpAttributes.SUCCESS_RELATIONSHIP);
    	
    	MockFlowFile transferredFlowFile = testRunner.getFlowFilesForRelationship(GetTcpDumpAttributes.SUCCESS_RELATIONSHIP).get(0);
    	transferredFlowFile.assertContentEquals(testInput);
    	transferredFlowFile.assertAttributeExists("dest.socket");
    	transferredFlowFile.assertAttributeExists("src.socket");
    	transferredFlowFile.assertAttributeEquals("dest.socket", "10.0.2.15.60536");
    	transferredFlowFile.assertAttributeEquals("src.socket", "173.194.204.105.443");
    	

    }

}
