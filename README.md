# Sketsa Plugins Collection

A consolidated collection of plugins for **Sketsa SVG Editor 9.1**.

Each plugin is self-contained in its own directory and includes:

- complete source code under `sources/`;
- a plugin-specific `README.md`;
- the corresponding installable `.nbm` package.

## Included plugins

- Animation — 1.6.19
- Audio — 0.16.12
- Input — 0.8.10
- Links — 0.1.8
- Patterns — 0.3.7
- Physics — 0.9.16
- Switch — 0.2.16
- Symbols — 0.1.10
- Text on Path — 0.1.10
- Text Spacing — 0.3.15

## Main updates in this collection

The plugins were reviewed and updated for improved integration with Sketsa 9.1. The work includes contextual value selection for appropriate fields, document-aware suggestions where applicable, and extensive Undo/Redo fixes across plugin editors.

Contextual menus may provide either predefined values or values discovered from the currently open SVG document, while keeping manual entry available where the field allows free-form input.

Undo/Redo behavior was audited across the collection. **Switch** still has a known limitation in some language-selection workflows; this is documented in its plugin-specific README. **Text Spacing 0.3.15** includes UI synchronization after Undo/Redo.

## Installation

Install the `.nbm` file from the relevant plugin directory through the NetBeans/Sketsa plugin manager.

## Building from source

The sources target the Sketsa 9.1 / NetBeans module platform setup used for these builds. Each plugin directory contains its original NetBeans module project under `sources/`.

## License

This collection is distributed under the **Apache License 2.0**. See [`LICENSE`](LICENSE).

Third-party components and notices are listed in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md). Individual plugin source trees may also contain their own legal or third-party license files; those files are preserved.
