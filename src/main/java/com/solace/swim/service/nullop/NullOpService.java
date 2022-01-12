package com.solace.swim.service.nullop;

import com.solace.swim.service.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "service.null-op", value = "enabled", havingValue = "true")
public class NullOpService implements IService {
    private static final Logger logger = LoggerFactory.getLogger(NullOpService.class);

    @Override
    public void invoke(Message<?> message) {
        // Do nothing
    }
}
