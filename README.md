# Sketsa Plug-ins

A collection of Java/NetBeans modules extending **Sketsa SVG Editor 9.1**.

This repository contains two groups:

- **Regular plug-ins** — SVG authoring utilities.
- **Gaming / interactive plug-ins** — Input, Physics and Audio runtimes for interactive web exports.

All plug-ins in this repository use an **independent, dockable, scrollable NetBeans TopComponent window**. The tools no longer inject their controls into Sketsa's native Properties pane. Each window can be moved, resized, docked, closed and reopened from the **Window** menu.

## Plug-ins

| Group | Plug-in | Version in this repository | Main purpose |
|---|---|---:|---|
| Regular | Animation | 1.6.12 | SVG/SMIL timeline, tracks, keyframes and preview |
| Regular | Links | 0.1.2 | SVG hyperlinks |
| Regular | Patterns | 0.3.1 | SVG pattern resources and paint application |
| Regular | Switch | 0.2.7 | SVG `<switch>` / language alternatives |
| Regular | Symbols | 0.1.2 | SVG `<symbol>` / `<use>` authoring |
| Regular | Text on Path | 0.1.2 | SVG `<textPath>` authoring |
| Regular | Text Spacing | 0.3.5 | `letter-spacing` / `word-spacing` editing |
| Gaming | Audio | 0.8.3 | Web Audio authoring, mixing, events and export |
| Gaming | Input | 0.8.1 | Keyboard/pointer/touch/gamepad/on-screen logical actions |
| Gaming | Physics | 0.9.7 | Matter.js 2D physics authoring and export |

Each plug-in directory contains its own **English `README.md`** with detailed feature documentation.

## Repository layout

```text
SketsaPlugIns/
├─ LICENSE
├─ README.md
├─ BUILDING.md
├─ THIRD_PARTY_NOTICES.md
├─ dist/
│  └─ nbm/                  compiled installable modules
├─ SketsaRegularPlugIns/
│  ├─ animation/
│  ├─ links/
│  ├─ patterns/
│  ├─ switch/
│  ├─ symbols/
│  ├─ text-on-path/
│  └─ text-spacing/
└─ SketsaGamingPlugIns/
   ├─ sketsa-audio/
   ├─ sketsa-input/
   └─ sketsa-physics/
```

Compiled `.nbm` files are included both with the corresponding source project and in `dist/nbm/` for convenience.

## Installation

1. Start Sketsa 9.1.
2. Open the NetBeans Platform plug-in manager used by Sketsa.
3. Select **Downloaded** / **Add Plugins...** as appropriate.
4. Choose one or more `.nbm` files from `dist/nbm/`.
5. Install and restart Sketsa when requested.
6. Open/reopen a tool through the **Window** menu.

Installing a newer NBM with the same module code name upgrades the existing module.

## Building from source

See [`BUILDING.md`](BUILDING.md) for the complete JDK 11 / NetBeans 11.3 / Sketsa 9.1 setup and both GUI and Ant workflows.

## Licensing

The repository is released under the **Apache License 2.0**, a permissive license that allows commercial use, modification, distribution and private use while retaining copyright/license notices and providing an explicit patent grant.

See [`LICENSE`](LICENSE).

Third-party components and resources retain their own upstream licenses; see [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) and any plug-in-specific files under `legal/`.

## Compatibility target

The source tree and NBM builds target:

- **Sketsa SVG Editor 9.1**
- **Apache NetBeans 11.3**
- **Java / JDK 11**
