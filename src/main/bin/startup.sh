#!/bin/bash 
# Absolute path this script is in, thus /home/user/bin
SCRIPTPATH=$(cd "$(dirname "$0")" || exit ; pwd)
ROOTPATH=$SCRIPTPATH/..
ulimit -HSn 65536 

nohup java -noverify -server -XX:+UseG1GC -Xloggc:logs/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -cp "$ROOTPATH:$ROOTPATH/lib/*:$ROOTPATH/config" py.processmanager.ProcessManager "$SCRIPTPATH/dd-launcher.sh"  "$SCRIPTPATH" > /dev/null  2>&1 &
