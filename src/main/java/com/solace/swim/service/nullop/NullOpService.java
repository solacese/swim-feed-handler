package com.solace.swim.service.nullop;

import com.solace.swim.service.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "service.null-op", value = "enabled", havingValue = "true")
public class NullOpService implements IService {
    private static final Logger logger = LoggerFactory.getLogger(NullOpService.class);

    @Override
    public void invoke(Map<String, ?> headers, String payload) {
        // Do nothing
    }
}
