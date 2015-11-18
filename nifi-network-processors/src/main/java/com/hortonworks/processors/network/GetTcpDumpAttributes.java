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

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.StreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

//Define the processor tags and description which will be displayed on Nifi UI
@Tags({"fetch","tcpdump","tcp", "network"})
@CapabilityDescription("Reads output of tcpdump and outputs the results as a Flowfile")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class GetTcpDumpAttributes extends AbstractProcessor {

    //Define properties for the processor
    public static final PropertyDescriptor MY_PROPERTY = new PropertyDescriptor
            .Builder().name("My Property")
            .description("Example Property")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    //Define relationships for the processor
    public static final Relationship SUCCESS_RELATIONSHIP = new Relationship.Builder()
            .name("success")
            .description("Success relationship")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;


    //Called at the start of Nifi
    //Note that unmodifiable List and Set used since Nifi is multi-threaded
    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        descriptors.add(MY_PROPERTY);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(SUCCESS_RELATIONSHIP);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {

    }

    //Called when flow file is passes to the processor
    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
		FlowFile flowFile = session.get();
		if ( flowFile == null ) {
			return;
		}

		flowFile.getAttributes();
		
		
		final Map<String, String> attributes = new HashMap<>();
		session.read(flowFile, new InputStreamCallback() {
			
			@Override
			public void process(InputStream in) throws IOException {
				// Parse the data as string and extract scr/dest host/ports
				String data = IOUtils.toString(in);
				String[] components = StringUtils.split(data);
				attributes.put("src.socket",components[2]);
				attributes.put("dest.socket",StringUtils.replace(components[4],":", ""));
				
			}
		});
		
		//write out attributes to Flow file
		flowFile = session.putAllAttributes(flowFile, attributes);
		
		session.transfer(flowFile, SUCCESS_RELATIONSHIP);

    }

}
