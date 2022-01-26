@echo off

set XDAG_VERSION="0.4.5"
set XDAG_JARNAME="xdagj-%XDAG_VERSION%-shaded.jar"
set XDAG_OPTS="-t"

#set JAVA_HOME="C:\Program Files\Java\jdk"

# default JVM options
set JAVA_OPTS="--add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED -server -Xms1g -Xmx1g"

java %JAVA_OPTS% -cp .;%XDAG_JARNAME% io.xdag.Bootstrap %*
