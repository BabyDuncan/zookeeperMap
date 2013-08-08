package com.babyduncan.zookeeperMap;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * a zookeeper client (wrap from apache zookeeper)
 * <p/>
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-7-8 16:35
 */
public class ZookeeperClient {

    private static final Logger logger = Logger.getLogger(ZookeeperClient.class);

    private final String zkConnect;

    private final int sessionTimeout;
    //    apache ZooKeeper
    private volatile ZooKeeper zookeeper;

    private volatile boolean connectionClosed = true;

    private volatile boolean serverShutdown = false;
    //     zookeeper session state
    private volatile SessionState sessionState;

    private final Set<Watcher> watcherSet = Collections.synchronizedSet(new HashSet<Watcher>());

    // for oath
    private final String zookeeperUser;

    private final String zookeeperPassword;

    public String getZookeeperUser() {
        return zookeeperUser;
    }

    public String getZookeeperPassword() {
        return zookeeperPassword;
    }


    public ZookeeperClient(String zkConnect, int sessionTimeout, String zookeeperUser, String zookeeperPassword) {

        // do some validate
        Preconditions.checkArgument(!Strings.isNullOrEmpty(zkConnect), "zkConnect must not be null or blank");
        if (sessionTimeout <= 0) {
            sessionTimeout = Constants.SESSION_TIMEOUT;
        }

        this.zkConnect = zkConnect;
        this.sessionTimeout = sessionTimeout;
        this.zookeeperUser = zookeeperUser;
        this.zookeeperPassword = zookeeperPassword;
    }

    /**
     * register a watcher
     *
     * @param watcher
     */
    public void registerWatcher(Watcher watcher) {
        Preconditions.checkArgument(watcher != null, "a watcher to be registered must not be null");
        this.watcherSet.add(watcher);
    }

    /**
     * remove a watcher
     *
     * @param watcher
     */
    public void removeWatcher(Watcher watcher) {
        Preconditions.checkArgument(watcher != null, "a watcher to be removed must not be null");
        this.watcherSet.remove(watcher);
    }

    /**
     * register a session timeout watcher
     *
     * @param runnable
     * @return
     */
    public Watcher registerExpirationWatcher(final Runnable runnable) {
        Watcher watcher = new ExpirationWatcher(runnable);
        registerWatcher(watcher);
        return watcher;
    }

    /**
     * is connection closed
     *
     * @return
     */
    public synchronized boolean isConnectionClosed() {
        return connectionClosed;
    }

    public ZooKeeper get(int connectionTimeout) {
        if (serverShutdown) {
            return null;
        }
        if (zookeeper != null) {
            return zookeeper;
        }
        synchronized (this) {
            if (zookeeper != null) {
                return zookeeper;
            }

            final CountDownLatch countDownLatch = new CountDownLatch(1);
            Watcher watcher = new ConnectWatcher(countDownLatch);
            if (sessionState != null) {
                try {
                    zookeeper = new ZooKeeper(this.zkConnect, this.sessionTimeout, watcher, sessionState.getSessionId(),
                            sessionState.getSessionPassword());
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }

            } else {
                try {
                    zookeeper = new ZooKeeper(this.zkConnect, this.sessionTimeout, watcher);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }

            }
            if (!Strings.isNullOrEmpty(this.zookeeperUser) && !Strings.isNullOrEmpty(this.zookeeperPassword)) {
                try {
                    zookeeper.addAuthInfo("digest", (new StringBuilder().append(this.zookeeperUser).append(":")
                            .append(this.zookeeperPassword).toString()).getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (connectionTimeout > 0) {
                try {
                    if (!countDownLatch.await(connectionTimeout, TimeUnit.MILLISECONDS)) {
                        close();
                    } else {
                        countDownLatch.await();
                    }
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            sessionState = new SessionState(zookeeper.getSessionId(), zookeeper.getSessionPasswd());
            connectionClosed = false;
            return zookeeper;
        }
    }

    public synchronized void close() {
        if (connectionClosed) {
            return;
        }
        connectionClosed = true;
        if (zookeeper != null) {
            try {
                zookeeper.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info(e.getMessage(), e);
            }
            zookeeper = null;
            sessionState = null;
        }
    }


    private class ConnectWatcher implements Watcher {

        private final CountDownLatch countDownLatch;

        private ConnectWatcher(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }


        @Override
        public void process(WatchedEvent watchedEvent) {
            switch (watchedEvent.getType()) {
                case None:
                    switch (watchedEvent.getState()) {
                        case Expired:
                            close();
                            break;
                        case SyncConnected:
                            countDownLatch.countDown();
                            break;
                        case Disconnected:
                            break;
                    }
            }
            synchronized (watcherSet) {
                for (Watcher watcher : watcherSet) {
                    watcher.process(watchedEvent);
                }
            }
        }
    }
}
