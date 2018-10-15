function doFrPrint(id) {
    debugger;
    alert('补偿单号：' + id);
    var url = "${ctx}/ReportServer";
    var isPopUp = true;
    var reportlets = [{reportlet: 'print/${loginer.areaCode?substring(0,4)}/compen_3L.cpt', compen_id: id}];
    var config = {url: url, isPopUp: isPopUp, data: {reportlets: reportlets}};
    // FR.doURLFlashPrint(config)
}

// localhost:8090/ncmsweb/admin/report/print.do?compenId=970116048&popUp=true

//9.0 打印
var printUrl = "http://192.168.88.235:8083/WebReport/ReportServer";
var reportlets = "[{reportlet: 'demo/TestDemo.cpt', startTime: '2018-01-01',endTime:'208-08-10'}]";

/**
 *
 * @param printUrl 为需要打印模板的应用地址和服务，如"http://localhost:8075/WebReport/ReportServer"允许为空。
 * 如果为空的话，就使用当前的 servlet 地址。如果需要跨域，则此参数不能为空
 * @param isPopUp 是否弹出设置窗口，true为弹出，false为不弹出
 * @param reportlets 为需要打印的模板及其参数，如"[{reportlet: '1.cpt', p1: 'a'}, {reportlet: '1.cpt', p1: 'b'}]"
 * @param printType 打印类型，0 为零客户端打印，1 为本地打印
 */
function doReportPrint(printUrl, isPopUp, reportlets, printType) {
    debugger;
    var config = {
        printUrl: printUrl,
        isPopUp: isPopUp,
        data: {reportlets: reportlets},
        printType: printType
    };
    debugger;
    FR.doURLPrint(config);
}

/**
 *
 * @param name
 * @param params
 */
function getReportlet(name, params) {
    va
}