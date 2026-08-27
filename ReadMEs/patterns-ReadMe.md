# Sketsa Patterns

## Overview

**Sketsa Patterns** adds an editor for creating and applying SVG patterns as fills or strokes. The project follows a **private, immutable application model**: the panel acts as a pattern draft editor, while every actual application creates a new independent `<pattern>` definition.

The manifest declares the module `kiyut.sketsa.modules.patterns` with specification version **0.3.0**, targeting Java 11.

## Pattern Types

The panel includes generators for:

- vertical stripes;
- horizontal stripes;
- checkerboard patterns;
- dots;
- custom or already existing patterns.

For patterns managed by the plugin, the editor provides two colors, cell size and position controls, and a choice between absolute and relative coordinate modes.

## Coordinates and Units

Two modes are supported:

- **Absolute (SVG units)** — uses `patternUnits="userSpaceOnUse"` and absolute SVG values;
- **Relative (%)** — uses `patternUnits="objectBoundingBox"` and relative/percentage values.

The panel converts displayed values when switching modes and shows a preview of the pattern being prepared.

## Private Application Model

The **Create / Update** command prepares the current pattern draft but does not retroactively modify patterns that have already been applied. Each **Apply Fill** or **Apply Stroke** operation instead creates a new definition with a unique ID, such as `checker-1`, `checker-2`, or `checker-3`.

This design avoids a common side effect of shared pattern definitions: later edits in the panel cannot accidentally change objects that were assigned an earlier version of the pattern.

## Available Operations

- Prepare or edit the pattern draft.
- Apply the pattern as `fill`.
- Apply the pattern as `stroke`.
- Remove the pattern from the fill.
- Remove the pattern from the stroke.
- Detect the pattern already used by the selected object.
- Read paint properties from either CSS style or SVG presentation attributes.
- Create `<defs>` automatically when needed.

## Undo / Redo

Pattern application uses dedicated edits that keep together:

1. the private `<pattern>` definition created for that specific application;
2. the previous `fill` or `stroke` state of the target object.

Undo therefore removes exactly the private definition created by the operation and restores the previous paint. Redo recreates both parts.

## SVG Structure

The generated structure follows standard SVG conventions:

```xml
<defs>
    <pattern id="checker-1" ...>
        <!-- SVG primitives composing the pattern -->
    </pattern>
</defs>

<rect fill="url(#checker-1)" ... />
```

Generated definitions are normal SVG nodes and remain part of the saved document.

## Main Source Files

- `src/kiyut/sketsa/modules/patterns/integration/PatternsPanel.java` — editor, preview, pattern generation, application, and Undo/Redo.
- `src/kiyut/sketsa/modules/patterns/integration/PatternsIntegrator.java` — panel integration.
- `src/kiyut/sketsa/modules/patterns/Installer.java` — initialization.

## Reference Environment

- Sketsa 9.1
- NetBeans 11.3
- Java 11
