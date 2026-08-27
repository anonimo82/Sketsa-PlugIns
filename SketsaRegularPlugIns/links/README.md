# Sketsa Links

## Overview

Sketsa Links adds hyperlink authoring to Sketsa 9.1. The plugin works directly on the SVG DOM and can create, edit or remove an SVG `<a>` wrapper around the selected graphic object.

Version **0.1.2** changes the UI integration model: Links now has its own **independent, dockable and scrollable NetBeans TopComponent window**. It is no longer injected into Sketsa's Properties window. The panel opens with the application and can be reopened from the **Window → Links** command.

## Features

- Create a link around one selected SVG object.
- Edit an existing SVG `<a>` wrapper.
- Edit URL, target and title.
- Supported target presets: empty, `_self`, `_blank`, `_parent`, `_top`.
- Write both `href` and `xlink:href` for compatibility.
- Detect a link even when the selected object is a descendant of an existing `<a>`.
- Remove a link while preserving/reinserting the wrapped SVG content.
- Follow the active `SVGEditorCookie` and current canvas selection.
- Update control availability according to selection state.

## SVG model

A typical generated structure is:

```xml
<a href="https://example.com"
   xlink:href="https://example.com"
   target="_blank"
   title="Example">
    <rect .../>
</a>
```

Removing the link removes only the wrapper, not its graphic children.

## Undo / Redo

Link creation/removal is performed through DOM operations integrated with the document history. Existing link edits use structural replacement of the `<a>` wrapper while retaining child nodes, allowing URL/target/title changes to participate correctly in Sketsa Undo/Redo.

## Independent window

The plugin registers `LinksTopComponent` through a NetBeans layer:

- dockable/movable like Animation;
- persistent window state;
- vertical/horizontal scrollbars as needed;
- no injection into `PropertiesTopComponent`;
- Window menu action for reopening the panel.

## Important source files

- `integration/LinksPanel.java` — UI, selection tracking and link DOM operations.
- `integration/LinksTopComponent.java` — independent scrollable window.
- `integration/OpenLinksPanelAction.java` — Window menu command.
- `integration/LinksIntegrator.java` — retained legacy integration code; no longer used at startup.
- `Installer.java` — module lifecycle hook.

## Requirements

- Sketsa SVG Editor 9.1
- Apache NetBeans 11.3
- JDK 11

## Building

See [`BUILDING.md`](../../BUILDING.md). Build the project with **Clean and Build**, then use **Create NBM**.

## License

Apache License 2.0. See [`LICENSE`](LICENSE).
