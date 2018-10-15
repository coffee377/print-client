package com.voc.print.config;

import com.fr.general.IOUtils;
import com.fr.json.JSONObject;
import com.voc.print.file.PrintConfig;
import com.fr.stable.ProductConstants;
import com.fr.stable.StableUtils;
import com.fr.third.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.File;
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
    private static final String CONFIG_FILE_PATH = StableUtils.pathJoin(ProductConstants.getEnvHome(), "print.config");

    /**
     * 打印机名称
     */
    private String printerName = "";
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
    private boolean isQuietPrint = false;
    /**
     * 索引
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
        ServerConfig var1 = getInstance();

        var1.setPrinterName(var0.optString("printerName"));
        var1.setCopy(var0.optInt("copy"));
        var1.setOrientation(var0.optInt("orientation"));
        var1.setPaperSizeText(var0.optString("paperSize"));
        float var2 = (float) var0.optDouble("marginTop");
        float var3 = (float) var0.optDouble("marginLeft");
        float var4 = (float) var0.optDouble("marginBottom");
        float var5 = (float) var0.optDouble("marginRight");
        var1.setMargin(var2, var3, var4, var5);
        var1.setIndex(var0.optString("index"));
        var1.setQuietPrint(var0.optBoolean("quietPrint"));
        var1.saveFile();
    }

    public void saveFile() {
        File file = new File(CONFIG_FILE_PATH);
        ObjectMapper var2 = new ObjectMapper();

        try {
            byte[] var3 = var2.writerWithDefaultPrettyPrinter().writeValueAsBytes(ConfigHolder.serverConfig);
            StableUtils.makesureFileExist(file);
            FileOutputStream var4 = new FileOutputStream(CONFIG_FILE_PATH);
            IOUtils.copyBinaryTo(new ByteArrayInputStream(var3), var4);
            var4.close();
        } catch (IOException var5) {
            var5.printStackTrace();
        }
    }

    private static ServerConfig readFromFile() {
        File file = new File(CONFIG_FILE_PATH);
        ObjectMapper var1 = new ObjectMapper();
        if (file.exists()) {
            try {
                return var1.readValue(file, ServerConfig.class);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        return new ServerConfig();
    }

    private static final class ConfigHolder {
        private static ServerConfig serverConfig = ServerConfig.readFromFile();

        private ConfigHolder() {
        }
    }
}
