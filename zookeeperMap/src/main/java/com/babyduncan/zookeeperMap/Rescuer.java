package com.babyduncan.zookeeperMap;

import com.google.common.base.Function;
import org.apache.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * do one job until success async.
 * <p/>
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-7-9 10:44
 */
public class Rescuer {

    private static final Logger logger = Logger.getLogger(Rescuer.class);

    private static final ScheduledExecutorService DEFAULT_EXECUTORS = Executors.newSingleThreadScheduledExecutor();

    private final ScheduledExecutorService scheduledExecutorService;


    public Rescuer(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public Rescuer() {
        this(DEFAULT_EXECUTORS);
    }

    public void rescue(final Function<Void, Boolean> function) {
        final RescueTask task = new RescueTask(function);
        Runnable runnable = new RescueTaskRunnable(task);
        scheduledExecutorService.schedule(runnable, 0, TimeUnit.MILLISECONDS);
    }

    public class RescueTaskRunnable implements Runnable {

        private final RescueTask rescueTask;


        public RescueTaskRunnable(RescueTask rescueTask) {
            this.rescueTask = rescueTask;
        }


        @Override
        public void run() {
            rescueTask.run();
            if (!rescueTask.isSuccess()) {
                long nextTime = rescueTask.getNextTime();
                scheduledExecutorService.schedule(this, nextTime, TimeUnit.MILLISECONDS);
            }
        }
    }


}
