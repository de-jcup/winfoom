:: Launcher for Winfoom - Basic Proxy Facade
@echo off

setlocal EnableDelayedExpansion

if "%1"=="--help" goto usage

set ARGS=-server -XX:+UseG1GC -XX:MaxHeapFreeRatio=30 -XX:MinHeapFreeRatio=10 -Dnashorn.args=--no-deprecation-warning

if defined FOOM_ARGS ARGS=%FOOM_ARGS% %ARGS%

for %%a in (%*) do (
    if not "%%a"=="--debug" if not "%%a"=="--systemjre" if not "%%a"=="--gui" (
		@echo Invalid command, try 'launch --help' for more information
		exit /B 1;
	)
	if "%%a"=="--debug" (
		set ARGS=%ARGS% -Dlogging.level.root=DEBUG -Dlogging.level.java.awt=INFO -Dlogging.level.sun.awt=INFO -Dlogging.level.javax.swing=INFO -Dlogging.level.jdk=INFO
	)
	if "%%a"=="--gui" (
		set ARGS=%ARGS% -Dspring.profiles.active=gui
	)
	if "%%a"=="--systemjre" (
		set JAVA_EXE=javaw
	)
)

del /F out.log

if exist out.log (
    @echo Is there another application's instance running?
	exit /B 2
)

if not defined JAVA_EXE set JAVA_EXE=.\jdk\bin\javaw

start /B %JAVA_EXE% %ARGS% -cp . -jar winfoom.jar > out.log 2>&1

@echo You can check the application log with:
@echo powershell -command Get-Content -Path "$HOME/.winfoom/logs/winfoom.log" -Wait
@echo If application is not launched in GUI mode, use foomcli script for management

exit /B %ERRORLEVEL%

:usage
@echo Usage: launch [arguments]
@echo where [arguments] must be any of the following:
@echo    --debug             start in debug mode
@echo    --systemjre         use the system jre
@echo    --gui               start with graphical user interface