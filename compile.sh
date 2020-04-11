#!/bin/bash
# there seems to be caching issues with the .exe
rm MyPCClientRemote.exe MyPCClientRemote.jar  
./gradlew shadowJar && ./gradlew createExe
