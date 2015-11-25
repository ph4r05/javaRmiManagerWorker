SETLOCAL ENABLEEXTENSIONS
SET ME=%~n0
SET PARENT=%~dp0
java -Djava.security.policy=%PARENT%\java.policy -jar %PARENT%\admwl-demo\target\admwl-demo-1.0-SNAPSHOT.jar %*