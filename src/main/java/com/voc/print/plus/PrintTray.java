package com.voc.print.plus;

import com.fr.general.IOUtils;
import com.fr.general.Inter;
import com.fr.stable.CoreGraphHelper;
import com.voc.print.socket.PrintClientServer;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * 打印托盘
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/10 11:03
 */
@Slf4j
public class PrintTray {
    private ExecutorService executorService = newFixedThreadPool(1);

    private static final int DISPLAY_SLEEP_TIME = 1000 / 5 - 10;
    private static final Image PRINT_LOGO = IOUtils.readImage("/com/voc/print/plus/image/print.png");
    private static final String[] PRINT_IMAGES = new String[]{
            "/com/voc/print/plus/image/print0.png",
            "/com/voc/print/plus/image/print1.png",
            "/com/voc/print/plus/image/print2.png",
            "/com/voc/print/plus/image/print3.png",
            "/com/voc/print/plus/image/print4.png"};

    private TrayIcon trayIcon;
    private boolean printing = false;
    private int num = 1;
    private static PrintTray tray = new PrintTray();


    public void setNum(int num) {
        this.num = num;
    }

    public static PrintTray getInstance() {
        return tray;
    }

    private PrintTray() {
        this.showTray();
    }

    /**
     * 显示托盘
     */
    private void showTray() {
        if (SystemTray.isSupported()) {
            SystemTray systemTray = SystemTray.getSystemTray();
            String trayIconName = MessageFormat.format(Inter.getLocText("printer.client.name"), "v1.1.1");
            PopupMenu popupMenu = new PopupMenu();
            MenuItem menuSetting = new MenuItem(Inter.getLocText("menu.setting"));
            menuSetting.addActionListener(e -> {
                log.info("弹出客户端设置窗口");
            });
            MenuItem menuExit = new MenuItem(Inter.getLocText("menu.exit"));
            menuExit.addActionListener(actionEvent -> {
                systemTray.remove(this.trayIcon);
                this.stopClientServer();
                System.exit(0);
            });
            popupMenu.add(menuSetting);
            popupMenu.add(menuExit);
            this.trayIcon = new TrayIcon(PRINT_LOGO, trayIconName, popupMenu);

            try {
                systemTray.add(this.trayIcon);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

    }

    /**
     * 打印动画
     */
    public void markPrintingTray() {
        this.printing = true;
        executorService.execute(() -> {
            while (PrintTray.this.printing) {
                PrintTray.this.loopDisplayImage();
            }
        });
    }

    /**
     * 打印结束
     */
    public void printOver() {
        this.printing = false;
        tray.num = 1;
        this.trayIcon.setImage(PRINT_LOGO);
    }

    /**
     * 打印过程循环显示打印图片
     */
    private void loopDisplayImage() {
        for (String image : PRINT_IMAGES) {
            if (!this.printing) {
                break;
            }

            BufferedImage bufferedImage = CoreGraphHelper.toBufferedImage(IOUtils.readImage(image));
            this.trayIcon.setImage(bufferedImage);

            try {
                Thread.sleep(DISPLAY_SLEEP_TIME);
            } catch (InterruptedException ex) {
                log.error(ex.getMessage(), ex);
            }
        }

    }

    private void stopClientServer() {
        if (log.isInfoEnabled()) {
            log.info("停止打印客户端服务");
        }
        PrintClientServer.getInstance().stop();
    }

}
