package com.solace.swim;
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

import com.solace.swim.config.ISCDSConsumer;
import com.solace.swim.util.MessageUtil;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;


/**
 * Main Application class.
 */
@SpringBootApplication
@EnableEncryptableProperties
@ImportResource({"classpath*:applicationContext.xml"})
@EnableIntegration
@IntegrationComponentScan("com.solace.swim")
public class SWIMFeedHandler {

	private static final Logger logger = LoggerFactory.getLogger(SWIMFeedHandler.class);

	// Spring Integration Gateway
	static ISCDSConsumer msgConsumer;

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(SWIMFeedHandler.class, args);

		// Spring Integration Gateway object that defines how to interact with the messaging channels
		msgConsumer = (ISCDSConsumer) ctx.getBean("msgConsumerGateway");
	}

	/**
	 * Static class designed to act as the JMS Listener.  Uses the configuration definitions found in
	 * com.solace.swim.ConsumerConfiguration to connect and consume messages from the SWIM broker.  It
	 * can support multiple connections to different queues found in the same Solace VPN by creating multiple
	 * '@JMSListener' annotations.
	 *
	 * See com.solace.swim.ConsumerConfiguration for additional configurations
	 */
	@Component
	static class SCDSMessageConsumer {

		// Repeatable annotations are supported by spring to allow connections to multiple destinations
		// Duplicate the @JMSListener annotation and modify the id and destination
		@JmsListener(id="queue0", destination = "${solace.jms.consumer.queue-name.0}", containerFactory = "cFactory", concurrency = "${solace.jms.consumer.max-listeners}")
		//@JmsListener(id="queue1", destination = "${solace.jms.consumer.queue-name.1}", containerFactory = "cFactory", concurrency = "${solace.jms.consumer.max-listeners}")
		//@JmsListener(id="queue2", destination = "${solace.jms.consumer.queue-name.2}", containerFactory = "cFactory", concurrency = "${solace.jms.consumer.max-listeners}")
		public void handleMsg(Message<?> msg) {
			// Print headers if DEBUG is turned on
			logger.info("SCDS Message received...{}",msg.getHeaders().get("id"));
			if (logger.isDebugEnabled()) {
				logger.debug("Headers:\n");
				logger.debug(MessageUtil.getHeaders(msg.getHeaders()));
			}

			try {
				msgConsumer.processMsg(msg);
			} catch (TaskRejectedException ex) {
				logger.info("Rejected Task: Trying to process message again.");
				msgConsumer.processMsg(msg);
				logger.info("Rejected Task: Message was processed successfully.");
			}
		}
	}

}
