#!/bin/bash
cd /usr1/ai-platform

PID_FILE=app.pid

if [ -f $PID_FILE ]; then
  PID=$(cat $PID_FILE)
  echo "Stopping app with PID $PID"
  kill $PID 2>/dev/null
  sleep 5
fi

PORT_PID=$(lsof -ti:8888 2>/dev/null)
if [ -n "$PORT_PID" ]; then
  echo "Killing process on port 8888: $PORT_PID"
  kill $PORT_PID 2>/dev/null
  sleep 2
fi

nohup /opt/jdk21/bin/java -Dspring.profiles.active=prod -jar ai_platform-1.0.0.jar > /usr1/logs/ai_platform/app.log 2>&1 &
echo $! > $PID_FILE
echo "Started app with PID $!"
