package com.voc.print.util;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/10 16:49
 */
public class WebUtils {

    public static URI getDomain(URI uri) {
        URI effectiveURI;

        try {
            effectiveURI = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null);
        } catch (Throwable var4) {
            effectiveURI = null;
        }

        return effectiveURI;
    }

    /**
     * 替换 URI 域名
     *
     * @param source 源URI
     * @param scheme 协议
     * @param host   主机
     * @param port   端口
     * @return URI
     */
    public static URI replaceDomain(URI source, String scheme, String host, int port) {
        URI effectiveURI = null;

        try {
            effectiveURI = new URI(scheme, source.getUserInfo(), host, port, source.getPath(), source.getQuery(), source.getFragment());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return effectiveURI;
    }

    /**
     * 替换 URI 域名
     *
     * @param source      URI
     * @param replacement 协议
     * @return URI
     */
    public static URI replaceDomain(URI source, URI replacement) {
        URI effectiveURI = null;

        try {
            effectiveURI = new URI(replacement.getScheme(), source.getUserInfo(), replacement.getHost(), replacement.getPort(), source.getPath(), source.getQuery(), source.getFragment());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return effectiveURI;
    }

    /**
     * 获取域名字符串
     *
     * @param uri URI
     * @return String
     */
    public static String getDomainString(URI uri) {
        return getDomain(uri).toString();
    }


}
