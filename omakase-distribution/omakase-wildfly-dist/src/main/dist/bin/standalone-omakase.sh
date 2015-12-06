#!/bin/bash

if [[ -z "${IPADDR}" ]]
then
IPADDR=$(ip a s | sed -ne '/127.0.0.1/!{s/^[ \t]*inet[ \t]*\([0-9.]\+\)\/.*$/\1/p}')
fi

export JAVA_OPTS="
-Xms256m \
-Xmx512m \
-XX:MaxMetaspaceSize=512M \
-Djboss.bind.address=$IPADDR \
-Djboss.bind.address.management=$IPADDR \
-Djboss.node.name=server-$IPADDR \
$JAVA_OPTS"

if [[ ! -z "${OMAKASE_DB_ADDRESS}" ]]
then
export JAVA_OPTS="-Domakase.db.address=$OMAKASE_DB_ADDRESS $JAVA_OPTS"
fi

if [[ ! -z "${OMAKASE_ACTIVEMQ_ADDRESS}" ]]
then
export JAVA_OPTS="-Domakase.activemq.address=$OMAKASE_ACTIVEMQ_ADDRESS $JAVA_OPTS"
fi

if [[ ! -z "${S3_ACCESS_KEY}" ]] && [[ ! -z "${S3_SECRET_KEY}" ]] && [[ ! -z "${S3_BUCKET}" ]]
then
export JAVA_OPTS="
-Djboss.jgroups.s3_ping.access_key=$S3_ACCESS_KEY \
-Djboss.jgroups.s3_ping.secret_access_key=$S3_SECRET_KEY \
-Djboss.jgroups.s3_ping.bucket=$S3_BUCKET
$JAVA_OPTS"
fi

if [[ ! -z "${FORCE_IPV4}" ]]
then
export JAVA_OPTS="-Djava.net.preferIPv4Stack=$FORCE_IPV4 $JAVA_OPTS"
fi

if [[ ! -z "${OMAKASE_QUEUE_PROVIDER}" ]]
then
export JAVA_OPTS="
-Domakase.queue.provider=$OMAKASE_QUEUE_PROVIDER \
$JAVA_OPTS"
fi

if [[ ! -z "${SQS_ACCESS_KEY}" ]] && [[ ! -z "${SQS_SECRET_KEY}" ]]
then
export JAVA_OPTS="
-Domakase.sqs.access.key=$SQS_ACCESS_KEY \
-Domakase.sqs.secret.key=$SQS_SECRET_KEY \
$JAVA_OPTS"
fi

if [[ ! -z "${SQS_REGION}" ]]
then
export JAVA_OPTS="
-Domakase.sqs.region=$SQS_REGION
$JAVA_OPTS"
fi

if [[ ! -z "${JREBEL_HOME}" ]]
then
export JAVA_OPTS="
-agentpath:$JREBEL_HOME/lib/libjrebel64.dylib \
-Drebel.check_class_hash=true \
$JAVA_OPTS"
fi

if [[ ! -z "${PROFILE}" ]]
then
export JAVA_OPTS="-agentlib:hprof=cpu=samples,depth=200,interval=3,lineno=n,thread=n,file=output.hprof $JAVA_OPTS"
fi

`dirname $0`/standalone.sh $@