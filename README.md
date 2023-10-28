# Sample App With Lifecycle Woes

## Problem Summary
Weird class loading issues on shutdown when it fails.

**When running in IDEA:**
* STOP button works
* `kill <pid>` works

**When running in ./gradlew bootRun:**
* STOP button works
* `kill <pid>` works

**When running in terminal (java -jar):**
* `CTRL-C` fails
* `kill <pid>` fails

## Steps to Reproduce

### IDEA (passes)
* Build app in IDEA
* Click "Run" button on `PulsarApplication` class
* Click "Stop" button on left nav of run "Run" panel

**Result:** Shutdown is fine

### Terminal (fails)
* Open terminal in project directory
* Run `docker-compose -f hardcoded-compose.yaml up` 
  * (auto support does not work w/ `java -jar` ??)
* Open another terminal in project directory
* Run `./gradlew clean build -x test`
* Run `java -jar build/libs/pulsar-0.0.1-SNAPSHOT.jar`
* Enter `CTRL-C`
* Run `docker-compose -f hardcoded-compose.yaml down`
**Result:** Fails to shutdown w/ the following:

````
java.lang.NoClassDefFoundError: org/apache/pulsar/common/api/proto/CommandCloseConsumer
at org.apache.pulsar.common.api.proto.BaseCommand.setCloseConsumer(BaseCommand.java:707) ~[pulsar-client-all-3.1.0.jar!/:3.1.0]
at org.apache.pulsar.common.protocol.Commands.newCloseConsumer(Commands.java:741) ~[pulsar-client-all-3.1.0.jar!/:3.1.0]
at org.apache.pulsar.client.impl.ConsumerImpl.closeAsync(ConsumerImpl.java:1050) ~[pulsar-client-all-3.1.0.jar!/:3.1.0]
at org.apache.pulsar.client.impl.ConsumerBase.close(ConsumerBase.java:730) ~[pulsar-client-all-3.1.0.jar!/:3.1.0]
at org.springframework.pulsar.listener.DefaultPulsarMessageListenerContainer.doStop(DefaultPulsarMessageListenerContainer.java:152) ~[spring-pulsar-1.0.0-RC1.jar!/:1.0.0-RC1]
at org.springframework.pulsar.listener.AbstractPulsarMessageListenerContainer.stop(AbstractPulsarMessageListenerContainer.java:116) ~[spring-pulsar-1.0.0-RC1.jar!/:1.0.0-RC1]
at org.springframework.pulsar.listener.ConcurrentPulsarMessageListenerContainer.doStop(ConcurrentPulsarMessageListenerContainer.java:141) ~[spring-pulsar-1.0.0-RC1.jar!/:1.0.0-RC1]
at org.springframework.pulsar.listener.AbstractPulsarMessageListenerContainer.stop(AbstractPulsarMessageListenerContainer.java:116) ~[spring-pulsar-1.0.0-RC1.jar!/:1.0.0-RC1]
at org.springframework.context.SmartLifecycle.stop(SmartLifecycle.java:117) ~[spring-context-6.1.0-RC1.jar!/:6.1.0-RC1]
at org.springframework.pulsar.config.GenericListenerEndpointRegistry.stop(GenericListenerEndpointRegistry.java:208) ~[spring-pulsar-1.0.0-RC1.jar!/:1.0.0-RC1]
at org.springframework.context.support.DefaultLifecycleProcessor.doStop(DefaultLifecycleProcessor.java:332) ~[spring-context-6.1.0-RC1.jar!/:6.1.0-RC1]
at org.springframework.context.support.DefaultLifecycleProcessor$LifecycleGroup.stop(DefaultLifecycleProcessor.java:471) ~[spring-context-6.1.0-RC1.jar!/:6.1.0-RC1]
at java.base/java.lang.Iterable.forEach(Iterable.java:75) ~[na:na]
at org.springframework.context.support.DefaultLifecycleProcessor.stopBeans(DefaultLifecycleProcessor.java:301) ~[spring-context-6.1.0-RC1.jar!/:6.1.0-RC1]
at org.springframework.context.support.DefaultLifecycleProcessor.onClose(DefaultLifecycleProcessor.java:202) ~[spring-context-6.1.0-RC1.jar!/:6.1.0-RC1]
at org.springframework.context.support.AbstractApplicationContext.doClose(AbstractApplicationContext.java:1079) ~[spring-context-6.1.0-RC1.jar!/:6.1.0-RC1]
at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.doClose(ServletWebServerApplicationContext.java:174) ~[spring-boot-3.2.0-RC1.jar!/:3.2.0-RC1]
at org.springframework.context.support.AbstractApplicationContext.close(AbstractApplicationContext.java:1038) ~[spring-context-6.1.0-RC1.jar!/:6.1.0-RC1]
at org.springframework.boot.SpringApplicationShutdownHook.closeAndWait(SpringApplicationShutdownHook.java:145) ~[spring-boot-3.2.0-RC1.jar!/:3.2.0-RC1]
at java.base/java.lang.Iterable.forEach(Iterable.java:75) ~[na:na]
at org.springframework.boot.SpringApplicationShutdownHook.run(SpringApplicationShutdownHook.java:114) ~[spring-boot-3.2.0-RC1.jar!/:3.2.0-RC1]
at java.base/java.lang.Thread.run(Thread.java:833) ~[na:na]
Caused by: java.lang.ClassNotFoundException: org.apache.pulsar.common.api.proto.CommandCloseConsumer
at java.base/java.net.URLClassLoader.findClass(URLClassLoader.java:445) ~[na:na]
at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:587) ~[na:na]
at org.springframework.boot.loader.net.protocol.jar.JarUrlClassLoader.loadClass(JarUrlClassLoader.java:104) ~[pulsar-0.0.1-SNAPSHOT.jar:0.0.1-SNAPSHOT]
at org.springframework.boot.loader.launch.LaunchedClassLoader.loadClass(LaunchedClassLoader.java:91) ~[pulsar-0.0.1-SNAPSHOT.jar:0.0.1-SNAPSHOT]
at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:520) ~[na:na]
... 22 common frames omitted
````
