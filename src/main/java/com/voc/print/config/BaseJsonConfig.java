package com.voc.print.config;

import com.alibaba.fastjson.JSON;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/12/13 09:56
 */
public abstract class BaseJsonConfig implements Serializable {

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    /**
     * 配置文件名称
     *
     * @return String
     */
    public abstract String getName();

}
