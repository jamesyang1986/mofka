#!/bin/sh

cur_path=`dirname $0;pwd`
CLASS_PATH=''

echo "current work path is $cur_path..\n"

LIB_PATH=$cur_path/lib
for i in $LIB_PATH/*.jar;
do
  CLASS_PATH=$CLASS_PATH:$i
done

CLASS_PATH=$cur_path/mofka.conf:$cur_path/log4j.properties:$CLASS_PATH:
echo $CLASS_PATH

if [[ "$JAVA_HOME" != "" ]];then
  JAVA=$JAVA_HOME/bin/java
else
  JAVA=java
fi

MAIN_CLASS="com.kole.mofka.bootstrap.Bootstrap"

TIME=`date +"%Y%m%d%H%M%S"`

echo "cur time is $TIME"
JVM_ARG="-xmx=3g -xms=3g  -XX:+PrintGCDetails -Xloggc:$cur_path/logs/gc$TIME.log"

JAVA -cp $CLASS_PATH $JVM_ARG $MAIN_CLASS $@



