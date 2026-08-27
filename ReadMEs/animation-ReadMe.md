# Sketsa Animation Editor

## Overview

**Sketsa Animation Editor** is the largest project in the archive. It adds a visual track-and-keyframe editor for SVG/SMIL animation to Sketsa, including a timeline, property inspector, playback/scrubbing controls, and live preview on the canvas.

The source code identifies the module as **Animation Editor 1.6.11 – M5 Multi Object Linked Edit Fix**. The `manifest.mf` file, however, still contains `OpenIDE-Module-Specification-Version: 1.6.8`. These values are therefore documented separately because they belong to different metadata found in the project.

The module targets Java 11 and integrates into the NetBeans Platform as a `TopComponent` window named **Animation Editor - SMIL**.

## Editing Model

The editor represents animation through:

- SVG objects;
- expandable SMIL tracks;
- keyframes containing time and value information;
- a timeline playhead;
- an inspector for properties, timing, and composition.

`Timeline`, `TimelineModel`, and `SMILTrack` separate the timeline representation from DOM manipulation and preview logic.

## Authorable Track Types

The **Add Track** menu directly supports the following categories.

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
- `set` tracks for `x`, `y`, `opacity`, `fill`, and `visibility`

### Transforms

- `translate`
- `scale`
- `rotate`
- `skewX`
- `skewY`

### Motion

- `animateMotion` through **Motion Path**;
- references to an existing path through `<mpath>` and `href` / `xlink:href`;
- inline SVG path data stored directly in the `path` attribute;
- motion rotation control;
- motion anchor handling.

### Generic Tracks

- numeric properties;
- color properties;
- discrete properties.

These generic tracks make it possible to animate SVG attributes that are not exposed as dedicated presets while still using standard SVG/SMIL structures.

## SMIL Timing and Composition

The inspector handles the main timing and composition properties, including:

- `begin`;
- `end`;
- duration;
- `repeatCount`;
- `repeatDur`;
- `restart`;
- `fill` (`freeze` / `remove`);
- `calcMode`, including `linear`, `discrete`, `paced`, and `spline`;
- `keySplines`;
- `additive` (`replace` / `sum`);
- `accumulate` (`none` / `sum`).

The code also contains parsing and resolution logic for clock-based, event-based, and syncbase timing expressions, as well as support for indefinite duration or repetition.

## Keyframes and Interpolation

The editor can add, move, and remove keyframes and evaluate track values during scrubbing and playback. Its internal runtime includes logic for:

- numeric interpolation;
- color interpolation;
- `calcMode="discrete"`;
- cubic spline easing;
- paced timing for transforms;
- path `d` morphing when path topology is compatible;
- additive and accumulated value composition where supported.

## Transforms and Pivot Handling

For `rotate`, `scale`, `skewX`, and `skewY`, transforms authored by the editor use the object's **local visual center** as the default pivot.

To keep the result portable as standard SVG/SMIL, scale and skew operations may be accompanied by helper `animateTransform` elements implementing the translate-to-pivot, transform, and translate-back sequence. These helpers remain hidden from the timeline, share the timing of the main track, and are removed together with it.

`translate` does not require a pivot.

## Motion Path

Motion authoring accepts:

- `#id` or `id` references to an existing path, creating or updating an `<mpath>` element;
- inline SVG path data beginning with a valid path command, stored in the `path` attribute of `<animateMotion>`.

The implementation also maintains compatibility with both `href` and `xlink:href` references on `<mpath>`.

## Multi-Object Authoring

When multiple objects are selected on the canvas, a new track can be applied to all targets as one native Undo operation. The first selected object becomes the timeline editing reference, while related tracks can remain synchronized during multi-object authoring.

## Preview and Playback

The editor does more than modify the SVG DOM. It includes a preview runtime that updates Batik's rendered state directly during scrubbing and playback, with support for:

- transforms and motion;
- geometry and paths;
- opacity and visibility;
- fill and stroke;
- generic properties;
- SMIL events;
- playback with timeline extension when required.

Play, Pause, Stop, Zoom In, and Zoom Out controls are included.

## Restoring Static State

An important part of the implementation handles deletion of an active animation track. After removal, the editor immediately restores the object's static authoring state and reevaluates the remaining tracks at the current time.

Restoration covers transforms, motion, geometry, paths, opacity, visibility, fill/stroke, and generic properties, preventing a Batik preview state from remaining visually "stuck" after its animation is removed.

## Undo / Redo and Persistence

Authoring operations are integrated with Sketsa's Undo/Redo system. Animations are stored as normal SVG/SMIL elements (`<animate>`, `<animateTransform>`, `<animateMotion>`, `<set>`, and `<mpath>`), so they remain part of the document and can be saved and reopened without depending on proprietary preview state.

## Main Components

- `AnimationTopComponent.java` — NetBeans window, document binding, and global Undo/Redo integration.
- `AnimationEditor.java` — main UI, authoring logic, inspector, preview, and playback.
- `Timeline.java` — timeline view and interaction.
- `TimelineModel.java` — tree model for objects and tracks.
- `SMILTrack.java` — abstraction for an SMIL track and its attributes/values.
- `TimingCellEditor.java`, `TimingCellRenderer.java`, `TimingHeaderRenderer.java`, `TimingValue.java` — timing editing and rendering.
- `NameCellRenderer.java` — timeline name rendering.

## Tests Included in the Project

The project contains an M5 regression suite (`TESTS-1.6.11-M5-FULL-AUTHORING-EXPANSION.txt`) covering:

- transforms;
- visibility;
- path `d` morphing;
- numeric, color, and discrete properties;
- advanced timing and composition;
- referenced and inline motion paths;
- multi-object authoring;
- SVG/SMIL portability;
- previous milestone regressions and real-world animation cases.

The fix-only notes also document targeted corrections for track deletion, stroke refresh, transform pivots, visibility, and motion authoring.

## Reference Environment

- Sketsa
- NetBeans Platform
- Java 11
- SVG + SMIL
