package com.voc.print.file;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/12 12:56
 */
public class ImgPrintable implements Printable {
    private ImageIcon printImage;

    public ImgPrintable(String var1) {
        this.printImage = new ImageIcon(var1);
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        Graphics2D var4 = (Graphics2D)graphics;
        graphics.translate((int)pageFormat.getImageableX(), (int)pageFormat.getImageableY());
        double var5 = pageFormat.getImageableWidth();
        double var7 = pageFormat.getImageableHeight();
        double var9 = (double)this.printImage.getIconWidth();
        double var11 = (double)this.printImage.getIconHeight();
        if (var9 > var5 || var11 > var7) {
            double var13 = var5 / var9;
            double var15 = var7 / var11;
            double var17 = Math.min(var13, var15);
            var4.scale(var17, var17);
        }

        int var19 = (int)(var5 - var9) / 2;
        int var14 = (int)(var7 - var11) / 2;
        if (var19 < 0) {
            var19 = 0;
        }

        if (var14 < 0) {
            var14 = 0;
        }

        graphics.drawImage(this.printImage.getImage(), var19, var14, null);
        return 0;
    }
}
