package com.babyduncan.zookeeperMap;

import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-7-22 18:14
 */
public class FunctionUtil {
    private static final Logger logger = LoggerFactory.getLogger(FunctionUtil.class);

    /**
     * String -> byte[] 的转换函数
     */
    public static class StringToByteArray implements Function<String, byte[]> {
        @Override
        public byte[] apply(String input) {
            if (input == null) {
                return null;
            }
            try {
                return input.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.error(e.toString(), e);
            }
            return null;
        }
    }

    /**
     * byte[] -> String 的转换函数
     */
    public static class ByteArrayToString implements Function<byte[], String> {
        @Override
        public String apply(byte[] input) {
            if (input == null) {
                return null;
            }
            try {
                return new String(input, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.error(e.toString(), e);
            }
            return null;
        }
    }
}
