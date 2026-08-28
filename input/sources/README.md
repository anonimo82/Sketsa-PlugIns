# Sketsa Input

## Overview

Sketsa Input is a Java/Sketsa SDK authoring plugin plus a browser-side input runtime. It normalizes keyboard, pointer, touch, gamepad and on-screen controls into **logical actions** so other runtimes can consume `move-left`, `jump`, `fire`, etc. without knowing which physical device produced them.

Version **0.8.1** preserves the certified Input 0.8.0 functionality and moves the editor into its own **independent, dockable and scrollable TopComponent window**.

## Logical-action model

Physical inputs map to named actions. For example:

```text
move-left
  <- ArrowLeft
  <- KeyA
  <- gamepad axis X negative
  <- on-screen D-pad left
```

Consumers receive action state/events, not raw browser objects.

## Keyboard

Keyboard mappings support:

- `KeyboardEvent.code`;
- multiple key codes per logical action;
- down/up/repeat;
- continuous pressed/value state;
- optional default-event prevention.

Using `code` keeps physical-key mappings stable across keyboard layouts.

## Pointer / mouse

The runtime uses Pointer Events and supports:

- pointer position;
- client coordinates;
- SVG coordinates;
- normalized `[0..1]` coordinates;
- pointer down/up;
- click;
- drag start/move/end;
- wheel;
- mouse/pen/touch pointer types.

## Touch / multi-pointer

Multi-pointer state includes:

- `pointerId`;
- `pointerType`;
- `isPrimary`;
- active pointer collection;
- `pointercancel`;
- simultaneous pointer tracking.

The same pointer infrastructure is used for desktop pointer and touch-capable exports.

## Gamepad

Gamepad support includes:

- `navigator.getGamepads()` polling;
- connected/disconnected detection;
- digital and analog buttons;
- axes;
- configurable deadzone;
- direction filtering;
- controller index filtering;
- logical action mapping;
- down/up/changed events;
- continuous value/pressed state.

## On-screen controls

Metadata can describe mobile/on-screen controls:

- virtual button;
- D-pad;
- virtual stick;
- configurable directional logical actions;
- stick deadzone;
- multi-pointer interaction.

These controls feed the same logical action system as keyboard/gamepad.

## Input Actions API

The neutral runtime contract includes:

- `window.SketsaInput`
- `sketsa:input:ready`
- `sketsa:input:event`
- `sketsa:input:action`
- `sketsa:input:actionResult`
- `sketsa:runtime:event` mirror

Action state contains simple values such as:

- action name;
- pressed;
- value;
- source.

Action requests include state/snapshot/reset operations. Snapshots avoid exposing browser `KeyboardEvent`, `PointerEvent` or `Gamepad` objects.

## Input → Physics interoperability

Input metadata can map an action event to a neutral Physics action, for example:

```text
move-right / down
  -> sketsa:physics:action
  -> applyForce
```

or:

```text
jump / down
  -> applyImpulse
```

No Java dependency on Physics is required; integration is purely export/runtime based.

## Companion runtime export

When **Include companion runtimes** is enabled and Physics metadata is present, Input export can include the compatible Physics/Matter runtime so an Input-controlled simulation works in one HTML output.

## Robustness

The 0.8 line includes:

- duplicate binding deduplication;
- duplicate/id-less SVG mapping tolerance;
- shared logical actions from multiple mapping elements;
- gamepad binding deduplication;
- interop binding deduplication;
- repeated-export cleanup;
- preservation of companion runtime behavior.

## Independent window

`InputTopComponent` is independent from Sketsa Properties, persistent and scrollable, and can be opened from **Window → Input**.

## Important source files

- `integration/InputPanel.java` — authoring UI/metadata.
- `integration/InputTopComponent.java` — independent window.
- `integration/InputRuntimeContract.java` — runtime contract constants.
- `integration/RuntimeHtmlExporter.java`
- `integration/PhysicsCompanionExporter.java`
- `integration/sketsa-input-runtime.js`

## Requirements / build

Sketsa 9.1, NetBeans 11.3 and JDK 11. See [`BUILDING.md`](../../BUILDING.md).

## License

Apache License 2.0.
