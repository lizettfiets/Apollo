del /S /F /Q %1\jre
rmdir /S /Q %1\jre

curl -o jre.zip https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_windows-x64_bin.zip
unzip jre.zip

ren jdk-11.0.1 jre