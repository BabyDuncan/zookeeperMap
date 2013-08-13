package com.babyduncan.zookeeperMap.toturial;

import com.babyduncan.zookeeperMap.FunctionUtil;
import com.babyduncan.zookeeperMap.ZookeeperClient;
import com.babyduncan.zookeeperMap.ZookeeperMap;

/**
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-8-13 15:01
 */
public class SimpleGet {

    public static void main(String... args) {
        FunctionUtil.ByteArrayToString byteArrayToString = new FunctionUtil.ByteArrayToString();
        ZookeeperClient zookeeperClient = new ZookeeperClient("localhost:2181", 6000, null, null);
        ZookeeperMap<String> zookeeperMap = ZookeeperMap.createZookeeperMap(zookeeperClient, "/babyduncan", byteArrayToString);
        System.out.println(zookeeperMap.delegate());
    }

}
