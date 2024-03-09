[![Release](https://jitpack.io/v/umjammer/vavi-apps-hub.svg)](https://jitpack.io/#umjammer/vavi-apps-hub)
[![Java CI](https://github.com/umjammer/vavi-apps-hub/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-apps-hub/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-apps-hub/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-apps-hub/actions/workflows/¥)
![Java](https://img.shields.io/badge/Java-17-b07219)

# vavi-apps-hub

<img src="https://github.com/umjammer/vavi-apps-hub/assets/493908/5efff428-15df-46bb-a7b0-929e31caf3c2" width="128" alt="hub logo" /><sub><a href="https://www.silhouette-illust.com/illust/49214">© silhouette illust</a></sub>

My Hub ❤

## Install

 1. `$ git clone https://github.com/umjammer/vavi-apps-hub`
 2. `$ mvn package`
 3. `$ cp target/Hub/Hub.app` ~/Application 

## References

 * desktop notification
   * https://github.com/sshtools/two-slices
 * REST
   * server
     * [vavi-speech-rpc](https://github.com/umjammer/vavi-speech-rpc) 
 * WebSocket
   * server
     * [vavi-apps-webhook](https://github.com/umjammer/vavi-apps-webhook)
 * WebTransport
   * [netty](https://netty.io/)
   * jetty

## TODO

 * ~~hub for notification center over inet~~
 * hub for remote mouse input (wip) -> this websocket server
 * ~~apple remote event?~~ -> this rest server
 * ~~coexistence websocket and jersey on jetty~~ see [Main.java](src/main/java/vavi/apps/hub/Main.java) 