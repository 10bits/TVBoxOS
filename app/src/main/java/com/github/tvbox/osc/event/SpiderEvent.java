package com.github.tvbox.osc.event;

public class SpiderEvent {
    public static final int TYPE_DATA_INIT_OK = 1;
    public static final int TYPE_JAR_INIT_OK = 2;
    public static final int TYPE_DATA_INIT_SUCCESS = 3;
    public static final int TYPE_JAR_INIT_SUCCESS = 4;
    public int type;

    public SpiderEvent(int type) {
        this.type = type;
    }
}
