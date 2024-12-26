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
SCRIPTPATH=$(cd "$(dirname "$0")" || exit; pwd)
ROOTPATH=$SCRIPTPATH/..
ulimit -HSn 65536 
CONFIGFILE=$ROOTPATH/config/jvm.properties

findStr()
{
    local target=$1
    local file=$2
    #echo target : $target
    #echo file : $file
    sed '/^\#/d' ${file} | grep ${target} | sed -e 's/ //g' |
        while read LINE
        do
            local KEY=`echo $LINE | cut -d "=" -f 1`
            local VALUE=`echo $LINE | cut -d "=" -f 2`
            [ ${KEY} = ${target} ] && {
                local UNKNOWN_NAME=`echo $VALUE | grep '\${.*}' -o | sed 's/\${//' | sed 's/}//'`
                if [ $UNKNOWN_NAME ];then
                    local UNKNOWN_VALUE=`findStr ${UNKNOWN_NAME} ${file}`
                    echo ${VALUE} | sed s/\$\{${UNKNOWN_NAME}\}/${UNKNOWN_VALUE}/
                else
                    echo $VALUE
                fi
                return 
            }
        done
    return
}

xms=$( findStr initial.mem.pool.size $CONFIGFILE )
xmx=$( findStr max.mem.pool.size $CONFIGFILE )
max_gc_pause_ms=$( findStr max.gc.pause.ms $CONFIGFILE )
gc_pause_interval_ms=$( findStr gc.pause.internal.ms $CONFIGFILE )
parallel_gc_threads=$( findStr parallel.gc.threads $CONFIGFILE )

java -noverify -server -Xms$xms -Xmx$xmx -XX:+UseG1GC -XX:MaxGCPauseMillis=$max_gc_pause_ms -XX:GCPauseIntervalMillis=$gc_pause_interval_ms -XX:ParallelGCThreads=$parallel_gc_threads -Xloggc:logs/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -cp "$ROOTPATH:$ROOTPATH/lib/*:$ROOTPATH/config" py.dd.Launcher  > /dev/null 2>&1 
