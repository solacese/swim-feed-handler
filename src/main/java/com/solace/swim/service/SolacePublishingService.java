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
package com.solace.swim.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.support.converter.MessagingMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * Class designed to publish a Message object to a Solace Broker instance.  This instance can
 * be local or cloud based.  The target destination is a Topic defined via the JMS header
 * jms_destination.  This should be the Topic the message was initially published to inside
 * of SWIM.
 *
 * This service is only enabled when the property service.solace-publishing.enabled=true.
 */
@Service
@ConditionalOnProperty(prefix = "service.solace-publishing", value = "enabled", havingValue = "true")
public class SolacePublishingService {
    private static final Logger logger = LoggerFactory.getLogger(SolacePublishingService.class);

    @Autowired
    private JmsTemplate producerJmsTemplate;

    public void publish(Message<?> msg) {
        try {
            Topic topic = (Topic)msg.getHeaders().get("jms_destination");

            // Need to convert spring framework message to javax.jms.Message
            producerJmsTemplate.send(topic, new MessageCreator() {
                @Override
                public javax.jms.Message createMessage(Session session) throws JMSException {
                    MessagingMessageConverter converter = new MessagingMessageConverter();
                    return converter.toMessage(msg, session);
                }
            });
            logger.info("Published message to {}.", topic.getTopicName());
        } catch (Exception ex) {
            logger.error("Unable to send message", ex);
            ex.printStackTrace();
        }
    }
}
