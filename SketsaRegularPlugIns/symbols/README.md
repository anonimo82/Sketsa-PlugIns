# Sketsa Symbols

## Overview

Sketsa Symbols adds SVG `<symbol>` and `<use>` authoring to Sketsa. It can turn selected artwork into reusable symbol definitions, create instances and later detach an instance back into concrete SVG content.

Version **0.1.2** preserves the symbol functionality and moves the UI into its own **independent, dockable and scrollable window**.

## Main features

- Create a `<symbol>` from the selected SVG object.
- Update an existing symbol definition by ID.
- Insert a `<use>` instance.
- Update the referenced symbol of a selected `<use>`.
- Edit instance x/y position.
- Detach a `<use>` into concrete SVG content.
- Automatically create `<defs>` if required.
- Support both `href` and `xlink:href`.
- Refresh dependent `<use>` instances after definition changes.

## Definition creation

The selected element is cloned into the symbol definition rather than moved. The top-level ID of the clone is removed to avoid an immediate duplicate with the source object.

```xml
<defs>
  <symbol id="mySymbol">
    <!-- cloned artwork -->
  </symbol>
</defs>
```

## Instances

The plugin creates compatible references:

```xml
<use href="#mySymbol"
     xlink:href="#mySymbol"
     x="0"
     y="0"/>
```

Definition changes explicitly refresh matching `<use>` references because Batik may cache resolved referenced content.

## Detach

**Detach Use** replaces an instance with concrete content cloned from the symbol. Instance x/y placement is preserved using translation where required. The original symbol definition remains available for other instances.

## Undo / Redo

Dedicated edits cover:

- symbol definition creation/replacement;
- use insertion;
- use changes;
- detach;
- refresh of referencing instances after Undo/Redo.

## Independent window

`SymbolsTopComponent` is a persistent, scrollable NetBeans tool window and is available from **Window → Symbols**. It no longer modifies the layout of `PropertiesTopComponent`.

## Important source files

- `integration/SymbolsPanel.java`
- `integration/SymbolsTopComponent.java`
- `integration/OpenSymbolsPanelAction.java`
- `integration/SymbolsIntegrator.java` (legacy integration helper)

## Requirements / build

Sketsa 9.1, NetBeans 11.3, JDK 11. See [`BUILDING.md`](../../BUILDING.md).

## License

Apache License 2.0.
