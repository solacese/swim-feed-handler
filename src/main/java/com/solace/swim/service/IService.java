package com.solace.swim.service;

import org.springframework.messaging.Message;

public interface IService {

    public void invoke(Message<?> message);
}
