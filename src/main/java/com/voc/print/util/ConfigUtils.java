package com.voc.print.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.voc.print.config.BaseJsonConfig;
import com.voc.print.config.ClientConfig;
import com.voc.print.config.PrintPlusConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/14 16:12
 */
@Slf4j
public class ConfigUtils {

    /**
     * 保存配置文件
     *
     * @param file  File save file name
     * @param datas byte[]
     */
    public static void saveConfigFile(File file, byte[] datas) {
        try {
            FileUtils.writeByteArrayToFile(file, datas);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * JSON格式保存配置文件
     *
     * @param file   save file name
     * @param config config class
     * @param <T>    T extends BaseConfig
     */
    public static <T extends BaseJsonConfig> void saveJSONConfig(File file, T config) {
        byte[] bytes = JSON.toJSONBytes(config, SerializerFeature.PrettyFormat);
        saveConfigFile(file, bytes);
    }

    /**
     * JSON格式保存配置文件
     *
     * @param file  File save file name
     * @param datas JSONObject
     */
    public static void saveJSONConfig(File file, JSONObject datas) {
        byte[] bytes = JSON.toJSONBytes(datas, SerializerFeature.PrettyFormat);
        saveConfigFile(file, bytes);
    }

    /**
     * 读取配置文件
     *
     * @param file File
     * @return Class<T>
     */
    public static <T> T read4JSONConfigFile(File file, Class<T> clazz) throws IOException {
        byte[] bytes = IOUtils.toByteArray(new FileInputStream(file));
        return JSON.parseObject(bytes, clazz);
    }

    /**
     * 读取配置文件
     *
     * @param json JSONObject
     * @return Class<T>
     */
    public static <T> T read4JSONObject(JSONObject json, Class<T> clazz) {
        byte[] bytes = JSON.toJSONBytes(json);
        return JSON.parseObject(bytes, clazz);
    }

    /**
     * 保存客户端配置文件
     *
     * @param config JSONObject
     */
    @Deprecated
    public static void saveOrUpdateClientConfig(JSONObject config) {
        String s = JSON.toJSONString(config);
        ClientConfig clientConfig = JSON.parseObject(s, ClientConfig.class);
        saveOrUpdateClientConfig(clientConfig);
    }

    /**
     * 保存客户端配置文件
     *
     * @param clientConfig ClientConfig
     */
    @Deprecated
    public static void saveOrUpdateClientConfig(ClientConfig clientConfig) {
        File file = new File(PrintPlusConfiguration.CONFIG_PRINT_PLUS_JSON);
        saveOrUpdateClientConfig(file, clientConfig);
    }

    /**
     * 保存客户端配置文件
     *
     * @param file         File
     * @param clientConfig ClientConfig
     */
    @Deprecated
    public static void saveOrUpdateClientConfig(File file, ClientConfig clientConfig) {
        byte[] bytes = JSONObject.toJSONBytes(clientConfig, SerializerFeature.PrettyFormat);
        saveConfigFile(file, bytes);
    }

    /**
     * 生成默认配置文件
     */
    @Deprecated
    public static void saveDefaultClientConfig() {
        saveOrUpdateClientConfig(new ClientConfig());
    }

}
