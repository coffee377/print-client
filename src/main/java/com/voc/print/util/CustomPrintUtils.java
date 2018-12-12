package com.voc.print.util;

import com.alibaba.fastjson.JSONException;
import com.fr.general.ComparatorUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fr.stable.StringUtils;
import com.fr.third.com.lowagie.text.PageSize;
import com.fr.third.com.lowagie.text.Rectangle;
import com.fr.third.com.lowagie.text.pdf.PdfReader;
import com.voc.print.file.FileUtils;
import com.voc.print.file.ImgPrintable;
import com.voc.print.file.PrintConfig;
import com.voc.print.config.ServerConfig;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.PageRanges;
import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/12 12:49
 */
public class CustomPrintUtils {
    public CustomPrintUtils() {
    }

    public static void printWithJsonArg(JSONObject var0) {
        try {
            String var1 = var0.getString("url");
            String var2 = FileUtils.getFileName(var1);
            String var3 = FileUtils.makeSureExistAndReturnPath(var1, var2);
            if (FileUtils.isFileExist(var3)) {
                PrintConfig var4;
                if (ServerConfig.getInstance().isQuietPrint()) {
                    var4 = PrintConfig.loadFromServer();
                } else {
                    var4 = PrintConfig.load(var0);
                    ServerConfig.update(var4);
                }

                doPrint(var3, var4);
            }
        } catch (JSONException var5) {
            var5.printStackTrace();
        }

    }

    public static void readConfigToData(JSONObject var0, String var1) {
        try {
            var0.put("marginLeft", 0);
            var0.put("marginRight", 0);
            var0.put("marginTop", 0);
            var0.put("marginBottom", 0);
            JSONArray var2 = new JSONArray();
            Field[] var3 = PageSize.class.getDeclaredFields();
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                Field var6 = var3[var5];
                if ("Rectangle".equals(var6.getType().getSimpleName())) {
                    String var7 = var6.getName();
                    JSONObject var8 = new JSONObject();
                    var8.put("text", var7);
                    var8.put("value", var7);
                    var2.add(var8);
                }
            }

            var0.put("paperSizeNames", var2);
        } catch (JSONException var11) {
            var11.printStackTrace();
        }

        if (var1.toUpperCase().endsWith(".PDF")) {
            String var12 = FileUtils.getFileName(var1);
            String var13 = FileUtils.makeSureExistAndReturnPath(var1, var12);
            if (FileUtils.isFileExist(var13)) {
                try {
                    PdfReader var14 = new PdfReader(var13);
                    var0.put("reportTotalPage", var14.getNumberOfPages());
                    Rectangle var15 = var14.getPageSize(1);
                    float var16 = var15.getWidth();
                    float var17 = var15.getHeight();
                    var0.put("paperSize", getPaperSize(var16, var17));
                    var0.put("orientation", getOrientation(var16, var17));
                } catch (IOException var9) {
                    var9.printStackTrace();
                } catch (JSONException var10) {
                    var10.printStackTrace();
                }
            }

        }
    }

    private static PrintService getPrintService(PrintConfig var0) {
        PrintService var1 = null;
        PrintService[] var2 = PrintServiceLookup.lookupPrintServices(DocFlavor.INPUT_STREAM.AUTOSENSE, (AttributeSet)null);
        if (StringUtils.isNotEmpty(var0.getPrinterName())) {
            for(int var3 = 0; var3 < var2.length; ++var3) {
                if (ComparatorUtils.equals(var0.getPrinterName(), var2[var3].getName())) {
                    var1 = var2[var3];
                    break;
                }
            }
        }

        if (var1 == null) {
            var1 = PrintServiceLookup.lookupDefaultPrintService();
        }

        if (var1 == null) {
            if (var2.length > 0) {
                var1 = var2[0];
            } else {
                JOptionPane.showMessageDialog((Component)null, "no printer found!");
            }
        }

        return var1;
    }

    private static Printable getPrintable(String var0, PrintConfig var1) throws IOException {
        if (FileUtils.isPDF(var0)) {
            PDDocument var2 = PDDocument.load(new File(var0));
            var1.setReportTotalPage(var2.getNumberOfPages());
            return new PDFPrintable(var2);
        } else {
            return new ImgPrintable(var0);
        }
    }

    private static void doPrint(String var0, PrintConfig var1) {
        try {
            PrintService var2 = getPrintService(var1);
            if (var2 == null) {
                return;
            }

            PrinterJob var3 = PrinterJob.getPrinterJob();
            var3.setPrintService(var2);
            HashPrintRequestAttributeSet var4 = new HashPrintRequestAttributeSet();
            if (StringUtils.isNotEmpty(var1.getIndex())) {
                String[] var5 = var1.getIndex().split("-");
                int var6 = Integer.parseInt(var5[0]);
                int var7 = var5.length > 1 ? Integer.parseInt(var5[1]) : var6;
                PageRanges var8 = new PageRanges(var6, var7);
                var4.add(var8);
            }

            PageFormat var13 = var3.getPageFormat(var4);
            var13.setOrientation(var1.getOrientation() == 0 ? 1 : 0);
            Paper var14 = new Paper();
            var14.setSize(var1.getWidth(), var1.getHeight());
            var14.setImageableArea((double)var1.getMarginLeftPix(), (double)var1.getMarginTopPix(), var1.getWidth() - (double)(var1.getMarginLeftPix() + var1.getMarginRightPix()), var1.getHeight() - (double)(var1.getMarginTopPix() + var1.getMarginBottomPix()));
            var13.setPaper(var14);
            Printable var15 = getPrintable(var0, var1);
            Book var16 = new Book();

            for(int var9 = 0; var9 < var1.getCopy(); ++var9) {
                var16.append(var15, var13, var1.getReportTotalPage());
            }

            var3.setPageable(var16);
            var3.print();
        } catch (Exception var10) {
            var10.printStackTrace();
        }

    }

    private static int getOrientation(float var0, float var1) {
        return var0 > var1 ? 1 : 0;
    }

    private static String getPaperSize(float var0, float var1) {
        if (getOrientation(var0, var1) == 1) {
            float var2 = var0;
            var0 = var1;
            var1 = var2;
        }

        for(int var5 = 10; var5 >= 0; --var5) {
            String var3 = "A" + var5;
            Rectangle var4 = PageSize.getRectangle(var3);
            if (var4.getWidth() >= var0 && var4.getHeight() >= var1) {
                return var3;
            }
        }

        return "A4";
    }

    public static void main(String[] var0) {
        String var1 = "/Users/plough/Downloads/gettingstarted.pdf";
        String var2 = FileUtils.getFileName(var1);
        String var3 = FileUtils.makeSureExistAndReturnPath(var1, var2);
        if (FileUtils.isFileExist(var3)) {
            JSONObject var4 = new JSONObject();

            try {
                var4.put("marginLeft", "0");
                var4.put("marginTop", "0");
                var4.put("paperSize", "A2");
                var4.put("orientation", 1);
            } catch (JSONException var6) {
                var6.printStackTrace();
            }

            PrintConfig var5 = PrintConfig.load(var4);
            doPrint(var3, var5);
        }

    }
}
