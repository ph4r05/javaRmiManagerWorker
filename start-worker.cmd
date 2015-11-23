SETLOCAL ENABLEEXTENSIONS
SET ME=%~n0
SET PARENT=%~dp0
java -Djava.security.policy=%PARENT%\java.policy -jar %PARENT%\target\admwl-1.0-SNAPSHOT.jar --worker %*