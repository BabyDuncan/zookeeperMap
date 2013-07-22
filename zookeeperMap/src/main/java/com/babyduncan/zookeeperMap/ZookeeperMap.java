package com.babyduncan.zookeeperMap;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * zookeeper mapping
 * <p/>
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-7-9 10:37
 */
public final class ZookeeperMap<V> extends ForwardingMap<String, V> {

    private static final Logger logger = Logger.getLogger(ZookeeperMap.class);

    private final Rescuer rescuer = new Rescuer();

    private final String nodePath;

    private final Function<byte[], V> decoder;

    private final boolean isLocalcacheUsableWhenConnectionBroken;

    private volatile ConcurrentMap<String, V> localcache = Maps.newConcurrentMap();

    private volatile boolean isConnectionBroken = false;

    private volatile boolean reRegisterWatchers = false;

    private final ZookeeperClient zookeeperClient;

    private final byte[] lock = new byte[0];

    public ZookeeperMap(ZookeeperClient zookeeperClient, String nodePath, Function<byte[], V> decoder, boolean localcacheUsableWhenConnectionBroken) {

        // do some validate
        Preconditions.checkNotNull(nodePath);
        Preconditions.checkNotNull(decoder);
        Preconditions.checkNotNull(zookeeperClient);

        this.nodePath = nodePath;
        this.decoder = decoder;
        this.isLocalcacheUsableWhenConnectionBroken = localcacheUsableWhenConnectionBroken;
        this.zookeeperClient = zookeeperClient;
    }

    public static <V> ZookeeperMap<V> createZookeeperMap(ZookeeperClient zookeeperClient, String nodePath, Function<byte[], V> decoder) {
        ZookeeperMap<V> zookeeperMap = new ZookeeperMap<V>(zookeeperClient, nodePath, decoder, true);
        zookeeperMap.init();
        return zookeeperMap;
    }

    /**
     * you can never use localcache when connection is broken ,so it is rigid .
     *
     * @param zookeeperClient
     * @param nodePath
     * @param decoder
     * @param <V>
     * @return
     */
    public static <V> ZookeeperMap<V> createRigidZookeeperMap(ZookeeperClient zookeeperClient, String nodePath, Function<byte[], V> decoder) {
        ZookeeperMap<V> zookeeperMap = new ZookeeperMap<V>(zookeeperClient, nodePath, decoder, false);
        zookeeperMap.init();
        return zookeeperMap;
    }

    private void init() {
        Watcher expiredWatcher = zookeeperClient.registerExpirationWatcher(new Runnable() {
            @Override
            public void run() {
                getDataFromZookeeperServer();
            }
        });
        DisconnectedWatcher disconnectedWatcher = new DisconnectedWatcher();
        zookeeperClient.registerWatcher(disconnectedWatcher);

        try {
            updateChildren();
        } catch (Exception e) {
            zookeeperClient.removeWatcher(expiredWatcher);
            zookeeperClient.removeWatcher(disconnectedWatcher);
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public V get(Object key) {
        if (!isLocalcacheUsableWhenConnectionBroken && isConnectionBroken) {
            throw new IllegalStateException("zookeeper is down !");
        }
        return super.get(key);
    }

    @Override
    protected Map<String, V> delegate() {
        if (!isLocalcacheUsableWhenConnectionBroken && isConnectionBroken) {
            throw new IllegalStateException("zookeeper is down !");
        }
        return localcache;
    }

    private class DisconnectedWatcher implements Watcher {

        @Override
        public void process(WatchedEvent watchedEvent) {
            if (watchedEvent.getType() == Event.EventType.None && watchedEvent.getState() == Event.KeeperState.Disconnected) {
                if (!ZookeeperMap.this.isLocalcacheUsableWhenConnectionBroken) {
                    getDataFromZookeeperServer();
                }
            }
        }
    }

    private class RootNodeWatcher implements Watcher {

        @Override
        public void process(WatchedEvent watchedEvent) {
            synchronized (lock) {
                if (watchedEvent.getType() == Event.EventType.NodeDeleted) {
                    localcache.clear();
                    try {
                        getZookeeper().exists(nodePath, new RootNodeWatcher());
                    } catch (KeeperException e) {
                        logger.error(e.getMessage(), e);
                        getDataFromZookeeperServer();
                    } catch (InterruptedException ie) {
                        logger.error(ie.getMessage(), ie);
                        Thread.currentThread().interrupt();
                    }
                } else if (watchedEvent.getType() == Event.EventType.NodeCreated) {
                    try {
                        updateChildren();
                    } catch (KeeperException e) {
                        logger.error(e.getMessage(), e);
                        getDataFromZookeeperServer();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error(ie.getMessage(), ie);
                    }
                }
            }
        }
    }

    private class ChildrenWatcher implements Watcher {

        @Override
        public void process(WatchedEvent watchedEvent) {
            synchronized (lock) {
                if (isConnectionBroken) {
                    return;
                }
                if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
                    try {
                        updateChildren();
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                        Thread.currentThread().interrupt();
                    } catch (KeeperException e) {
                        logger.error(e.getMessage(), e);
                        getDataFromZookeeperServer();
                    }
                }

            }
        }
    }

    private class ChildDataWatcher implements Watcher {
        private final String child;


        private ChildDataWatcher(String child) {
            this.child = child;
        }

        @Override
        public void process(WatchedEvent watchedEvent) {
            synchronized (lock) {
                if (watchedEvent.getType() == Event.EventType.NodeDataChanged) {
                    try {
                        addChild(child);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                        Thread.currentThread().interrupt();
                    } catch (KeeperException e) {
                        logger.error(e.getMessage(), e);
                        getDataFromZookeeperServer();
                    }
                }
            }
        }
    }


    private void getDataFromZookeeperServer() {
        logger.info("get data from zookeeper!!");
        synchronized (lock) {
            if (reRegisterWatchers) {
                return;
            }
            this.reRegisterWatchers = true;
            this.isConnectionBroken = true;

            rescuer.rescue(new Function<Void, Boolean>() {
                @Override
                public Boolean apply(java.lang.Void aVoid) {
                    try {
                        synchronized (lock) {
                            updateChildren();
                            isConnectionBroken = false;
                            reRegisterWatchers = false;
                        }
                        return true;
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    return false;
                }
            });
        }
    }

    private void updateChildren() throws InterruptedException, KeeperException {
        synchronized (lock) {
            Stat stat = getZookeeper().exists(nodePath, new RootNodeWatcher());
            if (stat == null) {
                throw new KeeperException.NoNodeException(nodePath);
            }
            List<String> children = getZookeeper().getChildren(nodePath, new ChildrenWatcher());
            HashSet<String> zookeeperChildren = Sets.newHashSet(children);
            Set<String> addedChildren = Sets.difference(zookeeperChildren, localcache.keySet());
            Set<String> removedChildren = Sets.difference(localcache.keySet(), zookeeperChildren);
            for (String child : addedChildren) {
                addChild(child);
            }
            for (String child : removedChildren) {
                localcache.remove(child);
            }
        }

    }

    private void addChild(final String child) throws InterruptedException, KeeperException {
        synchronized (lock) {
            final Watcher nodeWatcher = new ChildDataWatcher(child);
            try {
                V value = decoder.apply(getZookeeper().getData(new StringBuilder().append(this.nodePath).append("/").append(child).toString(), nodeWatcher, null));
                localcache.put(child, value);
            } catch (KeeperException.NoNodeException e) {
                localcache.remove(child);
            }
        }
    }


    private ZooKeeper getZookeeper() {
        return zookeeperClient.get(6000);
    }

}
