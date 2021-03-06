#!/bin/sh
if [ -z "${DISTDEEP_HOME}" ]; then
  export DISTDEEP_HOME="$(cd ..; pwd)"
fi
#echo "DISTDEEP_HOME = $DISTDEEP_HOME"

if [ "$DISTDEEP_PORT" = "" ]; then
  DISTDEEP_PORT=2551
fi
#echo "DISTDEEP_PORT = $DISTDEEP_PORT"

if [ "$DISTDEEP_HOST" = "" ]; then
  DISTDEEP_HOST="$(wget http://ipecho.net/plain -O - -q ; echo)"
fi
#echo "DISTDEEP_HOST = $DISTDEEP_HOST"

CLASS="com.doohh.akkaClustering.worker.WorkerMain"
java -Xmx5g -cp $DISTDEEP_HOME/jars/akkaClustering-0.0.1-allinone.jar $CLASS -h $DISTDEEP_HOST
echo $!> worker.pid