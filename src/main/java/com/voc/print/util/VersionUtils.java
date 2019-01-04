package com.voc.print.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2019/01/04 14:35
 */
@Slf4j
public class VersionUtils {

    public static String getVersion() {
        InputStream resourceAsStream = VersionUtils.class.getResourceAsStream("/build.txt");
        String version = "v1.0.0";
        try {
            version = IOUtils.toString(resourceAsStream, "UTF-8");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return version;
    }

}
