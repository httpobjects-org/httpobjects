## What's this?

This is an incubating extension to httpobjects that aims to facilitate the [websockets protocol](https://datatracker.ietf.org/doc/html/rfc6455) with the same design philosophy and approach used for the http protocol

## What's the status?

It works, and is used on real, stable projects, but ... 
  - the protocol representation is incomplete in spots
  - it probably doesn't reflect [the spec](https://datatracker.ietf.org/doc/html/rfc6455) as well as it should
  - the only implementation support is for netty-4
  - it may change in backwards-incompatible ways pretty frequently until it passes through the "incubating" stage.

## Blockers for leaving the incubator
  - Support continuation frames ([this page](https://www.openmymind.net/WebSocket-Framing-Masking-Fragmentation-and-More/) is helpful)
  - Remove the kotlin dependency (should be easy to convert to plain java)
  - Make sure it matches the spec fully and really well (terminology, concepts, etc)
    - e.g. close statuses, custom frame types
  - Integrate it into the core library as an optional extension (in the sense that the websockets protocol uses the http extension mechanism) & all the implementations that can support it
