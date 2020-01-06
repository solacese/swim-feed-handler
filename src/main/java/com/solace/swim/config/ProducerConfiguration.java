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
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.jndi.JndiTemplate;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Configuration for the SolacePublishingService.
 */
@Configuration
public class ProducerConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ProducerConfiguration.class);

    @Autowired
    Hashtable envProducer;

    @Value("${service.solace-publishing.jms.connectionFactory}")
    private String producerConnectionFactoryName;

    private JndiTemplate jndiTemplate;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.putAll(envProducer);
        jndiTemplate = new JndiTemplate(props);
    }

    private JndiObjectFactoryBean producerConnectionFactory() {
        JndiObjectFactoryBean factoryBean = new JndiObjectFactoryBean();
        factoryBean.setJndiTemplate(jndiTemplate);
        factoryBean.setJndiName(producerConnectionFactoryName);
        // following ensures all the properties are injected before returning
        try {
            factoryBean.afterPropertiesSet();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return factoryBean;
    }

    private CachingConnectionFactory producerCachingConnectionFactory() {
        CachingConnectionFactory ccf = new CachingConnectionFactory((ConnectionFactory) producerConnectionFactory().getObject());
        ccf.setSessionCacheSize(10);
        return ccf;
    }

    // Configure the destination resolver for the producer:
    // Here we are using JndiDestinationResolver for JNDI destinations
    // Other options include using DynamicDestinationResolver for non-JNDI destinations
    private JndiDestinationResolver producerJndiDestinationResolver() {
        JndiDestinationResolver jdr = new JndiDestinationResolver();
        jdr.setCache(true);
        jdr.setJndiTemplate(jndiTemplate);
        return jdr;
    }

    @Bean
    public JmsTemplate producerJmsTemplate() {
        JmsTemplate jt = new JmsTemplate(producerCachingConnectionFactory());
        jt.setDeliveryPersistent(true);
        jt.setDestinationResolver(producerJndiDestinationResolver());
        return jt;
    }





}
