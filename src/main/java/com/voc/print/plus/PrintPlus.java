package com.voc.print.plus;

import com.alibaba.fastjson.JSON;
import com.fr.base.*;
import com.fr.general.Inter;
import com.fr.general.xml.GeneralXMLTools;
import com.alibaba.fastjson.JSONObject;
import com.fr.page.BaseSinglePagePrintable;
import com.fr.page.BaseSingleReportCache;
import com.fr.page.PageXmlProvider;
import com.fr.page.PaperSettingProvider;
import com.fr.print.PrintUtils;
import com.fr.print.SinglePageSet;
import com.fr.report.stable.ReportConstants;
import com.fr.stable.StringUtils;
import com.fr.stable.bridge.StableFactory;
import com.fr.stable.unit.MM;
import com.fr.xml.ReportXMLUtils;
import com.voc.print.config.PrintPlusConfiguration;
import com.voc.print.config.ServerConfig;
import com.voc.print.socket.PrintClientServer;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/10 11:03
 */
@Slf4j
public class PrintPlus {
    private static final String PREFIX = "finereport:";
    private static final String PAGE_NUMBER_SEPARATOR = "-";
    private static final String IS_SHOW_DIALOG = "isShowDialog";
    private static final Pattern PAGE_PATTERN = Pattern.compile(PAGE_NUMBER_SEPARATOR);
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^[1-9]\\d*$");
    private BaseSinglePagePrintable[] pages;
    private static PrintPlus singleton;

    private PrintPlus() {
    }

    public static PrintPlus getInstance() {
        if (singleton == null) {
            singleton = new PrintPlus();
        }

        return singleton;
    }

    /**
     * 打印
     *
     * @param args 打印参数
     */
    public void printWithArgs(String[] args) {
        if (args.length == 0) {
            JOptionPane.showMessageDialog(null, Inter.getLocText("FR-Engine_NativePrint_Invalid_Para"));
        } else {
            JSONObject var2 = null;
            if (args.length == 1) {
                var2 = this.getJsonObjectForModernBrowser(args[0], var2);
            } else {
                var2 = this.getJsonObjectForIE(args, var2);
            }

            if (var2 == null || StringUtils.isEmpty(var2.toString())) {
                return;
            }

            this.printWithJsonArg(var2);

        }
    }

    /**
     * JSON 参数打印
     *
     * @param jsonObject JSONObject
     */
    public void printWithJsonArg(JSONObject jsonObject) {
        this.printWithJsonArg(jsonObject, null);
    }

    /**
     * JSON 参数打印
     *
     * @param jsonObject JSONObject
     * @param uuid       UUID
     */
    public void printWithJsonArg(JSONObject jsonObject, UUID uuid) {
        PrintTray.getInstance().markPrintingTray();
        if (StringUtils.isEmpty(jsonObject.getString(IS_SHOW_DIALOG))) {
            this.init(jsonObject, uuid);
        }
        PrintTray.getInstance().printOver();
    }

    /**
     * 现代浏览器获取 JSON 对象
     *
     * @param param      String
     * @param jsonObject JSONObject
     * @return JSONObject
     */
    private JSONObject getJsonObjectForModernBrowser(String param, JSONObject jsonObject) {
        String var3 = param.substring(PREFIX.length());

        try {
            String var4 = URLDecoder.decode(var3, "UTF-8");
            jsonObject = JSON.parseObject(var4);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return jsonObject;
    }

    private JSONObject getJsonObjectForIE(String[] var1, JSONObject var2) {
//        int var3 = var1.length;
//        AtomicReference<StringBuffer> var4 = new AtomicReference<>(new StringBuffer());
//
//        try {
//            for (int var5 = 0; var5 < var3; ++var5) {
//                var4.get().append(var1[var5]);
//            }
//
//            var2 = JSONObject.create();
//            String var15 = var4.get().substring(PREFIX.length() + 1, var4.get().lastIndexOf("}"));
//            String[] var6 = var15.split(",");
//            String[] var7 = var6;
//            int var8 = var6.length;
//
//            for (int var9 = 0; var9 < var8; ++var9) {
//                String var10 = var7[var9];
//                int var11 = var10.indexOf(":");
//                if (var11 != -1) {
//                    String var12 = var10.substring(0, var11);
//                    String var13 = var10.substring(var11 + 1);
//                    var2.put(var12, var13);
//                }
//            }
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//            PrintTray.getInstance().printOver();
//        }

        return var2;
    }


    /**
     * 打印初始化
     *
     * @param jsonObject JSONObject
     * @param uuid       UUID
     */
    private void init(JSONObject jsonObject, UUID uuid) {
        try {
            String urlString = jsonObject.getString("url");
            if (StringUtils.isEmpty(urlString)) {
                return;
            }
            URL url = new URL(urlString);
            this.printAsApplet(jsonObject, url, uuid);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, " e=" + e.getMessage());
            log.error(e.getMessage(), e);
        }

    }

