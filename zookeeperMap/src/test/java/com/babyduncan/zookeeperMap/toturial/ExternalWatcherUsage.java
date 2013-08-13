package com.babyduncan.zookeeperMap.toturial;

import com.babyduncan.zookeeperMap.FunctionUtil;
import com.babyduncan.zookeeperMap.ZookeeperClient;
import com.babyduncan.zookeeperMap.ZookeeperMap;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.HashMap;
import java.util.Map;

/**
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-8-13 15:05
 */
public class ExternalWatcherUsage {

    public static void main(String... args) throws Exception {
        FunctionUtil.ByteArrayToString byteArrayToString = new FunctionUtil.ByteArrayToString();
        ZookeeperClient zookeeperClient = new ZookeeperClient("localhost:2181", 6000, null, null);
        Map<Watcher.Event.EventType, Watcher> watcherMap = new HashMap<Watcher.Event.EventType, Watcher>();
        CCWatcher ccWatcher = new CCWatcher();
        DCWatcher dcWatcher = new DCWatcher();
        watcherMap.put(Watcher.Event.EventType.NodeDataChanged, dcWatcher);
        watcherMap.put(Watcher.Event.EventType.NodeChildrenChanged, ccWatcher);
        ZookeeperMap<String> zookeeperMap = ZookeeperMap.createZookeeperMap(zookeeperClient, "/babyduncan", byteArrayToString, watcherMap);
        for (int i = 0; i < 10; i++) {
            System.out.println(zookeeperMap.delegate());
            Thread.sleep(6000);
        }
    }


    /**
     * {hello=world, foo=foo, helloHello=ok}
     * {hello=world, foo=foo, helloHello=ok}
     * {hello=world, foo=foo, helloHello=ok}
     * 2013-08-13 15:18:13,804 INFO  -> Process event:WatchedEvent state:SyncConnected type:NodeChildrenChanged path:/babyduncan watcher:com.babyduncan.zookeeperMap.toturial.ExternalWatcherUsage$CCWatcher@62da3a1e
     * a children changed .../babyduncan
     * {hello=world, foo=foo, helloHello=ok, zgh=zgh}
     * {hello=world, foo=foo, helloHello=ok, zgh=zgh}
     * 2013-08-13 15:18:26,238 INFO  -> Process event:WatchedEvent state:SyncConnected type:NodeDataChanged path:/babyduncan/zgh watcher:com.babyduncan.zookeeperMap.toturial.ExternalWatcherUsage$DCWatcher@2b03be0
     * a node data changed .../babyduncan/zgh
     * {hello=world, foo=foo, helloHello=ok, zgh=foo}
     */

    //children changed watcher
    public static class CCWatcher implements Watcher {

        @Override
        public void process(WatchedEvent event) {
            System.out.println("a children changed ..." + event.getPath());
        }
    }

    //node data changed watcher
    public static class DCWatcher implements Watcher {

        @Override
        public void process(WatchedEvent event) {
            System.out.println("a node data changed ..." + event.getPath());
        }
    }


}
