#!/bin/sh
#
# starts the Workflow server
#

@cdInstall@
#
# Read basic setup
#
. @etc@/startup.properties

#
# check whether the server might be already running
#
if [ -e $PID ] 
 then
  if [ -d /proc/$(cat $PID) ]
  then
   echo "A Workflow server instance may be already running with process id "$(cat $PID)
   echo "If this is not the case, delete the file $INST/$PID and re-run this script"
   exit 1
  fi
fi

#
# put all jars in lib/ on the classpath
#
CP=$(@cdRoot@find "$LIB" -name "*.jar" -exec printf ":{}" \;)

#
# go
#

CLASSPATH=$CP; export CLASSPATH

nohup $JAVA ${MEM} ${OPTS} ${DEFS} de.fzj.unicore.uas.UAS ${PARAM} WORKFLOW > $STARTLOG 2>&1  & echo $! > $PID

echo "Workflow engine starting."


