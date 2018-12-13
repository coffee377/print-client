package com.voc.print.socket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/10 19:04
 */
public class ChatObject implements Serializable {
    private JSONObject jsonObject;

    public String getMessage() {
        return JSON.toJSONString(jsonObject);
    }

    public JSONObject getJSONMessage() {
        return jsonObject;
    }

    private ChatObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    private ChatObject(String message) {
        this(JSON.parseObject(message));
    }

    public static ChatObject of(String message) {
        return new ChatObject(message);
    }

    public static ChatObject of(JSONObject message) {
        return new ChatObject(message);
    }

}
