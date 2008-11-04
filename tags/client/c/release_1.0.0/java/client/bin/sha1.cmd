@echo off

setlocal

set client_jar=%~dp0..\lib\avis-client.jar
set tools_jar=%~dp0..\lib\avis-tools.jar
set java_opts=-Xverify:none

if not exist "%tools_jar%" then goto no_jar

java -cp "%tools_jar%;%client_jar%" %java_opts% org.avis.tools.Hash SHA1 %*

goto :eof

:no_jar

echo Cannot find avis-tools.jar or avis-client.jar
exit 1
