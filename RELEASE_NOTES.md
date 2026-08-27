# Repository Consolidation Release

Date: 2026-08-28

This repository revision applies the requested repository-wide consolidation to all ten Sketsa plug-ins.

## UI integration

Every plug-in now uses an independent NetBeans tool window:

- Animation 1.6.12
- Links 0.1.2
- Patterns 0.3.1
- Switch 0.2.7
- Symbols 0.1.2
- Text on Path 0.1.2
- Text Spacing 0.3.5
- Sketsa Audio 0.8.3
- Sketsa Input 0.8.1
- Sketsa Physics 0.9.7

Animation already used an independent TopComponent; its existing scrollable timeline/inspector model is retained.

The other nine plug-ins now register their own persistent `TopComponent`, expose a Window-menu action, and no longer inject controls into `PropertiesTopComponent`. Their windows wrap the authoring panel in scrollable containers. Audio/Input/Physics also retain their existing internally scrollable forms.

## Documentation

Every plug-in directory contains a new English `README.md` describing:

- purpose and feature set;
- SVG/runtime model;
- Undo/Redo or persistence behavior where applicable;
- cross-plugin/runtime interoperability where applicable;
- independent window behavior;
- principal source files;
- requirements, build and licensing.

Repository-wide build instructions are in `BUILDING.md`.

## License

The repository uses the Apache License 2.0. This is a permissive license with an explicit patent grant and is compatible with the Apache-licensed Animation source already present in the original archive.

Third-party notices are retained separately.

## Binary packages

Each source project includes a matching current `.nbm`. All ten NBMs are also copied to `dist/nbm/`.

The included binaries were compiled with Java 11 bytecode compatibility (`--release 11`) and packaged as NetBeans modules using the dependency metadata from the supplied working NBM builds.

## Scope

This consolidation intentionally changes UI integration/documentation/licensing and does not redesign the functional behavior of the plug-ins. Audio/Input/Physics browser runtime feature versions remain the previously certified runtime implementations; their module versions are bumped to reflect the independent-window repository release.
