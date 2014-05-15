:ds disable echo
@ECHO OFF

:ds set size
mode con:cols=110 lines=30

:ds check if java dir is not set
if "%JAVA_HOME%"=="" (

:ds look for java.exe
for %%X in (java.exe) do (set FOUND=%%~$PATH:X)

:ds if we found the executable
if defined FOUND (

:ds run from PATH
echo "LAUNCH: JAVA_HOME not set - calling java from PATH variable"
java -cp bin\.;thirdparty\*; main.CMain 
pause

) else (

:ds if programfiles(x86) is not set
if "%ProgramFiles(x86)%"=="" (

:ds run from default directory
echo "LAUNCH: no useful environment variable set - running from standart path"
"%ProgramFiles%\Java\jre7\bin\java.exe" -cp bin\.;thirdparty\*; main.CMain
pause

) else (

:ds run from default directory
echo "LAUNCH: ProgramFiles(x86) set - calling java.exe"
"%ProgramFiles(x86)%\Java\jre7\bin\java.exe" -cp bin\.;thirdparty\*; main.CMain
pause

)

)

) else (

:ds run from JAVA_HOME
echo "LAUNCH: JAVA_HOME set - calling java from JAVA_HOME"
%JAVA_HOME%\bin\java -cp bin\.;thirdparty\*; main.CMain
pause

)