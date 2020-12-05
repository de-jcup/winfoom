:: Client for Winfoom - Basic Proxy Facade

@echo off

setlocal ENABLEEXTENSIONS

if [%1]==[] (
   @echo Invalid command, try 'foomcli --help' for more information
   exit /B 1
)

if "%1"=="--help" goto usage

if not "%1"=="start" if not "%1"=="stop" if not "%1"=="status" if not "%1"=="validate" if not "%1"=="shutdown" if not "%1"=="test" if not "%1"=="config" if not "%1"=="autodetect" if not "%1"=="reset" (
   @echo Invalid command, try 'foomcli --help' for more information
   exit /B 1
)

if "%1"=="config" if not [%2]==[] if not "%2"=="-f"  if not "%2"=="-t" (
   @echo Invalid command, try 'foomcli --help' for more information
   exit /B 1
)

if "%1"=="config" if "%2"=="-f" if [%3]==[]  (
   @echo Invalid command, try 'foomcli --help' for more information
   exit /B 1
)

if "%1"=="config" if "%2"=="-t" if [%3]==[]  (
   @echo Invalid command, try 'foomcli --help' for more information
   exit /B 1
)

if "%1"=="config" if "%2"=="-d" if [%3]==[]  (
   @echo Invalid command, try 'foomcli --help' for more information
   exit /B 1
)

set "CTL_USER=admin:winfoom"

if not defined FOOM_LOCATION set "FOOM_LOCATION=localhost:9999"

if "%1"=="config" (
	if "%2"=="-f" (
	curl -X POST -H "Content-Type: application/json" -d @%3 --user %CTL_USER% http://%FOOM_LOCATION%/%1
	) else (
	    if "%2"=="-t" (
             goto setproxytype
		) else (
		    if "%2"=="-d" (
			    curl -X POST -H "Content-Type: application/json" -d %3 --user %CTL_USER% http://%FOOM_LOCATION%/%1
			) else (
			    curl --user %CTL_USER% http://%FOOM_LOCATION%/%1
			)
		)
    )	
) else (
    if "%1"=="validate" @echo It may take some time, please be pacient ...
	curl --user %CTL_USER% http://%FOOM_LOCATION%/%1
)

goto exit

:setproxytype
for /f "usebackq delims=" %%I in (`powershell "\"%3\".toUpper()"`) do set "proxyTypeUpper=%%~I"
@echo Attempt to change proxy type to: %proxyTypeUpper% ...
curl -X POST -H "Content-Type: application/json" -d "{\"proxyType\": \"%proxyTypeUpper%\"}" --user %CTL_USER% http://%FOOM_LOCATION%/%1

:exit
if %ERRORLEVEL%==7 (
@echo Is the application running?
)
exit /B %ERRORLEVEL%

:usage
@echo Usage: foomcli [command] [arguments]
@echo It manages winfoom application"
@echo Note:  It requires 'curl' tool"
@echo.
@echo [command] must be one of the following:
@echo    start                              start the local proxy facade
@echo    stop                               stop the local proxy facade
@echo    status                             get the current status of the local proxy facade
@echo    shutdown                           shutdown the application
@echo    validate                           test the local proxy facade configuration
@echo    autodetect                         attempt to apply Internet Explorer settings 
@echo    config                             print the current configuration
@echo    config -f [json_filepath]          apply the proxy configuration, where the [json_filepath] is
@echo                                       the path to the JSON file containing the configuration to be applied
@echo    config -d [json_content]           apply the proxy configuration, where the [json_content] is
@echo                                       the JSON object containing the configuration to be applied
@echo    config -t [proxy_type]             change the proxy type, where [proxy_type] can be 
@echo                                       one of: direct, http, pac, socks4, socks5