    /**
     * 小程序打印
     *
     * @param jsonObject JSONObject
     * @param url        URL
     * @param uuid       UUID
     * @throws Exception 异常
     */
    private void printAsApplet(JSONObject jsonObject, URL url, UUID uuid) throws Exception {
        this.initModule();
        ServerConfig.update(jsonObject);
        /*是否显示对话框*/
        boolean isShowDialog = jsonObject.getBooleanValue("isShowDialog");
        /*是否单表*/
        boolean isSingleSheet = jsonObject.getBooleanValue("isSingleSheet");
        /*打印页码*/
        String index = jsonObject.getString("index");
        InputStream inputStream = url.openStream();
        PageXmlProvider pageXmlOperator = StableFactory.getMarkedInstanceObjectFromClass("PageXmlOperator", PageXmlProvider.class);
        BaseSinglePagePrintable[] baseSinglePagePrintables = pageXmlOperator.deXmlizable4SinglePage(inputStream);
//        PrintableSet printableSet = new SinglePageSet(baseSinglePagePrintables);
//        PrintUtils.print(printableSet,true,"");
        BaseSingleReportCache reportCache = StableFactory.getMarkedInstanceObjectFromClass("SingleReportCache", BaseSingleReportCache.class);
//        URL var11 = new URL(url.toString().replaceAll("op=fr_applet&cmd=print", "op=fr_applet&cmd=printover&printvalue=false"));
//        var11.openStream();
        /*打印页面校验*/
        this.validIndex(index, baseSinglePagePrintables, url, reportCache);
        /*根据客户端配置更新页面配置，静默打印应该页面根据给定的纸张自适应打印*/
        if (!PrintPlusConfiguration.getInstance().getClientConfig().isQuietPrint()) {
            this.updatePageSetting();
        }
        this.setUpCopies(ServerConfig.getInstance().getCopy());
        this.cachePaperSetting(isSingleSheet);
        if (uuid != null) {
            PrintClientServer.getInstance().onBeforePrint(uuid);
        }
        this.print(false, ServerConfig.getInstance().getPrinterName());
        reportCache.clearReportPageCache();
//        URL var12 = new URL(url.toString().replaceAll("op=fr_applet&cmd=print", "op=fr_applet&cmd=printover&printvalue=true"));
//        var12.openStream();
    }

    /**
     * 根据客户端配置更新页面相关设置
     */
    private void updatePageSetting() {
        ServerConfig serverConfig = ServerConfig.getInstance();
        BaseSinglePagePrintable[] pagePrintTables = this.pages;

        for (BaseSinglePagePrintable printable : pagePrintTables) {
            PaperSettingProvider paperSetting = printable.getPaperSetting();
            paperSetting.setOrientation(serverConfig.getOrientation());
            String paperSizeText = serverConfig.getPaperSizeText();
            PaperSize paperSize = new PaperSize();
            MM width;
            MM height;
            if (paperSizeText.contains(",")) {
                String[] sizeArr = paperSizeText.split(",\\s*");
                width = new MM(Float.parseFloat(sizeArr[0]));
                height = new MM(Float.parseFloat(sizeArr[1]));
                paperSize = new PaperSize(width, height);
            } else {
                for (int i = 0; i < ReportConstants.PaperSizeNameSizeArray.length; ++i) {
                    if (ReportConstants.PaperSizeNameSizeArray[i][0].toString().equals(paperSizeText)) {
                        paperSize = (PaperSize) ReportConstants.PaperSizeNameSizeArray[i][1];
                        break;
                    }
                }
            }

            paperSetting.setPaperSize(paperSize);
            MM top = new MM(serverConfig.getMarginTop());
            MM left = new MM(serverConfig.getMarginLeft());
            MM bottom = new MM(serverConfig.getMarginBottom());
            MM right = new MM(serverConfig.getMarginRight());
            Margin margin = new Margin(top, left, bottom, right);
            paperSetting.setMargin(margin);
        }

    }

    /**
     * 打印份数
     *
     * @param copies int
     */
    private void setUpCopies(int copies) {
        if (copies > 1) {
            ArrayList<BaseSinglePagePrintable> pagePrintables = new ArrayList<>();
            List<BaseSinglePagePrintable> printables = Arrays.asList(this.pages);

            for (int i = 0; i < copies; ++i) {
                pagePrintables.addAll(printables);
            }

            this.pages = pagePrintables.toArray(new BaseSinglePagePrintable[0]);
        }
    }

    /**
     * 初始化模块
     *
     * @throws Exception 异常
     */
    private void initModule() throws Exception {
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        }

