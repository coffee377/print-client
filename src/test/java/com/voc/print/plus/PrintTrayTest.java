package com.voc.print.plus;

import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/13 13:13
 */
public class PrintTrayTest {

    @Test
    public void print() throws InterruptedException {
        PrintTray instance = PrintTray.getInstance();
        instance.markPrintingTray();

        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000L);
        }

        instance.printOver();
    }


}