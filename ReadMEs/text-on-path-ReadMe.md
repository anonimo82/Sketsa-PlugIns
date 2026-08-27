# Sketsa Text on Path

## Overview

**Sketsa Text on Path** adds a panel for attaching a text element to an SVG path through `<textPath>`, changing the referenced path and `startOffset`, or detaching the text while preserving its content.

The manifest declares the module `kiyut.sketsa.modules.textonpath` with specification version **0.1.1**, targeting Java 11.

## Main Features

- Attach a single selected `<text>` element to a `<path>` identified by ID.
- Update an existing `<textPath>`.
- Change the `startOffset` value.
- Write both `href` and `xlink:href` for the referenced path.
- Verify that the requested ID exists and refers to an actual `<path>` element.
- Detach text from the path without losing text nodes or content.
- Automatically recognize a selection that is already inside a `<textPath>`.

## SVG Structure

The generated link uses standard SVG markup:

```xml
<text>
    <textPath href="#curve1"
              xlink:href="#curve1"
              startOffset="25%">
        Text on the path
    </textPath>
</text>
```

`startOffset` is preserved as an SVG string, so it can represent either a numeric value or a percentage valid for the document.

## Update Strategy

Version 0.1.1 uses a **complete snapshot strategy for the `<text>` element**. For Attach, Update, and Detach operations, the new structure is built first and the current `<text>` node is then replaced with the new version.

This approach prevents one logical operation from being fragmented into many small DOM changes and solves both visual `startOffset` refresh issues and Undo/Redo isolation.

## Undo / Redo

`TextSnapshotEdit` stores the previous and next state of the entire `<text>` element. The edit is inserted explicitly into the current `DOMUndoManager` entry, so Attach, Update, and Detach each behave as a single reversible operation.

After replacement, the plugin updates its reference to the live element and attempts to restore its selection on the canvas.

## Main Source Files

- `src/kiyut/sketsa/modules/textonpath/integration/TextOnPathPanel.java` — UI, path resolution, text snapshots, and Undo/Redo.
- `src/kiyut/sketsa/modules/textonpath/integration/TextOnPathIntegrator.java` — panel integration.
- `src/kiyut/sketsa/modules/textonpath/Installer.java` — initialization.

## Reference Environment

- Sketsa 9.1
- NetBeans 11.3
- Java 11
