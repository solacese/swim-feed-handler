/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.solace.swim.service.aws;

import com.solace.swim.service.IServiceActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Async;

/**
 * Service Activator pattern.  The input is the internal message channel msg.scds.service.
 * Invokes the AWSS3PutService.
 */
@MessageEndpoint
@ConditionalOnProperty(prefix = "service.aws-s3-put", value = "enabled", havingValue = "true")
public class AWSS3PutServiceActivator implements IServiceActivator {

    private static final Logger logger = LoggerFactory.getLogger(AWSS3PutServiceActivator.class);

    @Autowired
    AWSS3PutService service;

    @ServiceActivator(inputChannel = "msg.scds.service")
    @Async
    @Override
    public void processMessage(Message msg) {
        service.invoke(msg);
        return;
    }
}
