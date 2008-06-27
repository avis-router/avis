@echo off

setlocal

set tools_jar=%~dp0..\lib\avis-tools.jar
set java_opts=-Xverify:none

if not exist "%tools_jar%" then goto no_jar

java -cp "%tools_jar%" %java_opts% org.avis.tools.Ep %*

goto :eof

:no_jar

echo Cannot find avis-tools.jar
exit 1
