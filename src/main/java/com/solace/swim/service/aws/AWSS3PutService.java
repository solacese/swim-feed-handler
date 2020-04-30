package com.solace.swim.service.aws;
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

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.solace.swim.service.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

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
public class AWSS3PutService implements IService {

    private static final Logger logger = LoggerFactory.getLogger(AWSS3PutService.class);

    @Value("${service.aws-s3-put.region-name}")
    private String regionName;

    @Value("${service.aws-s3-put.access-key}")
    private String accessKey;

    @Value("${service.aws-s3-put.secret-key}")
    private String secretKey;

    @Value("${service.aws-s3-put.bucket-name}")
    private String bucketName;

    @Value("${service.aws-s3-put.folder-name:''}")
    private String folderName;

    private AmazonS3 s3Client;

    @PostConstruct
    private void init() {
        try {

            logger.info("Creating AWS S3 connection...");
            s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(Regions.fromName(regionName))
                    .withCredentials(new AWSStaticCredentialsProvider(
                            new BasicAWSCredentials(accessKey, secretKey)))
                    .build();
            logger.info("AWS S3 connection successful.");


            if (!s3Client.doesBucketExistV2(bucketName)) {
                logger.info("S3 bucket {} does not exist.  Creating it...", bucketName);
                s3Client.createBucket(new CreateBucketRequest(bucketName));
                logger.info("S3 bucket {} successfully created in region {}.", bucketName, regionName);
            }
            if (!folderName.isEmpty()) {
                if (!folderName.endsWith("/")) {
                    folderName += "/";
                }
                createFolder(bucketName, folderName, s3Client);
            }
        } catch (Exception e) {
            logger.error("Error in configuration of AWS S3.  Check proper application configuration.", e);
            throw e;
        }
    }

    private void createFolder(String bucketName, String folderName, AmazonS3 client) {
        // create meta-data for your folder and set content-length to 0
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);
        // create empty content
        InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
        // create a PutObjectRequest passing the folder name suffixed by /
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,
                folderName , emptyContent, metadata);
        // send request to S3 to create folder
        client.putObject(putObjectRequest);
    }


        @Override
    public void invoke(Map<String, ?> headers, String payload) {
        try {
            logger.info("Message received. Attempting to store to AWS S3...");
            Object id = (headers.get("id")!=null)?headers.get("id"):Long.toString(System.currentTimeMillis());
            String name = folderName + id.toString();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(payload.length());
            s3Client.putObject(
                    new PutObjectRequest(
                            bucketName,
                            name,
                            new ByteArrayInputStream( payload.getBytes() ),
                            metadata
                    )
            );
            logger.info("Message store successful to AWS S3 bucket {}:{}", bucketName,folderName);
        } catch (Exception e) {
            logger.error("Unable to store message to AWS S3.", e);
        }
    }
}
