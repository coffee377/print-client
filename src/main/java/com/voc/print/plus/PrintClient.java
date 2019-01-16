package com.voc.print.plus;

import com.fr.general.GeneralContext;
import com.fr.general.Inter;
import com.voc.print.config.PrintPlusConfiguration;
import com.voc.print.socket.PrintClientServer;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
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
        File flagFile = new File(PrintPlusConfiguration.RUNNING_FLAG_FILE);
        try {
            if (!flagFile.createNewFile()) {
                if (log.isWarnEnabled()) {
                    log.warn("A previous instance is already running....");
                }
                System.exit(1);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        flagFile.deleteOnExit();

        if (log.isInfoEnabled()) {
            log.info("This is the first instance of this program...");
        }

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
