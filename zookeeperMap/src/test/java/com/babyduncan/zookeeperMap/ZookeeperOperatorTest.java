package com.babyduncan.zookeeperMap;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-8-8 16:41
 */
public class ZookeeperOperatorTest {

    private static Logger logger = Logger.getLogger(ZookeeperOperatorTest.class);

    @Test
    public void test() throws Exception {

        FunctionUtil.ByteArrayToString byteArrayToString = new FunctionUtil.ByteArrayToString();
        FunctionUtil.StringToByteArray stringToByteArray = new FunctionUtil.StringToByteArray();

        ZookeeperClient zookeeperClient = new ZookeeperClient("localhost:2181", 6000, null, null);

        ZookeeperMap<String> zkMap = ZookeeperMap.createRigidZookeeperMap(zookeeperClient, "/babyduncan", byteArrayToString);
        ZookeeperOperator<String> zkOpe = new ZookeeperOperator<String>(zookeeperClient, "/babyduncan", stringToByteArray, false);

        System.out.println(zkMap.delegate());
        zkOpe.put("foo1", "bar1");
        zkOpe.remove("foo");
        Thread.sleep(5000);
        System.out.println(zkMap.delegate());
        Thread.sleep(20000);
        System.out.println(zkMap.delegate());

    }

}
