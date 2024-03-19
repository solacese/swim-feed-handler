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
package com.solace.swim.service.publish;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import com.solace.messaging.publisher.PersistentMessagePublisher;
import com.solace.messaging.resources.Topic;
import com.solace.messaging.util.SolaceSDTMap;
import com.solace.messaging.util.SolaceSDTMapToMessageConverter;
import com.solace.swim.service.IService;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.SDTStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jms.JMSException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
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
    Hashtable<String,String> envProducer;

    // New Solace Java API messaging service
    private MessagingService messagingService;

    // New Solace Java API messaging publisher
    private PersistentMessagePublisher messagePublisher;

    @PostConstruct
    private void init() {
        Properties props = new Properties();
        props.putAll(envProducer);
        props.setProperty(SolaceProperties.TransportLayerProperties.RECONNECTION_ATTEMPTS, "-1");
        props.setProperty(SolaceProperties.TransportLayerProperties.CONNECTION_RETRIES, "-1");

        messagingService = MessagingService.builder(ConfigurationProfile.V1).fromProperties(props).build();
        messagingService.connect();

        messagingService.addServiceInterruptionListener(serviceEvent -> {
            logger.error("### SERVICE INTERRUPTION: "+serviceEvent.getCause());
        });
        messagingService.addReconnectionAttemptListener(serviceEvent -> {
            logger.info("### RECONNECTING ATTEMPT: "+serviceEvent);
        });
        messagingService.addReconnectionListener(serviceEvent -> {
            logger.info("### RECONNECTED: "+serviceEvent);
        });

        messagePublisher = messagingService.createPersistentMessagePublisherBuilder()
                .build()
                .start();

        final PersistentMessagePublisher.MessagePublishReceiptListener deliveryConfirmationListener = (publishReceipt) -> {
            // process delivery confirmation for some message ...
        };

        messagePublisher.setMessagePublishReceiptListener(deliveryConfirmationListener);
    }

    @Override
    public void invoke(Message<?> message) {
        OutboundMessageBuilder messageBuilder = messagingService.messageBuilder();

        OutboundMessage outboundMessage;

        // Copy the message properties
        Properties properties = new Properties();
        properties.putAll(message.getHeaders());

        try {
            Object incomingPayload = message.getPayload();
            if (incomingPayload instanceof byte[]) {
                outboundMessage = messageBuilder.build((byte[]) message.getPayload(), properties);
            } else if (incomingPayload instanceof String) {
                outboundMessage = messageBuilder.build((String)message.getPayload(), properties);
            } else if (incomingPayload instanceof SDTStream) {
                outboundMessage = messageBuilder.build(((SDTStream)incomingPayload).readBytes(), properties);
            } else if (incomingPayload instanceof SDTMap) {
                SolaceSDTMap content = new SolaceSDTMap();
                content.putAll((SDTMap)incomingPayload);
                outboundMessage = messageBuilder.build(content, new SolaceSDTMapToMessageConverter());
            } else if (incomingPayload instanceof Serializable) {
                ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);

                objectOutputStream.writeObject(incomingPayload);
                objectOutputStream.flush();
                objectOutputStream.close();

                outboundMessage = messageBuilder.build(byteOutputStream.toByteArray(), properties);
            } else {
                String msg = String.format(
                        "Invalid payload received. Expected %s. Received: %s",
                        String.join(", ",
                                byte[].class.getSimpleName(),
                                String.class.getSimpleName(),
                                SDTStream.class.getSimpleName(),
                                SDTMap.class.getSimpleName(),
                                Serializable.class.getSimpleName()
                        ), incomingPayload.getClass().getName());
                MessageConversionException exception = new MessageConversionException(msg);
                logger.error(msg, exception);
                throw exception;
            }

            javax.jms.Topic jmsTopic;
            Topic topic;

            try {
                jmsTopic = (javax.jms.Topic)message.getHeaders().get("jms_destination");
                assert jmsTopic != null;
                topic = Topic.of(jmsTopic.getTopicName());
            } catch (ClassCastException | JMSException | AssertionError e) {
                Object jmsDestination = message.getHeaders().get("jms_destination");
                assert jmsDestination != null;
                topic = Topic.of(jmsDestination.toString());
            }

            messagePublisher.publish(outboundMessage, topic);

            properties = null;
            logger.info("Published message id {} to topic {}.", message.getHeaders().get("jms_messageId"), topic.getName());
        } catch (IllegalStateException ex) {
            logger.error("Error in state of publisher. Trying to recover...");
            if (!messagePublisher.isTerminated()) {
                messagePublisher.terminate(100);
            }
            messagingService.disconnect();
            init();
            invoke(message);
        }
        catch (Exception ex) {
            logger.error("Unable to send message", ex);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (!messagePublisher.isTerminated()) {
            messagePublisher.terminate(0L);
        }

        if (messagingService.isConnected()) {
            messagingService.disconnect();
        }
    }
}
