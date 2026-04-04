@echo off
"C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot\bin\java" -Dmaven.multiModuleProjectDirectory="." -cp ".\.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain clean compile
