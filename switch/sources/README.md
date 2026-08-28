# Sketsa Switch

## Overview

Sketsa Switch provides authoring tools for the SVG `<switch>` element, which selects the first child whose conditional processing attributes are satisfied. The plugin is useful for language/content alternatives and fallback content.

Version **0.2.7** preserves the existing SVG behavior and moves the UI into its own **independent, dockable and scrollable NetBeans window**.

## Main features

- Wrap selected content in `<switch>`.
- Add an alternative.
- Edit the selected alternative's `systemLanguage`.
- Remove an alternative.
- Extract an alternative back out of the `<switch>`.
- Simulate language matching in the authoring panel.
- Track the active Sketsa selection.
- Maintain fallback alternatives without `systemLanguage`.
- Keep synchronization from overwriting a language value while the user is still typing.

## `systemLanguage` editing

The language field uses a dirty-state protection mechanism. While the user is editing, automatic DOM synchronization is suspended so a refresh cannot overwrite uncommitted text. **Update Language** commits the value and resumes synchronization.

Changing selection discards uncommitted UI text and reloads the selected alternative's actual DOM state.

## Typical SVG

```xml
<switch>
  <g systemLanguage="it">...</g>
  <g systemLanguage="en">...</g>
  <g>Fallback content</g>
</switch>
```

When an alternative is extracted, the plugin moves it out of the switch and removes its conditional language attribute as appropriate.

## Undo / Redo

Structural operations are grouped through Sketsa's `DOMUndoManager`, including wrapping, adding/removing alternatives, language updates and extraction.

## Independent window

`SwitchTopComponent` is:

- separate from Sketsa Properties;
- dockable/movable/resizable;
- scrollable;
- persisted by the NetBeans window system;
- reopenable through **Window → Switch**.

## Important source files

- `integration/SwitchPanel.java` — UI, switch structure, language simulation, synchronization and Undo/Redo.
- `integration/SwitchTopComponent.java` — independent window.
- `integration/OpenSwitchPanelAction.java` — Window menu command.
- `integration/SwitchIntegrator.java` — legacy integration helper retained in source but not used on startup.

## Requirements / build

Sketsa 9.1, NetBeans 11.3, JDK 11. See [`BUILDING.md`](../../BUILDING.md).

## License

Apache License 2.0.
