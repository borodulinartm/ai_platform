mvn clean install -Dmaven.test.skip=true
nohup /opt/jdk21/bin/java -jar target/ai_platform-0.0.1-SNAPSHOT.jar > /dev/null &