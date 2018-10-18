package com.voc.print.socket;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fr.general.GeneralUtils;
import com.fr.json.JSONArray;
import com.fr.json.JSONException;
import com.fr.json.JSONObject;
import com.fr.report.stable.ReportConstants;
import com.fr.stable.BuildContext;
import com.fr.stable.CodeUtils;
import com.voc.print.config.ClientConfig;
import com.voc.print.config.PrintPlusConfiguration;
import com.voc.print.plus.PrintPlus;
import com.voc.print.plus.PrintTray;
import com.voc.print.util.ConfigUtils;
import com.voc.print.util.CustomPrintUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/10 18:54
 */
@Slf4j
public class PrintClientServer {
    private final static String HOST = "localhost";
    private final static int PORT = 9092;
    private final static String EVENT_NGINX_PROXY = "nginxProxy";
    private final static String EVENT_ALIVE_CHECKING = "aliveChecking";
    private final static String EVENT_BEFORE_PRINT = "beforePrint";
    private final static String EVENT_START_PRINT = "startPrint";
    private final static String EVENT_AFTER_PRINT = "afterPrint";
    private final static String EVENT_GET_CONFIG_DATA = "getConfigData";

    private static final String IS_CUSTOM_PRINT = "isCustomPrint";
    private static final String QUIET_PRINT = "quietPrint";
    private static final String CUSTOM_FILE_URL = "customFileUrl";

    private SocketIOServer socketIOServer;
    private boolean start;

    private PrintClientServer() {
        BuildContext.setBuildFilePath("/com/fr/stable/build.properties");
        final Configuration configuration = new Configuration();
        configuration.setHostname(HOST);
        configuration.setPort(PORT);
        ExecutorService executorService = newFixedThreadPool(1);
        executorService.execute(() -> {
            PrintClientServer.this.socketIOServer = new SocketIOServer(configuration);
            PrintClientServer.this.socketIOServer.start();
            PrintClientServer.this.socketIOServer.addEventListener(EVENT_NGINX_PROXY, ChatObject.class,
                    (client, data, request) -> {
                        ClientConfig clientConfig = PrintPlusConfiguration.getInstance().getClientConfig();
                        JSONObject clientData = JSONObject.create();
                        if (log.isDebugEnabled()) {
                            log.debug("Nginx Proxy: {}", clientConfig.isProxy());
                        }
                        clientData.put("proxy", clientConfig.isProxy());
                        clientData.put("serverURL", clientConfig.getServerURL());
                        String encode = CodeUtils.cjkEncode(clientData.toString());
                        client.sendEvent(EVENT_NGINX_PROXY, new ChatObject(encode));
                    });
            PrintClientServer.this.socketIOServer.addEventListener(EVENT_ALIVE_CHECKING, ChatObject.class,
                    (client, data, request) -> client.sendEvent(EVENT_ALIVE_CHECKING, new ChatObject("")));
            PrintClientServer.this.socketIOServer.addEventListener(EVENT_GET_CONFIG_DATA, ChatObject.class,
                    (client, data, request) -> {
                        JSONObject clientConfigData = this.getClientConfigData(data);
                        String message = CodeUtils.cjkEncode(clientConfigData.toString());
                        client.sendEvent(EVENT_GET_CONFIG_DATA, new ChatObject(message));
                    });
            PrintClientServer.this.socketIOServer.addEventListener(EVENT_START_PRINT,
                    ChatObject.class, (client, data, request) -> {
                        JSONObject jsonObject = new JSONObject(data.getMessage());
                        ClientConfig clientConfig = PrintPlusConfiguration.getInstance().getClientConfig();
                        /*客户在页面设置了静默打印，保存到配置文件*/
                        if (jsonObject.optBoolean(QUIET_PRINT) && !clientConfig.isQuietPrint()) {
                            clientConfig.setQuietPrint(true);
                            ConfigUtils.saveOrUpdateClientConfig(clientConfig);
                        }
                        if (jsonObject.optBoolean(IS_CUSTOM_PRINT)) {
                            CustomPrintUtils.printWithJsonArg(jsonObject);
                        } else {
                            PrintPlus.getInstance().printWithJsonArg(jsonObject, client.getSessionId());
                        }
                        client.sendEvent(EVENT_AFTER_PRINT);
                    });

            try {
                Thread.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }

            PrintClientServer.this.socketIOServer.stop();
        });
    }

    /**
     * 获取客户端配置
     *
     * @return JSONArray
     */
    private JSONObject getClientConfigData(ChatObject chatObject) throws JSONException {
        ClientConfig clientConfig = PrintPlusConfiguration.getInstance().getClientConfig();
        String[] printerNameArray = GeneralUtils.getSystemPrinterNameArray();
        JSONObject clientData = JSONObject.create();
        JSONArray printers = new JSONArray(new ArrayList());
//        Pattern compile = Pattern.compile("nginx.serverURL.pattern");
//        if (compile.matcher("").matches()) {
//
//        }
        int var7 = 0;

        int var8;

        for (var8 = printerNameArray.length; var7 < var8; ++var7) {
            JSONObject printer = JSONObject.create();
            printer.put("text", printerNameArray[var7]);
            printer.put("value", printerNameArray[var7]);
            printers.put(printer);
        }

        clientData.put("printers", printers);
        /*客户端可用页面纸张*/
        JSONArray paperSizeNames = new JSONArray(new ArrayList());

        for (var8 = 0; var8 < ReportConstants.PaperSizeNameSizeArray.length; ++var8) {
            String var14 = ReportConstants.PaperSizeNameSizeArray[var8][0].toString();
            JSONObject var10 = JSONObject.create();
            var10.put("text", var14);
            var10.put("value", var14);
            paperSizeNames.put(var10);
        }

        clientData.put("paperSizeNames", paperSizeNames);


        JSONObject serverData = new JSONObject(chatObject.getMessage());

        if (serverData.optBoolean(IS_CUSTOM_PRINT)) {
            CustomPrintUtils.readConfigToData(clientData, serverData.getString(CUSTOM_FILE_URL));
        }

        /*是否静默打印*/
        clientData.put("isQuietPrint", clientConfig.isQuietPrint());
        log.debug("isQuietPrint:{}", clientConfig.isQuietPrint());

        /*默认打印机名称*/
        clientData.put("printerName", clientConfig.getPrinterName());
        return clientData;
    }

    public void onBeforePrint(UUID uuid) {
        SocketIOClient client = this.socketIOServer.getClient(uuid);
        if (client != null) {
            client.sendEvent(EVENT_BEFORE_PRINT);
        }

    }

    public static PrintClientServer getInstance() {
        return PrintClientServer.ServerHolder.server;
    }

    public void stop() {
        this.socketIOServer.stop();
        this.start = false;
    }

    public void start() {
        PrintTray instance = PrintTray.getInstance();
        /*配置文件实时监听*/
        PrintPlusConfiguration plusConfiguration = PrintPlusConfiguration.getInstance();
        this.start = instance != null && plusConfiguration.listening();
    }

    public boolean isStart() {
        return this.start;
    }

    private static class ServerHolder {
        private static PrintClientServer server = new PrintClientServer();

        private ServerHolder() {
        }
    }
}
