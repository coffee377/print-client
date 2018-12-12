package com.voc.print.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fr.stable.ProductConstants;
import com.fr.stable.StableUtils;
import com.voc.print.file.PrintConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 服务配置
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/10 13:49
 */
@Getter
@Setter
@Slf4j
public class ServerConfig {
    private static final String CONFIG_FILE_PATH =
            StableUtils.pathJoin(ProductConstants.getEnvHome(), "print.config");

    /**
     * 打印机名称
     */
    private String printerName;
    /**
     * 打印份数
     */
    private int copy;
    /**
     * 打印方向
     */
    private int orientation;
    /**
     * 页面尺寸
     */
    private String paperSizeText;
    /**
     * 上边距
     */
    private float marginTop;
    /**
     * 左边距
     */
    private float marginLeft;
    /**
     * 下边距
     */
    private float marginBottom;
    /**
     * 右边距
     */
    private float marginRight;
    /**
     * 是否静默打印
     */
    private boolean quietPrint;
    /**
     * 打印页码
     */
    private String index;

    /**
     * 设置边距
     */
    public void setMargin(float top, float bottom, float left, float right) {
        this.marginTop = top;
        this.marginLeft = left;
        this.marginBottom = bottom;
        this.marginRight = right;
    }

    private ServerConfig() {
    }

    public static ServerConfig getInstance() {
        return ServerConfig.ConfigHolder.serverConfig;
    }

    /**
     * 更新配置文件
     *
     * @param printConfig 打印配置
     */
    public static void update(PrintConfig printConfig) {
        ServerConfig config = getInstance();
        config.setPrinterName(printConfig.getPrinterName());
        config.setCopy(printConfig.getCopy());
        config.setOrientation(printConfig.getOrientation());
        config.setPaperSizeText(printConfig.getPaperSizeText());
        float top = printConfig.getMarginTop();
        float left = printConfig.getMarginLeft();
        float bottom = printConfig.getMarginBottom();
        float right = printConfig.getMarginRight();
        config.setMargin(top, bottom, left, right);
        config.setQuietPrint(printConfig.isQuietPrint());
        config.setIndex(printConfig.getIndex());
        config.saveFile();
    }

    public static void update(JSONObject var0) {
        String s = JSON.toJSONString(var0);
        ServerConfig serverConfig = JSON.parseObject(s, ServerConfig.class);
//        ServerConfig config = getInstance();
//        config.setPrinterName(var0.getString("printerName"));
//        config.setCopy(var0.getInteger("copy"));
//        config.setOrientation(var0.getInteger("orientation"));
//        config.setPaperSizeText(var0.getString("paperSize"));
//        float var2 = (float) var0.getDoubleValue("marginTop");
//        float var3 = (float) var0.getDoubleValue("marginLeft");
//        float var4 = (float) var0.getDoubleValue("marginBottom");
//        float var5 = (float) var0.getDoubleValue("marginRight");
//        config.setMargin(var2, var3, var4, var5);
//        config.setIndex(var0.getString("index"));
//        config.setQuietPrint(var0.getBooleanValue("quietPrint"));
        serverConfig.saveFile();
    }

    private void saveFile() {
//        File file = new File(CONFIG_FILE_PATH);
//        ObjectMapper var2 = new ObjectMapper();
//
//        try {
//            byte[] var3 = var2.writerWithDefaultPrettyPrinter().writeValueAsBytes(ConfigHolder.serverConfig);
//            StableUtils.makesureFileExist(file);
//            FileOutputStream var4 = new FileOutputStream(CONFIG_FILE_PATH);
//            IOUtils.copyBinaryTo(new ByteArrayInputStream(var3), var4);
//            var4.close();
//        } catch (IOException e) {
//            log.error(e.getMessage());
//        }
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE_PATH)) {
            byte[] bytes = JSON.toJSONBytes(this, SerializerFeature.PrettyFormat);
            IOUtils.write(bytes, out);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }

    private static ServerConfig readFromFile() {
        try {
            byte[] bytes = IOUtils.toByteArray(new FileInputStream(CONFIG_FILE_PATH));
            ServerConfig config = JSON.parseObject(bytes, ServerConfig.class);
            return config;
        } catch (IOException e) {
           log.error(e.getMessage());
        }
//        File file = new File(CONFIG_FILE_PATH);
//        ObjectMapper var1 = new ObjectMapper();
//        if (file.exists()) {
//            try {
//                return var1.readValue(file, ServerConfig.class);
//            } catch (IOException e) {
//                log.error(e.getMessage());
//            }
//        }

        return new ServerConfig();
    }

    private static final class ConfigHolder {
        private static ServerConfig serverConfig = ServerConfig.readFromFile();

        private ConfigHolder() {
        }
    }

}
