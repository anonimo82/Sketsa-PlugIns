# Sketsa Text Spacing

## Overview

**Sketsa Text Spacing** extends Sketsa's text styling tools with independent controls for the SVG/CSS `letter-spacing` and `word-spacing` properties.

The module `kiyut.sketsa.modules.textspacing` has specification version **0.3.4** and targets Java 11.

## Main Features

- Adjust **Letter spacing** in pixels.
- Adjust **Word spacing** in pixels.
- Control range from `-1000` to `1000`, with a `0.5` step.
- Apply changes immediately to the current single text selection.
- Support both CSS style properties and SVG presentation attributes.
- Explicitly refresh the canvas after a change so Batik/Sketsa rendering updates immediately.
- Integrate into the native **Text Style** component instead of creating a separate editor.

## Selection and Focus Handling

The panel preserves the last valid text element when focus moves from the document to the plugin controls. This prevents the editor from becoming inactive merely because the NetBeans global lookup temporarily becomes empty while the user interacts with a spinner.

The spinner arrow buttons are also made non-focusable, reducing interference with SVG document focus.

## Supported Property Formats

`DOMUtilities.updateProperty()` can write a property according to Sketsa's formatting preferences:

- in the element's CSS `style`;
- as an SVG presentation attribute.

The plugin reads both forms, so reselecting text does not reset the controls when the document uses a different representation from the expected one.

A zero value is written as absence of the property; integer and decimal values are serialized in `px`.

## Undo / Redo

Applying letter spacing and word spacing is wrapped in a single `DOMUndoManager` transaction named **Text Spacing**, so the two properties are undone or restored together.

## Visual Refresh

Version 0.3.4 explicitly calls `VectorCanvas.refresh()` after `DOMUtilities.updateProperty()`. This forces Sketsa/Batik to rebuild the rendered text immediately after a change instead of waiting for a later repaint or selection event.

## Main Source Files

- `src/kiyut/sketsa/modules/textspacing/integration/TextSpacingPanel.java` — spacing controls, property reading/writing, and Undo/Redo.
- `src/kiyut/sketsa/modules/textspacing/integration/TextStyleIntegrator.java` — integration into the Text Style panel.
- `src/kiyut/sketsa/modules/textspacing/Installer.java` — initialization.

## Reference Environment

- Sketsa 9.1
- NetBeans 11.3
- Java 11
