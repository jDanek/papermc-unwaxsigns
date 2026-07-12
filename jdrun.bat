@echo off
cls
:menu
echo ===================================================
echo   jDanek - Gradle Build Menu
echo ===================================================
echo  1] Quick build (creates testing jar)
echo  2] Start test Minecraft server (runServer)
echo  3] Start experimental Minecraft server
echo  4] Publish version to Hangar (ignores config cache)
echo  5] Clean project (removes build folder)
echo  6] Exit (or press 'q')
echo ===================================================
set /p option="Select an option (1-6): "

if "%option%"=="1" goto build
if "%option%"=="2" goto server
if "%option%"=="3" goto experimental
if "%option%"=="4" goto hangar
if "%option%"=="5" goto clean
if "%option%"=="6" goto exit
if /i "%option%"=="q" goto exit

echo.
echo Invalid option, please try again.
echo.
pause
cls
goto menu

:build
echo Running clean build...
call .\gradlew clean build
goto end

:server
echo Starting test Minecraft server...
call .\gradlew runServer
goto end

:experimental
echo Starting experimental Minecraft server...
call .\gradlew runServerExperimental
goto end

:hangar
echo Publishing to Hangar (without configuration cache)...
call .\gradlew build publishPluginPublicationToHangar --no-configuration-cache
goto end

:clean
echo Cleaning temporary Gradle files...
call .\gradlew clean
goto end

:end
echo.
echo Done!
echo.
pause
cls
goto menu

:exit
exit