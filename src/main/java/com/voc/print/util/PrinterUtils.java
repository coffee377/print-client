package com.voc.print.util;

import com.alibaba.fastjson.JSON;

import javax.print.DocFlavor;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.MediaTray;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/12/27 16:55
 */
public class PrinterUtils {

    /**
     * 获取打印机
     *
     * @return 打印机名称
     */
    public static List<PrintService> getPrinterArray() {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(DocFlavor.INPUT_STREAM.AUTOSENSE, new HashPrintRequestAttributeSet());
        return new ArrayList<>(Arrays.asList(printServices));
    }

    /**
     * 获取打印机名称
     *
     * @return 打印机名称
     */
    public static List<String> getPrinterNameArray() {
        List<String> names = new ArrayList<>();
        List<PrintService> printService = getPrinterArray();
        printService.forEach(s -> names.add(s.getName()));
        return names;
    }

    public static void main(String[] args) throws FileNotFoundException, PrintException {
        List<String> printerNameArray = getPrinterNameArray();
        System.out.println(JSON.toJSONString(printerNameArray));

        List<PrintService> printService = getPrinterArray();

        DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
        if (printer.isAttributeCategorySupported(Media.class)) {
            Object attributeValues = printer.getSupportedAttributeValues(Media.class, null, null);
            if (attributeValues instanceof Media[]) {
                Media[] medias = (Media[]) attributeValues;
                for (Media media : medias) {
                    if (media instanceof MediaSizeName) {
                        MediaSize mediaSize = MediaSize.getMediaSizeForName((MediaSizeName) media);
                        System.out.println("纸张型号：" + mediaSize.getMediaSizeName() + " "
                                + getMediaName(mediaSize.getMediaSizeName().toString()) + " Width:"
                                + mediaSize.getX(Size2DSyntax.MM) + " Height:" + mediaSize.getY(Size2DSyntax.MM));
                    } else if (media instanceof MediaTray) {
                        System.out.println("纸张来源：" + media);
                    }
                }
            }
        }

    }

    private static ResourceBundle messageRB = ResourceBundle.getBundle("sun.print.resources.serviceui");

    private static String getMediaName(String var1) {
        try {
            String var2 = var1.replace(' ', '-');
            var2 = var2.replace('#', 'n');
            return messageRB.getString(var2);
        } catch (MissingResourceException var3) {
            return var1;
        }
    }
}
