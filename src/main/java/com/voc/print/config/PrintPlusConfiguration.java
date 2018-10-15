package com.voc.print.config;

import com.alibaba.fastjson.JSON;
import com.voc.print.util.ConfigUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/14 17:05
 */
@Slf4j
@Getter
@Setter
public class PrintPlusConfiguration {
    /**
     * 配置文件路径
     */
    public static final String CONFIG_PRINT_PLUS_JSON = "../config/PrintPlus.json";

    /**
     * 客户端配置文件
     */
    private ClientConfig clientConfig;

    private static PrintPlusConfiguration printPlusConfiguration = new PrintPlusConfiguration();

    private PrintPlusConfiguration() {
        this.clientConfig = this.readFromFile(new File(CONFIG_PRINT_PLUS_JSON));
    }

    public static PrintPlusConfiguration getInstance() {
        return printPlusConfiguration;
    }

    /**
     * 监听配置的变动
     *
     * @return 是否监听成功
     */
    public boolean listening() {
        try {
            File file = new File(CONFIG_PRINT_PLUS_JSON);
            /*轮询间隔 5 秒*/
            long interval = TimeUnit.SECONDS.toMillis(5);
            /*创建一个文件观察器用于处理文件的格式*/
            FileAlterationObserver observer = new FileAlterationObserver(file.getParentFile(),
                    FileFilterUtils.and(FileFilterUtils.fileFileFilter(), FileFilterUtils.nameFileFilter(file.getName())));
            /*设置文件变化监听器*/
            observer.addListener(new FileListener());
            FileAlterationMonitor monitor = new FileAlterationMonitor(interval, observer);
            monitor.start();
            return true;
        } catch (Exception e) {
            log.error("listener error! e:{}", e);
            return false;
        }
    }

    /**
     * 读取客户端配置文件
     *
     * @param file File
     * @return ClientConfig
     */
    private ClientConfig readFromFile(File file) {
        byte[] bytes = new byte[0];
        try {
            bytes = IOUtils.toByteArray(new FileInputStream(file));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return JSON.parseObject(bytes, ClientConfig.class);
    }

    /**
     * 文件监听类
     */
    class FileListener extends FileAlterationListenerAdaptor {

        @Override
        public void onFileChange(File file) {
            PrintPlusConfiguration.this.clientConfig = PrintPlusConfiguration.this.readFromFile(file);
            if (log.isInfoEnabled()) {
                log.info("{} changed: {}", file.getName(), PrintPlusConfiguration.this.clientConfig.toString());
            }
        }


        @Override
        public void onFileDelete(File file) {
            log.warn("{} deleted, default config file will be created", file.getName());
            ConfigUtils.saveDefaultClientConfig();
        }

    }
}
