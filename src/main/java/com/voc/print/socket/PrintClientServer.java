package com.voc.print.socket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fr.general.GeneralUtils;
import com.fr.report.stable.ReportConstants;
import com.fr.stable.BuildContext;
import com.fr.stable.StringUtils;
import com.voc.print.config.ClientConfig;
import com.voc.print.config.PrintPlusConfiguration;
import com.voc.print.config.Result;
import com.voc.print.plus.PrintPlus;
import com.voc.print.plus.PrintTray;
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
    private final static int PORT = 10227;
    private final static String EVENT_TYPE_CHECKING = "typeChecking";
    private final static String EVENT_GET_CONFIG_DATA = "getConfigData";
    private final static String EVENT_BEFORE_PRINT = "beforePrint";
    private final static String EVENT_PRINT = "print";
    private final static String EVENT_PREVIEW = "preview";
    private final static String EVENT_AFTER_PRINT = "afterPrint";
    private final static String EVENT_ERROR_OCCURS = "errorOccurs";

    private static final String QUIET_PRINT = "quietPrint";

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
            PrintClientServer.this.socketIOServer.addEventListener(EVENT_TYPE_CHECKING, ChatObject.class,
                    (client, data, request) -> client.sendEvent(EVENT_BEFORE_PRINT)
            );
            PrintClientServer.this.socketIOServer.addEventListener(EVENT_BEFORE_PRINT, ChatObject.class,
                    (client, data, request) -> {
                        JSONObject jsonMessage = data.getJSONMessage();
                        /*静默打印*/
                        if (jsonMessage.getBooleanValue(QUIET_PRINT)) {
                            ClientConfig clientConfig = PrintPlusConfiguration.getInstance().getClientConfig();
                            JSONObject clientData = JSON.parseObject(clientConfig.toString());
                            client.sendEvent(EVENT_PRINT, clientData);
                        }
                        /*打印预览*/
                        else {
                            JSONObject previewConfig = this.getPreviewConfig(data);
                            client.sendEvent(EVENT_PREVIEW, previewConfig);
                        }

                    });
            PrintClientServer.this.socketIOServer.addEventListener(EVENT_PRINT,
                    ChatObject.class, (client, data, request) -> {
                        boolean success = PrintPlus.getInstance().printWithJsonArg(data.getJSONMessage(),
                                client.getSessionId());
                        client.sendEvent(EVENT_AFTER_PRINT, success ? Result.success("打印成功")
                                : Result.failure(1, "打印失败"));
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
     * 获取客户端配置及报表页面信息
     *
     * @param data ChatObject
     * @return JSONObject
     */
    private JSONObject getPreviewConfig(ChatObject data) {
        JSONObject jsonMessage = data.getJSONMessage();

        ClientConfig clientConfig = PrintPlusConfiguration.getInstance().getClientConfig();
        JSONObject clientData = JSON.parseObject(clientConfig.toString());

        //打印预览地址
        String sb = jsonMessage.get("url") + "?op=fr_print&cmd=no_client&preview=true&sessionID=" + jsonMessage.get("sessionID");
        clientData.put("previewUrl", sb);

        //根据报表获取后设置
        clientConfig.setCopy(1);
        clientConfig.setIndex(null);
        clientConfig.setQuietPrint(false);
        clientConfig.setPaperSizeText("");
        clientConfig.setMarginTop(null);
        clientConfig.setMarginBottom(null);
        clientConfig.setMarginLeft(null);
        clientConfig.setMarginRight(null);

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

        return clientData;
    }

    public void onErrorOccurs(UUID uuid, int errorCode, String errorMessage) {
        SocketIOClient client = this.socketIOServer.getClient(uuid);
        if (StringUtils.isNotEmpty(errorMessage)) {
            client.sendEvent(EVENT_ERROR_OCCURS, Result.failure(errorCode, errorMessage));
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
