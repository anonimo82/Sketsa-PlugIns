# Sketsa Audio

## Overview

Sketsa Audio is a Java/Sketsa SDK authoring plugin with a browser-side **Web Audio API** export runtime. It associates audio behavior with SVG objects through structured `data-sketsa-audio-*` metadata and exports self-contained web runtime assets.

Version **0.8.3** preserves the certified Audio 0.8.2 runtime/export feature set while moving the editor into its own **independent, dockable and scrollable NetBeans TopComponent window**. Audio is no longer injected into Sketsa's Properties panel.

## Authoring panel

The Audio window provides controls for:

- audio source file;
- autoplay;
- loop;
- source volume;
- playback rate;
- start offset;
- event ID;
- source mute;
- bus name;
- bus volume/mute;
- master volume/mute;
- stereo pan;
- automatic pan from SVG X position;
- runtime trigger source/type/event ID/action;
- **Include companion runtimes**;
- Runtime HTML export.

The panel follows the current selection and stores its settings on the selected SVG element/document as metadata.

## Audio source/player

The runtime supports:

- load/decode through Web Audio;
- play;
- pause;
- stop;
- restart;
- loop;
- volume;
- playback rate;
- start offset;
- overlapping voices when requested;
- source state such as loaded/playing/paused/stopped/ended.

## Asset management

Audio assets are copied into the generated export rather than depending on arbitrary absolute editor paths.

The asset layer provides:

- local export paths;
- SHA-256 content deduplication;
- deterministic/safe generated names;
- shared asset reuse when several SVG objects reference identical audio;
- externalization of embedded data-URI audio;
- cleanup on repeated export;
- missing-asset error handling;
- support for direct `file://` opening through the plugin's local-compatible loader strategy.

## Mixing

The Web Audio graph supports:

- source gain/mute;
- named buses;
- bus gain/mute;
- master gain/mute;
- source → bus → master → destination routing;
- runtime reassignment of a source to another bus.

## Stereo / spatial 2D

Each source may use:

- manual stereo pan `[-1, +1]`;
- automatic pan derived from the selected SVG object's X position.

Automatic pan can be recomputed as geometry changes. The plugin intentionally stays 2D; it does not introduce a 3D scene model.

## Events & Actions API

The runtime exposes a neutral API rather than Web Audio node objects:

- `window.SketsaAudio`
- `sketsa:audio:ready`
- `sketsa:audio:event`
- `sketsa:audio:action`
- `sketsa:audio:actionResult`
- mirror events on `sketsa:runtime:event`

Events include load/play/pause/stop/ended/error. Snapshots contain plain data and do not expose `AudioBuffer`, `GainNode`, `StereoPannerNode` or voice internals.

## Cross-plugin interoperability

Audio can consume neutral runtime events and translate matching events into Audio actions. A typical binding is:

```text
Physics collisionStart
    -> sketsa:runtime:event
    -> Audio trigger binding
    -> sketsa:audio:action
    -> play
```

There is no direct Java dependency on the Physics plugin.

## Companion runtime export

With **Include companion runtimes** enabled, Audio can include compatible Physics runtime assets when Physics metadata is present. The generated HTML is therefore able to run Physics + Audio together without a CDN dependency for the final runtime.

## Robustness

The runtime/export layer handles:

- duplicate SVG IDs through deterministic runtime IDs;
- elements without IDs;
- shared assets;
- missing audio assets;
- repeated exports;
- persistence of trigger metadata;
- target-aware action results.

## Independent window

`AudioTopComponent` is:

- separate from `PropertiesTopComponent`;
- dockable/movable/resizable;
- scrollable;
- persistent;
- available from **Window → Audio**.

The existing internal scrollable form is retained, and the independent TopComponent provides a window-level scrolling boundary as well.

## Important source files

- `integration/AudioPanel.java` — authoring UI and SVG metadata.
- `integration/AudioTopComponent.java` — independent scrollable window.
- `integration/RuntimeHtmlExporter.java` — HTML/runtime asset export.
- `integration/PhysicsCompanionExporter.java` — Physics companion profile.
- `integration/sketsa-audio-runtime.js` — Web Audio runtime.
- `AudioRuntimeContract.java` — public contract constants.

## Requirements / build

Sketsa 9.1, NetBeans 11.3 and JDK 11. See [`BUILDING.md`](../../BUILDING.md).

## License

Apache License 2.0. Browser Web Audio is a web-platform API. Any third-party companion runtime retains its own upstream license.
