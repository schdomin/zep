-------------------------------------------------------------
README: ZEP Debug Build XXXX-XX-XX



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
[LEFT ARROW KEY] : Visit previous image - deletes the last classification action
[DOWN ARROW KEY] : Classifies the current image as Dislike - opens next image
[RIGHT ARROW KEY]: Classifies the current image as Like - opens next image



-------------------------------------------------------------
TROUBLESHOOTING:

- Command java not found: make sure the java binary path is set in the $PATH environment variable
- Make sure to use a unique username without any magic, hebrew letters
- Continuous logging available through the console



-------------------------------------------------------------
CONFIG FILE:

- Create a local file: config.txt in the zep directory (top level)
- Make sure to set the MySQL login information correctly (username, password)
- The file has the following syntax (only the lines with = signs matter):

//ds GUI
m_iWindowWidth=1200
m_iWindowHeight=800
            
//ds MySQL
m_strMySQLServerURL=jdbc:mysql://pc-10129.ethz.ch:3306/domis
m_strMySQLUsername=username
m_strMySQLPassword=password

