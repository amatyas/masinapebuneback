
#!/usr/bin/env bash
BASEDIR=$(dirname "$0")
echo "$BASEDIR"
cd "$BASEDIR"
java -jar visiologic-batch-api-1.0.jar --spring.profiles.active=prod -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=7124 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false

