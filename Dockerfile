FROM oracle/graalvm-ce:20.2.0-java11

COPY ./target/scala-2.13/point.jar /app.jar

EXPOSE 8080

WORKDIR "/"

ENTRYPOINT exec java \
  -XX:InitialRAMPercentage=25 \
  -XX:MaxRAMPercentage=25 \
  -XX:MinRAMPercentage=25 \
  -XshowSettings:vm \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Dcom.sun.management.jmxremote=true \
  -Dcom.sun.management.jmxremote.local.only=false \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -jar /app.jar
