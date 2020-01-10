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
package com.solace.swim.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Service class designed to write a String object to an AWS S3 store.  The current time in
 * milliseconds is used as a key for the object store.
 *
 * This service assumes the message payload is text based.
 *
 * This service is only enabled when the property service.aws-s3-put.enabled=true.
 */
@Service
@ConditionalOnProperty(prefix = "service.aws-s3-put", value = "enabled", havingValue = "true")
public class AWSS3PutService {

    private static final Logger logger = LoggerFactory.getLogger(AWSS3PutService.class);

    @Value("${service.aws-s3-put.region-name}")
    private String regionName;

    @Value("${service.aws-s3-put.access-key}")
    private String accessKey;

    @Value("${service.aws-s3-put.secret-key}")
    private String secretKey;

    @Value("${service.aws-s3-put.bucket-name}")
    private String bucketName;

    private AmazonS3 s3Client;

    @PostConstruct
    private void init() {
        try {
            logger.info("Creating AWS S3 connection...");
            s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(Regions.fromName(regionName))
                    .withCredentials(new AWSStaticCredentialsProvider(
                            new BasicAWSCredentials(accessKey,secretKey)))
                    .build();
            logger.info("AWS S3 connection successful.");

            if (!s3Client.doesBucketExistV2(bucketName)) {
                logger.info("S3 bucket {} does not exist.  Creating it...", bucketName);
                s3Client.createBucket(new CreateBucketRequest(bucketName));
                logger.info("S3 bucket {} successfully created in region {}.", bucketName, regionName);
            }
        } catch (Exception e) {
            logger.error("Error in configuration of AWS S3.  Check proper application configuration.", e);
            throw e;
        }
    }

    public void putObject(String message) {
        try {
            logger.info("Message received. Attempting to store to AWS S3...");
            s3Client.putObject(bucketName, Long.toString(System.currentTimeMillis()), message);
            logger.info("Message store successful to AWS S3 bucket {}", bucketName);
        } catch (Exception e) {
            logger.error("Unable to store message to AWS S3.", e);
        }
    }
}
