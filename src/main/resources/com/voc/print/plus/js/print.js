(function ($) {
    var $PrintDiv, $PDFReader, $flashIframe, $appletDiv, showFlashPrintSetting, fitPaper, showAppletPrintSetting = null;
    var appletJarName = "/fr-applet-8.0.jar";
    var isLoadingNativePrint = false;
    var nativePrintSocket;
    var nativePrintLoadingDialog;
    var nativePrintPreviewTimer;
    var floatRegexText = "^\\d+(\\.\\d+)?$";

    function isShowFlashPrintSetting() {
        if (typeof showFlashPrintSetting != "boolean") {
            $(function () {
                FR.ajax({
                    url: FR.servletURL,
                    data: {op: "flash_print", cmd: "get_fp_setting"},
                    async: false,
                    complete: function (res, status) {
                        if (status == "success") {
                            showFlashPrintSetting = res.responseText == "true"
                        }
                    }
                })
            })
        }
        return showFlashPrintSetting
    }

    function isPrintAsPPAPI() {
        var isPPAPI = false;
        var type = "application/x-shockwave-flash";
        var mimeTypes = navigator.mimeTypes;
        var endsWith = function (str, suffix) {
            return str.indexOf(suffix, str.length - suffix.length) !== -1
        };
        if (mimeTypes && mimeTypes[type] && mimeTypes[type].enabledPlugin && (mimeTypes[type].enabledPlugin.filename === "pepflashplayer.dll" || mimeTypes[type].enabledPlugin.filename === "libpepflashplayer.so" || mimeTypes[type].enabledPlugin.filename == "PepperFlashPlayer.plugin" || endsWith(mimeTypes[type].enabledPlugin.filename, "Chrome.plugin"))) {
            isPPAPI = true
        }
        return isPPAPI
    }

    function isFitPaper() {
        if (!fitPaper) {
            $(function () {
                FR.ajax({
                    url: FR.servletURL,
                    data: {op: "flash_print", cmd: "fit_fs_paper"},
                    async: false,
                    complete: function (res, status) {
                        if (status == "success") {
                            fitPaper = res.responseText == "true"
                        }
                    }
                })
            })
        }
        return fitPaper
    }

    function supportPDFPrint() {
        return FR.Browser.isIE()
    }

    function supportCodebase() {
        return FR.Browser.isIE()
    }

    function checkPDFPrintRegister() {
        var result = "";
        FR.ajax({
            url: FR.servletURL,
            data: {op: "fr_pdfprint", cmd: "check_register"},
            async: false,
            complete: function (res, status) {
                try {
                    var returnData = FR.jsonDecode(res.responseText);
                    result = returnData.exception !== "FAILPASS"
                } catch (Error) {
                    FR.Msg.toast("AnalysisData Error!")
                }
            }
        });
        return result
    }

    function isShowAppletPrintSetting() {
        if (typeof showAppletPrintSetting != "boolean") {
            $(function () {
                FR.ajax({
                    url: FR.servletURL,
                    data: {op: "fr_applet", cmd: "applet_print_setting"},
                    async: false,
                    complete: function (res, status) {
                        var result = FR.jsonDecode(res.responseText);
                        if (result.exception === "FAILPASS") {
                            showAppletPrintSetting = {exception: "FAILPASS"};
                            return
                        }
                        if (status === "success") {
                            showAppletPrintSetting = res.responseText == "true"
                        }
                    }
                })
            })
        } else {
            FR.ajax({
                url: FR.servletURL,
                data: {op: "fr_applet", cmd: "check_register"},
                async: false,
                complete: function (res, status) {
                    var result = FR.jsonDecode(res.responseText);
                    if (result.exception === "FAILPASS") {
                        showAppletPrintSetting = {exception: "FAILPASS"}
                    }
                }
            })
        }
        return showAppletPrintSetting
    }

    function isAppletPrintOver(sessionID) {
        var appletPrintOver;
        FR.ajax({
            url: FR.servletURL,
            data: {op: "fr_applet", cmd: "is_printover", sessionID: sessionID, data: new Date().getTime()},
            async: false,
            complete: function (res, status) {
                if (status == "success") {
                    appletPrintOver = res.responseText == "true"
                }
            }
        });
        return appletPrintOver
    }

    function isSingleSheetFunc(sessionID) {
        var isSingleSheet;
        FR.ajax({
            url: FR.servletURL,
            data: {op: "fr_applet", cmd: "is_singleSheet", sessionID: sessionID},
            async: false,
            complete: function (res, status) {
                if (status == "success") {
                    isSingleSheet = res.responseText == "true"
                }
            }
        });
        return isSingleSheet
    }

    function checkPDFPrint(sessionID, popupSetup) {
        FR.ajax({
            url: FR.servletURL,
            type: "POST",
            data: {op: "fr_pdfprint", cmd: "pt_check", sessionID: sessionID},
            complete: function (res, status) {
                var resText = res.responseText;
                if ("ok" == resText) {
                    (function () {
                        FR.closeDialog();
                        if (popupSetup === true) {
                            $PDFReader[0].print()
                        } else {
                            $PDFReader[0].printAllFit(true)
                        }
                        if (_g()) {
                            _g().fireEvent("afterpdfprint")
                        }
                    }).defer(2000)
                } else {
                    if ("gening" == resText) {
                        checkPDFPrint.defer(300, this, [sessionID, popupSetup])
                    } else {
                        FR.Msg.toast(resText)
                    }
                }
            }
        })
    }

    function __getSessionID__(opts) {
        var config = {};
        if (arguments.length === 1) {
            config = arguments[0]
        } else {
            if (arguments.length === 2) {
                config.url = arguments[0];
                $.extend(config, arguments[1])
            }
        }
        var sessionID = null;
        var udata;
        config.url = config.url.replace("op=", "op_=");
        if (config && config.form) {
            var $form = $(config.form);
            udata = $.param($form.serializeArray().concat([{name: "op", value: "getSessionID"}]))
        } else {
            if (config && config["formid"]) {
                $form = $("#" + config["formid"]);
                udata = $.param($form.serializeArray().concat([{name: "op", value: "getSessionID"}]))
            } else {
                if (config && config.data) {
                    udata = $.extend({op: "getSessionID"}, config.data)
                } else {
                    udata = {op: "getSessionID"}
                }
            }
        }
        if (config.callback == null) {
            FR.ajax({
                url: config.url, type: "POST", data: udata, async: false, complete: function (res, status) {
                    if (status == "success") {
                        sessionID = res.responseText
                    }
                }
            });
            return sessionID
        } else {
            FR.ajax({
                url: config.url, data: udata, dataType: "jsonp", jsonp: "callback", success: function (res) {
                    config.callback(res.sessionID)
                }
            })
        }
    }

    $.extend(FR, {
        doNewNativePrint: function (sessionID, config) {

            var printUrl = config.printUrl == null ? FR.serverURL + FR.servletURL : config.printUrl;

            console.log("压缩版"
                + "\nsessionID：" + sessionID
                + "\nFR.serverURL：" + FR.serverURL
                + "\nFR.servletURL：" + FR.servletURL
                + "\nconfig.printUrl：" + config.printUrl
                + "\nconfig:" + JSON.stringify(config)
                + "\n打印地址(原始)：" + printUrl
            );

            nativePrintLoadingDialog = new FR.Dialog({
                destroyOnClose: true,
                animate: false,
                title: "",
                titleHeight: 0,
                border: false,
                resizeable: false,
                draggable: true,
                adaptivity: {
                    enabled: true,
                    minWidth: 1000,
                    minHeight: 560,
                    topGap: 0,
                    leftGap: 40,
                    bottomGap: 40,
                    rightGap: 40
                },
                contentHtml: ""
            });
            if (!config.isCustomPrint) {
                FR.$defaultImport("/com/fr/web/core/js/socket.io.js", "js")
            }
            isLoadingNativePrint = true;
            if (nativePrintSocket == null) {
                nativePrintSocket = io.connect("http://localhost:9092")
            } else {
                nativePrintSocket.removeAllListeners();
                if (!nativePrintSocket.connected) {
                    nativePrintSocket.connect()
                }
            }
            nativePrintSocket.on("nginxProxy", function (e) {
                var data = FR.jsonDecode(FR.cjkDecode(e.message));
                console.log("nginxProxy 交互前：printUrl :" + printUrl);
                if (data.proxy) {
                    printUrl = config.printUrl == null ? data.serverURL + FR.servletURL : config.printUrl;
                }
                console.log("nginxProxy 交互后：printUrl :" + printUrl);
            });
            nativePrintSocket.on("aliveChecking", function () {
                if (isLoadingNativePrint) {
                    /*Nginx 代理交互处理*/
                    nativePrintSocket.emit("nginxProxy");
                    console.log("aliveChecking 交互成功");
                    isLoadingNativePrint = false;
                    nativePrintSocket.emit("getConfigData", FR.jsonEncode(FR.getPureConfig(config)))
                }
            });
            nativePrintSocket.on("getConfigData", function (e) {
                var data = FR.jsonDecode(FR.cjkDecode(e.message));
                console.log("获取客户端配置成功：" + JSON.stringify(data));
                if (data.isQuietPrint) {
                    console.log("静默打印");
                    if (config.isCustomPrint) {
                        console.log("自定义打印");
                        FR.newNativePrintWithArgs({url: config.customFileUrl, isCustomPrint: true})
                    } else {
                        console.log("非自定义打印");
                        FR.newNativePrintWithArgs({
                            url: printUrl + "?sessionID=" + sessionID + "&op=fr_applet&cmd=print",
                            sessionID: sessionID
                        })
                    }
                } else {
                    console.log("非静默打印");
                    if (config.isPopUp) {
                        console.log("弹出窗口");
                        nativePrintLoadingDialog.destroy();
                        config.printers = data.printers;
                        config.paperSizeNames = data.paperSizeNames;
                        /*修正打印机为客户端默认打印机*/
                        if (data.printerName) {
                            config.printerName = data.printerName;
                        } else {
                            config.printerName = "";
                        }
                        if (config.isCustomPrint) {
                            console.log("自定义打印");
                            config = $.extend(data, config);
                            FR.startNewNativePrintPreview(sessionID, config, printUrl)
                        } else {
                            console.log("非自定义打印");
                            FR.getDefaultNewNativePrintConfig(sessionID, printUrl, function (data) {
                                config = $.extend(data, config);
                                FR.startNewNativePrintPreview(sessionID, config, printUrl)
                            })
                        }
                    } else {
                        console.log("不弹出窗口");
                        config = $.extend(data, config);
                        FR.defaultNewNativePrint(config, sessionID, printUrl)
                    }
                }
                setTimeout(function () {
                    nativePrintLoadingDialog.destroy()
                }, 5000)
            });
            nativePrintSocket.on("beforePrint", function () {
                nativePrintLoadingDialog.destroy()
            });
            nativePrintSocket.on("afterPrint", function () {
                if (config.isCustomPrint) {
                    if ($.isFunction(config.afterPrint)) {
                        config.afterPrint()
                    }
                    return
                }
                _g().fireEvent(FR.Events.APRINT)
            });

            nativePrintSocket.emit("aliveChecking");
            setTimeout(function () {
                if (isLoadingNativePrint) {
                    nativePrintLoadingDialog.setVisible(true);
                    var contentHtml = "<iframe width='100%' height='100%'  src='" + printUrl + "?op=resource&resource=/com/fr/web/core/dialog/nativePrintLoading.html'>";
                    if (config.isCustomPrint) {
                        contentHtml = "<iframe width='100%' height='100%'  src='nativePrintLoading.html'>"
                    }
                    nativePrintLoadingDialog.setContent({type: "contentHtml", content: contentHtml});
                    setTimeout(function () {
                        FR.checkNativePrintLoading(printUrl, config.isCustomPrint)
                    }, 10000);
                    FR.reconnectNativePrintSocket()
                }
            }, 1000)
        }, getPureConfig: function (config) {
            var conf = {};
            for (var key in config) {
                if ($.isFunction(config[key])) {
                    continue
                }
                conf[key] = config[key]
            }
            return conf
        }, reconnectNativePrintSocket: function () {
            setTimeout(function () {
                if (isLoadingNativePrint) {
                    nativePrintSocket.disconnect();
                    setTimeout(function () {
                        nativePrintSocket.connect();
                        nativePrintSocket.emit("aliveChecking");
                        FR.reconnectNativePrintSocket()
                    }, 2000)
                }
            }, 1000)
        }, startNewNativePrintPreview: function (sessionID, config, printUrl) {
            var o = $.extend({
                isCustomPrint: false,
                currentPageIndex: 1,
                reportTotalPage: 1,
                printers: [],
                printerName: "",
                copy: 1,
                pageType: 0,
                pageIndex: 1,
                orientation: 0,
                paperSize: "A4",
                fitPaper: true,
                marginTop: 6.85,
                marginLeft: 19.05,
                marginBottom: 6.85,
                marginRight: 19.05
            }, config);
            var previewDialog;

            function checkDialogValid() {
                var isValid = true;
                var widgets = [previewDialog.getWidgetByName("specifiedPages"), previewDialog.getWidgetByName("printCopy"), previewDialog.getWidgetByName("printMarginTop"), previewDialog.getWidgetByName("printMarginLeft"), previewDialog.getWidgetByName("printMarginBottom"), previewDialog.getWidgetByName("printMarginRight")];
                for (var i = 0; i < widgets.length; i++) {
                    if (!widgets[i].checkValid()) {
                        isValid = false
                    }
                }
                previewDialog.getWidgetByName("startPrintButton").setEnable(isValid);
                return isValid
            }

            var settingPaneConfig = {
                type: "border", items: [{
                    region: "north", height: 140, background: "#F0F0F1", el: {
                        type: "tablelayout",
                        baseCls: "grey-pane",
                        columnSize: [15, 156, 62, 10, 62, 15],
                        rowSize: [24, 22, 11, 17, 31, 20, 15],
                        vgap: 0,
                        items: [[{el: $("<div>")}], [{el: $("<div>")}, {
                            el: {
                                type: "label",
                                fontsize: 16,
                                fontFamily: "SimSun",
                                textalign: "left",
                                value: FR.i18nText("FR-Engine_Print")
                            }
                        }], [{el: $("<div>")}], [{el: $("<div>")}, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "left",
                                value: FR.i18nText("FR-Engine_Total_Page_Number_X", o.reportTotalPage)
                            }
                        }], [{el: $("<div>")}], [{el: $("<div>")}, {el: $("<div>")}, {
                            el: {
                                type: "quickbutton",
                                text: FR.i18nText("FR-Engine_Print"),
                                widgetName: "startPrintButton",
                                listeners: [{
                                    eventName: "click", action: function () {
                                        if (!checkDialogValid()) {
                                            return
                                        }
                                        FR.confirmNativePrint(o, previewDialog, sessionID, printUrl);
                                        previewDialog.doClose()
                                    }
                                }]
                            }
                        }, {el: $("<div>")}, {
                            el: {
                                type: "quickbutton",
                                style: "white",
                                text: FR.i18nText("FR-Engine_Close"),
                                listeners: [{
                                    eventName: "click", action: function () {
                                        previewDialog.doClose()
                                    }
                                }]
                            }
                        }, {el: $("<div>")}]]
                    }
                }, {
                    region: "south", height: 50, el: {
                        type: "tablelayout",
                        baseCls: "grey-pane",
                        columnSize: [15, 305],
                        rowSize: [5, 20, 5],
                        vgap: 10,
                        items: [[{el: $("<div>")}], [{el: $("<div>")}, {
                            el: {
                                type: "checkbox",
                                widgetName: "quietPrintCheck",
                                text: FR.i18nText("FR-Engine_Quiet_Print_Check_Tip"),
                                manualSelect: true,
                                listeners: [{
                                    eventName: "afteredit", action: function () {
                                        var isQuietPrint = this.getValue();
                                        if (!isQuietPrint) {
                                            FR.showQuietPrintConfirm(previewDialog)
                                        } else {
                                            previewDialog.getWidgetByName("quietPrintCheck").setSelected(false)
                                        }
                                    }
                                }],
                                selected: false
                            }
                        }], [{el: $("<div>")}]]
                    }
                }, {
                    region: "center", el: {
                        type: "tablelayout",
                        columnSize: [15, 67, 223, 15],
                        rowSize: [2, 20, 20, 70, 20, 20, 60],
                        vgap: 15,
                        items: [[{el: $("<div>")}], [{el: $("<div>")}, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "left",
                                value: FR.i18nText("FS-Generic-Simple_Printer") + ":"
                            }
                        }, {
                            el: {
                                type: "combo",
                                widgetName: "printerCombo",
                                items: o.printers,
                                value: o.printers.length > 0 ? o.printers[0] : ""
                            }
                        }], [{el: $("<div>")}, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "left",
                                value: FR.i18nText("FR-Engine_Print_Copy") + ":"
                            }
                        }, {
                            el: {
                                type: "text",
                                width: "60px",
                                fontSize: 12,
                                fontFamily: "SimSun",
                                widgetName: "printCopy",
                                regex: "^\\d+$",
                                value: o.copy,
                                listeners: [{
                                    eventName: "afteredit", action: function () {
                                        checkDialogValid()
                                    }
                                }]
                            }
                        }], [{el: $("<div>")}, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "left",
                                value: FR.i18nText("FR-Engine-Page_Number") + ":"
                            }
                        }, {
                            el: {
                                type: "tablelayout",
                                columnSize: [60, 3, 160],
                                rowSize: [20, 20, 20],
                                vgap: 5,
                                items: [[{
                                    el: {
                                        type: "radio",
                                        widgetName: "allPagesRadio",
                                        text: FR.i18nText("FR-Engine_All_Pages"),
                                        selected: true,
                                        fontSize: 12,
                                        fontFamily: "SimSun",
                                        only_be_selected: true,
                                        listeners: [{
                                            eventName: FR.Events.STATECHANGE, action: function () {
                                                if (this.isSelected()) {
                                                    previewDialog.getWidgetByName("currentPageRadio").setSelected(false);
                                                    previewDialog.getWidgetByName("specifiedPagesRadio").setSelected(false);
                                                    FR.resetSpecifiedPagesField(previewDialog)
                                                }
                                            }
                                        }]
                                    }
                                }], [{
                                    el: {
                                        type: "radio",
                                        widgetName: "currentPageRadio",
                                        text: FR.i18nText("FR-Engine_Current_Page"),
                                        fontSize: 12,
                                        fontFamily: "SimSun",
                                        only_be_selected: true,
                                        listeners: [{
                                            eventName: FR.Events.STATECHANGE, action: function () {
                                                if (this.isSelected()) {
                                                    previewDialog.getWidgetByName("allPagesRadio").setSelected(false);
                                                    previewDialog.getWidgetByName("specifiedPagesRadio").setSelected(false);
                                                    FR.resetSpecifiedPagesField(previewDialog)
                                                }
                                            }
                                        }]
                                    }
                                }], [{
                                    el: {
                                        type: "radio",
                                        widgetName: "specifiedPagesRadio",
                                        text: FR.i18nText("FR-Engine_Specified_Pages"),
                                        fontSize: 12,
                                        fontFamily: "SimSun",
                                        only_be_selected: true,
                                        listeners: [{
                                            eventName: FR.Events.STATECHANGE, action: function () {
                                                if (this.isSelected()) {
                                                    previewDialog.getWidgetByName("allPagesRadio").setSelected(false);
                                                    previewDialog.getWidgetByName("currentPageRadio").setSelected(false);
                                                    previewDialog.getWidgetByName("specifiedPages").setEnable(true)
                                                }
                                            }
                                        }]
                                    }
                                }, {el: $("<div>")}, {
                                    el: {
                                        type: "text",
                                        fontSize: 12,
                                        fontFamily: "SimSun",
                                        widgetName: "specifiedPages",
                                        regex: "^(\\d+-)?\\d+$",
                                        value: "",
                                        watermark: "(" + FR.i18nText("FR-Engine_Example") + ": 2,5,7-10,12)",
                                        listeners: [{
                                            eventName: "afteredit", action: function () {
                                                checkDialogValid()
                                            }
                                        }]
                                    }
                                }]]
                            }
                        }], [{el: $("<div>")}, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "left",
                                value: FR.i18nText("FR-Engine_Layout") + ":"
                            }
                        }, {
                            el: {
                                type: "tablelayout", columnSize: [60, 60], rowSize: [20], items: [[{
                                    el: {
                                        type: "radio",
                                        widgetName: "portraitLayoutRadio",
                                        text: FR.i18nText("FR-Engine-PageSetup_Portrait"),
                                        fontSize: 12,
                                        fontFamily: "SimSun",
                                        selected: true,
                                        only_be_selected: true,
                                        listeners: [{
                                            eventName: FR.Events.STATECHANGE, action: function () {
                                                if (this.isSelected()) {
                                                    previewDialog.getWidgetByName("landscapeLayoutRadio").setSelected(false)
                                                }
                                                FR.refreshNativePreviewFrame(o, sessionID, previewDialog, printUrl)
                                            }
                                        }]
                                    }
                                }, {
                                    el: {
                                        type: "radio",
                                        widgetName: "landscapeLayoutRadio",
                                        text: FR.i18nText("FR-Engine-PageSetup_Landscape"),
                                        fontSize: 12,
                                        fontFamily: "SimSun",
                                        only_be_selected: true,
                                        listeners: [{
                                            eventName: FR.Events.STATECHANGE, action: function () {
                                                if (this.isSelected()) {
                                                    previewDialog.getWidgetByName("portraitLayoutRadio").setSelected(false)
                                                }
                                                FR.refreshNativePreviewFrame(o, sessionID, previewDialog, printUrl)
                                            }
                                        }]
                                    }
                                }]]
                            }
                        }], [{el: $("<div>")}, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "left",
                                value: FR.i18nText("FR-Engine_Paper") + ":"
                            }
                        }, {
                            el: {
                                type: "combo",
                                widgetName: "paperSettingCombo",
                                items: o.paperSizeNames,
                                value: "A4",
                                listeners: [{
                                    eventName: "afteredit", action: function () {
                                        FR.refreshNativePreviewFrame(o, sessionID, previewDialog, printUrl)
                                    }
                                }]
                            }
                        }], [{el: $("<div>")}, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "left",
                                value: FR.i18nText("FR-Engine_Print_Margin") + ":"
                            }
                        }, {
                            el: {
                                type: "tablelayout",
                                columnSize: [17, 60, 26, 16, 17, 60, 26],
                                rowSize: [20, 20],
                                vgap: 10,
                                items: [[{
                                    el: {
                                        type: "label",
                                        fontsize: 12,
                                        fontFamily: "SimSun",
                                        textalign: "left",
                                        value: FR.i18nText("FR-Engine_Top")
                                    }
                                }, {
                                    el: {
                                        type: "text",
                                        fontSize: 12,
                                        fontFamily: "SimSun",
                                        widgetName: "printMarginTop",
                                        value: o.marginTop,
                                        regex: floatRegexText,
                                        listeners: [{
                                            eventName: "afteredit", action: function () {
                                                if (checkDialogValid()) {
                                                    FR.refreshNativePreviewFrame(o, sessionID, previewDialog, printUrl)
                                                }
                                            }
                                        }]
                                    }
                                }, {
                                    el: {
                                        type: "label",
                                        fontsize: 12,
                                        fontFamily: "SimSun",
                                        textalign: "right",
                                        value: FR.i18nText("FR-Engine_Unit_MM")
                                    }
                                }, {el: $("<div>")}, {
                                    el: {
                                        type: "label",
                                        fontsize: 12,
                                        fontFamily: "SimSun",
                                        textalign: "left",
                                        value: FR.i18nText("FR-Engine_Left")
                                    }
                                }, {
                                    el: {
                                        type: "text",
                                        fontSize: 12,
                                        fontFamily: "SimSun",
                                        widgetName: "printMarginLeft",
                                        value: o.marginLeft,
                                        regex: floatRegexText,
                                        listeners: [{
                                            eventName: "afteredit", action: function () {
                                                if (checkDialogValid()) {
                                                    FR.refreshNativePreviewFrame(o, sessionID, previewDialog, printUrl)
                                                }
                                            }
                                        }]
                                    }
                                }, {
                                    el: {
                                        type: "label",
                                        fontsize: 12,
                                        fontFamily: "SimSun",
                                        textalign: "right",
                                        value: FR.i18nText("FR-Engine_Unit_MM")
                                    }
                                }], [{
                                    el: {
                                        type: "label",
                                        fontsize: 12,
                                        fontFamily: "SimSun",
                                        textalign: "left",
                                        value: FR.i18nText("FR-Engine_Bottom")
                                    }
                                }, {
                                    el: {
                                        type: "text",
                                        fontSize: 12,
                                        fontFamily: "SimSun",
                                        widgetName: "printMarginBottom",
                                        value: o.marginBottom,
                                        regex: floatRegexText,
                                        listeners: [{
                                            eventName: "afteredit", action: function () {
                                                if (checkDialogValid()) {
                                                    FR.refreshNativePreviewFrame(o, sessionID, previewDialog, printUrl)
                                                }
                                            }
                                        }]
                                    }
                                }, {
                                    el: {
                                        type: "label",
                                        fontsize: 12,
                                        fontFamily: "SimSun",
                                        textalign: "right",
                                        value: FR.i18nText("FR-Engine_Unit_MM")
                                    }
                                }, {el: $("<div>")}, {
                                    el: {
                                        type: "label",
                                        fontsize: 12,
                                        fontFamily: "SimSun",
                                        textalign: "left",
                                        value: FR.i18nText("FR-Engine_Right")
                                    }
                                }, {
                                    el: {
                                        type: "text",
                                        fontSize: 12,
                                        fontFamily: "SimSun",
                                        widgetName: "printMarginRight",
                                        value: o.marginRight,
                                        regex: floatRegexText,
                                        listeners: [{
                                            eventName: "afteredit", action: function () {
                                                if (checkDialogValid()) {
                                                    FR.refreshNativePreviewFrame(o, sessionID, previewDialog, printUrl)
                                                }
                                            }
                                        }]
                                    }
                                }, {
                                    el: {
                                        type: "label",
                                        fontsize: 12,
                                        fontFamily: "SimSun",
                                        textalign: "right",
                                        value: FR.i18nText("FR-Engine_Unit_MM")
                                    }
                                }]]
                            }
                        }]]
                    }
                }]
            };
            var previewSrc = printUrl + "?op=fr_print&cmd=no_client&preview=true&sessionID=" + sessionID;
            if (FR.Browser.isIE11Before()) {
                previewSrc = printUrl + "?op=resource&resource=/com/fr/web/core/dialog/browserUnsupportPrintPreviewPrompt.html"
            }
            if (o.isCustomPrint) {
                previewSrc = "about:blank"
            }
            previewDialog = new FR.Dialog({
                destroyOnClose: true,
                animate: false,
                title: "",
                titleHeight: 0,
                border: false,
                resizeable: false,
                draggable: false,
                adaptivity: {
                    enabled: true,
                    minWidth: 1000,
                    minHeight: 560,
                    topGap: 0,
                    leftGap: 40,
                    bottomGap: 40,
                    rightGap: 40
                },
                contentWidget: {
                    type: "border",
                    widgetBackground: {background: "#ffffff"},
                    items: [{region: "west", width: 320, el: settingPaneConfig}, {
                        region: "center",
                        el: $('<iframe id="nativePrintPreviewFrame" frameborder="no" src="' + previewSrc + '"></iframe>')
                    }]
                }
            });
            previewDialog.setVisible(true);
            previewDialog.getWidgetByName("printerCombo").setText(o.printerName);
            if (o.pageType === 0) {
                previewDialog.getWidgetByName("allPagesRadio").setSelected(true)
            } else {
                if (o.pageType === 1) {
                    previewDialog.getWidgetByName("currentPageRadio").setSelected(true)
                } else {
                    previewDialog.getWidgetByName("specifiedPagesRadio").setSelected(true);
                    previewDialog.getWidgetByName("specifiedPages").setText(o.pageIndex)
                }
            }
            if (o.orientation === 0) {
                previewDialog.getWidgetByName("portraitLayoutRadio").setSelected(true)
            } else {
                previewDialog.getWidgetByName("landscapeLayoutRadio").setSelected(true)
            }
            previewDialog.getWidgetByName("paperSettingCombo").setText(o.paperSize)
        }, resetSpecifiedPagesField: function (previewDialog) {
            var textField = previewDialog.getWidgetByName("specifiedPages");
            textField.setText("");
            textField.checkValid();
            textField.setEnable(false)
        }, refreshNativePreviewFrame: function (config, sessionID, previewDialog, printUrl) {
            if (config.isCustomPrint) {
                return
            }
            clearTimeout(nativePrintPreviewTimer);
            nativePrintPreviewTimer = setTimeout(function () {
                FR.refreshNativePreviewFrameImmediately(sessionID, previewDialog, printUrl)
            }, 500)
        }, refreshNativePreviewFrameImmediately: function (sessionID, previewDialog, printUrl) {
            var paperConfig = {
                marginTop: previewDialog.getWidgetByName("printMarginTop").getText(),
                marginLeft: previewDialog.getWidgetByName("printMarginLeft").getText(),
                marginBottom: previewDialog.getWidgetByName("printMarginBottom").getText(),
                marginRight: previewDialog.getWidgetByName("printMarginRight").getText(),
                orientation: previewDialog.getWidgetByName("portraitLayoutRadio").isSelected() ? 0 : 1,
                paperSize: previewDialog.getWidgetByName("paperSettingCombo").getText()
            };
            FR.setPrintPaper(sessionID, paperConfig, printUrl, function () {
                var iframe = document.getElementById("nativePrintPreviewFrame");
                iframe.src = iframe.src
            })
        }, checkNativePrintLoading: function (printUrl, isCustomPrint) {
            if (isLoadingNativePrint) {
                nativePrintSocket.close();
                isLoadingNativePrint = false;
                var contentHtml = "<iframe width='100%' height='100%' src='" + printUrl + "?op=fr_print&cmd=native_print_loading_failed'>";
                if (isCustomPrint) {
                    contentHtml = "<iframe width='100%' height='100%' src='nativePrintLoadingFailed.html'>"
                }
                nativePrintLoadingDialog.setContent({type: "contentHtml", content: contentHtml})
            }
        }, confirmNativePrint: function (o, previewDialog, sessionID, printUrl) {
            var index;
            if (previewDialog.getWidgetByName("allPagesRadio").isSelected()) {
                index = ""
            } else {
                if (previewDialog.getWidgetByName("currentPageRadio").isSelected()) {
                    index = o.currentPageIndex
                } else {
                    index = previewDialog.getWidgetByName("specifiedPages").getText()
                }
            }
            var configp = {
                printerName: previewDialog.getWidgetByName("printerCombo").getText(),
                copy: previewDialog.getWidgetByName("printCopy").getText(),
                index: index,
                orientation: previewDialog.getWidgetByName("portraitLayoutRadio").isSelected() ? 0 : 1,
                paperSize: previewDialog.getWidgetByName("paperSettingCombo").getText(),
                marginTop: previewDialog.getWidgetByName("printMarginTop").getText(),
                marginLeft: previewDialog.getWidgetByName("printMarginLeft").getText(),
                marginBottom: previewDialog.getWidgetByName("printMarginBottom").getText(),
                marginRight: previewDialog.getWidgetByName("printMarginRight").getText(),
                quietPrint: previewDialog.getWidgetByName("quietPrintCheck").isSelected(),
                url: printUrl + "?sessionID=" + sessionID + "&op=fr_applet&cmd=print",
                sessionID: sessionID
            };
            if (o.isCustomPrint) {
                configp.url = o.customFileUrl;
                configp.isCustomPrint = true;
                configp.beforePrint = o.beforePrint
            }
            FR.newNativePrintWithArgs(configp)
        }, defaultNewNativePrint: function (config, sessionID, printUrl) {
            if (config.isCustomPrint) {
                FR.newNativePrintWithArgs($.extend({
                    url: config.customFileUrl,
                    beforePrint: config.beforePrint,
                    isCustomPrint: true
                }, config));
                return
            }
            FR.getDefaultNewNativePrintConfig(sessionID, printUrl, function (data) {
                var configp = $.extend(data, {
                    sessionID: sessionID,
                    url: printUrl + "?sessionID=" + sessionID + "&op=fr_applet&cmd=print"
                });
                FR.newNativePrintWithArgs(configp)
            })
        }, getDefaultNewNativePrintConfig: function (sessionID, printUrl, callback) {
            FR.ajax({
                url: printUrl,
                data: {op: "fr_print", cmd: "get_native_print_attr", sessionID: sessionID},
                dataType: "jsonp",
                jsonp: "callback",
                success: function (res) {
                    callback(res)
                }
            })
        }, newNativePrintWithArgs: function (configp) {
            if (configp.isCustomPrint) {
                if ($.isFunction(configp.beforePrint)) {
                    configp.beforePrint()
                }
            } else {
                if (_g().fireEvent(FR.Events.BPRINT) === false) {
                    return
                }
            }
            nativePrintSocket.emit("startPrint", FR.jsonEncode(FR.getPureConfig(configp)))
        }, showQuietPrintConfirm: function (previewDialog) {
            var quietPrintConfirmDialog = new FR.Dialog({
                title: FR.i18nText("FR-Engine_Save_Client_Habit"),
                text4OK: FR.i18nText("FS-Generic-Simple_Save"),
                text4Cancel: FR.i18nText("FS-Generic-Simple_Cancel"),
                onCancel: function () {
                    previewDialog.getWidgetByName("quietPrintCheck").setSelected(false)
                },
                onOK: function () {
                    previewDialog.getWidgetByName("quietPrintCheck").setSelected(true)
                },
                destroyOnClose: true,
                animate: false,
                width: 450,
                height: 180,
                confirm: true,
                border: true,
                closable: true,
                textAlign: "center",
                align: "center",
                contentWidget: {
                    type: "tablelayout",
                    columnSize: [30, 410, 10],
                    rowSize: [30, 100, 40],
                    vgap: 0,
                    items: [[{el: $("<div>")}], [{el: $("<div>")}, {el: $("<div>" + FR.i18nText("FR-Engine_Quiet_Print_Confirm_Tip") + "</div>")}], [{el: $("<div>")}]]
                }
            });
            quietPrintConfirmDialog.setVisible(true)
        }, doNoClientPrint: function (sessionID, popupSetup, printUrl) {
            if (printUrl == null) {
                printUrl = FR.serverURL + FR.servletURL
            }
            if (popupSetup) {
                var margin = {top: 6.85, left: 19.05, bottom: 6.85, right: 19.05};

                function checkDialogValid(dialog) {
                    var topW = dialog.getWidgetByName("PrintMarginTop");
                    var leftW = dialog.getWidgetByName("PrintMarginLeft");
                    var bottomW = dialog.getWidgetByName("PrintMarginBottom");
                    var rightW = dialog.getWidgetByName("PrintMarginRight");
                    var isValid = true;
                    var widgets = [topW, leftW, bottomW, rightW];
                    for (var i = 0; i < widgets.length; i++) {
                        if (!widgets[i].checkValid()) {
                            isValid = false;
                            break
                        }
                    }
                    dialog.win.okButton.setEnable(isValid);
                    return isValid
                }

                var dialog = new FR.Dialog({
                    title: FR.i18nText("FR-Engine_Setting"),
                    confirm: true,
                    resizeable: false,
                    width: 450,
                    height: 180,
                    contentWidget: {
                        type: "tablelayout",
                        columnSize: [100, 40, 60, 30, 60, 60, 30],
                        rowSize: [15, 20, 20],
                        vgap: 15,
                        items: [[{el: $("<div>")}], [{
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "right",
                                value: FR.i18nText("FR-Engine_Print_Margin") + ": "
                            }
                        }, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "right",
                                value: FR.i18nText("FR-Engine_Top") + ": "
                            }
                        }, {
                            el: {
                                type: "text",
                                fontSize: 12,
                                fontFamily: "SimSun",
                                widgetName: "PrintMarginTop",
                                regex: floatRegexText,
                                value: margin.top,
                                listeners: [{
                                    eventName: "afteredit", action: function () {
                                        checkDialogValid(dialog)
                                    }
                                }]
                            }
                        }, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "right",
                                value: FR.i18nText("FR-Engine_Unit_MM")
                            }
                        }, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "right",
                                value: FR.i18nText("FR-Engine_Left") + ": "
                            }
                        }, {
                            el: {
                                type: "text",
                                fontSize: 12,
                                fontFamily: "SimSun",
                                widgetName: "PrintMarginLeft",
                                regex: floatRegexText,
                                value: margin.left,
                                listeners: [{
                                    eventName: "afteredit", action: function () {
                                        checkDialogValid(dialog)
                                    }
                                }]
                            }
                        }, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "right",
                                value: FR.i18nText("FR-Engine_Unit_MM")
                            }
                        }], [{el: $("<div>")}, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "right",
                                value: FR.i18nText("FR-Engine_Bottom") + ": "
                            }
                        }, {
                            el: {
                                type: "text",
                                fontSize: 12,
                                fontFamily: "SimSun",
                                widgetName: "PrintMarginBottom",
                                regex: floatRegexText,
                                value: margin.bottom,
                                listeners: [{
                                    eventName: "afteredit", action: function () {
                                        checkDialogValid(dialog)
                                    }
                                }]
                            }
                        }, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "right",
                                value: FR.i18nText("FR-Engine_Unit_MM")
                            }
                        }, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "right",
                                value: FR.i18nText("FR-Engine_Right") + ": "
                            }
                        }, {
                            el: {
                                type: "text",
                                fontSize: 12,
                                fontFamily: "SimSun",
                                widgetName: "PrintMarginRight",
                                regex: floatRegexText,
                                value: margin.right,
                                listeners: [{
                                    eventName: "afteredit", action: function () {
                                        checkDialogValid(dialog)
                                    }
                                }]
                            }
                        }, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "right",
                                value: FR.i18nText("FR-Engine_Unit_MM")
                            }
                        }]]
                    },
                    onOK: function () {
                        if (!checkDialogValid(dialog)) {
                            return false
                        }
                        var marginConfig = {
                            marginTop: dialog.getWidgetByName("PrintMarginTop").getValue(),
                            marginLeft: dialog.getWidgetByName("PrintMarginLeft").getValue(),
                            marginBottom: dialog.getWidgetByName("PrintMarginBottom").getValue(),
                            marginRight: dialog.getWidgetByName("PrintMarginRight").getValue()
                        };
                        FR.doURLNoClientPrint(sessionID, marginConfig, printUrl)
                    }
                });
                dialog.setVisible(true)
            } else {
                FR.doURLNoClientPrint(sessionID, {}, printUrl)
            }
        }, doURLNoClientPrint: function (sessionID, paperConfig, printUrl) {
            FR.setPrintPaper(sessionID, paperConfig, printUrl, function () {
                if (!checkPDFPrintRegister()) {
                    FR.Msg.toast(FR.i18nText("FR-Engine-Export_Print_Not_Support"));
                    return
                }
                if (_g().fireEvent(FR.Events.BPRINT) === false) {
                    return
                }
                if (FR.Browser.isIE()) {
                    if (FR.Report.Plugin.Print.pdfURLPrint) {
                        FR.Report.Plugin.Print.pdfURLPrint(sessionID);
                        _g().fireEvent(FR.Events.APRINT);
                        return
                    }
                    FR.doPDFPrintForIE(sessionID, printUrl, true)
                } else {
                    FR.doPDFPrintForOthers(sessionID, printUrl)
                }
                _g().fireEvent(FR.Events.APRINT)
            })
        }, doPDFPrintForOthers: function (sessionID, printUrl) {
            var finalUrl = printUrl + "?op=fr_print&cmd=no_client&sessionID=" + sessionID;
            if (FR.isFireFox()) {
                window.open(finalUrl, "_blank")
            } else {
                var $printFrame = $("iframe#printPdf");
                if ($printFrame.length > 0) {
                    $printFrame.attr("src", finalUrl)
                } else {
                    $printFrame = $('<iframe id="printPdf" src = "' + finalUrl + '" height="0px"></iframe>');
                    $printFrame.appendTo($("body"))
                }
                if (FR.isSafari()) {
                    $printFrame.load(function () {
                        $("#printPdf")[0].contentWindow.print()
                    })
                }
            }
        }, doPDFPrintForIE: function (sessionID, printUrl, isPopUp) {
            if ($PrintDiv == null) {
                $PrintDiv = $("<div>").appendTo("body")
            }
            FR.showDialog(FR.i18nText("FR-Engine_Printing"), 250, 100, FR.i18nText("FR-Engine-Loading_Component") + "...");
            FR.ajax({
                url: printUrl,
                data: {sessionID: sessionID, op: "fr_pdfprint", cmd: "pt_print", frandom: Math.random()},
                type: "POST",
                complete: function (res, status) {
                    if (status == "success") {
                        var resText = res.responseText;
                        if (resText == "timeout") {
                            FR.closeDialog();
                            return
                        }
                        $PrintDiv[0].innerHTML = res.responseText;
                        $PDFReader = $PrintDiv.find("Object");
                        try {
                            $PDFReader[0].gotoFirstPage();
                            FR.showDialog(FR.i18nText("FR-Engine_Printing"), 250, 100, FR.i18nText("FR-Engine-Loading_PDF") + "...");
                            checkPDFPrint.defer(300, this, [sessionID, isPopUp])
                        } catch (e) {
                            var content = '<div style="text-align:center;">' + FR.i18nText("FR-Engine_Please_Install") + ' <a href="http://www.adobe.com/products/reader/" target="_blank">Adobe Reader</a>.' + "</div>";
                            FR.showDialog(FR.i18nText("FR-Engine_Alert"), 200, 80, content)
                        }
                    }
                }
            })
        }, isFireFox: function () {
            return /firefox/i.test(navigator.userAgent.toLowerCase())
        }, isSafari: function () {
            return /safari/i.test(navigator.userAgent.toLowerCase())
        }, setPrintPaper: function (sessionID, paperConfig, printUrl, callback) {
            var data = {op: "fr_print", cmd: "print_paper_setting", sessionID: sessionID};
            $.extend(data, paperConfig);
            FR.ajax({
                url: printUrl, data: data, dataType: "jsonp", jsonp: "callback", success: function (res) {
                    if (res.status == "success") {
                        callback()
                    }
                }
            })
        }, doURLPrint: function (config) {
            if (config.isCustomPrint) {
                FR.doNewNativePrint("", $.extend({customFileUrl: config.printUrl}, config));
                return
            }
            if (config.printUrl == FR.servletURL) {
                config.printUrl = FR.serverURL + FR.servletURL
            }
            config.callback = function (sessionID) {
                if (config.printType === 0) {
                    FR.doNoClientPrint(sessionID, config.isPopUp, config.printUrl)
                } else {
                    var o = {isPopUp: config.isPopUp, printUrl: config.printUrl, isCustomPrint: false};
                    FR.doNewNativePrint(sessionID, o)
                }
            };
            __getSessionID__(config.printUrl, config)
        }, doPDFPrint: function (sessionID, popupSetup) {
            var isShowDialog = false;
            FR.ajax({
                url: FR.servletURL,
                data: {op: "fr_pdfprint", cmd: "pdf_print_setting"},
                type: "POST",
                async: false,
                complete: function (res, status) {
                    isShowDialog = res.responseText === "true"
                }
            });
            var config = {"url": FR.servletURL + "?sessionID=" + sessionID, "isPopUp": isShowDialog};
            FR.doURLPDFPrint(config)
        }, doSimplePDFPrint: function (sessionID) {
            if (!checkPDFPrintRegister()) {
                FR.Msg.toast(FR.i18nText("FR-Engine-Export_Print_Not_Support"));
                return
            }
            window.open(FR.servletURL + "?op=fr_pdfprint&cmd=native&sessionID=" + sessionID, "_blank")
        }, doURLPDFPrint: function (config) {
            var url = arguments[0];
            var isPopUp;
            if (typeof url == "string") {
                isPopUp = arguments[1];
                config = arguments[2]
            } else {
                url = config.url;
                isPopUp = config.isPopUp
            }
            var sessionID = __getSessionID__(url, config);
            if (!checkPDFPrintRegister()) {
                FR.Msg.toast(FR.i18nText("FR-Engine-Export_Print_Not_Support"));
                return
            }
            if (!supportPDFPrint()) {
                window.open(FR.servletURL + "?op=fr_pdfprint&cmd=native&sessionID=" + sessionID, "_blank");
                return
            }
            if (FR.Report.Plugin.Print.pdfURLPrint) {
                FR.Report.Plugin.Print.pdfURLPrint(sessionID);
                return
            }
            FR.doPDFPrintForIE(sessionID, isPopUp)
        }, doURLAppletPrint: function (config) {
            var url = arguments[0];
            var isShowDialog;
            var pageIndex;
            var loadJVM;
            var printerName;
            var choosePrinter;
            var copies;
            if (typeof url == "string") {
                isShowDialog = arguments[1];
                if (isShowDialog == undefined || !(typeof isShowDialog == "boolean")) {
                    isShowDialog = isShowAppletPrintSetting()
                }
                config = arguments[2]
            } else {
                url = config.url;
                isShowDialog = config.isPopUp;
                pageIndex = config.pageIndex;
                loadJVM = config.loadJVM;
                choosePrinter = config.choosePrinter;
                printerName = config.printerName;
                copies = config.copies ? config.copies : 1;
                if (isShowDialog == undefined || !(typeof isShowDialog == "boolean")) {
                    isShowDialog = isShowAppletPrintSetting()
                }
            }
            if (isShowDialog.exception === "FAILPASS") {
                FR.Msg.toast(FR.i18nText("FR-Engine-Export_Print_Not_Support"));
                return
            }
            var index = 0;
            var isIE9 = FR.Browser.isIE9();
            if (isShowDialog) {
                var dialog = new FR.Dialog({
                    title: FR.i18nText("FR-Engine_Setting"),
                    confirm: true,
                    resizeable: false,
                    width: 450,
                    height: 190,
                    contentWidget: {
                        type: "tablelayout",
                        columnSize: [20, 100, 155, 150],
                        rowSize: [10, 20, 20, 20],
                        vgap: 10,
                        items: [[{el: $("<div>")}], [{el: $("<div>")}, {
                            el: {
                                type: "radio",
                                widgetName: "All_Pages",
                                text: FR.i18nText("HJS-All_Pages"),
                                selected: true,
                                fontSize: 12,
                                fontFamily: "SimSun",
                                listeners: [{
                                    eventName: FR.Events.STATECHANGE, action: function () {
                                        if (this.isSelected()) {
                                            dialog.getWidgetByName("Specified_Pages").setSelected(false)
                                        }
                                    }
                                }]
                            }
                        }, {el: $("<div>")}, {el: $("<div>")}], [{el: $("<div>")}, {
                            el: {
                                type: "radio",
                                widgetName: "Specified_Pages",
                                text: FR.i18nText("HJS-Specified_Pages") + ":",
                                fontSize: 12,
                                fontFamily: "SimSun",
                                listeners: [{
                                    eventName: FR.Events.STATECHANGE, action: function () {
                                        if (this.isSelected()) {
                                            dialog.getWidgetByName("All_Pages").setSelected(false)
                                        }
                                    }
                                }]
                            }
                        }, {
                            el: {
                                type: "text",
                                width: "155px",
                                fontSize: 12,
                                fontFamily: "SimSun",
                                widgetName: "CurrentPageNum",
                                value: "1-1"
                            }
                        }, {
                            el: {
                                type: "label",
                                fontsize: 12,
                                fontFamily: "SimSun",
                                textalign: "right",
                                value: "(" + FR.i18nText("FR-Engine_Example") + ": 2,5,7-10,12)"
                            }
                        }], [{el: $("<div>")}, {
                            el: {
                                type: "label",
                                value: FR.i18nText("FR-Engine_Print_Copy") + ": ",
                                fontSize: 12,
                                fontFamily: "SimSun",
                                textalign: "left"
                            }
                        }, {
                            el: {
                                type: "text",
                                width: "155px",
                                fontSize: 12,
                                fontFamily: "SimSun",
                                widgetName: "Copies_txt",
                                value: "1"
                            }
                        }]]
                    },
                    onOK: function () {
                        FR.closeDialog();
                        if (dialog.getWidgetByName("Specified_Pages").isSelected()) {
                            index = dialog.getWidgetByName("CurrentPageNum").getValue()
                        }
                        copies = dialog.getWidgetByName("Copies_txt").getValue();
                        if ($appletDiv == null) {
                            $appletDiv = $("<div>").appendTo("body")
                        }
                        var sessionID = __getSessionID__(url, config);
                        if (url.indexOf("reportlet") != -1 || url.indexOf("resultlets") != -1 || (config && config.data && config.data.reportlets)) {
                            if (url.indexOf("?") != -1) {
                                url += "&_=" + new Date().getTime()
                            } else {
                                url += "?_=" + new Date().getTime()
                            }
                            var sessionID = __getSessionID__(url, config);
                            url = FR.servletURL + "?sessionID=" + sessionID
                        }
                        var isSingleSheet = isSingleSheetFunc(sessionID);
                        var attributes = null;
                        var parameters = null;
                        if (supportCodebase()) {
                            attributes = {codebase: FR.server + "/jre.exe"};
                            parameters = {
                                code: "com.fr.print.PrintApplet",
                                archive: FR.server + appletJarName,
                                url: FR.serverURL + url + "&op=fr_applet&cmd=print",
                                isIE9: isIE9,
                                isShowDialog: isShowDialog || choosePrinter,
                                printerName: printerName,
                                index: index,
                                copies: copies,
                                isSingleSheet: isSingleSheet
                            }
                        } else {
                            attributes = {
                                code: "com.fr.print.PrintApplet.class",
                                archive: FR.server + appletJarName,
                                width: 0,
                                height: 0
                            };
                            parameters = {
                                url: FR.serverURL + url + "&op=fr_applet&cmd=print",
                                isIE9: isIE9,
                                isShowDialog: isShowDialog || choosePrinter,
                                printerName: printerName,
                                index: index,
                                copies: copies,
                                isSingleSheet: isSingleSheet
                            }
                        }
                        deployJava.runApplet(attributes, parameters, "1.4");
                        FR.closeDialog();
                        var isOverFunc = function () {
                            if (!isAppletPrintOver(sessionID)) {
                                return
                            }
                            clearInterval(sh);
                            if (_g()) {
                                _g().fireEvent("afterappletprint")
                            }
                            $("applet").remove()
                        };
                        var sh = setInterval(isOverFunc, 3000)
                    },
                    onCancel: function () {
                        index = 0;
                        FR.closeDialog()
                    }
                });
                dialog.setVisible(true)
            } else {
                if (pageIndex != undefined && (typeof pageIndex == "number")) {
                    index = pageIndex
                }
                var sessionID = __getSessionID__(url, config);
                if (url.indexOf("reportlet") != -1 || url.indexOf("resultlets") != -1 || (config && config.data && config.data.reportlets)) {
                    if (url.indexOf("?") != -1) {
                        url += "&_=" + new Date().getTime()
                    } else {
                        url += "?_=" + new Date().getTime()
                    }
                    var sessionID = __getSessionID__(url, config);
                    url = FR.servletURL + "?sessionID=" + sessionID
                }
                var isSingleSheet = isSingleSheetFunc(sessionID);
                if (supportCodebase()) {
                    attributes = {codebase: FR.server + "/jre.exe"};
                    parameters = {
                        code: "com.fr.print.PrintApplet",
                        archive: FR.server + appletJarName,
                        url: FR.serverURL + url + "&op=fr_applet&cmd=print",
                        isIE9: isIE9,
                        isShowDialog: isShowDialog,
                        printerName: printerName,
                        index: index,
                        isSingleSheet: isSingleSheet
                    }
                } else {
                    var attributes = {
                        code: "com.fr.print.PrintApplet.class",
                        archive: FR.server + appletJarName,
                        width: 0,
                        height: 0
                    };
                    var parameters = {
                        url: FR.serverURL + url + "&op=fr_applet&cmd=print",
                        isIE9: isIE9,
                        index: index,
                        isShowDialog: isShowDialog,
                        printerName: printerName,
                        isSingleSheet: isSingleSheet,
                        copies: copies
                    }
                }
                deployJava.runApplet(attributes, parameters, "1.4");
                FR.closeDialog();
                var isOverFunc = function () {
                    if (!isAppletPrintOver(sessionID)) {
                        return
                    }
                    clearInterval(sh);
                    if (_g()) {
                        _g().fireEvent("afterappletprint")
                    }
                    $("applet").remove()
                };
                var sh = setInterval(isOverFunc, 3000)
            }
        }, doAppletPrint: function (sessionID, choosePrinter, printerName) {
            var url = FR.servletURL + "?sessionID=" + sessionID;
            if (_g().fireEvent("beforeappletprint") === false) {
                return
            }
            var config = {"url": url, "choosePrinter": choosePrinter, "printerName": printerName};
            FR.doURLAppletPrint(config)
        }, doFlashPrint: function (sessionID, currentPageIndex) {
            if (_g().fireEvent("beforeflashprint") === false) {
                return
            }
            var fitPaper = isFitPaper();
            FR.doPrintURL(FR.servletURL + "?sessionID=" + sessionID, currentPageIndex, fitPaper)
        }, doPrintURL: function (url, currentPageIndex, fitPaper, config) {
            var config = arguments[3];
            if (config == undefined) {
                config = {"url": url, "pageIndex": currentPageIndex, "isAutoZoom": fitPaper}
            }
            FR.doURLFlashPrint(config)
        }, isInstalledFlash: function () {
            if (this.$i_flash === true) {
                return true
            }
            var i_flash = false;
            var n = navigator;
            if (n.plugins && n.plugins.length) {
                for (var ii = 0; ii < n.plugins.length; ii++) {
                    if (n.plugins[ii] && n.plugins[ii].name.indexOf("Shockwave Flash") != -1) {
                        i_flash = true;
                        break
                    }
                }
            } else {
                if (window.ActiveXObject) {
                    for (var ii = 11; ii >= 2; ii--) {
                        try {
                            var fl = eval("new ActiveXObject('ShockwaveFlash.ShockwaveFlash." + ii + "');");
                            if (fl) {
                                i_flash = true;
                                break
                            }
                        } catch (e) {
                        }
                    }
                }
            }
            this.$i_flash = i_flash;
            return i_flash
        }, flashInstall: function () {
            var content;
            FR.ajax({
                type: "POST",
                url: FR.servletURL + "?op=flash_print&cmd=get_fp_installpath",
                complete: function (res, status) {
                    if (status == "success") {
                        var path = res.responseText;
                        if (typeof path == "string" && path.length > 0 && path.length < 150) {
                            content = '<div style="text-align:center;">' + FR.i18nText("FR-Engine_Please_Install") + ' <a href="' + path + '">FlashPlayer</a>.' + FR.i18nText("FR-Engine_Restart_Browser_After_Installation") + "</div>"
                        }
                    }
                    if (!content) {
                        content = '<div style="text-align:center;">' + FR.i18nText("FR-Engine_Please_Install") + ' <a href="http://get.adobe.com/flashplayer/" target="_blank">FlashPlayer</a>.' + FR.i18nText("FR-Engine_Restart_Browser_After_Installation") + "</div>"
                    }
                    FR.showDialog(FR.i18nText("FR-Engine_Alert"), 305, 75, content)
                }
            })
        }, doURLFlashPrint: function (config) {
            var url = arguments[0];
            var currentPageIndex;
            var isShowDialog;
            var isAutoZoom;
            if (typeof url == "string") {
                isShowDialog = arguments[1];
                isAutoZoom = isFitPaper();
                if (isShowDialog == undefined || !(typeof isShowDialog == "boolean")) {
                    isShowDialog = isShowFlashPrintSetting()
                }
                config = arguments[2]
            } else {
                url = config.url;
                currentPageIndex = config.pageIndex;
                isShowDialog = config.isPopUp;
                isAutoZoom = config.isAutoZoom;
                if (isShowDialog == undefined || !(typeof isShowDialog == "boolean")) {
                    isShowDialog = isShowFlashPrintSetting()
                }
                if (isAutoZoom == undefined || !(typeof isAutoZoom == "boolean")) {
                    isAutoZoom = isFitPaper()
                }
            }
            if (FR.isInstalledFlash() === true) {
                if ($flashIframe == null) {
                    FR.__flash__ = function () {
                        FR.doPrintURL.call(this, url, currentPageIndex, isAutoZoom, config);
                        delete FR.__flash__
                    };
                    $flashIframe = $("<iframe>").css({
                        position: "absolute",
                        left: -1000,
                        top: -1000
                    }).attr("src", FR.servletURL + "?op=resource&resource=/com/fr/web/core/printer.html").appendTo("body")
                } else {
                    var checkVersion = FR.Browser.isIE() && (FR.Browser.isIE8Before() || !$.support.boxModel);
                    var fm = checkVersion ? $flashIframe[0].contentWindow["flashMovie"] : $flashIframe[0].contentDocument["flashMovie"];
                    if (fm != null && fm.doLoadFlash != null) {
                        var servletURL = url.split("?")[0];
                        currentPageIndex = currentPageIndex || 1;
                        var sessionID = __getSessionID__(url, config);
                        var isPPAPIPrint = isPrintAsPPAPI();
                        FR.ajax({
                            type: "POST",
                            url: servletURL,
                            data: {sessionID: sessionID, op: "flash_print", cmd: "get_fp_pageinfo"},
                            complete: function (res, status) {
                                if (!FR.versionRemind(res.responseText)) {
                                    return
                                }
                                var message_array = res.responseText.split("?");
                                var info_array = message_array[0].split(";");
                                if (info_array[0].indexOf("FAILPASS") != -1) {
                                    FR.Msg.toast(FR.i18nText("FR-Engine-Export_Print_Not_Support"));
                                    return
                                }
                                var init_page_setting = "1-" + info_array[0];
                                var offset = message_array.length > 1 ? message_array[1].split(";") : ["0.0", "0.0"];
                                var print_fn = function (page_setting, isAutoZoom, paperinfo, isPrintAsImage, isPPAPIPrint) {
                                    FR.showDialog(FR.i18nText("FR-Engine_Print"), 250, 100, FR.i18nText("FR-Engine_Start_Print") + "...");
                                    fm.doLoadFlash(servletURL, sessionID + "&id=" + (new Date().getTime()), FR.string2ints(page_setting).join(","), isAutoZoom, paperinfo, isPrintAsImage, isPPAPIPrint);
                                    $flashIframe = null;
                                    if (_g()) {
                                        _g().fireEvent("afterflashprint")
                                    }
                                };
                                if (isShowDialog) {
                                    var dialog = new FR.Dialog({
                                        title: FR.i18nText("FR-Engine_Setting"),
                                        confirm: true,
                                        resizeable: false,
                                        width: 450,
                                        height: 280,
                                        contentWidget: {
                                            type: "border", width: 420, height: 240, items: [{
                                                region: "north", el: {
                                                    type: "tablelayout",
                                                    columnSize: [20, 100, 155, 150],
                                                    rowSize: [10, 20, 20, 20],
                                                    height: 120,
                                                    vgap: 10,
                                                    items: [[{el: $("<div>")}], [{el: $("<div>")}, {
                                                        el: {
                                                            type: "radio",
                                                            fontSize: 12,
                                                            fontFamily: "SimSun",
                                                            widgetName: "All_Pages",
                                                            text: FR.i18nText("HJS-All_Pages"),
                                                            selected: true,
                                                            only_be_selected: true,
                                                            listeners: [{
                                                                eventName: FR.Events.STATECHANGE, action: function () {
                                                                    if (this.isSelected()) {
                                                                        dialog.getWidgetByName("Current_Page").setSelected(false);
                                                                        dialog.getWidgetByName("Specified_Pages").setSelected(false)
                                                                    }
                                                                }
                                                            }]
                                                        }
                                                    }, {el: $("<div>")}, {el: $("<div>")}], [{el: $("<div>")}, {
                                                        el: {
                                                            type: "radio",
                                                            fontSize: 12,
                                                            fontFamily: "SimSun",
                                                            widgetName: "Current_Page",
                                                            text: FR.i18nText("HJS-Current_Page") + ":",
                                                            only_be_selected: true,
                                                            listeners: [{
                                                                eventName: FR.Events.STATECHANGE,
                                                                action: function () {
                                                                    if (this.isSelected()) {
                                                                        dialog.getWidgetByName("All_Pages").setSelected(false);
                                                                        dialog.getWidgetByName("Specified_Pages").setSelected(false)
                                                                    }
                                                                }
                                                            }]
                                                        }
                                                    }, {
                                                        el: {
                                                            type: "label",
                                                            fontsize: 12,
                                                            fontFamily: "SimSun",
                                                            value: FR.i18nText("FR-Engine-Page_Number") + " " + FR.i18nText("FR-Engine_Is") + " " + currentPageIndex
                                                        }
                                                    }, {el: $("<div>")}], [{el: $("<div>")}, {
                                                        el: {
                                                            type: "radio",
                                                            fontSize: 12,
                                                            fontFamily: "SimSun",
                                                            widgetName: "Specified_Pages",
                                                            text: FR.i18nText("HJS-Specified_Pages") + ":",
                                                            only_be_selected: true,
                                                            listeners: [{
                                                                eventName: FR.Events.STATECHANGE, action: function () {
                                                                    if (this.isSelected()) {
                                                                        dialog.getWidgetByName("All_Pages").setSelected(false);
                                                                        dialog.getWidgetByName("Current_Page").setSelected(false)
                                                                    }
                                                                }
                                                            }]
                                                        }
                                                    }, {
                                                        el: {
                                                            type: "text",
                                                            fontSize: 12,
                                                            fontFamily: "SimSun",
                                                            widgetName: "SpecifiedPagesNum",
                                                            value: "1-2"
                                                        }
                                                    }, {
                                                        el: {
                                                            type: "label",
                                                            fontsize: 12,
                                                            fontFamily: "SimSun",
                                                            textalign: "right",
                                                            value: "(" + FR.i18nText("FR-Engine_Example") + " : 2,5,7-10,12)"
                                                        }
                                                    }]]
                                                }
                                            }, {
                                                region: "center",
                                                el: {
                                                    type: "tablelayout",
                                                    columnSize: [16, 300],
                                                    rowSize: [20, 20],
                                                    vgap: 0,
                                                    items: [[{el: $("<div>")}, {
                                                        el: {
                                                            type: "checkbox",
                                                            fontSize: 12,
                                                            fontFamily: "SimSun",
                                                            widgetName: "AsImage",
                                                            text: FR.i18nText("FR-Engine-Print_As_Image"),
                                                            selected: false,
                                                            disabled: false
                                                        }
                                                    }], [{el: $("<div>")}, {
                                                        el: {
                                                            type: "checkbox",
                                                            widgetName: "FitPaperSize",
                                                            fontSize: 12,
                                                            fontFamily: "SimSun",
                                                            text: FR.i18nText("Print-Print_To_Fit_Paper_Size"),
                                                            selected: isFitPaper(),
                                                            disabled: false
                                                        }
                                                    }]]
                                                }
                                            }, {
                                                region: "south",
                                                el: {
                                                    type: "tablelayout",
                                                    columnSize: [20, 300],
                                                    rowSize: [25],
                                                    height: 35,
                                                    items: [[{el: $("<div>")}, {
                                                        el: {
                                                            type: "label",
                                                            fontsize: 16,
                                                            fontFamily: "SimSun",
                                                            color: "red",
                                                            value: FR.i18nText("FR-Engine-Print_Set_Printer_Offset") + ": X=" + offset[0] + "mm Y=" + offset[1] + "mm"
                                                        }
                                                    }]]
                                                }
                                            }]
                                        },
                                        onOK: function () {
                                            FR.closeDialog();
                                            if (dialog.getWidgetByName("Current_Page").isSelected()) {
                                                init_page_setting = currentPageIndex + ""
                                            } else {
                                                if (dialog.getWidgetByName("Specified_Pages").isSelected()) {
                                                    init_page_setting = dialog.getWidgetByName("SpecifiedPagesNum").getValue()
                                                }
                                            }
                                            var printAsImage = dialog.getWidgetByName("AsImage").isSelected();
                                            var isFit = dialog.getWidgetByName("FitPaperSize").isSelected();
                                            print_fn(init_page_setting, isFit, info_array[1] ? info_array[1] : "", printAsImage, isPPAPIPrint)
                                        },
                                        onCancel: function () {
                                            FR.closeDialog()
                                        }
                                    });
                                    dialog.setVisible(true)
                                } else {
                                    print_fn(init_page_setting, isAutoZoom, info_array[1] ? info_array[1] : "", false, isPPAPIPrint)
                                }
                            }
                        })
                    } else {
                        FR.flashInstall()
                    }
                }
            } else {
                FR.flashInstall()
            }
        }
    })
})(jQuery);