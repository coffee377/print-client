package com.voc.print.util;

import com.voc.print.config.ClientConfig;
import com.voc.print.config.PrintPlusConfiguration;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/10/15 21:44
 */
public class ConfigUtilsTest {

    @Test
    public void readFromFile() {
        ClientConfig clientConfig = ConfigUtils.readFromFile( new File(PrintPlusConfiguration.CONFIG_PRINT_PLUS_JSON));
//        ClientConfig clientConfig = ConfigUtils.readFromFile( null);
        System.out.println(clientConfig);
    }
}