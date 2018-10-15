reg add "HKEY_CLASSES_ROOT\finereport" /v "URL Protocol" /f
reg add "HKEY_CLASSES_ROOT\finereport\DefaultIcon" /f
reg add "HKEY_CLASSES_ROOT\finereport\shell\open\command" /v "" /d "%~dp0bin\PrintPlus.exe \"^%%1\"" /f
pause