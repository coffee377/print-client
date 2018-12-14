package com.voc.print.config;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 客户端静默打印配置文件
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/14 00:12
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ClientConfig extends BaseJsonConfig {

    /**
     * 默认打印机名称
     */
    @JSONField(ordinal = 1)
    private String printerName;

    /**
     * applet 是否显示对话框
     */
    @JSONField(ordinal = 2)
    private boolean showDialog;

    /**
     * 是否静默打印（web 页面是否弹出配置或预览界面）
     */
    @JSONField(ordinal = 3)
    private boolean quietPrint = true;

    /**
     * 是否Nginx代理
     */
    @JSONField(ordinal = 4)
    private boolean proxy;

    /**
     * 代理服务地址 协议 + 域名 + 端口
     */
    @JSONField(ordinal = 5)
    private String serverURL;

    /**
     * 页面尺寸
     */
    @JSONField(ordinal = 6)
    private String paperSizeText;

    /**
     * 上边距
     */
    @JSONField(ordinal = 7)
    private Float marginTop;

    /**
     * 左边距
     */
    @JSONField(ordinal = 8)
    private Float marginLeft;

    /**
     * 下边距
     */
    @JSONField(ordinal = 9)
    private Float marginBottom;

    /**
     * 右边距
     */
    @JSONField(ordinal = 10)
    private Float marginRight;

    /**
     * 打印份数
     */
    @JSONField(ordinal = 11)
    private int copy = 1;

    /**
     * 打印方向（0 纵向 1 横向）
     *
     * @see PageOrientation
     */
    @JSONField(ordinal = 12)
    private int orientation = PageOrientation.VERTICAL_PRINT.getValue();

    /**
     * 打印页码
     */
    @JSONField(ordinal = 13)
    private String index;

    /**
     * 自动缩放
     */
    @JSONField(ordinal = 14)
    private boolean autoScaling;

    /**
     * 是否缓存页面设置
     */
    @JSONField(ordinal = 15)
    private boolean cachePaperSetting = true;

    @Override
    @JSONField(serialize = false)
    public String getName() {
        return "Client Configuration File";
    }

    /**
     * 是否内部修改
     */
    @JSONField(serialize = false)
    private boolean internalChanges;

}
