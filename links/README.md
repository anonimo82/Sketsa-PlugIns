# Links — Sketsa Plug-in

Version: **0.1.8**  
Reference platform: **Sketsa SVG Editor 9.1 / NetBeans Platform 11.3**  
Java target: **Java 11+**

## Contents

- `sources/` — complete NetBeans module source project.
- `links-0.1.8.nbm` — installable NBM built from this source revision.
- `README.md` — this file.

## Plug-in summary

- SVG link editor.
- Undo/Redo support was audited and corrected across editable fields.

## Undo / Redo status

Validated in user testing.

## Contextual value menus

No new contextual value menu was required for the main fields of this plug-in.

## Installation

Install `links-0.1.8.nbm` using the Sketsa 9.1 plug-in/module manager.

## Building from source

The module uses the NetBeans module build system. The reference build environment used during verification was:

- Sketsa SVG Editor 9.1 as the NetBeans platform;
- NetBeans 11.3 harness;
- Java 11;
- Ant.

If `sources/nbproject/platform.properties` contains machine-specific paths, update or override them so they point to your local Sketsa 9.1 installation and NetBeans harness before building.

Typical target:

```text
ant clean nbm
```

## Regression check

1. Open or create a compatible SVG document.
2. Select an element supported by this plug-in.
3. Change a plug-in value and commit/apply it if required.
4. Run Undo and verify that only the plug-in change is reverted.
5. Run Redo and verify that the new value is restored.
6. Where contextual menus exist, add/remove compatible IDs or resources and verify that the menu reflects the current document.
