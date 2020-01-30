package com.solace.swim.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;


public class BlockCallerExecutionPolicy implements RejectedExecutionHandler {
    private static final Logger logger = LoggerFactory.getLogger(BlockCallerExecutionPolicy.class);

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        try {
            logger.info("Putting message back on queue for processing.");
            executor.getQueue().put(r);
        }
        catch (InterruptedException e) {
            throw new RejectedExecutionException("Unexpected InterruptedException while waiting to add Runnable to ThreadPoolExecutor queue...", e);
        }
    }
}
