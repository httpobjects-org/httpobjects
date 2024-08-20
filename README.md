# [httpobjects]
a watertight API for speaking http(s)

## what is httpobjects?

- a watertight API for implementing JVM code that speaks http(s)
- implementations of that API for various popular http servers & containers
- a set of companion libraries for interoperating with various popular JVM libraries

## what are the design goals?

- immutable - everything expressed as pure functions
- watertight - strives to be a non-leaky abstraction of the http(s) protocol
- focused - do one thing, do it well
- performant - suitable for stream processing of very large requests
- portable - not tied to any other API, works well with popular JVM languages
- stable - suitable as a foundation for mission-critical components
- non-blocking - suitable for highly concurrent applications
- self-documenting - the API documents itself
- self-contained - core library has no dependencies, works on JDK8 & above
- compatible - interoperates well with popular JVM languages

## how do I get it?

- Development version: `git clone && cd httpobjects && mvn install`
- [Release versions are available in Maven Central](https://central.sonatype.com/artifact/org.httpobjects/httpobjects)

```xml
<!-- You'll need the core API -->
<dependency>
    <groupid>org.httpobjects</groupid>
    <artifactid>httpobjects</artifactid>
    <version>0.54.0</version>
</dependency>

<!-- Plus one of the following implementations -->

<dependency>
    <groupid>org.httpobjects.servlet</groupid>
    <artifactid>httpobjects-servlet</artifactid>
    <version>0.54.0</version>
</dependency>

<dependency>
    <groupid>org.httpobjects.jetty</groupid>
    <artifactid>httpobjects-jetty-9-and-10</artifactid>
    <version>0.54.0</version>
</dependency>

<dependency>
    <groupid>org.httpobjects.jetty</groupid>
    <artifactid>httpobjects-jetty-12</artifactid>
    <version>0.54.0</version>
</dependency>

<dependency>
    <groupid>org.httpobjects.netty</groupid>
    <artifactid>httpobjects-netty-4</artifactid>
    <version>0.54.0</version>
</dependency>
```

## can I use my servlet/J2EE container?

Yes. First, include the servlet implementation

```xml
<dependency>
    <groupid>org.httpobjects.servlet</groupid>
    <artifactid>httpobjects-servlet</artifactid>
    <version>0.54.0</version>
</dependency>
```

Then:

- Create a subclass of org.httpobjects.servlet.ServletFilter that references your httpobjects
- Register that class as a filter in your web.xml

## can I use embedded jetty?

Yes. Choose one of the following:

```xml
<dependency>
    <groupid>org.httpobjects.jetty</groupid>
    <artifactid>httpobjects-jetty-9-and-10</artifactid>
    <version>0.54.0</version>
</dependency>

<dependency>
    <groupid>org.httpobjects.jetty</groupid>
    <artifactid>httpobjects-jetty-12</artifactid>
    <version>0.54.0</version>
</dependency>
```

And then launch the server:

```java
/**
 * Java
 */
import org.httpobjects.*;
import org.httpobjects.jetty.HttpObjectsJettyHandler;

public class Example {
    public static void main(String[] args){

        HttpObject speaker = new HttpObject("/speak"){
            @Override
            public Response get(Request req) {
                return OK(Html("Hello World"));
            }
        };

        HttpObjectsJettyHandler.launchServer(8080, speaker);
    }
}
```

```scala
/**
  * Scala
  */
import org.httpobjects.jetty.HttpObjectsJettyHandler
import org.httpobjects._
import org.httpobjects.DSL._

object Example {
  def main(args: Array[String]) {
    HttpObjectsJettyHandler.launchServer(8080, 
      new HttpObject("/speak"){
        override def get(request:Request) = OK(Html("Hello World"))
      }
    )
  }
}
```

## can I use netty?

Yes. There is a sample netty implementation that you can either use as-is or modify to fit into your existing netty.

```xml
<!-- The netty implementation -->

<dependency>
    <groupid>org.httpobjects.netty</groupid>
    <artifactid>httpobjects-netty-4</artifactid>
    <version>0.54.0</version>
</dependency>
```

```java
/**
 * Java
 */
import org.httpobjects.*;
import org.httpobjects.netty.HttpobjectsNettySupport;

public class Example {
    public static void main(String[] args){

        HttpObject speaker = new HttpObject("/speak"){
            @Override
            public Response get(Request req) {
                return OK(Html("Hello World"));
            }
        };

        HttpobjectsNettySupport.serve(port, Arrays.asList(speaker));
    }
}
```

## who's responsible for this?
[![Commission Junction](https://httpobjects.org/cj-logo.png)](https://engineering.cj.com/blog)

httpobjects was born as a thought experiment in response to needs at CJ. CJ has also been an early adopter, using httpobjects in a variety of internal projects and generously providing developer time to address bugs and limitations.
