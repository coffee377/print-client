package com.voc.print.plus;

import com.alibaba.fastjson.JSONObject;
import com.fr.base.*;
import com.fr.general.Inter;
import com.fr.general.xml.GeneralXMLTools;
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
import com.voc.print.config.ClientConfig;
import com.voc.print.config.PrintPlusConfiguration;
import com.voc.print.config.Result;
import com.voc.print.socket.PrintClientServer;
import com.voc.print.util.ConfigUtils;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.print.PrinterException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
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
    private static final String PAGE_NUMBER_SEPARATOR = "-";
    private static final Pattern PAGE_PATTERN = Pattern.compile(PAGE_NUMBER_SEPARATOR);
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^[1-9]\\d*$");
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("win");
    public static final String QUIET_PRINT = "quietPrint";
    public static final String SHOW_DIALOG = "showDialog";
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
     * JSON 参数打印
     *
     * @param json JSONObject
     * @param uuid UUID
     */
    public void printWithJsonArg(JSONObject json, UUID uuid) {
        PrintTray.getInstance().markPrintingTray();
        this.initializingPrinting(json, uuid);
        PrintTray.getInstance().printOver();
    }

    /**
     * 打印初始化
     *
     * @param jsonObject JSONObject
     * @param uuid       UUID
     */
    private void initializingPrinting(JSONObject jsonObject, UUID uuid) {
        try {
            String urlString = jsonObject.getString("url");
            if (StringUtils.isEmpty(urlString)) {
                return;
            }
            URL url = new URL(urlString);
            this.printAsApplet(jsonObject, url, uuid);
        } catch (Exception e) {
            boolean showDialog = jsonObject.getBooleanValue(SHOW_DIALOG);
            if (showDialog) {
                JOptionPane.showMessageDialog(null, " e=" + e.getMessage());
            }
            if (jsonObject.getBooleanValue(QUIET_PRINT)) {
                if (uuid != null) {
                    PrintClientServer.getInstance().onErrorOccurs(uuid, -1, e.getMessage());
                }
            }
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
        /*1.打印配置*/
        ClientConfig clientConfig = validatingConfig(jsonObject);
        InputStream inputStream = url.openStream();
        PageXmlProvider pageXmlOperator = StableFactory.getMarkedInstanceObjectFromClass("PageXmlOperator", PageXmlProvider.class);
        BaseSinglePagePrintable[] baseSinglePagePrintables = pageXmlOperator.deXmlizable4SinglePage(inputStream);
        BaseSingleReportCache reportCache = StableFactory.getMarkedInstanceObjectFromClass("SingleReportCache", BaseSingleReportCache.class);
        /*2.打印页面校验*/
        this.validIndex(clientConfig.getIndex(), baseSinglePagePrintables, url, reportCache);
        /*2.根据客户端配置更新页面配置，静默打印应该页面根据给定的纸张自适应打印*/
        this.updatePageSetting(clientConfig);
        /*3.打印份数*/
        this.setUpCopies(clientConfig.getCopy());
        /*4.缓存页面设置*/
        this.cachePaperSetting(clientConfig.isCachePaperSetting());
        /*打印前事件*/
        if (uuid != null) {
            PrintClientServer.getInstance().onBeforePrint(uuid);
        }
        /*5.使用不显示applet打印对话框窗口*/
        Optional<Result> print = this.print(false, clientConfig.getPrinterName());
        /*打印后事件*/
        if (uuid != null && print.isPresent()) {
            PrintClientServer.getInstance().oneAfterPrint(uuid, print.get());
        }
        /*6.清除报表缓存*/
        reportCache.clearReportPageCache();
    }

    /**
     * 配置校验并返回最终打印的配置
     *
     * @param jsonObject JSONObject
     * @return ClientConfig
     */
    private ClientConfig validatingConfig(JSONObject jsonObject) {
        //页面传递过来配置
        ClientConfig webConfig = ConfigUtils.read4JSONObject(jsonObject, ClientConfig.class);
        if (webConfig.isQuietPrint()) {
            //静默打印配置：打印机，纸张，方向，自适应
            return webConfig;
        }
        //非静默打印时保存静默打印配置，以最后一次保存为主
        else {
            PrintPlusConfiguration instance = PrintPlusConfiguration.getInstance();
            ClientConfig clientConfig = instance.getClientConfig();
            if (!clientConfig.equals(webConfig)) {
                instance.setClientConfig(webConfig);
            }
        }
        return webConfig;
    }

    /**
     * 获取页面尺寸
     *
     * @param paperSizeText 尺寸文本
     * @return PaperSize
     */
    private PaperSize getPaperSize(String paperSizeText) {
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
        return paperSize;
    }


    private Margin getMargin(ClientConfig config) {
        MM top = new MM(config.getMarginTop());
        MM left = new MM(config.getMarginLeft());
        MM bottom = new MM(config.getMarginBottom());
        MM right = new MM(config.getMarginRight());
        return new Margin(top, left, bottom, right);
    }

    /**
     * 根据客户端配置更新页面相关设置
     *
     * @param config ClientConfig
     */
    private void updatePageSetting(ClientConfig config) {
        BaseSinglePagePrintable[] pagePrintTables = this.pages;
        PaperSettingProvider paperSetting;

        //打印预览
        if (!config.isQuietPrint()) {
            for (BaseSinglePagePrintable printable : pagePrintTables) {
                paperSetting = printable.getPaperSetting();
                /*1.设置纸张*/
                String paperSizeText = config.getPaperSizeText();
                paperSetting.setPaperSize(getPaperSize(paperSizeText));
                /*2.设置打印方向*/
                paperSetting.setOrientation(config.getOrientation());
                /*3.设置边距*/
                paperSetting.setMargin(getMargin(config));
            }
        }

        //静默打印，默认A4页面
        if (config.isQuietPrint()) {
            // TODO: 2018/12/27 0027 17:55 静默打印，默认A4页面
            for (BaseSinglePagePrintable printable : pagePrintTables) {
                paperSetting = printable.getPaperSetting();
                /*1.设置纸张(width,height) width <= height, 否则页面打印存在问题*/
                paperSetting.setPaperSize(getPaperSize("210,297"));
                /*2.设置打印方向：使用报表设计时的方向*/
                /*3.设置边距：使用报表设计时的边距*/
            }
        }

        //静默打印 && 自动缩放
        if (config.isQuietPrint() && config.isAutoScaling()) {
            // TODO: 2018/12/27 0027 17:55 静默打印 && 自动缩放
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
        if (IS_WINDOWS) {
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
            Class clazz = Class.forName(className);
            Method method = clazz.getMethod("init", (Class[]) null);
            method.invoke(clazz.newInstance());
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
     * 打印
     *
     * @param isShowDialog 是否显示applet对话框
     * @param printerName  打印机名称
     * @return Optional<Result>  打印结果
     */
    private Optional<Result> print(boolean isShowDialog, String printerName) {
        Result result = null;
        if (this.pages != null) {
            try {
                PrintUtils.print(new SinglePageSet(this.pages), isShowDialog, printerName);
                result = Result.success("打印成功");
            } catch (PrinterException e) {
                log.error(e.getMessage());
                result = Result.failure(1, "打印失败", e.getMessage());
            }
        }
        return Optional.ofNullable(result);
    }

}
