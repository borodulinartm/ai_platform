PID_FILE=app.pid

if [ -f $PID_FILE ]; then
  PID=$(cat $PID_FILE)
  echo "Stopping app with PID $PID"
  kill $PID
  sleep 5
fi

mvn clean install -Dmaven.test.skip=true
nohup /opt/jdk21/bin/java -Dspring.profiles.active=prod -jar ai_platform-1.0.0-SNAPSHOT.jar > /dev/null &
echo $! > $PID_FILE
