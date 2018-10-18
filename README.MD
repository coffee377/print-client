打印服务端
=========

### 1. JavaScript [print.js]

    9.0 改良后的打印 js 文件[dist/js/print.js]，需替换实际服务包中相应的js,具体路径如下
    fr-report-XXX!\com\fr\web\core\js\print.js
    
### 2. PrintClient

打印客户端
=========
1 .下载列表

| id | media file type | display name | media file path |
|---|---|---|---|
|36	|windows	|Windows 32bit	|Print_Plus_windows_1_0_0.exe|
|30	|windows	|Windows 64bit	|Print_Plus_windows-x64_1_0_0.exe|
|39	|windows	|Windows Archive 32bit	|Print_Plus_windows_1_0_0.zip|
|34	|windows	|Windows Archive 64bit	|Print_Plus_windows-x64_1_0_0.zip|

2. 配置文件 PrintPlus.json

    
    /**
     * 默认打印机名称
     */
    private String printerName = "";
    
    /**
     * 是否静默打印
     */
    @JSONField(ordinal = 1)
    private boolean quietPrint = false;
    
    /**
     * 是否Nginx代理
     */
    @JSONField(ordinal = 2)
    private boolean proxy = true;
    
    /**
     * 代理服务地址 协议 + 域名 + 端口
     */
    @JSONField(ordinal = 3)
    private String serverURL = "http://42.123.97.19:8086";