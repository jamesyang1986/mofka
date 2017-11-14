#!/bin/sh

MOFKA_ROOT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}")"/../  && pwd)"
CLASS_PATH=""

echo "mofka current working dir is: $MOFKA_ROOT_DIR..\n"

LIB_PATH=$MOFKA_ROOT_DIR/lib
for i in $LIB_PATH/*.jar;
do
  CLASS_PATH=$CLASS_PATH:$i
done

CLASS_PATH=$MOFKA_ROOT_DIR/conf/mofka.conf:$MOFKA_ROOT_DIR/conf/log4j.properties:$CLASS_PATH:
echo $CLASS_PATH

echo "the JAVA_HOME is $JAVA_HOME...\n"

if [[ "x$JAVA_HOME" != "x" ]];then
  JAVA=$JAVA_HOME/bin/java
else
  JAVA=java
fi

MAIN_CLASS="com.kole.mofka.bootstrap.Bootstrap"

TIME=`date +"%Y%m%d%H%M%S"`

echo "cur time is $TIME"
JVM_ARG="-Xmx3g -Xms3g  -XX:+PrintGCDetails -Xloggc:$MOFKA_ROOT_DIR/log/gc_$TIME.log \
         -XX:+PrintGCDateStamps -verbose:gc"

JVM_ARG="${JVM_ARG} \
    -XX:+ParallelRefProcEnabled \
    -XX:-UseAdaptiveSizePolicy \
    -XX:MaxTenuringThreshold=10 \
    -XX:SurvivorRatio=8 -XX:NewRatio=2
    -XX:+UseParNewGC \
    -XX:+UseConcMarkSweepGC \
    -XX:CMSInitiatingOccupancyFraction=50 -XX:+UseCMSInitiatingOccupancyOnly \
    -XX:+UseCMSCompactAtFullCollection -XX:CMSFullGCsBeforeCompaction=16"

MOFKA_PID_FILE=$MOFKA_ROOT_DIR/mofka.pid

case $1 in
start)
    shift
    echo  "starting the mofka  server....\n"
    if [[ -f "$MOFKA_PID_FILE" ]];
    then
        echo "the mofka server is started, the pid is "$(cat "$MOFKA_PID_FILE")", please check it. "
        if  kill -0 "$(cat "$MOFKA_PID_FILE")"  2>&1  > /dev/null;
        then
            exit 0
        fi
    else
        mkdir -p "$MOFKA_PID_FILE"
        nohup JAVA -Dmofka.log.dir=$MOFKA_ROOT_DIR/log -cp $CLASS_PATH $JVM_ARG $MAIN_CLASS $@  2>&1  &

        if [ $? -eq 0 ];
        then
          if /bin/echo  $! > "$MOFKA_PID_FILE"
           then
             sleep 1
             echo "success echo PID"
           else
             echo "fail to echo PID"
           fi
        else
            echo "fail to start the mofka server... please check it.."
            exit 0
        fi
    fi
    ;;
stop)
    shift
    echo "stoping the mofka server for pid:$(cat "$MOFKA_PID_FILE") "
    kill -9 "$( cat "$MOFKA_PID_FILE")"
    rm -rf $MOFKA_PID_FILE
    echo "MOFKA SERVER STOPED"
    ;;
restart)
    shift
    echo "restart the  mofka server....\n"
    "$0" stop ${@}
    sleep 3
    "$0" start ${@}
    ;;
*)
    echo "Usage: $0 {start|stop|restart|status}" >&2
esac






