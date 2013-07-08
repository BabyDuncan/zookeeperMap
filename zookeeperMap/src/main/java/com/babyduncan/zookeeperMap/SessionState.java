package com.babyduncan.zookeeperMap;

/**
 * zookeeper session state
 * <p/>
 * User: guohaozhao (guohaozhao@sohu-inc.com)
 * Date: 13-7-8 17:11
 */
public class SessionState {
    private final long sessionId;

    private final byte[] sessionPassword;


    public SessionState(long sessionId, byte[] sessionPassword) {
        this.sessionId = sessionId;
        this.sessionPassword = sessionPassword;
    }

    public byte[] getSessionPassword() {
        return sessionPassword;
    }

    public long getSessionId() {
        return sessionId;
    }
}
