# Sketsa Text on Path

## Overview

Sketsa Text on Path edits SVG `<textPath>` structures. A selected `<text>` can be attached to a path by ID, its reference/start offset can be updated, and the text can later be detached without losing content.

Version **0.1.2** keeps the existing DOM/Undo behavior and moves the editor into an **independent, dockable and scrollable window**.

## Main features

- Attach a single selected `<text>` element to a `<path>`.
- Update an existing `<textPath>`.
- Set path ID/reference.
- Edit `startOffset`.
- Write both `href` and `xlink:href`.
- Validate that the referenced ID resolves to a real SVG `<path>`.
- Detect text already inside `<textPath>`.
- Detach while preserving text nodes/content.
- Restore selection where possible after structural replacement.

## SVG representation

```xml
<text>
  <textPath href="#curve1"
            xlink:href="#curve1"
            startOffset="25%">
    Text on the path
  </textPath>
</text>
```

The offset is preserved as an SVG string, so percentages and document-valid numeric values remain possible.

## Snapshot update strategy

Attach, Update and Detach are implemented through a complete snapshot/replacement strategy for the owning `<text>` element. This keeps one logical action from being fragmented into unrelated DOM changes and ensures visible Batik refresh after start-offset/reference changes.

## Undo / Redo

`TextSnapshotEdit` stores the before/after state of the entire text structure and participates in the active `DOMUndoManager` transaction. Each high-level operation therefore behaves as a single reversible edit.

## Independent window

`TextOnPathTopComponent` is separate from Sketsa Properties, persistent and scrollable, and can be reopened through **Window → Text on Path**.

## Important source files

- `integration/TextOnPathPanel.java`
- `integration/TextOnPathTopComponent.java`
- `integration/OpenTextOnPathPanelAction.java`
- `integration/TextOnPathIntegrator.java` (legacy integration helper)

## Requirements / build

Sketsa 9.1, NetBeans 11.3, JDK 11. See [`BUILDING.md`](../../BUILDING.md).

## License

Apache License 2.0.
