package com.solace.swim.service;

import java.util.Map;

public interface IService {

    public void invoke(Map<String, ?> messageHeaders, String messagePayload);
}
