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

2) - Annotate your beans with @MBean:

```java
    import javax.enterprise.context.ApplicationScoped;
    
    import com.cenerino.jmxext.MBean;
    
    @ApplicationScoped
    @MBean(description = "Session statistics")
    public class SessionManager {
    
        private AtomicInteger activeSessionsCount;
    
        public void incrementActiveSessions() {
            activeSessionsCount.incrementAndGet();
        }
    
        public void decrementActiveSessions() {
            activeSessionsCount.decrementAndGet();
        }
    
        public AtomicInteger getActiveSessionsCount() {
            return activeSessionsCount;
        }
    }
```
