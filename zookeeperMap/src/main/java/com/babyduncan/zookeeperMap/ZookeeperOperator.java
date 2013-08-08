package com.babyduncan.zookeeperMap;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-8-8 15:56
 */
public class ZookeeperOperator<V> {

    private static final Logger logger = Logger.getLogger(ZookeeperOperator.class);

    private final ZookeeperClient zookeeperClient;

    private final String nodePath;

    private final Function<V, byte[]> encoder;

    private volatile ConcurrentMap<String, V> localcache = Maps.newConcurrentMap();

    private final boolean persistent;

    private final byte[] lock = new byte[0];

    private final Rescuer rescuer = new Rescuer();

    public ZookeeperOperator(ZookeeperClient zookeeperClient, String nodePath, Function<V, byte[]> function, boolean persistent) {
        this.zookeeperClient = Preconditions.checkNotNull(zookeeperClient);
        this.nodePath = Preconditions.checkNotNull(nodePath);
        this.encoder = Preconditions.checkNotNull(function);
        this.persistent = persistent;

        if (!this.persistent) {
            this.zookeeperClient.registerExpirationWatcher(new Runnable() {
                @Override
                public void run() {
                    logger.warn("cache a expire ,try to put all data .");
                    rescuer.rescue(new TryToPutAllFunction());
                }
            });
        }
    }

    private ZooKeeper getZookeeper() {
        try {
            return zookeeperClient.get(6000);
        } catch (RuntimeException e) {
            logger.error("Can't get zkClient in 6000 ms.", e);
        }
        return null;
    }

    private String makePath(String key) {
        return nodePath + "/" + key;
    }

    public boolean put(String key, V data) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(data);
        ZooKeeper zooKeeper = getZookeeper();
        if (zooKeeper == null) {
            return false;
        }
        String path = makePath(key);
        CreateMode mode = this.persistent ? CreateMode.PERSISTENT : CreateMode.EPHEMERAL;
        synchronized (lock) {
            try {
                if (zooKeeper.exists(path, false) == null) {
                    List<ACL> acl = createACL(zookeeperClient.getZookeeperUser(), zookeeperClient.getZookeeperPassword());
                    zooKeeper.create(path, encoder.apply(data), acl, mode);
                } else {
                    zooKeeper.setData(path, encoder.apply(data), -1);
                }

                if (!persistent) {
                    localcache.put(key, data);
                }
                return true;
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            } catch (KeeperException e) {
                logger.error("Add key [" + key + "] for node [" + nodePath + "] fail.", e);
            } catch (NoSuchAlgorithmException e) {
                logger.error("Add key [" + key + "] for node [" + nodePath + "] fail.", e);
            }
        }
        return false;
    }

    public boolean remove(String key) {
        ZooKeeper zooKeeper = getZookeeper();
        if (zooKeeper == null) {
            return false;
        }
        synchronized (lock) {
            try {
                zooKeeper.delete(makePath(key), -1);
                if (!persistent) {
                    localcache.remove(key);
                }
                return true;
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            } catch (KeeperException.NoNodeException e) {
                logger.error("Remove key [" + key + "] from node [" + nodePath + "] not exist.", e);
                return true;
            } catch (KeeperException e) {
                logger.error("Remove key [" + key + "] from node [" + nodePath + "] fail.", e);
            }
        }
        return false;
    }

    private List<ACL> createACL(String user, String password) throws NoSuchAlgorithmException {
        if (user != null && password != null) {
            String digest = DigestAuthenticationProvider.generateDigest(user + ":" + password);
            ACL all = new ACL(ZooDefs.Perms.ALL, new Id("digest", digest));
            return Lists.newArrayList(all);
        } else {
            return ZooDefs.Ids.OPEN_ACL_UNSAFE;
        }
    }


    private class TryToPutAllFunction implements Function<Void, Boolean> {
        @Override
        public Boolean apply(Void input) {
            synchronized (lock) {
                boolean result = true;
                for (String key : localcache.keySet()) {
                    V data = localcache.get(key);
                    if (!put(key, data)) {
                        result = false;
                        break;
                    }
                }
                return result;
            }
        }
    }

}
