package com.rcloud.server.sealtalk.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ThreadFactoryUtil {



    public static void ofVirtual(ThrowRunable run) {
        Thread.ofVirtual().start(() -> {
            try {
                run.run();
            } catch (Throwable e) {
                log.error("", e);
            }
        });
    }
}
