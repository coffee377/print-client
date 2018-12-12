package com.voc.print.file;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.printing.Scaling;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterIOException;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/12 13:13
 */
public class PDFPrintable implements Printable {

    private final PDDocument document;
    private final PDFRenderer renderer;
    private final boolean showPageBorder;
    private final Scaling scaling;
    private final float dpi;
    private final boolean center;

    public PDFPrintable(PDDocument var1) {
        this(var1, Scaling.SHRINK_TO_FIT);
    }

    public PDFPrintable(PDDocument var1, Scaling var2) {
        this(var1, var2, false, 0.0F);
    }

    public PDFPrintable(PDDocument var1, Scaling var2, boolean var3) {
        this(var1, var2, var3, 0.0F);
    }

    public PDFPrintable(PDDocument var1, Scaling var2, boolean var3, float var4) {
        this(var1, var2, var3, var4, true);
    }

    public PDFPrintable(PDDocument var1, Scaling var2, boolean var3, float var4, boolean var5) {
        this.document = var1;
        this.renderer = new PDFRenderer(var1);
        this.scaling = var2;
        this.showPageBorder = var3;
        this.dpi = var4;
        this.center = var5;
    }

    @Override
    public int print(Graphics var1, PageFormat var2, int var3) throws PrinterException {
        var3 %= this.document.getNumberOfPages();
        if (var3 >= 0 && var3 < this.document.getNumberOfPages()) {
            try {
                Graphics2D var4 = (Graphics2D)var1;
                PDPage var5 = this.document.getPage(var3);
                PDRectangle var6 = getRotatedCropBox(var5);
                double var7 = var2.getImageableWidth();
                double var9 = var2.getImageableHeight();
                double var11 = 1.0D;
                if (this.scaling != Scaling.ACTUAL_SIZE) {
                    double var13 = var7 / (double)var6.getWidth();
                    double var15 = var9 / (double)var6.getHeight();
                    var11 = Math.min(var13, var15);
                    if (var11 > 1.0D && this.scaling == Scaling.SHRINK_TO_FIT) {
                        var11 = 1.0D;
                    }

                    if (var11 < 1.0D && this.scaling == Scaling.STRETCH_TO_FIT) {
                        var11 = 1.0D;
                    }
                }

                var4.translate(var2.getImageableX(), var2.getImageableY());
                if (this.center) {
                    var4.translate((var7 - (double)var6.getWidth() * var11) / 2.0D, (var9 - (double)var6.getHeight() * var11) / 2.0D);
                }

                Graphics2D var18 = null;
                BufferedImage var14 = null;
                if (this.dpi > 0.0F) {
                    float var19 = this.dpi / 72.0F;
                    var14 = new BufferedImage((int)(var7 * (double)var19 / var11), (int)(var9 * (double)var19 / var11), 2);
                    var18 = var4;
                    var4 = var14.createGraphics();
                    var18.scale(var11 / (double)var19, var11 / (double)var19);
                    var11 = (double)var19;
                }

                AffineTransform var20 = (AffineTransform)var4.getTransform().clone();
                var4.setBackground(Color.WHITE);
                this.renderer.renderPageToGraphics(var3, var4, (float)var11);
                if (this.showPageBorder) {
                    var4.setTransform(var20);
                    var4.setClip(0, 0, (int)var7, (int)var9);
                    var4.scale(var11, var11);
                    var4.setColor(Color.GRAY);
                    var4.setStroke(new BasicStroke(0.5F));
                    var1.drawRect(0, 0, (int)var6.getWidth(), (int)var6.getHeight());
                }

                if (var18 != null) {
                    var18.setBackground(Color.WHITE);
                    var18.clearRect(0, 0, var14.getWidth(), var14.getHeight());
                    var18.drawImage(var14, 0, 0, (ImageObserver)null);
                    var4.dispose();
                }

                return 0;
            } catch (IOException var17) {
                throw new PrinterIOException(var17);
            }
        } else {
            return 1;
        }
    }

    static PDRectangle getRotatedCropBox(PDPage var0) {
        PDRectangle var1 = var0.getCropBox();
        int var2 = var0.getRotation();
        return var2 != 90 && var2 != 270 ? var1 : new PDRectangle(var1.getLowerLeftY(), var1.getLowerLeftX(), var1.getHeight(), var1.getWidth());
    }

    static PDRectangle getRotatedMediaBox(PDPage var0) {
        PDRectangle var1 = var0.getMediaBox();
        int var2 = var0.getRotation();
        return var2 != 90 && var2 != 270 ? var1 : new PDRectangle(var1.getLowerLeftY(), var1.getLowerLeftX(), var1.getHeight(), var1.getWidth());
    }

}
