# Sketsa Switch

## Overview

**Sketsa Switch** adds tools for creating and managing SVG `<switch>` elements, which are useful for alternative content selected according to conditions such as `systemLanguage`.

The module is identified as `kiyut.sketsa.modules.switcher`; the manifest reports specification version **0.2.6**, and the project targets Java 11.

## Main Features

The panel can:

- wrap a selected SVG object inside a new `<switch>`;
- add an object as an alternative to an existing `<switch>`;
- set or edit the selected alternative's `systemLanguage` attribute;
- remove an alternative;
- extract an alternative from the `<switch>` and reinsert it into the document;
- simulate a language to check which alternative should be active;
- synchronize the panel with the current selection and DOM changes.

## `systemLanguage` Editing

Version 0.2.6 introduces specific protection for editing the language field. When the user starts typing, the field is marked **dirty** and automatic synchronization from the DOM is temporarily suspended.

This prevents a periodic refresh timer from overwriting text that has not yet been committed. Pressing **Update Language** writes the value to the DOM and clears the dirty state. From that point onward, Undo/Redo can once again update the field according to the actual document state.

Changing the selection discards any uncommitted field value and loads the `systemLanguage` of the newly selected alternative.

## SVG Structure

The plugin works with standard structures such as:

```xml
<switch>
    <g systemLanguage="it">
        <!-- Italian content -->
    </g>
    <g systemLanguage="en">
        <!-- English content -->
    </g>
    <g>
        <!-- fallback -->
    </g>
</switch>
```

When an alternative is extracted, the plugin removes `systemLanguage` from the extracted node and moves it outside the `<switch>` while preserving its content.

## Undo / Redo

Structural changes are grouped through Sketsa's `DOMUndoManager`. Creating the container, adding/removing alternatives, updating the language, and extracting an alternative can therefore participate in the document's normal Undo/Redo history.

## Sketsa Integration

The panel uses the active `VectorCanvas` and listens to the current selection. Periodic synchronization keeps the UI and DOM consistent without interfering with a `systemLanguage` value that is still being typed.

## Main Source Files

- `src/kiyut/sketsa/modules/switcher/integration/SwitchPanel.java` — UI, `<switch>` management, language simulation, and Undo/Redo.
- `src/kiyut/sketsa/modules/switcher/integration/SwitchIntegrator.java` — integration into the Sketsa interface.
- `src/kiyut/sketsa/modules/switcher/Installer.java` — plugin bootstrap.

## Reference Environment

- Sketsa 9.1
- NetBeans 11.3
- Java 11
