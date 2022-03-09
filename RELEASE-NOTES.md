#### 1.9.0
 - 2022-03-09 - [changes](https://github.com/snf4j/snf4j/compare/v1.8.0...v1.9.0)
 - Support for connection through SOCKS proxy [(#81)](https://github.com/snf4j/snf4j/pull/81)
 - Added proxy basic HTTP authentication scheme [(#82)](https://github.com/snf4j/snf4j/pull/82)
 - Added builders for SSLContext and SSLEngine [(#83)](https://github.com/snf4j/snf4j/pull/83)

#### 1.8.0
 - 2021-11-01 - [changes](https://github.com/snf4j/snf4j/compare/v1.7.0...v1.8.0)
 - Migrated from Travis-CI to GitHub Action [(#67)](https://github.com/snf4j/snf4j/pull/67)
 - Generic inbound type in IBaseDecoder [(#68)](https://github.com/snf4j/snf4j/pull/68)
 - Improved output splitting produced by ZlibDecoder [(#70)](https://github.com/snf4j/snf4j/pull/70)
 - Controlling session closing by type of thrown exception [(#72)](https://github.com/snf4j/snf4j/pull/72)
 - Added direct task execution from session object [(#75)](https://github.com/snf4j/snf4j/pull/75)
 - Added support for pipeline of sessions operating on single connection [(#76)](https://github.com/snf4j/snf4j/pull/76)
 - Added support for connection through HTTP proxy [(#77)](https://github.com/snf4j/snf4j/pull/77)
 - Support for WebSocket protocol [(#78)](https://github.com/snf4j/snf4j/pull/78)

#### 1.7.0
 - 2021-03-20 - [changes](https://github.com/snf4j/snf4j/compare/v1.6.0...v1.7.0)
 - Added channel context for selector loop genericity [(#55)](https://github.com/snf4j/snf4j/pull/55)
 - Added support for SCTP protocol [(#60)](https://github.com/snf4j/snf4j/pull/60)
 - Fixed #56: Encoding does not release buffers when the last encoder has byte[] as the inbound type [(#61)](https://github.com/snf4j/snf4j/pull/61)
 - Fixed #57: Future objects returned by write/send methods of datagram-oriented sessions may signal completion too quickly [(#61)](https://github.com/snf4j/snf4j/pull/61)
 - Fixed #62: Adding/Removal of an event-driven codec to/from a codec pipeline is not signaled in open sessions [(#63)](https://github.com/snf4j/snf4j/pull/63)
 
#### 1.6.0
 - 2020-12-30 - [changes](https://github.com/snf4j/snf4j/compare/v1.5.0...v1.6.0)
 - Improved handling of empty buffers in engine handlers [(#48)](https://github.com/snf4j/snf4j/pull/48)
 - Added thread-local caching allocator [(#51)](https://github.com/snf4j/snf4j/pull/51)
 - Improved SSL/TLS max buffer size configuration [(#52)](https://github.com/snf4j/snf4j/pull/52)
 - Access to engine's internal session from engine-driven sessions [(#53)](https://github.com/snf4j/snf4j/pull/53)

#### 1.5.0
 - 2020-12-09 - [changes](https://github.com/snf4j/snf4j/compare/v1.4.0...v1.5.0)
 - Added ZLIB and GZIP codecs [(#41)](https://github.com/snf4j/snf4j/pull/41)
 - Performance improvements [(#43)](https://github.com/snf4j/snf4j/pull/43)

#### 1.4.0
 - 2020-08-02 - [changes](https://github.com/snf4j/snf4j/compare/v1.3.0...v1.4.0)
 - Added datagram server handler [(#34)](https://github.com/snf4j/snf4j/pull/34)
 - Added support for session timer [(#35)](https://github.com/snf4j/snf4j/pull/35)
 - Improved closing in session events [(#36)](https://github.com/snf4j/snf4j/pull/36)
 - Codec improvements for datagram server sessions [(#37)](https://github.com/snf4j/snf4j/pull/37)
 - Support for DTLS [(#38)](https://github.com/snf4j/snf4j/pull/38)
 - Added DTLS server/client example [(#39)](https://github.com/snf4j/snf4j/pull/39)

#### 1.3.0
 - 2019-12-14 - [changes](https://github.com/snf4j/snf4j/compare/v1.2.0...v1.3.0)
 - Added support for protocol engines [(#23)](https://github.com/snf4j/snf4j/pull/23)
 - Added support for codec executors [(#27)](https://github.com/snf4j/snf4j/pull/27)
 - Execution of tasks in selector loop's thread [(#28)](https://github.com/snf4j/snf4j/pull/28)
 - Logging enhancements [(#30)](https://github.com/snf4j/snf4j/pull/30)
 - Added compound decoder/encoder [(#31)](https://github.com/snf4j/snf4j/pull/31)
 - Support for jdk11 (TLS 1.3) [(#32)](https://github.com/snf4j/snf4j/pull/32)
 
#### 1.2.0
 - 2019-02-27 - [changes](https://github.com/snf4j/snf4j/compare/v1.1.0...v1.2.0)
 - Added logger binder for Log4j 2 API [(#21)](https://github.com/snf4j/snf4j/pull/21)
 - Fixes #19: Fixed session gentle closing [(#20)](https://github.com/snf4j/snf4j/pull/20)
 
 #### 1.1.0
 - 2019-02-10 
 - Initial publishing
