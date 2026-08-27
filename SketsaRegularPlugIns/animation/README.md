# Sketsa Animation Editor

## Overview

Sketsa Animation Editor is a Java/NetBeans module for authoring SVG/SMIL animation directly inside Sketsa 9.1. It provides a dedicated **independent NetBeans TopComponent window**, comparable to a normal editor/tool window rather than an extension embedded into Sketsa's Properties pane. The window can be docked, moved, resized, closed and reopened from the **Window** menu.

The editor is designed around SVG objects, expandable animation tracks, keyframes, a timeline playhead and an inspector. All authored animation is stored as standard SVG/SMIL markup, so saved documents remain portable and do not depend on a proprietary project format.

This repository build is version **1.6.12**. It preserves the 1.6.11 authoring feature set and standardizes the repository documentation/licensing while keeping the independent Animation window model.

## User interface

The Animation window contains:

- timeline object rows and expandable animation tracks;
- keyframe editing;
- Play, Pause and Stop controls;
- Zoom In and Zoom Out controls;
- a time slider/playhead;
- a timing/property inspector;
- scrollable timeline and inspector areas;
- live preview on the active Sketsa canvas.

The component follows the active `SVGEditorCookie`, integrates with Sketsa Undo/Redo and persists as a NetBeans window.

## Authorable track types

### Geometry

- `x`
- `y`
- `cx`
- `cy`
- `r`
- `width`
- `height`
- path `d`

### Appearance

- `opacity`
- `fill`
- `fill-opacity`
- `stroke`
- `stroke-opacity`
- `stroke-width`

### State

- `visibility`
- `set` tracks for `x`, `y`, `opacity`, `fill` and `visibility`

### Transforms

- translate
- scale
- rotate
- skewX
- skewY

### Motion

- `animateMotion`
- referenced paths through `<mpath>`
- `href` and `xlink:href` compatibility
- inline SVG path data
- motion rotation
- motion anchor controls

### Generic tracks

The editor also supports generic numeric, color and discrete animation tracks, allowing SVG attributes that are not represented by a dedicated preset to be animated through normal SMIL structures.

## Timing and composition

The inspector supports the principal SMIL timing/composition attributes:

- `begin`
- `end`
- duration
- `repeatCount`
- `repeatDur`
- `restart`
- `fill`
- `calcMode`
- `keySplines`
- `additive`
- `accumulate`

The implementation contains handling for clock values, event timing, syncbase timing and indefinite timing where applicable.

## Keyframes and interpolation

The editor can create, move and delete keyframes and evaluate values during scrubbing/playback. The preview path includes support for:

- numeric interpolation;
- color interpolation;
- discrete mode;
- cubic spline easing;
- paced timing;
- compatible path-`d` morphing;
- transform composition;
- additive/accumulated values where supported by the track.

## Transform pivots and motion paths

Rotate, scale and skew authoring use the object's local visual center as the default pivot. Where necessary, helper `animateTransform` elements reproduce pivot translation using portable SMIL. Helper tracks share the timing of their owning track and are removed with it.

Motion paths may reference an existing path by ID or store inline path data. Both modern `href` and legacy `xlink:href` forms are handled.

## Multi-object authoring

When multiple SVG objects are selected, track creation can target all selected objects as one logical authoring operation. The first selection acts as the timeline editing reference while related tracks can be authored together.

## Preview, cleanup and persistence

The preview runtime updates Batik-rendered state during playback/scrubbing. Removing an active animation restores the underlying static authoring state so the canvas does not remain visually stuck on a preview value.

Authored animation is persisted as normal SVG/SMIL elements such as:

- `<animate>`
- `<animateTransform>`
- `<animateMotion>`
- `<set>`
- `<mpath>`

## Undo / Redo

Animation authoring is integrated with Sketsa's Undo/Redo infrastructure. The module also exposes the active document Undo/Redo through its TopComponent.

## Important source files

- `windows/AnimationTopComponent.java` — independent NetBeans window and active-document binding.
- `windows/AnimationEditor.java` — main UI, inspector, authoring and preview logic.
- `timeline/Timeline.java` — visual timeline.
- `timeline/TimelineModel.java` — object/track hierarchy.
- `timeline/SMILTrack.java` — SMIL track abstraction.
- timing cell editor/renderer classes — timing UI.

## Included tests

The project contains multiple SVG tests and the M5 regression suite covering transforms, visibility, path morphing, generic animation, timing/composition, motion paths, multi-object authoring and real-world SMIL documents.

## Requirements

- Sketsa SVG Editor 9.1
- Apache NetBeans 11.3
- JDK 11
- Sketsa/NetBeans platform registered as described in the repository-level `BUILDING.md`

## Building

See the repository-level [`BUILDING.md`](../../BUILDING.md). In NetBeans use **Clean and Build**, then **Create NBM**.

## License

Apache License 2.0. See [`LICENSE`](LICENSE). The project also retains separate notices/licenses for third-party icon resources under `legal/`.
