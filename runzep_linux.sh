#!/bin/bash

#ds check if java dir is not set
if [ "$JAVA_HOME" = "" ]; then

    #run zep from PATH variable
    echo "Running ZEP from PATH variable"
    java -cp bin/.:thirdparty/* main.CMain
    
else

    #run zep from JAVA_HOME variable
    echo "Running ZEP from JAVA_HOME variable"
    $JAVA_HOME/bin/java -cp bin/.:thirdparty/* main.CMain 
    
fi

