package com.voc.print.file;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/12 12:53
 */
public class FileUtils {
    public FileUtils() {
    }

    public static String makeSureExistAndReturnPath(String var0, String var1) {
        if (isLocalFilePath(var0)) {
            return var0;
        } else {
            if (!isCacheFileExist(var1)) {
                downloadFile(var0, var1);
            }

            return isCacheFileExist(var1) ? getFilePath(var1) : "";
        }
    }

    public static boolean isCacheFileExist(String var0) {
        return isFileExist(getFilePath(var0));
    }

    public static boolean isFileExist(String var0) {
        return (new File(var0)).exists();
    }

    public static boolean isPDF(String var0) {
        return var0.toLowerCase().endsWith(".pdf");
    }

    public static boolean isImg(String var0) {
        var0 = var0.toLowerCase();
        return var0.endsWith("jpg") || var0.endsWith("jpeg") || var0.endsWith("gif") || var0.endsWith("png");
    }

    public static String getFileName(String var0) {
        try {
            var0 = URLDecoder.decode(var0, "UTF-8");
            String[] var1 = var0.split("/");
            return var1[var1.length - 1];
        } catch (UnsupportedEncodingException var2) {
            var2.printStackTrace();
            return "";
        }
    }

    public static String getFilePath(String var0) {
        File var1 = new File(System.getProperty("user.dir").concat("/downFile"));
        if (!var1.exists() && !var1.isDirectory()) {
            var1.mkdir();
        }

        String var2 = var1 + "/" + var0;
        return var2;
    }

    public static boolean isLocalFilePath(String var0) {
        return (new File(var0)).isFile();
    }

    public static boolean downloadFile(String var0, String var1) {
        try {
            URL var2 = new URL(var0);
            HttpURLConnection var3 = (HttpURLConnection)var2.openConnection();
            DataInputStream var4 = new DataInputStream(var3.getInputStream());
            DataOutputStream var5 = new DataOutputStream(new FileOutputStream(getFilePath(var1)));
            byte[] var6 = new byte[4096];

            int var7;
            while((var7 = var4.read(var6)) > 0) {
                var5.write(var6, 0, var7);
            }

            var5.close();
            var4.close();
            return true;
        } catch (Exception var8) {
            return false;
        }
    }
}
