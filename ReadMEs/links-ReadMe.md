# Sketsa Links

## Overview

**Sketsa Links** is a Sketsa module that adds a dedicated panel for creating and editing SVG hyperlinks. The plugin works directly on the document DOM and can wrap a selected graphic element inside an `<a>` element without losing the original node.

The project is written in Java for the NetBeans Platform used by Sketsa and targets Java 11. The manifest declares the module `kiyut.sketsa.modules.links` with specification version **0.1.1**.

## Main Features

- Create a link around a single selected SVG object.
- Edit an existing `<a>` link.
- Manage **URL**, **Target**, and **Title** fields.
- Write both `href` and `xlink:href` for compatibility with SVG documents using either form.
- Remove a link without deleting the graphic content wrapped by `<a>`.
- Automatically detect the link associated with the selected object.
- Integrate with Sketsa's current selection and active canvas.

## Undo / Redo

Version 0.1.1 treats updates to an existing link as a **structural replacement of the `<a>` wrapper**, while preserving its child nodes. This allows Sketsa's `DOMUndoManager` to record changes to URL, target, and title correctly and restore them through Undo/Redo.

Creation and removal of the wrapper are also performed through DOM operations compatible with the document history.

## SVG Structure

A link created by the plugin has, in simplified form, the following structure:

```xml
<a href="https://example.com"
   xlink:href="https://example.com"
   target="_blank"
   title="Example">
    <!-- selected SVG element -->
</a>
```

The plugin preserves the selected element's content and, when a link is removed, reinserts the wrapper's children in its place.

## Sketsa Integration

`LinksIntegrator` installs the panel into the Sketsa interface. During module restoration, `Installer` retries the integration through a Swing timer so that the plugin can attach even when the application's UI components have not yet been fully created at initial module load time.

## Main Source Files

- `src/kiyut/sketsa/modules/links/integration/LinksPanel.java` — UI, selection handling, and DOM operations.
- `src/kiyut/sketsa/modules/links/integration/LinksIntegrator.java` — panel integration into Sketsa.
- `src/kiyut/sketsa/modules/links/Installer.java` — module initialization.

## Reference Environment

- Sketsa 9.1
- NetBeans 11.3
- JDK / Java 11
