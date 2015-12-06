#!/bin/bash
#
# Author: Richard Lucas
#
# Starts an Omakase Worker instance.

CWD=$(cd "$(dirname "$0")"; pwd)
HOST="localhost"
PORT=8080
USER="admin"
PASSWORD="password"
FREQUENCY=10
JPORT=8778
DELAY=0

function show_usage() {
  echo -e "\nUSAGE:\n\t$(basename $0) [OPTION]..."
  echo -e "\n\t-h HOST the hostname of the Omakase instance the worker is serving, default is localhost."
  echo -e "\n\t-P PORT the port of the Omakase instance the worker is serving, default is 8080."
  echo -e "\n\t-u USER the username the worker should use to connect to the omakase instance, default is admin."
  echo -e "\n\t-p USER the username the worker should use to connect to the omakase instance, default is password."
  echo -e "\n\t-f FREQUENCY the frequency in seconds the worker should poll the omakase instance, default is 10."
  echo -e "\n\t-d DELAY the amount of time in seconds before the worker starts, the default is 0."
  echo -e "\n\t-j JOLOKIA_PORT the port Jolokia is registered to use, default is 8778."
  exit 1
}

while getopts h:P:u:p:f:d:j: opt
do
  case ${opt} in
    h) HOST="$OPTARG"
    ;;
    P) PORT="$OPTARG"
    ;;
    u) USER="$OPTARG"
    ;;
    p) PASSWORD="$OPTARG"
    ;;
    f) FREQUENCY="$OPTARG"
    ;;
    d) DELAY="$OPTARG"
    ;;
    j) JPORT="$OPTARG"
    ;;
    \?) show_usage
    ;;
  esac
done

pushd ${CWD}

CLASSPATH=
for i in ../lib/*jar; do CLASSPATH=${CLASSPATH}:${i};
done
echo "classpath: "${CLASSPATH}

JAVA_OPTS="
-javaagent:../jolokia/jolokia-jvm.jar=host=0.0.0.0,port=$JPORT,discoveryEnabled=false \
-Xms128m \
-Xmx256m \
-XX:MaxMetaspaceSize=256M \
-Djava.util.logging.manager=org.jboss.logmanager.LogManager \
-Dlogging.configuration=file://$CWD/../logging.properties \
-Domakase.url=http://$HOST:$PORT/ \
-Domakase.user=$USER
-Domakase.password=$PASSWORD
-Dtool.poll.frequency.in.secs=$FREQUENCY
$JAVA_OPTS"

#JAVA_OPTS="$JAVA_OPTS -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=44409"
#JAVA_OPTS="$JAVA_OPTS -agentpath:$JREBEL_HOME/lib/libjrebel64.dylib -Drebel.check_class_hash=true "

echo "Waiting "${DELAY}" before starting ..."
sleep ${DELAY}

exec java ${JAVA_OPTS} -cp ${CLASSPATH} org.projectomakase.omakase.worker.Worker
popd