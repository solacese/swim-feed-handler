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
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.Hashtable;
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
        // Set the retry to forever
        JCSMPChannelProperties channelProperties = (JCSMPChannelProperties) jcsmpProperties.getProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES);
        channelProperties.setConnectRetries(-1);
        channelProperties.setReconnectRetries(-1);

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
    public void invoke(Message<?> message) {
        try {
            XMLMessage xmlMessage;
            Object payload = message.getPayload();
            if (payload instanceof byte[]) {
                BytesMessage bytesMessage = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
                bytesMessage.setData((byte[]) payload);
                xmlMessage = bytesMessage;
            } else if (payload instanceof String) {
                TextMessage textMessage = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                textMessage.setText((String) payload);
                xmlMessage = textMessage;
            } else if (payload instanceof SDTStream) {
                StreamMessage streamMessage = JCSMPFactory.onlyInstance().createMessage(StreamMessage.class);
                streamMessage.setStream((SDTStream) payload);
                xmlMessage = streamMessage;
            } else if (payload instanceof SDTMap) {
                MapMessage mapMessage = JCSMPFactory.onlyInstance().createMessage(MapMessage.class);
                mapMessage.setMap((SDTMap) payload);
                xmlMessage = mapMessage;
            } else if (payload instanceof Serializable) {
                BytesMessage bytesMessage = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
                bytesMessage.setData((byte[]) payload);
                xmlMessage = bytesMessage;
            } else {
                String msg = String.format(
                        "Invalid payload received. Expected %s. Received: %s",
                        String.join(", ",
                                byte[].class.getSimpleName(),
                                String.class.getSimpleName(),
                                SDTStream.class.getSimpleName(),
                                SDTMap.class.getSimpleName(),
                                Serializable.class.getSimpleName()
                        ), payload.getClass().getName());
                MessageConversionException exception = new MessageConversionException(msg);
                logger.warn(msg, exception);
                throw exception;
            }

            SDTMap properties = JCSMPFactory.onlyInstance().createMap();
            for (String header : message.getHeaders().keySet()) {
                if (logger.isDebugEnabled()) {
                    logger.debug(header + "=" + message.getHeaders().get(header));
                }
                Object value = message.getHeaders().get(header);
                try {
                    properties.putObject(header, message.getHeaders().get(header));
                } catch (IllegalArgumentException e) {
                    logger.warn("{}. Converting header {} to String", e.getMessage(),message.getHeaders().get(header).toString());
                    properties.putString(header, message.getHeaders().get(header).toString());
                }
            }

            javax.jms.Topic jmsTopic = null;
            com.solacesystems.jcsmp.Topic topic = null;
            try {
                jmsTopic = (javax.jms.Topic)message.getHeaders().get("jms_destination");
                topic = JCSMPFactory.onlyInstance().createTopic(jmsTopic.getTopicName());
            } catch (ClassCastException e) {
                //String jmsDestination = (String) message.getHeaders().get("jms_destination");
                Object jmsDestination = message.getHeaders().get("jms_destination");
                topic = JCSMPFactory.onlyInstance().createTopic(jmsDestination.toString());
            }

            xmlMessage.setDeliveryMode(DeliveryMode.PERSISTENT);
            xmlMessage.setProperties(properties);

            producer.send(xmlMessage, topic);

            properties = null;
            xmlMessage = null;
            logger.info("Published message id {} to topic {}.", message.getHeaders().get("jms_messageId"), topic);
        } catch (Exception ex) {
            logger.error("Unable to send message", ex);
        }
    }
}
