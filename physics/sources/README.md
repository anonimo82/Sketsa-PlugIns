# Sketsa Physics

## Overview

Sketsa Physics is a Java/Sketsa SDK authoring plugin with an exported **Matter.js 2D physics runtime**. Physics properties are stored as structured SVG metadata and converted into bodies, constraints, forces, collision rules and runtime events in the exported HTML.

Version **0.9.7** preserves the certified Physics 0.9.6 behavior while moving the authoring UI into its own **independent, dockable and scrollable NetBeans TopComponent window**. It is no longer injected into the Properties pane.

## Bodies and colliders

Supported body/collider authoring includes:

- dynamic/static bodies;
- auto collider;
- rectangle collider;
- circle collider;
- polygon collider;
- mass;
- density;
- friction;
- static friction;
- air friction;
- restitution;
- angle;
- linear velocity X/Y;
- angular velocity;
- sensor mode.

## World and forces

Document-level world settings include:

- gravity X/Y;
- gravity scale;
- time scale;
- sleeping.

Per-body/runtime controls include:

- force X/Y;
- impulse X/Y;
- torque;
- initial sleeping;
- pause/resume;
- reset;
- sleep/wake dynamic bodies.

## Constraints

Supported constraint forms include:

- none;
- distance;
- pin;
- body-to-body targets by SVG ID;
- distance body-to-world;
- point A / point B offsets;
- explicit/automatic length;
- stiffness;
- damping.

Constraint visualization follows simulated endpoints in exported runtime rather than remaining a static SVG line.

## Collision filtering and sensors

Physics supports Matter-style collision configuration through neutral metadata:

- category;
- mask;
- group;
- positive/negative group semantics;
- sensors/triggers.

## Collision events

The runtime handles:

- collisionStart;
- collisionActive;
- collisionEnd.

Event payloads are converted to plain Sketsa data and do not expose Matter collision objects to consumers. Events can be identified through an authorable event ID.

## SVG ↔ Physics synchronization

The runtime builds bodies from effective SVG transforms and supports:

- nested transforms;
- translate/rotate/scale;
- non-uniform scaling;
- transformed polygons;
- deterministic runtime IDs;
- duplicate SVG IDs;
- missing SVG IDs;
- preservation of original SVG IDs in event/state data.

Runtime rendering updates SVG transform state from the physical body while respecting document-space transforms.

## Export runtime

Physics Runtime HTML export:

- uses Matter.js 0.20.0;
- exports runtime assets locally;
- avoids a runtime CDN dependency;
- caches/verifies Matter.js on the editor side;
- includes Physics assets only when Physics metadata is present;
- can operate with direct local HTML output according to the established export model.

## Events & Actions API

The neutral public runtime exposes:

- `window.SketsaPhysics`
- `sketsa:physics:ready`
- `sketsa:physics:event`
- `sketsa:physics:action`
- `sketsa:physics:actionResult`
- `sketsa:runtime:event` mirror

Supported runtime actions include operations such as:

- pause/resume/reset;
- set position;
- set velocity;
- apply force;
- apply impulse;
- sleep/wake;
- state/snapshot access.

Snapshots contain plain data rather than Matter.js bodies/engine objects.

## Cross-plugin interoperability

Physics publishes collision/runtime events on the neutral bus. Audio can consume collision events and Input can generate Physics actions. There is no direct Java dependency among Physics, Audio and Input.

## Companion runtimes

The Physics panel includes **Include companion runtimes**. When enabled, export detects compatible metadata and can include:

- Sketsa Input runtime;
- Sketsa Audio runtime and audio assets;
- Matter.js + Physics runtime.

Runtime ordering is chosen so companion listeners are available before simulation events begin.

## Independent window

`PhysicsTopComponent` provides a dedicated Physics tool window:

- separate from Properties;
- dockable/movable/resizable;
- persistent;
- scrollable;
- available from **Window → Physics**.

The original height-bounded internal Physics form remains scrollable as well.

## Important source files

- `integration/PhysicsPanel.java`
- `integration/PhysicsTopComponent.java`
- `integration/RuntimeHtmlExporter.java`
- `integration/InputCompanionExporter.java`
- `integration/AudioCompanionExporter.java`
- `integration/PhysicsRuntimeContract.java`

## Requirements / build

Sketsa 9.1, NetBeans 11.3, JDK 11. See [`BUILDING.md`](../../BUILDING.md).

## License

Apache License 2.0. Matter.js is a third-party runtime distributed under its upstream MIT license.
