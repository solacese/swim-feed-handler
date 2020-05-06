package com.solace.swim.service.logging;
/*
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


import com.solace.swim.service.IServiceActivator;
import com.solacesystems.jms.message.SolMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Async;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * Service Activator pattern.  The input is the internal message channel msg.scds.service.
 * Invokes the MessageLoggingService.
 */
@MessageEndpoint
@ConditionalOnProperty(prefix = "service.message-logging", value = "enabled", havingValue = "true")
public class MessageLoggingServiceActivator implements IServiceActivator {
    private static final Logger logger = LoggerFactory.getLogger(MessageLoggingServiceActivator.class);

    @Autowired
    MessageLoggingService service;

    // jms_destination must be the minimal
    @Value("#{'${service.message-logging.header-remove-list}'!=''?'${service.message-logging.header-remove-list}':'jms_destination'}")
    String headersToRemove;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SS'Z'").withZone(ZoneOffset.UTC);

    /**
     * This is used to inject message processing before the service. In this instance,
     * the need to remove specific message headers are necessary before sending off to
     * the logging service.  The specific headers to remove are identified via the
     * header-remove-list property.  This is a comma separated list of headers to remove.
     *
     * This method uses Spring DSL to invoke the headerFilter, a built in Spring Integration
     * message transformer.
     *
     * Once the headers are removed from the original message, then the message is sent
     * to the msg.scds.service.log-message channel for the next processing step.
     *
     * @return IntegrationFlow used by Spring Integration
     */
    @Bean
    public IntegrationFlow filterHeaders() {
        if (logger.isDebugEnabled()) logger.debug("Headers to Remove: {}", headersToRemove);
        return IntegrationFlows.from("msg.scds.service")
                .headerFilter(headersToRemove,true)
                .enrichHeaders(Collections.singletonMap("capture-timestamp", dateFormatter.format(Instant.now())))
                .channel("msg.scds.service.log-message")
                .get();
    }

    @ServiceActivator(inputChannel = "msg.scds.service.log-message")
    @Async
    public void processMessage(Message<?> msg) {
        String payload;
        if (msg.getPayload() instanceof String) {
            payload = (String)msg.getPayload();
        } else if (msg.getPayload() instanceof SolMessage) {
            SolMessage obj = (SolMessage) msg.getPayload();
            payload = obj.dump();
        } else {
            payload = msg.getPayload().toString();
        }
        service.invoke(msg.getHeaders(), payload);
    }


}