        GeneralXMLTools.Object_Tokenizer = new ReportXMLUtils.ReportObjectTokenizer();
        GeneralXMLTools.Object_XML_Writer_Finder = new ReportXMLUtils.ReportObjectXMLWriterFinder();
        StableFactory.registerXMLDescription("Formula", new Formula());
        StableFactory.registerXMLDescription("TFC", new ResultFormula());
        StableFactory.registerXMLDescription("Parameter", new Parameter());
        StableFactory.registerXMLDescription("MultiFieldParameter", new MultiFieldParameter());
        this.startModule("com.fr.page.module.PageModule");
        this.startModule("com.fr.chart.module.ChartModule");
    }

    /**
     * 校验
     *
     * @param index                    String
     * @param baseSinglePagePrintables BaseSinglePagePrintable[]
     * @param url                      URL
     * @param baseSingleReportCache    BaseSingleReportCache
     */
    private void validIndex(String index, BaseSinglePagePrintable[] baseSinglePagePrintables,
                            URL url, BaseSingleReportCache baseSingleReportCache) {
        if (index != null && index.contains(PAGE_NUMBER_SEPARATOR)) {
            String[] var6 = PAGE_PATTERN.split(index);
            int var7 = Integer.parseInt(var6[0]) - 1;
            int var8 = Integer.parseInt(var6[1]) - 1;
            if (var7 > baseSinglePagePrintables.length - 1 || var8 > baseSinglePagePrintables.length - 1) {
                JOptionPane.showMessageDialog(null, Inter.getLocText("FR-Engine_NativePrint_Invalid_Index"));
                return;
            }

            if (var7 >= 0 && var8 >= var7) {
                int var9 = var8 - var7 + 1;
                this.initIndexedPages(var9, var7, baseSinglePagePrintables, url, baseSingleReportCache);
            }
        } else if (index != null && this.isInteger(index)) {
            int var5 = Integer.parseInt(index) - 1;
            if (var5 > baseSinglePagePrintables.length - 1) {
                JOptionPane.showMessageDialog(null, Inter.getLocText("FR-Engine_NativePrint_Invalid_Index"));
                return;
            }

            this.initIndexedPages(1, var5, baseSinglePagePrintables, url, baseSingleReportCache);
        } else {
            this.initIndexedPages(baseSinglePagePrintables.length, 0, baseSinglePagePrintables, url, baseSingleReportCache);
        }

    }

    /**
     * 缓存页面设置
     *
     * @param enable boolean
     */
    private void cachePaperSetting(boolean enable) {
        if (this.pages.length > 0 && enable) {
            for (int i = 0; i < this.pages.length; ++i) {
                if (i != 0) {
                    this.pages[i].setReadreportsettings(false);
                }
            }
        }

    }

    /**
     * 初始化打印页码
     *
     * @param var1
     * @param var2
     * @param var3
     * @param var4
     * @param var5
     */
    private void initIndexedPages(int var1, int var2, BaseSinglePagePrintable[] var3, URL var4, BaseSingleReportCache var5) {
        this.pages = new BaseSinglePagePrintable[var1];

        for (int var6 = 0; var6 < var1; ++var6) {
            PaperSettingProvider var7 = var3[var2 + var6].getPaperSetting();
            HashMap<String, Class> var8 = new HashMap<>(2);
            var8.put("1", PaperSettingProvider.class);
            var8.put("3", BaseSingleReportCache.class);
            Object[] var9 = new Object[]{var4, var7, var2 + var6, var5};
            this.pages[var6] = StableFactory.getMarkedInstanceObjectFromClass("SinglePagePrintable", var9, var8, BaseSinglePagePrintable.class, null);
        }

    }

    /**
     * 启动模块
     *
     * @param className 全路径类名
     */
    @SuppressWarnings("unchecked")
    private void startModule(String className) {
        try {
            Class var2 = Class.forName(className);
            Method var3 = var2.getMethod("init", (Class[]) null);
            var3.invoke(var2.newInstance());
        } catch (Exception ignored) {

        }

    }

    /**
     * 是否为整数
     *
     * @param value String
     * @return boolean
     */
    private boolean isInteger(String value) {
        if (StringUtils.isEmpty(value)) {
            return false;
        } else {
            return INTEGER_PATTERN.matcher(value).matches();
        }
    }

    /**
     * 默认打印机打印
     *
     * @param isShowDialog 是否显示对话框
     */
    private void print(boolean isShowDialog) {
        this.print(isShowDialog, null);
    }

    /**
     * 打印
     *
     * @param isShowDialog 是否显示对话框
     * @param printerName  打印机名称
     */
    private void print(boolean isShowDialog, String printerName) {
        if (this.pages != null) {
            try {
                PrintUtils.print(new SinglePageSet(this.pages), isShowDialog, printerName);
            } catch (Exception e) {
                if (StringUtils.isEmpty(e.getMessage())) {
                    return;
                }

                log.error(e.getMessage(), e);
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        }

    }
}
