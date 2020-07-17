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
package com.solace.swim.service.publish;

import com.solace.swim.service.IService;
import com.solacesystems.jcsmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

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
public class SolacePublishingService implements IService {
    private static final Logger logger = LoggerFactory.getLogger(SolacePublishingService.class);

    @Autowired
    Hashtable envProducer;

    private JCSMPFactory solaceFactory;

    private JCSMPProperties jcsmpProperties;
    private JCSMPSession jcsmpSession;
    private XMLMessageProducer producer;

    @PostConstruct
    private void init() throws Exception {
        Properties props = new Properties();
        props.putAll(envProducer);
        jcsmpProperties = JCSMPProperties.fromProperties(props);
        jcsmpSession = JCSMPFactory.onlyInstance().createSession(jcsmpProperties);
        producer = jcsmpSession.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
            @Override
            public void handleError(String s, JCSMPException e, long l) {
                logger.error(s,e);
            }

            @Override
            public void responseReceived(String s) {
                //do nothing
            }
        });
        jcsmpSession.connect();
    }

    @Override
    public void invoke(Map<String, ?> headers, String payload) {
        try {
            MessageConverter messageConverter = new SimpleMessageConverter();

            javax.jms.Topic jmsTopic = null;
            com.solacesystems.jcsmp.Topic topic = null;
            try {
                jmsTopic = (javax.jms.Topic)headers.get("jms_destination");
                topic = JCSMPFactory.onlyInstance().createTopic(jmsTopic.getTopicName());
            } catch (ClassCastException e) {
                String jmsDestination = (String) headers.get("jms_destination");
                topic = JCSMPFactory.onlyInstance().createTopic(jmsDestination);
            }

            //XMLContentMessage jcsmpMsg = JCSMPFactory.onlyInstance().createMessage(XMLContentMessage.class);
            TextMessage jcsmpMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            SDTMap properties = JCSMPFactory.onlyInstance().createMap();

            for (String header : headers.keySet()) {
                if (logger.isDebugEnabled()) {
                    logger.debug(header + "=" + headers.get(header));
                }
                properties.putString(header, headers.get(header).toString());
            }
            jcsmpMsg.setText(payload);
            jcsmpMsg.setDeliveryMode(DeliveryMode.DIRECT);
            jcsmpMsg.setProperties(properties);

            producer.send(jcsmpMsg, topic);

            properties = null;
            jcsmpMsg = null;
            logger.info("Published message to {}.", topic);
        } catch (Exception ex) {
            logger.error("Unable to send message", ex);
        }
    }
}
