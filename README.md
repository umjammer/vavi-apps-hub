[![Release](https://jitpack.io/v/umjammer/vavi-apps-hub.svg)](https://jitpack.io/#umjammer/vavi-apps-hub)
[![Java CI](https://github.com/umjammer/vavi-apps-hub/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-apps-hub/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-apps-hub/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-apps-hub/actions/workflows/¥)
![Java](https://img.shields.io/badge/Java-17-b07219)

# vavi-apps-hub

<img src="https://github.com/umjammer/vavi-apps-hub/assets/493908/5efff428-15df-46bb-a7b0-929e31caf3c2" width="128" alt="hub logo" />

🌐 hub for plugins that controls a computer 

### plugins

 * remote notificator
 * remote trackpad (wip)
 * 🏅 gamepad binder (Minecraft, MuseScore3)
 * hand gesture recognizer (tbd)

## Install

 * [maven](https://jitpack.io/#umjammer/vavi-apps-hub)

## Usage

### server

```shell
 $ git clone https://github.com/umjammer/vavi-apps-hub
 $ mvn package
 $ open -a target/Hub/Hub.app
```

### client

#### notificator

```shell
 curl "http://your_destination:60080/notification/notify?message=your_message&title=your_title&from=your_source" 
```

#### remote trackpad

 * accsess `http://localhost:60080/`

#### gamepad binder

* let Minecraft or MuseScore3 most front

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
 * ~~hub for remote mouse input~~ -> this websocket server (done)
   * touchpad (wip)
 * ~~apple remote event?~~ -> this rest server (done)
 * ~~coexistence websocket and jersey on jetty~~ see [Main.java](src/main/java/vavi/apps/hub/Main.java)
 * gamepad configuration, dsl?, json?

---
<sub>image by <a href="https://www.silhouette-illust.com/illust/49214">silhouette illust</a></sub>