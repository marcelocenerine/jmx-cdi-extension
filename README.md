[![Build Status](https://travis-ci.org/marcelocenerine/jmx-cdi-extension.svg)](https://travis-ci.org/marcelocenerine/jmx-cdi-extension)  [![Coverage Status](https://coveralls.io/repos/marcelocenerine/jmx-cdi-extension/badge.svg?branch=master&service=github&)](https://coveralls.io/github/marcelocenerine/jmx-cdi-extension?branch=master)
# jmx-cdi-extension
Simple CDI extension for JMX.

# How to use:

1) - Add the dependency to your project:

- Gradle:
```groovy
    dependencies {
        compile 'com.cenerino.jmxext:jmx-cdi-ext:1.0.0'
    }
```

- Maven:
```xml
  <dependency>
    <groupId>com.cenerino.jmxext</groupId>
    <artifactId>jmx-cdi-ext</artifactId>
    <version>1.0.0</version>
  </dependency>
```

2) - Decorate your beans with the @MBean annotation (make sure they are also decorated with @ApplicationScoped or @Singleton, otherwise the result will not be what you may expect):

```java
    @ApplicationScoped
    @MBean(description = "Session statistics")
    public class SessionStatistics {
    
        private AtomicInteger sessionsCount = new AtomicInteger();
    
        public void incrementActiveSessions() {
            sessionsCount.incrementAndGet();
        }
    
        public void decrementActiveSessions() {
            sessionsCount.decrementAndGet();
        }
    
        public AtomicInteger getSessionsCount() {
            return sessionsCount;
        }
    }
```

3) - Open JConsole (or any other JVM monitoring tool) to see what is happening inside your application :)
