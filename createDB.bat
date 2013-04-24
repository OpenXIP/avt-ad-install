SET helper=d:\AVT
%helper:~0,2%
d:\AVT\ad-install\CPAU.exe -u db2admin -p "wakawaka123" -ex  "db2cmd /i /c /w db2 CREATE DATABASE ad PAGESIZE 8192" -lwp -wait