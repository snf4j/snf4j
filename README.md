[![MIT License](http://img.shields.io/badge/license-MIT-green.svg) ](https://github.com/snf4j/snf4j/blob/master/LICENSE) ![Build project](https://github.com/snf4j/snf4j/actions/workflows/ci.yml/badge.svg) [![Coverage Status](https://img.shields.io/codecov/c/github/snf4j/snf4j.svg)](https://codecov.io/github/snf4j/snf4j)

# Introduction
The Simple Network Framework for Java (SNF4J) is an asynchronous event-driven network application framework for quick and easy development of network applications.

The SNF4J serves as a simple, lightweight and independent network application framework helping in developing high performance and scalable network applications. It provides an asynchronous API via the Java NIO to handle various transports like TCP/IP, UDP/IP and SCTP/IP.

The framework is designed to be simple in use and moderately easy to understand. Application developers that like to keep their ear to the ground will not be overwhelmed by a tone of source code. It is oriented towards delivering core networking functionality as transferring data between two or more communicating network applications in a simple but effective way. To provide privacy and integrity of transferred data it provides support for the SSL/TLS/DTLS protocols.

## Design

* Unified API for transport layer types: TCP, UDP and SCTP
* Event-driven API for developing asynchronous network applications
* Customizable protocol engines for transport layer types: TCP and UDP	
* Utilities for development of UDP server applications
* Simple API for customizable encoder-decoder pipelines
* Provide ease of programing multi-thread applications in a single-thread way
* Building pipelines of sessions sharing the same socket channel

## Performance and Scalability

* Support for minimized or even zero unnecessary memory copying
* Support for low memory utilization in multi-session applications
* Lower latency
* zlib and gzip compression
* Customizable selector loop pooling
* Customizable byte buffer allocators

## Thread model

* All handler's events (i.e. read, event, timer, exception, incident) are fired from the thread that performs I/O for the channel (I/O thread) 
* All session's methods are thread-safe and can be called from any thread including the I/O thread and non-I/O threads 
* Any handler's event triggered as a side effect of calling a session's method are fired from the I/O thread 
* Codec's code is always processed in the I/O thread 
* Engine's code is always processed in the I/O thread 

## Supported Protocols

* HTTP Web Proxy Connect Protocol
* SOCKS Protocols
* WebSocket Protocol
* Toolkit for TLS 1.3 Protocol (parsers/formatters, handshake engine, record layer utils and complete TLS engine)
 
## Security

* Complete support for SSL/TLS/DTLS protocols
* Secure Web Proxy Connect

## Integration

* No additional dependencies, JDK 8 or JDK 9 (for DTLS) is enough
* Fully customizable logging (SLF4J, Log4j 2 already here)
* Customizable thread factories
* Customizable session timers 
* Customizable packet retransmission models during DTLS handshakes

# Compiling

You need Apache maven 3.8 or above , Java 8 or above

    mvn install

# Links

* [Web Site](https://snf4j.org/)
* [@snf4jorg](https://twitter.com/snf4jorg)
