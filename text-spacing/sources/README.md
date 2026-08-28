# Sketsa Text Spacing

## Overview

Sketsa Text Spacing edits the SVG/CSS `letter-spacing` and `word-spacing` properties of selected text.

Version **0.3.5** keeps the 0.3.4 spacing/refresh behavior but changes the UI integration: the plugin now has its own **independent, dockable and scrollable window** rather than inserting controls into the native Text Style/Properties area.

## Main features

- Letter spacing control.
- Word spacing control.
- Range `-1000` to `1000`.
- Step `0.5`.
- Read both CSS style properties and SVG presentation attributes.
- Write according to Sketsa DOM/style handling.
- Remove the property when the effective value is zero.
- Serialize non-zero values in `px`.
- Refresh the vector canvas immediately after edits.
- Preserve the last valid text selection while focus moves into plugin controls.

## Selection/focus behavior

The panel tracks the active `SVGEditorCookie` and selected text element. Spinner interactions are designed not to lose the working text selection merely because focus moves away from the SVG canvas.

## Undo / Redo

Letter and word spacing updates are grouped into the document's Undo history so a spacing change can be reverted/restored as a logical operation.

## Rendering refresh

The panel explicitly calls `VectorCanvas.refresh()` after DOM style updates. This is important with Batik because a repaint alone may not immediately rebuild text layout after spacing changes.

## Independent window

`TextSpacingTopComponent`:

- is independent from native Text Style and Properties;
- is dockable/movable/resizable;
- provides scrollbars when needed;
- persists in the NetBeans window system;
- can be reopened from **Window → Text Spacing**.

## Important source files

- `integration/TextSpacingPanel.java`
- `integration/TextSpacingTopComponent.java`
- `integration/OpenTextSpacingPanelAction.java`
- `integration/TextStyleIntegrator.java` (legacy helper retained but not installed on startup)

## Requirements / build

Sketsa 9.1, NetBeans 11.3, JDK 11. See [`BUILDING.md`](../../BUILDING.md).

## License

Apache License 2.0.
