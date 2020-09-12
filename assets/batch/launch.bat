:: Launcher for Winfoom - Basic Proxy Facade

@echo off

setlocal EnableDelayedExpansion

set ARGS=-server -Dnashorn.args=--no-deprecation-warning

FOR %%a IN (%*) DO (
    SET "_arg_=%%~a"
    IF NOT "%%a"=="--debug" IF NOT "%%a"=="--systemjre" IF NOT "!_arg_:~0,2!"=="-D" (
		echo Unknow parameter: %%a
		exit /B 1;
	)

	IF "%%a"=="--debug" (
		SET ARGS=%ARGS% -Dlogging.level.root=DEBUG
	)

	IF "%%a"=="--systemjre" (
		SET JAVA_EXE=javaw
	)

	IF "!_arg_:~0,2!"=="-D" (
        SET "ARGS=!ARGS! !_arg_!"
    )

)

IF NOT DEFINED JAVA_EXE set JAVA_EXE=jdk/bin/javaw

start %JAVA_EXE% %ARGS% -cp . -jar winfoom.jar