package com.voc.print.util;

import com.alibaba.fastjson.JSONObject;
import com.voc.print.config.ClientConfig;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/12/13 10:16
 */
public class ConfigUtilsTest {

    @Test
    public void _1saveConfigFile() {
        ConfigUtils.saveConfigFile(new File("test.txt"), "测试".getBytes());
    }

    @Test
    public void _2saveJSONConfig() {
        ConfigUtils.saveJSONConfig(new File("test2.txt"), new ClientConfig());
    }

    @Test
    public void _3saveJSONConfig1() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "coffee");
        ConfigUtils.saveJSONConfig(new File("test3.txt"), jsonObject);
    }

    @Test
    public void _4read4JSONConfigFile() throws IOException {
        ClientConfig clientConfig = ConfigUtils.read4JSONConfigFile(new File("test2.txt"), ClientConfig.class);
        System.out.println(clientConfig);
    }

    @Test
    public void _5read4JSONObject() {
        ClientConfig clientConfig = ConfigUtils.read4JSONObject(new JSONObject(), ClientConfig.class);
        System.out.println(clientConfig);
    }

}