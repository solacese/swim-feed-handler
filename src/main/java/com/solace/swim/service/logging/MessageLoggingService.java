package com.solace.swim.service.logging;

import com.solace.swim.service.IService;
import com.solace.swim.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "service.message-logging", value = "enabled", havingValue = "true")
public class MessageLoggingService implements IService {

    private static final Logger logger = LoggerFactory.getLogger(MessageLoggingService.class);

    // Determine if the message headers should also be written to file
    @Value("${service.message-logging.write-headers:true}")
    private boolean writeHeaders;


    @Override
    public void invoke(Map<String, ?> headers, String payload) {
        StringBuilder builder = new StringBuilder();
        if (writeHeaders) {
            builder.append("<!--");
            builder.append(MessageUtil.getHeadersAsJSON(headers));
            builder.append("-->\n");
        }
        builder.append(payload);

        logger.info(builder.toString());
    }

}
