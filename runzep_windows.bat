:ds disable echo
@ECHO OFF

:ds check if java dir is not set
IF "%JAVA_HOME%"=="" (

ECHO JAVA_HOME not set - calling java from PATH variable
java -cp bin\.;thirdparty\*; main.CMain

) ELSE (

ECHO JAVA_HOME set - calling java from JAVA_HOME
%JAVA_HOME%\bin\java -cp bin\.;thirdparty\*; main.CMain

)