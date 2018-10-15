package com.voc.print.file;

import com.fr.json.JSONObject;
import com.voc.print.config.ServerConfig;
import com.fr.stable.unit.MM;
import com.fr.third.com.lowagie.text.PageSize;
import com.fr.third.com.lowagie.text.Rectangle;
import lombok.Getter;
import lombok.Setter;

/**
 * 打印配置
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/10 14:09
 */
@Getter
@Setter
public class PrintConfig {
    /**
     * 页面宽度
     */
    private double width;

    /**
     * 页面高度
     */
    private double height;

    /**
     * 页面尺寸
     */
    private String paperSizeText;

    /**
     * 上边距
     */
    private float marginTop;

    /**
     * 下边距
     */
    private float marginBottom;

    /**
     * 左边距
     */
    private float marginLeft;

    /**
     * 右边距
     */
    private float marginRight;

    /**
     * 打印方向
     */
    private int orientation;

    /**
     * 打印页面
     */
    private String index;

    /**
     * 打印份数
     */
    private int copy;

    /**
     * 打印机名称
     */
    private String printerName;

    /**
     * 报表总页数
     */
    private int reportTotalPage;

    /**
     * 是否静默打印
     */
    private boolean quietPrint;

    public int getMarginTopPix() {
        return mm2Pix(this.marginTop);
    }

    public int getMarginBottomPix() {
        return mm2Pix(this.marginBottom);
    }

    public int getMarginLeftPix() {
        return mm2Pix(this.marginLeft);
    }

    public int getMarginRightPix() {
        return mm2Pix(this.marginRight);
    }

    private int mm2Pix(float mm) {
        return (new MM(mm)).toPixI(72);
    }

    public PrintConfig() {
        this.init(JSONObject.create());
    }

    private PrintConfig(JSONObject json) {
        this.init(json);
    }

    private void init(JSONObject jsonObject) {
        String pageText = jsonObject.optString("paperSize", "A4");
        this.paperSizeText = pageText;
        Rectangle rectangle = PageSize.getRectangle(pageText);
        this.width = (double) rectangle.getWidth();
        this.height = (double) rectangle.getHeight();
        this.marginTop = (float) jsonObject.optDouble("marginTop", 0.0D);
        this.marginLeft = (float) jsonObject.optDouble("marginLeft", 0.0D);
        this.marginBottom = (float) jsonObject.optDouble("marginBottom", 0.0D);
        this.marginRight = (float) jsonObject.optDouble("marginRight", 0.0D);
        this.orientation = jsonObject.optInt("orientation", 0);
        this.index = jsonObject.optString("index");
        this.copy = jsonObject.optInt("copy", 1);
        this.printerName = jsonObject.optString("printerName", "");
        this.reportTotalPage = jsonObject.optInt("reportTotalPage", 1);
        this.quietPrint = jsonObject.optBoolean("quietPrint", false);
    }

    public static PrintConfig load(JSONObject jsonObject) {
        return new PrintConfig(jsonObject);
    }

    public static PrintConfig loadFromServer() {
        PrintConfig var0 = new PrintConfig(JSONObject.create());
        ServerConfig var1 = ServerConfig.getInstance();
        Rectangle var2 = PageSize.getRectangle(var1.getPaperSizeText());
        var0.paperSizeText = var1.getPaperSizeText();
        var0.width = (double)var2.getWidth();
        var0.height = (double)var2.getHeight();
        var0.marginTop = var1.getMarginTop();
        var0.marginLeft = var1.getMarginLeft();
        var0.marginBottom = var1.getMarginBottom();
        var0.marginRight = var1.getMarginRight();
        var0.orientation = var1.getOrientation();
        var0.index = var1.getIndex();
        var0.copy = var1.getCopy();
        var0.printerName = var1.getPrinterName();
        var0.quietPrint = var1.isQuietPrint();
        return var0;
    }
}
