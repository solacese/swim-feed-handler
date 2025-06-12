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
package com.solace.swim.service.file;

import com.solace.swim.service.IService;
import com.solace.swim.util.MessageUtil;
import com.solacesystems.jms.message.SolMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Service class designed to write data to disk.  The payload of the message will be written as a file.
 * An associated .header file will also be written with all message headers.  The filename
 * will be the current date including milliseconds in format of "yyyyMMdd_mmssSS".
 *
 * This service assumes the message payload is text based.
 *
 * This service is only enabled when the property service.file-output.enabled=true.
 */
@Service
@ConditionalOnProperty(prefix = "service.file-output", value = "enabled", havingValue = "true")
public class FileOutputService implements IService {

    private static final Logger logger = LoggerFactory.getLogger(FileOutputService.class);

    // Get the output directory from property file otherwise default to "log/data"
    @Value("${service.file-output.directory:log/data}")
    private String outputDirectory;

    // Determine if the message headers should also be written to file
    @Value("${service.file-output.write-headers:true}")
    private boolean writeHeaders;

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd_mmssSS");

    @PostConstruct
    private void init() {
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public void invoke(Message<?> message) {
        logger.info("File being written...");
        String filename = MessageUtil.getHeaderValue(message.getHeaders(), "id") ;

        if (writeHeaders) {
            File header = new File(outputDirectory + File.separator + filename + ".header");

            try (FileOutputStream stream = new FileOutputStream(header)) {
                stream.write(MessageUtil.getHeaders(message.getHeaders()).getBytes());
            } catch (FileNotFoundException e) {
                logger.error("File not found", e);
            } catch (IOException e) {
                logger.error("Failed to close the file", e);
            }
        }

        String payload = "";
        if (message.getPayload() instanceof String) {
            payload = (String)message.getPayload();
        } else if (message.getPayload() instanceof SolMessage) {
            SolMessage obj = (SolMessage) message.getPayload();
            payload = obj.dump();
        } else if (message.getPayload() instanceof Object) {
            payload = message.getPayload().toString();
        }

        File file = new File(outputDirectory + File.separator + filename);
        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(payload.getBytes());
        } catch (FileNotFoundException e) {
            logger.error("File not found", e);
        } catch (IOException e) {
            logger.error("Failed to close the file", e);
        } catch (Exception ex) {
            logger.error("Error in getting content", ex);
        }
    }
}
