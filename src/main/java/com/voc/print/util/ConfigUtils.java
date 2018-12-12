package com.voc.print.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
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
     * 报存配置文件
     *
     * @param file  File
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
     * 保存客户端配置文件
     *
     * @param clientConfig ClientConfig
     */
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
    public static void saveOrUpdateClientConfig(File file, ClientConfig clientConfig) {
        byte[] bytes = JSONObject.toJSONBytes(clientConfig, SerializerFeature.PrettyFormat);
        saveConfigFile(file, bytes);
    }

    /**
     * 生成默认配置文件
     */
    public static void saveDefaultClientConfig() {
        saveOrUpdateClientConfig(new ClientConfig());
    }

    /**
     * 读取客户端配置文件
     *
     * @param file File
     * @return ClientConfig
     */
    public static ClientConfig readFromFile(File file) {
        byte[] bytes;
        try {
            bytes = IOUtils.toByteArray(new FileInputStream(file));
        } catch (Exception e) {
            String result = new ClientConfig().toString();
            return JSON.parseObject(result, ClientConfig.class);
        }
        return JSON.parseObject(bytes, ClientConfig.class);
    }
}
