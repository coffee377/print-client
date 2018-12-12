package com.voc.print.socket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fr.general.GeneralUtils;
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

import java.util.UUID;
import java.util.concurrent.ExecutorService;

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
    private final static String EVENT_GET_CONFIG_DATA = "getConfigData";
    private final static String EVENT_BEFORE_PRINT = "beforePrint";
    private final static String EVENT_START_PRINT = "startPrint";
    private final static String EVENT_AFTER_PRINT = "afterPrint";

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
            //建立连接事件
            PrintClientServer.this.socketIOServer.addConnectListener(
                    socketIOClient -> log.info("与客户端[{}]建立连接", socketIOClient.getSessionId())
            );
            //失去连接事件
            PrintClientServer.this.socketIOServer.addDisconnectListener(
                    socketIOClient -> log.warn("与客户端[{}]失去连接", socketIOClient.getSessionId())
            );
            PrintClientServer.this.socketIOServer.addEventListener(EVENT_NGINX_PROXY, ChatObject.class, (client, data, request) -> {
                ClientConfig clientConfig = PrintPlusConfiguration.getInstance().getClientConfig();
                JSONObject clientData = new JSONObject();
                if (log.isDebugEnabled()) {
                    log.debug("Nginx Proxy: {}", clientConfig.isProxy());
                }
                clientData.put("proxy", clientConfig.isProxy());
                clientData.put("serverURL", clientConfig.getServerURL());
                String encode = CodeUtils.cjkEncode(clientData.toString());
                client.sendEvent(EVENT_NGINX_PROXY, new ChatObject(encode));
            });
            /*存活检查事件*/
            PrintClientServer.this.socketIOServer.addEventListener(EVENT_ALIVE_CHECKING, ChatObject.class,
                    (client, data, request) -> client.sendEvent(EVENT_ALIVE_CHECKING, new ChatObject("")));
            PrintClientServer.this.socketIOServer.addEventListener(EVENT_GET_CONFIG_DATA, ChatObject.class,
                    (client, data, request) -> {
                        JSONObject clientConfigData = this.getClientConfigData(data);
                        String message = JSON.toJSONString(clientConfigData);
                        client.sendEvent(EVENT_GET_CONFIG_DATA, new ChatObject(message));
                    });
            PrintClientServer.this.socketIOServer.addEventListener(EVENT_START_PRINT,
                    ChatObject.class, (client, data, request) -> {
                        JSONObject jsonObject = JSON.parseObject(data.getMessage());
                        ClientConfig clientConfig = PrintPlusConfiguration.getInstance().getClientConfig();
                        /*客户在页面设置了静默打印，保存到配置文件*/
                        if (jsonObject.getBooleanValue(QUIET_PRINT) && !clientConfig.isQuietPrint()) {
                            clientConfig.setQuietPrint(true);
                            ConfigUtils.saveOrUpdateClientConfig(clientConfig);
                        }
                        if (jsonObject.getBooleanValue(IS_CUSTOM_PRINT)) {
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
        JSONObject clientData = new JSONObject();

        /*1.客户端可用打印机*/
        JSONArray printers = new JSONArray();
        String[] printerNameArray = GeneralUtils.getSystemPrinterNameArray();
        JSONObject printer;
        for (String printerName : printerNameArray) {
            printer = new JSONObject();
            printer.put("text", printerName);
            printer.put("value", printerName);
            printers.add(printer);
        }
        clientData.put("printers", printers);

        /*2.客户端可用页面纸张*/
        JSONArray paperSizeNames = new JSONArray();
        JSONObject paperSize;
        for (int i = 0; i < ReportConstants.PaperSizeNameSizeArray.length; ++i) {
            paperSize = new JSONObject();
            String paperSizeName = ReportConstants.PaperSizeNameSizeArray[i][0].toString();
            paperSize.put("text", paperSizeName);
            paperSize.put("value", paperSizeName);
            paperSizeNames.add(paperSize);
        }
        clientData.put("paperSizeNames", paperSizeNames);


        JSONObject serverData = JSON.parseObject(chatObject.getMessage());
        if (serverData.getBooleanValue(IS_CUSTOM_PRINT)) {
            CustomPrintUtils.readConfigToData(clientData, serverData.getString(CUSTOM_FILE_URL));
        }

        /*3.是否静默打印*/
        clientData.put("isQuietPrint", clientConfig.isQuietPrint());
        if (log.isDebugEnabled()) {
            log.debug("isQuietPrint:{}", clientConfig.isQuietPrint());
        }

        /*4.默认打印机名称*/
        clientData.put("printerName", clientConfig.getPrinterName());
        if (log.isDebugEnabled()) {
            log.debug("printerName:{}", clientConfig.getPrinterName());
        }
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
