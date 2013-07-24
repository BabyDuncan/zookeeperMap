package com.babyduncan.zookeeperMap;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-7-22 18:12
 */
public class ZookeeperMapTest {

    @Test
    public void testDelegate() throws InterruptedException {
        FunctionUtil.ByteArrayToString byteArrayToString = new FunctionUtil.ByteArrayToString();

        ZookeeperClient zookeeperClient = new ZookeeperClient("localhost:2181", 6000, null, null);

        Map<String, ZookeeperMap<String>> m = new HashMap<String, ZookeeperMap<String>>();

        ZookeeperMap<String> zkMap1 = ZookeeperMap.createZookeeperMap(zookeeperClient, "/babyduncan", byteArrayToString);
        ZookeeperMap<String> zkMap2 = ZookeeperMap.createZookeeperMap(zookeeperClient, "/foo", byteArrayToString);

        m.put("1", zkMap1);
        m.put("2", zkMap2);

        for (int i = 0; i < 30; i++) {
            System.out.println(m);
            Thread.sleep(2000);
        }
    }

    @Test
    public void testRigidDelegate() throws InterruptedException {
        FunctionUtil.ByteArrayToString byteArrayToString = new FunctionUtil.ByteArrayToString();

        ZookeeperClient zookeeperClient = new ZookeeperClient("localhost:2181", 6000, null, null);

        ZookeeperMap<String> zkMap = ZookeeperMap.createRigidZookeeperMap(zookeeperClient, "/babyduncan", byteArrayToString);

        for (int i = 0; i < 30; i++) {
            try {
                System.out.println(zkMap.delegate());
                Thread.sleep(1000);
            } catch (Exception e) {
                continue;
            }
        }
    }
}
