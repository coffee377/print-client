package com.voc.print.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

/**
 * 客户端配置文件
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/14 00:12
 */
@Getter
@Setter
public class ClientConfig {

    /**
     * 默认打印机名称
     */
    private String printerName = "";

    /**
     * 是否静默打印
     */
    @JSONField(ordinal = 1)
    private boolean quietPrint = true;

    /**
     * 是否Nginx代理
     */
    @JSONField(ordinal = 2)
    private boolean proxy = false;

    /**
     * 代理服务地址 协议 + 域名 + 端口
     */
    @JSONField(ordinal = 3)
    private String serverURL = "";

    /**
     * 页面尺寸
     */
    private String paperSizeText;

    /**
     * 打印份数
     */
    private int copy;


    /**
     * 打印方向
     */
    private int orientation;

    /**
     * 打印页码
     */
    private String index;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

}
