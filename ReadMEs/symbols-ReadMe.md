# Sketsa Symbols

## Overview

**Sketsa Symbols** provides tools for creating SVG `<symbol>` definitions, inserting and editing `<use>` instances, and converting instances back into independent SVG content.

The manifest identifies the module as `kiyut.sketsa.modules.symbols` with specification version **0.1.1**. The project targets Java 11.

## Main Features

- Create a `<symbol>` from the selected SVG object.
- Update an existing `<symbol>` definition with the same ID.
- Insert new `<use>` instances of a symbol.
- Edit the referenced symbol and the `x` / `y` coordinates of a selected `<use>`.
- Detach a `<use>` instance into concrete SVG content while preserving the original `<symbol>` definition.
- Automatically create `<defs>` when the document does not already contain one.
- Handle both `href` and `xlink:href` references.

## Creating Definitions

When a symbol is created, the plugin **clones** the selected element instead of moving it into `<defs>`. Any `id` on the cloned top-level node is removed to avoid an immediate duplicate with the original object.

The resulting definition follows this model:

```xml
<defs>
    <symbol id="mySymbol">
        <!-- clone of the original object -->
    </symbol>
</defs>
```

## `<use>` Instances

Instances created by the plugin use both reference forms:

```xml
<use href="#mySymbol"
     xlink:href="#mySymbol"
     x="0"
     y="0" />
```

When a `<symbol>` definition is created, updated, restored through Undo, or reapplied through Redo, the plugin also forces a refresh of the `<use>` instances that reference it.

This behavior is intentional: Batik may cache the rendering of a resolved reference even after the definition DOM has changed. Rewriting the same references invalidates that cache and makes the updated symbol visible immediately.

## Detach

**Detach Use** replaces an instance with concrete content derived from the symbol definition. The instance coordinates are preserved through a translation transform when required. The original definition in `<defs>` is not deleted, so other instances remain valid.

## Undo / Redo

The source contains dedicated edits for:

- changing the state of a `<use>`;
- inserting a new instance;
- detaching an instance;
- creating or replacing a `<symbol>` definition.

Definition edits also refresh related instances after Undo and Redo.

## Main Source Files

- `src/kiyut/sketsa/modules/symbols/integration/SymbolsPanel.java` — symbol and instance management, detach, Batik refresh, and Undo/Redo.
- `src/kiyut/sketsa/modules/symbols/integration/SymbolsIntegrator.java` — panel integration.
- `src/kiyut/sketsa/modules/symbols/Installer.java` — initialization.

## Reference Environment

- Sketsa 9.1
- NetBeans 11.3
- Java 11
