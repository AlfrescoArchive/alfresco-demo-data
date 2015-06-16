Alfresco Trials Data Bootstrap
---

This module loads some demo data into Alfresco at boot time.

Test local
---
You need to use Java 8 to run this project:
```
export JAVA_HOME="`/usr/libexec/java_home -v '1.8*'`"
```

Deploy/Release
---
mvn release:prepare release:perform
