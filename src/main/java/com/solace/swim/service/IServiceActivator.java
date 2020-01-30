package com.solace.swim.service;

import org.springframework.messaging.Message;

public interface IServiceActivator {

    public void processMessage(Message<?> message);
}
