reg add "HKEY_CLASSES_ROOT\PrintPlus" /v "URL Protocol" /f
reg add "HKEY_CLASSES_ROOT\PrintPlus\DefaultIcon" /f
reg add "HKEY_CLASSES_ROOT\PrintPlus\shell\open\command" /v "" /d "%~dp0PrintPlus.exe \"^%%1\"" /f
