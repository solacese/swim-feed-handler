package com.solace.swim.service.nullop;

import com.solace.swim.service.IServiceActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Async;

@MessageEndpoint
@ConditionalOnProperty(prefix = "service.null-op", value = "enabled", havingValue = "true")
public class NullOpServiceActivator implements IServiceActivator {
    private static final Logger logger = LoggerFactory.getLogger(NullOpServiceActivator.class);

    @Autowired
    NullOpService service;

    @ServiceActivator(inputChannel = "msg.scds.service")
    @Async
    @Override
    public void processMessage(Message msg) {
        service.invoke(msg.getHeaders(), (String)msg.getPayload());
        return;
    }
}
