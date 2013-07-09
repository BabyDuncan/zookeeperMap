package com.babyduncan.zookeeperMap;

import com.google.common.base.Function;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-7-9 10:49
 */
public class RescueTask implements Runnable {

    public static final int MAX_WAIT_TIME = 10000; //10s

    private final Function<Void, Boolean> function;

    private volatile boolean success = false;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private long waitTime = 50;


    public RescueTask(Function<Void, Boolean> function) {
        this.function = function;
    }

    @Override
    public void run() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            if (function.apply(null)) {
                success = true;
            }
        } finally {
            running.compareAndSet(true, false);
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public long getNextTime() {
        long nextTime = Math.min(MAX_WAIT_TIME, waitTime * 2);
        waitTime = nextTime;
        return nextTime;
    }


}
