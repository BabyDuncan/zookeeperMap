package com.babyduncan.zookeeperMap;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

/**
 * session timeout watcher
 * <p/>
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-7-8 17:27
 */
public class ExpirationWatcher implements Watcher {

    private final Runnable runnable;


    public ExpirationWatcher(Runnable runnable) {
        this.runnable = runnable;
    }


    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getType() == Event.EventType.None && watchedEvent.getState() == Event.KeeperState.Expired) {
            runnable.run();
        }

    }
}
