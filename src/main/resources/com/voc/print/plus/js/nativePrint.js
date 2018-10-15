;
(function ($) {
    function getSessionID(opts){
        var config = {};
        if (arguments.length === 1) {
            config = arguments[0];
        } else if (arguments.length === 2) {
            config.url = arguments[0];
            $.extend(config, arguments[1]);
        }

        var sessionID = null;
        var udata;
        config.url = config.url.replace('op=', 'op_=');

        if (config && config.form) {
            var $form = $(config.form);
            udata = $.param($form.serializeArray().concat([
                {
                    name: 'op',
                    value: 'getSessionID'
                }
            ]));
        } else if (config && config['formid']) {
            $form = $('#' + config['formid']);
            udata = $.param($form.serializeArray().concat([
                {
                    name: 'op',
                    value: 'getSessionID'
                }
            ]));
        } else if (config && config.data) {
            udata = $.extend({
                op: 'getSessionID'
            }, config.data);
        } else {
            udata = {
                op: 'getSessionID'
            }
        }

        FR.ajax({
            url: config.url,
            type: 'POST',
            data: udata,
            async: false,
            complete: function (res, status) {
                if (status == 'success') {
                    sessionID = res.responseText;
                }
            }
        });
        return sessionID;
    }

    function isShowNativeDialog(){
        var isShowDialog = true;
        FR.ajax({
            url: FR.servletURL,
            data: {
                op: 'native_print',
                cmd: 'get_setting'
            },
            async: false,
            complete: function (res, status) {
                if (status == 'success') {
                    isShowDialog = (res.responseText == 'true');
                }
            }
        })
        return isShowDialog;
    }

    function printWithArgs(config, isShowDialog) {
        var index = 0;
        var copies = 1;
        var printerName;
        var url;

        if (config) {
            if (typeof config === "string") {
                url = config;
            } else {
                isShowDialog = config.isPopUp;
                index = config.index ? config.index : index;
                copies = config.copies ? config.copies : copies;
                printerName = config.printerName;
                url = config.url;
            }
        }

        var loading = function () {
            FR.showLoadingDialog();
            setTimeout("FR.hideLoadingDialog()", 3000);
        };

        if (isShowDialog) {
            var dialog = new FR.Dialog({
                title: FR.i18nText("FR-Engine_Setting"),
                confirm: true,
                resizeable: false,
                width: 450,
                height: 190,
                contentWidget: {
                    type: 'tablelayout',
                    columnSize: [20, 100, 155, 150],
                    rowSize: [10, 20, 20, 20],
                    vgap: 10,
                    items: [
                        [{el: $('<div>')}],
                        [
                            {el: $('<div>')},
                            {
                                el: {
                                    type: 'radio',
                                    widgetName: 'All_Pages',
                                    text: FR.i18nText("HJS-All_Pages"),
                                    selected: true,
                                    fontSize: 12,
                                    fontFamily: 'SimSun',
                                    listeners: [{
                                        eventName: FR.Events.STATECHANGE,
                                        action: function () {
                                            if (this.isSelected()) {
                                                dialog.getWidgetByName('Specified_Pages').setSelected(false);
                                            }
                                        }
                                    }]
                                }
                            },
                            {el: $('<div>')},
                            {el: $('<div>')}
                        ],
                        [
                            {el: $('<div>')},
                            {
                                el: {
                                    type: 'radio',
                                    widgetName: 'Specified_Pages',
                                    text: FR.i18nText("HJS-Specified_Pages") + ":",
                                    fontSize: 12,
                                    fontFamily: 'SimSun',
                                    listeners: [{
                                        eventName: FR.Events.STATECHANGE,
                                        action: function () {
                                            if (this.isSelected()) {
                                                dialog.getWidgetByName('All_Pages').setSelected(false);
                                            }
                                        }
                                    }]
                                }
                            },
                            {
                                el: {
                                    type: 'text',
                                    width: '155px',
                                    fontSize: 12,
                                    fontFamily: 'SimSun',
                                    widgetName: 'CurrentPageNum',
                                    value: '1-1'
                                }
                            },
                            {
                                el: {
                                    type: 'label',
                                    fontsize: 12,
                                    fontFamily: 'SimSun',
                                    textalign: "right",
                                    value: '(' + FR.i18nText("FR-Engine_Example") + ': 2,5,7-10,12)'
                                }
                            }
                        ],
                        [
                            {el: $('<div>')},
                            {
                                el: {
                                    type: 'label',
                                    value: FR.i18nText("FR-Engine_Print_copy") + ": ",
                                    fontSize: 12,
                                    fontFamily: 'SimSun',
                                    textalign: "left"
                                }
                            },
                            {
                                el: {
                                    type: 'text',
                                    width: '155px',
                                    fontSize: 12,
                                    fontFamily: 'SimSun',
                                    widgetName: 'Copies_txt',
                                    value: '1'
                                }
                            }
                        ]
                    ]
                },
                onOK: function () {
                    FR.closeDialog();
                    if (dialog.getWidgetByName('Specified_Pages').isSelected()) {
                        index = dialog.getWidgetByName('CurrentPageNum').getValue();
                    }
                    copies = dialog.getWidgetByName('Copies_txt').getValue();

                    var sessionID = url ? getSessionID(url, config) : contentPane.currentSessionID;
                    var configp = {
                        url: FR.serverURL + FR.servletURL + "?sessionID=" + sessionID + "&op=fr_applet&cmd=print",
                        copy: copies,
                        isShowDialog: isShowDialog,
                        index: index,
                        printerName: printerName
                    }

                    window.location = "finereport:" + encodeURIComponent(FR.jsonEncode(configp));

                    loading();
                },
                onCancel: function () {
                    index = 0;
                    FR.closeDialog();
                }
            });
            dialog.setVisible(true);
        } else {
            var sessionID = url ? getSessionID(url, config) : contentPane.currentSessionID;

            var configp = {
                url: FR.serverURL + FR.servletURL + "?sessionID=" + sessionID + "&op=fr_applet&cmd=print",
                copy: copies,
                isShowDialog: isShowDialog,
                index: index,
                printerName: printerName
            }

            window.location = "finereport:" + encodeURIComponent(FR.jsonEncode(configp));
            loading();
        }
    }

    function showPrintTipDialog(hideKey, config, isShowDialog) {
        var tipDialog = new FR.Dialog({
            title: FR.i18nText('FR-Engine_Printer_Native_Tip'),
            width: 440,
            height: 200,
            modal: true,
            confirm: false,
            doSize: true,
            border: true,
            closable: true,
            style: "blue",
            needSeparate: 8,
            otherButton: true,
            button4Other: [{
                bT: FR.i18nText('FR-Plugin_Keep-Print'),
                bW: 80,
                baseCls: 'fr-core-btn',
                bF: function () {
                    //根据勾选的情况, 修改cookie里的值
                    var hideDialog = tipDialog.getWidgetByName('noMoreTip').isSelected();
                    if (hideDialog) {
                        FR.CookieInfor.addCookie(hideKey, true, 1024);
                    }
                    tipDialog.setVisible(false);
                    printWithArgs(config, isShowDialog);
                }
            }, {
                bT: FR.i18nText('FR-Plugin_Download-Install'),
                bW: 80,
                baseCls: 'fr-core-btn',
                bF: function () {
                    window.location = "http://download.finedevelop.com/FinePrint_windows_1_0.exe";
                    tipDialog.setVisible(false);
                }
            }],
            contentWidget: {
                type: 'border',
                width: 420,
                height: 240,
                items: [{
                    region: 'north',
                    el: {
                        type: 'tablelayout',
                        columnSize: [30, 300],
                        rowSize: [25, 35],
                        height: 60,
                        items: [
                            [
                                {el: $('<div>')}
                            ],
                            [
                                {el: $('<div>')},
                                {
                                    el: {
                                        type: 'label',
                                        fontweight: 600,
                                        value: FR.i18nText('FR-Plugin_Print-Tip-Content')
                                    }
                                }
                            ]
                        ]
                    }
                },
                    {
                        region: 'center', el: {
                        type: 'tablelayout',
                        columnSize: [27, 300],
                        rowSize: [30],
                        vgap: 0,
                        items: [
                            [
                                {el: $('<div>')},
                                {
                                    el: {
                                        type: 'checkbox',
                                        fontFamily: 'SimSun',
                                        widgetName: 'noMoreTip',
                                        text: FR.i18nText('FR-Plugin_No-More-Tip'),
                                        selected: false,
                                        disabled: false
                                    }
                                }
                            ]
                        ]
                    }
                    }
                ]
            }
        });
        tipDialog.setVisible(true);
    }

    $.extend(FR, {

        /**
         * 通过js调用本地程序打印, 需要安装FinePrint.exe
         * @param {json} config 打印需要传递的参数
         *
         * FR.doNativePrint({
         *      //是否弹窗
         *      isPopUp : false,
         *      //打印指定页面1, 3, 5-6
         *      index : 0,
         *      //打印份数
         *      copies : 1,
         *      //指定打印机
         *      printerName : "",
         *      //需要打印的报表url
         *      url : "http://localhost:8075/WebReport/ReportServer?reportlet=GettingStarted.cpt"
         * });
         */
        doNativePrint: function (config) {
            var hideKey = "FineReport_Hide_Print_Tip";
            //优先从config里取, 方便客户自定义打印, 其次是cookie, 默认不隐藏
            var hidePrintTip = config && config.hidePrintTip ? config.hidePrintTip : false;
            if (FR.CookieInfor.getCookieByName(hideKey)){
                //如果cookie里有,并且为true
                hidePrintTip = true;
            }

            var isShowDialog = false;
            if (hidePrintTip){
                isShowDialog = isShowNativeDialog();
                printWithArgs(config, isShowDialog);
            }else{
                //如果已经显示出来了本地打印提示框, 那么就不显示设置页码页面的框了, 不然弹两个太恶心了.
                //提示安装框里也可以直接进行打印.
                showPrintTipDialog(hideKey, config, isShowDialog);
            }
        }
    });
})(jQuery);