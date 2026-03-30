PID_FILE=app.pid

if [ -f $PID_FILE ]; then
  PID=$(cat $PID_FILE)
  echo "Stopping app with PID $PID"
  kill $PID
  sleep 5
fi

mvn clean install -Dmaven.test.skip=true
nohup /opt/jdk21/bin/java -jar target/ai_platform-0.0.1-SNAPSHOT.jar > /dev/null &
echo $! > $PID_FILE