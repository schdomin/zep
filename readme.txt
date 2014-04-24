-------------------------------------------------------------
README: ZEP Debug Build 2014-04-23



-------------------------------------------------------------
RUN GUI:

- open a console
- change to directory: zep
- make sure the folders bin and thirdparty are visible

LINUX:
- run: 
java -cp bin/.:thirdparty/*: main.CMain

WINDOWS
- run: 
java -cp bin\.;thirdparty\*; main.CMain



-------------------------------------------------------------
KEY BINDINGS:

[Escape]         : Quits GUI and closes the application
[R]              : Resets learning process - meaning that a new dataset is fetched to start with and all learning memory is erased
[LEFT ARROW KEY] : Visit previous image - deletes the last classification action
[DOWN ARROW KEY] : Classifies the current image as Dislike - opens next image
[RIGHT ARROW KEY]: Classifies the current image as Like - opens next image



-------------------------------------------------------------
TROUBLESHOOTING:

- Command java not found: make sure the java binary path is set in the $PATH environment variable
- Make sure to use a unique username without any magic, hebrew letters
- Continuous logging available through the console
