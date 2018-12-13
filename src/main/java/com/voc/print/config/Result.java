package com.voc.print.config;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 响应数据封装
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/11/04 23:07
 */
@Getter
@Setter
public class Result implements Serializable {

    /**
     * 是否成功
     */
    @JSONField
    private boolean success = false;

    /**
     * 响应编码
     */
    @JSONField(ordinal = 1)
    private String code;

    /**
     * 响应信息
     */
    @JSONField(ordinal = 2)
    private String message;

    /**
     * 响应数据
     */
    @JSONField(ordinal = 3)
    private Object data;

    private Result() {
    }

    public static Result of(boolean success, String code, String message, Object data) {
        Result item = new Result();
        item.setSuccess(success);
        item.setCode(code);
        item.setMessage(message);
        item.setData(data);
        return item;
    }

    public static Result success(Object data) {
        return of(true, "0", "SUCCESS", data);
    }

    public static Result success() {
        return success(new Object());
    }

    public static Result failure(String code, String message, Object data) {
        return of(false, code, message, data);
    }

    public static Result failure(String code, String message) {
        return failure(code, message, new Object());
    }

}
