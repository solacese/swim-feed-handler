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
package com.solace.swim.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ErrorHandler;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

/**
 * Configuration for consumer
 */
@EnableJms
@Configuration
public class ConsumerConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerConfiguration.class);

    InitialContext initialContextConsumer;

    // Uses values from application.context to populate a hashtable of environment properties
    // The values are defined in the application.properties files so there is no need to modify
    // the values in application.context
    @Autowired
    Hashtable envConsumer;

    @Value("${solace.jms.consumer.connection-factory}")
    private String consumerConnectionFactoryName;

    /**
     * Initialize the InitialConext based on the environment properties that is autowired via Spring.
     * @throws NamingException
     */
    @PostConstruct
    public void init() throws NamingException {
        initialContextConsumer = new InitialContext(envConsumer);
    }

    /**
     * Generate a custom JMS Listener Connection Factory.
     * @param errorHandler - simple error handler
     * @return DefaultJmsListenerContainerFactory
     * @throws Exception
     */
    @Bean
    public DefaultJmsListenerContainerFactory cFactory(SwimErrorHandler errorHandler) throws Exception {
        ConnectionFactory connectionFactory = (ConnectionFactory)initialContextConsumer.lookup(consumerConnectionFactoryName);
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setErrorHandler(errorHandler);
        return factory;
    }

    /**
     * Simple Error Handler for any errors encountered during consumption of a message.
     */
    @Service
    public class SwimErrorHandler implements ErrorHandler {

        public void handleError(Throwable t) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os);
            t.printStackTrace(ps);
            try {
                String output = os.toString("UTF8");
                logger.error("============= Error processing message: " + t.getMessage()+"\n"+output);
            } catch (UnsupportedEncodingException e) {
                logger.error("An error occurred." + t.getMessage());
            }

        }
    }

}

