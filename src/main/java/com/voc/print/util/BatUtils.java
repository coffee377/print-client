package com.voc.print.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2019/01/09 14:27
 */
@Slf4j
public class BatUtils {


    public static void exec(String batFileDir, String batFileName) {
        String[] split = batFileDir.replace("\\", "/").split("/");
        StringBuffer sb;
        sb = new StringBuffer();
        boolean hasSpace = false;
        for (String s : split) {
            if (s.contains(" ")) {
                hasSpace = true;
            }
            sb.append(s).append("/");
        }
        String cmd1 = hasSpace ? "\"" + sb.toString() + "nircmd.exe\"" : sb.toString() + "nircmd.exe";
        String cmd2 = hasSpace ? "\"" + sb.toString() + batFileName + "\"" : sb.toString() + batFileName;
        try {
            Runtime.getRuntime().exec(cmd1 + " elevate " + cmd2);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }

}
