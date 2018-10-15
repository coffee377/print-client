package com.voc.print.socket;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/10 19:04
 */
public class ChatObject {
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ChatObject() {
    }

    public ChatObject(String message) {
        this.message = message;
    }

}
