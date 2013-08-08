package com.babyduncan.zookeeperMap.toturial;

import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.List;

/**
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-8-8 17:20
 */
public class ZkTutorial {

    private static final Logger logger = Logger.getLogger(ZkTutorial.class);

    public static void main(String... args) throws Exception {
        ZooKeeper zooKeeper = new ZooKeeper("127.0.0.1:2181", 6000, new ConnectionWatcher());
        System.out.println(zooKeeper);
        List<String> children = zooKeeper.getChildren("/babyduncan", new DataWatcher(zooKeeper));
        System.out.println("children is " + children);
        for (String s : children) {
            String data = new String(zooKeeper.getData("/babyduncan/" + s, new ChildrenDataWatcher(zooKeeper, "/babyduncan/" + s), null));
            System.out.println("key " + s + " value " + data);
        }
        List<String> children2 = zooKeeper.getChildren("/zzz", new DataWatcher(zooKeeper));
        zooKeeper.exists("/sss", new DataWatcher(zooKeeper));
        Thread.sleep(50000);
    }


    private static class ConnectionWatcher implements Watcher {

        @Override
        public void process(WatchedEvent event) {
            if (event.getType().equals(Event.EventType.None)) {
                if (event.getState().equals(Event.KeeperState.Disconnected)) {
                    System.out.println("DisConnected..");
                }
                if (event.getState().equals(Event.KeeperState.SyncConnected)) {
                    System.out.println("Connected..");
                }
                if (event.getState().equals(Event.KeeperState.Expired)) {
                    System.out.println("Expired ..");
                }
            }
        }
    }

    private static class DataWatcher implements Watcher {

        private ZooKeeper zooKeeper;

        public DataWatcher(ZooKeeper zooKeeper) {
            this.zooKeeper = zooKeeper;
        }

        @Override
        public void process(WatchedEvent event) {
            if (event.getType().equals(Event.EventType.NodeChildrenChanged)) {
                try {
                    System.out.println(zooKeeper.getChildren("/babyduncan", null));
                } catch (KeeperException e) {
                    logger.error(e.getMessage(), e);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            } else if (event.getType().equals(Event.EventType.NodeCreated)) {
                System.out.println("node created ...");
            } else if (event.getType().equals(Event.EventType.NodeDeleted)) {
                System.out.println("node deleted ...");
            }
        }
    }

    private static class ChildrenDataWatcher implements Watcher {

        private ZooKeeper zooKeeper;

        private String path;

        public ChildrenDataWatcher(ZooKeeper zooKeeper, String path) {
            this.zooKeeper = zooKeeper;
            this.path = path;
        }

        @Override
        public void process(WatchedEvent event) {
            if (event.getType().equals(Event.EventType.NodeDataChanged)) {
                try {
                    System.out.println(path + " data changed to ->" + new String(zooKeeper.getData(path, null, null)));
                } catch (KeeperException e) {
                    logger.error(e.getMessage(), e);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

}
