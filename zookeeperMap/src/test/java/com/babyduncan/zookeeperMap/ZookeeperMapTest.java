package com.babyduncan.zookeeperMap;

import org.junit.Test;

/**
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-7-22 18:12
 */
public class ZookeeperMapTest {

    @Test
    public void testDelegate() throws InterruptedException {
        FunctionUtil.ByteArrayToString byteArrayToString = new FunctionUtil.ByteArrayToString();

        ZookeeperClient zookeeperClient = new ZookeeperClient("localhost:2181", 6000, null, null);

        ZookeeperMap<String> zkMap = ZookeeperMap.createZookeeperMap(zookeeperClient, "/babyduncan", byteArrayToString);

        for (int i = 0; i < 30; i++) {
            System.out.println(zkMap.delegate());
            Thread.sleep(1000);
        }
    }

    @Test
    public void testRigidDelegate() throws InterruptedException {
        FunctionUtil.ByteArrayToString byteArrayToString = new FunctionUtil.ByteArrayToString();

        ZookeeperClient zookeeperClient = new ZookeeperClient("localhost:2181", 6000, null, null);

        ZookeeperMap<String> zkMap = ZookeeperMap.createRigidZookeeperMap(zookeeperClient, "/babyduncan", byteArrayToString);

        for (int i = 0; i < 30; i++) {
            System.out.println(zkMap.delegate());
            Thread.sleep(1000);
        }
    }
}
