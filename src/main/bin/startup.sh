#!/bin/bash 
# Copyright (C) 2013-2024 Nanjing Pengyun Network Technology Co., Ltd.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 
# Absolute path this script is in, thus /home/user/bin
SCRIPTPATH=$(cd "$(dirname "$0")" || exit ; pwd)
ROOTPATH=$SCRIPTPATH/..
ulimit -HSn 65536 

nohup java -noverify -server -XX:+UseG1GC -Xloggc:logs/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -cp "$ROOTPATH:$ROOTPATH/lib/*:$ROOTPATH/config" py.processmanager.ProcessManager "$SCRIPTPATH/dd-launcher.sh"  "$SCRIPTPATH" > /dev/null  2>&1 &
