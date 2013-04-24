SETLOCAL
SET helper=d:\AVT
%helper:~0,2%
cd d:\AVT\XIPHost
set host_dir=%CD%
cd ..\ad-install
..\jdk\bin\java -jar build\lib\ad-install.jar -f "%host_dir%\connection.properties"