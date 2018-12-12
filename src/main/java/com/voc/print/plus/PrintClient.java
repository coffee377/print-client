package com.voc.print.plus;

import com.fr.general.GeneralContext;
import com.fr.general.Inter;
import com.voc.print.socket.PrintClientServer;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/10 11:01
 */
@Slf4j
public class PrintClient {

    public PrintClient() {
    }

    public static void main(String[] args) {
        PrintClientServer clientServer = PrintClientServer.getInstance();
        if (clientServer.isStart()) {
            log.info("打印服务已启动");
            System.exit(0);
        } else {
            Locale locale = GeneralContext.getLocale();
            Inter.loadLocaleFile(locale, "com/voc/print/plus/locale/message");
            clientServer.start();
        }
    }

}
