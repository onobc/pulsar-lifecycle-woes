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
* Run `./gradlew clean build -x test`
* Run `java -jar build/libs/pulsar-0.0.1-SNAPSHOT.jar`
* Enter `CTRL-C`

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

### Update 2023-10-29 23:07 UTC-5

Here is the **DIFFERENCE**, when running inside **IDEA** or via **bootRun**, the classloader hierarchy at the point of failure starting w/ `Thread.currentThread().getContextClassLoader()` is:
```
*** CURRENT THREAD: Thread[SpringApplicationShutdownHook,5,main]
*** LOADER_1: jdk.internal.loader.ClassLoaders$AppClassLoader@251a69d7
*** LOADER_2: jdk.internal.loader.ClassLoaders$PlatformClassLoader@4722110
*** LOADER_3: null
*** LOADED w/ 1
```
and the class is successfully loaded w/ `AppClassLoader`.

**HOWEVER**, when running via **java -jar**, the classloader hierarchy is:
```
*** CURRENT THREAD: Thread[SpringApplicationShutdownHook,5,main]
*** LOADER_1: org.springframework.boot.loader.launch.LaunchedClassLoader@568db2f2
*** LOADER_2: jdk.internal.loader.ClassLoaders$AppClassLoader@5ffd2b27
*** LOADER_3: jdk.internal.loader.ClassLoaders$PlatformClassLoader@611a990b
```
and the class is not loadable by any of them.

Furthermore, the LaunchedClassLoader has the following URLs, and closeables (not that all but the Tomcat `NestedJarFile` is closed).

```
*** LOADER_1: org.springframework.boot.loader.launch.LaunchedClassLoader@568db2f2
URLs = {
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/classes/!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-boot-docker-compose-3.2.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-boot-loader-tools-3.2.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-pulsar-1.0.0-SNAPSHOT.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-webmvc-6.1.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-web-6.1.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/jackson-module-parameter-names-2.15.3.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/jackson-datatype-jdk8-2.15.3.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/jackson-datatype-jsr310-2.15.3.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/jackson-databind-2.15.3.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-boot-autoconfigure-3.2.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-boot-3.2.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/commons-compress-1.23.0.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-context-6.1.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-messaging-6.1.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-tx-6.1.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-aop-6.1.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-beans-6.1.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-expression-6.1.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-core-6.1.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/jakarta.annotation-api-2.1.1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/snakeyaml-2.2.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-pulsar-cache-provider-caffeine-1.0.0-SNAPSHOT.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/jackson-core-2.15.3.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/pulsar-client-all-3.1.0.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/pulsar-package-core-3.1.0.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/guava-32.1.1-jre.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/jsr305-3.0.2.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/micrometer-observation-1.12.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-retry-2.0.4.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-pulsar-cache-provider-1.0.0-SNAPSHOT.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/tomcat-embed-websocket-10.1.15.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/tomcat-embed-core-10.1.15.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/tomcat-embed-el-10.1.15.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/jackson-annotations-2.15.3.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-jcl-6.1.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/logback-classic-1.4.11.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/log4j-to-slf4j-2.21.0.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/jul-to-slf4j-2.0.9.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/micrometer-commons-1.12.0-RC1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/pulsar-client-admin-api-3.1.0.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/pulsar-client-api-3.1.0.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/bouncy-castle-bc-3.1.0-pkg.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/bcpkix-jdk18on-1.75.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/bcutil-jdk18on-1.75.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/bcprov-jdk18on-1.75.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/bcprov-ext-jdk18on-1.75.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/slf4j-api-2.0.9.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/jcip-annotations-1.0.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/jakarta.ws.rs-api-3.1.0.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/jakarta.xml.bind-api-4.0.1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/jakarta.activation-api-2.1.2.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/logback-core-1.4.11.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/log4j-api-2.21.0.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/commons-lang3-3.13.0.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/failureaccess-1.0.1.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/checker-qual-3.33.0.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/error_prone_annotations-2.18.0.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/j2objc-annotations-2.8.jar!/ ]
[ jar:nested:/Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar/!BOOT-INF/lib/spring-boot-jarmode-layertools-3.2.0-RC1.jar!/ ]}
CLOSEABLES = {
[ /Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/spring-core-6.1.0-RC1.jar / closed? true ]
[ /Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/spring-webmvc-6.1.0-RC1.jar / closed? true ]
[ /Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/spring-tx-6.1.0-RC1.jar / closed? true ]
[ /Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/spring-beans-6.1.0-RC1.jar / closed? true ]
[ /Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/spring-context-6.1.0-RC1.jar / closed? true ]
[ /Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/spring-pulsar-1.0.0-SNAPSHOT.jar / closed? true ]
[ /Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/tomcat-embed-core-10.1.15.jar / closed? false ]
[ /Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/spring-boot-autoconfigure-3.2.0-RC1.jar / closed? true ]
[ /Users/cbono/Desktop/ps2023/pulsar-lifecycle-woes/build/libs/pulsar-0.0.1-SNAPSHOT.jar!/BOOT-INF/classes/ / closed? true ]
}
```
