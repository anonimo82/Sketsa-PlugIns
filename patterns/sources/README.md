# Sketsa Patterns

## Overview

Sketsa Patterns provides visual authoring of SVG `<pattern>` resources and applies them as fills or strokes. Version **0.3.1** keeps the existing pattern feature set and moves the plugin into its own **independent, dockable and scrollable window**.

The panel works as a pattern draft editor. Applying a pattern creates/updates SVG resources in `<defs>` and updates the selected element through standard SVG paint references.

## Pattern generators

Built-in pattern types:

- Vertical stripes
- Horizontal stripes
- Checkerboard
- Dots
- Custom / existing

Editable properties include:

- pattern ID;
- two pattern colors;
- x/y;
- width/height;
- absolute SVG-unit mode;
- relative/percentage mode;
- visual preview.

## Authoring operations

- Create or update a pattern definition.
- Apply pattern to `fill`.
- Apply pattern to `stroke`.
- Remove a pattern fill from the selected object.
- Remove a pattern stroke from the selected object.
- Create `<defs>` when required.
- Resolve and inspect existing pattern references.
- Synchronize the editor with the active object/selection.

The implementation deliberately separates editing a draft from applying it so objects can receive independent pattern definitions instead of being unintentionally coupled through mutable shared state.

## SVG representation

Pattern resources are normal SVG definitions referenced by paint values such as:

```xml
<defs>
  <pattern id="pattern1" ...>
    ...
  </pattern>
</defs>

<rect fill="url(#pattern1)" />
```

The exact children differ according to the selected generator.

## Units

Absolute mode writes SVG-unit values. Relative mode allows percentage-oriented authoring where supported by the pattern fields, making it possible to create patterns that scale according to the intended coordinate system.

## Undo / Redo

Pattern definition changes and application/removal operations are integrated with Sketsa's document mutation/Undo infrastructure. The panel refreshes from the current selection rather than maintaining a separate proprietary project state.

## Independent window

Version 0.3.1 registers `PatternsTopComponent` as a normal NetBeans window:

- independent of the Properties pane;
- dockable, movable and resizable;
- scrollable in both directions as needed;
- persisted by the window system;
- reopenable from **Window → Patterns**.

## Important source files

- `integration/PatternsPanel.java` — pattern generators, preview and SVG operations.
- `integration/PatternsTopComponent.java` — independent scrollable window.
- `integration/OpenPatternsPanelAction.java` — Window menu action.
- `integration/PatternsIntegrator.java` — retained legacy integration helper, no longer used at startup.

## Requirements / build

Sketsa 9.1, NetBeans 11.3 and JDK 11. See [`BUILDING.md`](../../BUILDING.md).

## License

Apache License 2.0. See [`LICENSE`](LICENSE).
