package com.voc.print.config;

import lombok.Getter;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Wu Yujie
 * @email coffee377@dingtalk.com
 * @time 2018/12/13 17:08
 */
@Getter
public enum PageOrientation {

    VERTICAL_PRINT(0, "纵向"),
    HORIZONTAL_PRINT(1, "横向");

    private int value;
    private String name;

    PageOrientation(int value, String name) {
        this.value = value;
        this.name = name;
    }

}

