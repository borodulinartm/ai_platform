export BASE_PATH_CLOUD=/tmp/cloud
mvn clean install -Dmaven.test.skip=true
java -jar target/ai_platform-0.0.1-SNAPSHOT.jar