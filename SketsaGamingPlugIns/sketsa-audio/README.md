# Sketsa Audio Plugin / Audio Tree

**Final module build:** 0.16.3  
**Audio Tree runtime:** 0.16.3  
**Audio Tree runtime contract:** 1.2  
**Audio Tree IR:** 1.5  
**Target application:** Sketsa SVG Editor 9.1  
**Implementation:** Java / NetBeans Platform plugin, with JavaScript Web Audio runtimes for exported HTML

## Overview

The Sketsa Audio plugin adds audio authoring and Web Audio runtime export to Sketsa SVG Editor. Its main architecture is the **Audio Tree**, a hierarchical audio model stored directly inside the SVG document as metadata and compiled at export time into a browser-side Web Audio graph.

The plugin is intentionally split between two layers:

- **Editor/plugin layer:** implemented in Java against the Sketsa/NetBeans APIs. It provides the Audio and Audio Tree windows, inspectors, SVG persistence, validation, editing commands, import/export tools, and runtime packaging.
- **Export/runtime layer:** implemented in JavaScript and based on the browser **Web Audio API**. The editor compiles the SVG Audio Tree into an intermediate representation (IR), then packages that IR and the runtime next to the exported HTML.

The Audio Tree is the primary and preferred model. A legacy object-level Audio panel is still available as a fallback for SVG documents that do not contain an Audio Tree.

---

## Main Features

### 1. Persistent Audio Tree stored inside SVG

An Audio Tree is stored inside SVG `<metadata>` under the namespace:

```xml
urn:sketsa:audio-tree:1
```

The root metadata element uses the stable ID:

```text
sketsa-audio-tree
```

Ownership is represented by the XML hierarchy itself. Audio nodes are nested under their owning parent nodes, while non-hierarchical relationships are represented by first-class reference elements.

This design means the audio model travels with the SVG document and survives save/reopen without a separate project database.

### 2. Stable IDs and references

Every owned node and reference uses a persistent ID. References address targets by `targetId`, so routing, sends, modulation, events, and other non-hierarchical links are not tied to transient UI state.

The editor supports:

- stable node IDs;
- stable reference IDs;
- broken-reference detection;
- **Go to Target** navigation;
- reference preservation after target deletion;
- ID regeneration when branches are duplicated or imported;
- internal-reference remapping during duplication/import;
- preservation of external references when they intentionally point outside the copied branch.

### 3. Hierarchical routing

The Audio Tree compiles ownership into a default Web Audio routing rule:

```text
child -> parent
```

This provides a compact, readable audio graph in the editor. Explicit references can add non-hierarchical connections such as sends, modulation, event bindings, and sidechain metadata.

Routing is validated before runtime export. Invalid routing can block export.

### 4. Core Web Audio nodes

The Audio Tree supports these node types:

| Audio Tree type | Runtime behavior |
| --- | --- |
| `master` | Pass-through gain node connected to `AudioContext.destination` |
| `bus` | Pass-through gain node used as a hierarchical routing container |
| `return` | Gain node used as a shared return path |
| `source` | `OscillatorNode` feeding an output `GainNode` |
| `gain` | `GainNode` |
| `pan` | `StereoPannerNode`, with pass-through fallback when unavailable |
| `filter` | `BiquadFilterNode` |
| `compressor` | `DynamicsCompressorNode` |
| `analyser` | `AnalyserNode` |
| `delay` | `DelayNode` |
| `reverb` | `ConvolverNode` using a generated impulse response |
| `lfo` | Oscillator + gain control path used for modulation |
| `automation` | Automation controller compiled into scheduled `AudioParam` changes |
| `effect` | Structural/pass-through audio node |
| `group` | Structural/pass-through audio node |

### 5. Node parameters exposed by the inspector

The Audio Tree inspector exposes the parameters relevant to the selected node type.

#### Source

- waveform: sine, square, sawtooth, triangle;
- frequency;
- output level.

#### Gain

- gain.

#### Stereo Pan

- pan.

#### Filter

- filter type: lowpass, highpass, bandpass, lowshelf, highshelf, peaking, notch, allpass;
- frequency;
- Q;
- gain.

#### Compressor

- threshold;
- knee;
- ratio;
- attack;
- release.

#### Analyser

- FFT size;
- smoothing.

#### Return

- return gain.

#### Delay

- delay time.

#### Reverb

- generated impulse duration;
- decay.

#### LFO

- waveform;
- frequency;
- depth.

#### Automation

- serialized time/value curve, for example:

```text
0:0.15,0.5:0.65,1.0:0.25
```

At runtime, valid points are sorted by time and scheduled on the target `AudioParam` using `setValueAtTime` followed by linear ramps.

---

## Reference Types

References are first-class Audio Tree items. The final plugin recognizes these roles:

### `route`

Creates an explicit audio connection from the owning node to the target node.

### `send`

Creates a parallel send path. The reference has an `amount`, compiled to a dedicated `GainNode` between the source and target.

Send amounts are validated and clamped to the supported range when necessary.

### `modulation`

Connects a control source, typically an LFO, to a specific exposed `AudioParam` on another node.

Supported target parameters include, depending on node type:

- `gain`;
- `pan`;
- `frequency`;
- `q` / `Q`;
- `delayTime`;
- compressor `threshold`, `knee`, `ratio`, `attack`, and `release`.

A modulation reference also carries an independent modulation `amount`.

### `event` and `event-target`

Bind a neutral runtime event to an audio action. Event bindings are independent of the plugin that produced the event.

Supported actions are:

- `trigger`;
- `start`;
- `stop`;
- `toggle`;
- `set-param`.

`set-param` supports numeric mapping:

```text
mappedValue = value * scale + offset
```

Optional `min` and `max` values clamp the result.

### `sidechain`

Stores and validates a sidechain relationship in the IR. In the final 0.16.x runtime it is deliberately **reference-only metadata** rather than a generic DSP connection, because the Web Audio API does not provide one universal sidechain input applicable to every node type.

This preserves the architectural contract without pretending that unsupported DSP behavior exists.

---

## Sends, Returns, and Shared Effects

The Audio Tree supports shared processing paths instead of forcing every effect to live inline in a single parent chain.

Typical structure:

```text
Master
|- Main Bus
|  |- Source -> Gain -> Filter
|  `- send reference -> Shared Return
`- Shared Return
   `- Delay / Reverb
```

A `send` reference creates a parallel connection with its own amount. Returns are normal owned nodes and can contain shared Delay/Reverb processing.

This allows multiple branches to target the same return while preserving stable IDs and explicit references.

---

## Modulation and Automation

### LFO modulation

An LFO is an owned controller node. It is not required to be part of the main audio route.

One LFO can modulate multiple target parameters through multiple `modulation` references, each with an independent amount.

At runtime an LFO is implemented as an oscillator feeding a gain control stage, which is then connected to the chosen `AudioParam`.

### Automation

An Automation node stores a time/value curve and targets a parameter through a modulation-style reference.

The compiler validates the target node and parameter. The runtime then schedules the curve against the current `AudioContext` time.

---

## Runtime Integration

When an SVG contains a valid Audio Tree, the Audio Tree is the **authoritative audio backend** for exported HTML.

The exporter avoids creating two independent audio engines for the same document:

- with an Audio Tree present: the Audio Tree runtime is used as the primary backend;
- without an Audio Tree: legacy object-level audio can still use the legacy runtime.

The Audio Tree path exposes both:

```js
window.SketsaAudioTree
```

and the public facade:

```js
window.SketsaAudio
```

`window.SketsaAudio` identifies the backend as:

```text
backend = "audio-tree"
isTreePrimary = true
```

Both facades delegate to the same Web Audio graph and the same `AudioContext`.

### Public runtime API

The main runtime methods are:

```js
SketsaAudio.build()
SketsaAudio.resume()
SketsaAudio.snapshot()
SketsaAudio.getContext()
SketsaAudio.emitRuntimeEvent(payload)
```

The same core methods are available through `SketsaAudioTree`.

`build()` creates the graph once and returns a diagnostic snapshot. `resume()` builds if necessary and resumes the browser audio context. `snapshot()` exposes the current compiled/runtime state for diagnostics and automated tests.

---

## Runtime Event Bus and Plugin Interoperability

Audio interoperability is intentionally **decoupled** from other Sketsa plugins.

The Audio plugin does not need Java dependencies on Input or Physics to receive runtime control. Instead, exported runtimes communicate through the neutral DOM event:

```text
sketsa:runtime:event
```

A typical payload can contain:

```js
{
  source: "physics",
  type: "collision",
  name: "ball-hit-wall",
  value: 1,
  data: { ... }
}
```

Audio bindings match the event `name` and apply the configured action to their target.

The Audio runtime can also publish events and exposes:

```js
SketsaAudio.emitRuntimeEvent({...})
```

This allows Physics, Input, custom exported code, or future plugins to communicate with Audio through a stable runtime contract rather than direct plugin dependencies.

Audio Tree runtime events also emit `sketsa:audio-tree:*` diagnostic events and mirror appropriate notifications onto the neutral runtime bus.

---

## Companion Runtimes

Both runtime export workflows support the option:

**Include companion runtimes**

- The legacy Audio panel exposes this option directly in its export controls.
- The Audio Tree now exposes the same option in the **Export Runtime** file chooser under **Runtime options**.
- The option is enabled by default.

When enabled, the exporter looks for supported companion metadata in the SVG and packages compatible runtime assets alongside the audio runtime. The current bundled companion exporter handles the Sketsa Physics companion runtime when Physics metadata is present.

The Audio Tree still remains independent of the Physics plugin at compile time; interoperability happens through exported runtime contracts and the neutral event bus.

Disabling **Include companion runtimes** exports only the audio-side runtime package, even when companion metadata is present.

---

## Runtime Export

Use **Audio Tree -> Export Runtime** to generate a standalone HTML entry point and a sibling assets directory.

For an Audio Tree export, the package contains versioned assets such as:

```text
<name>.html
<name>-assets/
    sketsa-audio-tree-ir-0.16.3.js
    sketsa-audio-tree-runtime-0.16.3.js
    ...optional companion runtime assets...
```

The generated HTML contains metadata identifying the runtime, contract, and primary backend.

### Export behavior

The exporter:

- validates the Audio Tree before export;
- blocks export when routing/compiler errors are present;
- compiles SVG metadata to a JavaScript IR file;
- packages the versioned Audio Tree runtime locally;
- avoids CDN dependencies;
- moves executable inline SVG scripts into normal HTML script elements after runtime loading;
- removes stale Audio Tree runtime assets from previous exports;
- removes stale legacy audio runtime assets when Audio Tree is primary;
- supports repeated export to the same HTML filename;
- loads Audio before Physics companion scripts so the neutral event consumer is ready before simulation begins.

### Browser audio activation

Browsers can suspend `AudioContext` until a user gesture. Exported audio pages include an **Enable Audio** control that resumes the active backend.

---

## Validation

**Validate Routing** compiles and checks the current Audio Tree without exporting it.

The validation area reports separate severity classes:

```text
ERROR
WARNING
INFO
```

Examples of final validation checks include:

- duplicate stable IDs;
- missing/broken reference targets;
- routing cycles;
- invalid send amounts;
- unsupported modulation target parameters;
- invalid event actions;
- event references without an event name;
- `set-param` references without a target parameter;
- invalid numeric event mapping fields;
- `min > max` event mapping;
- invalid automation target references;
- malformed/empty automation curves;
- sidechain targets that cannot be resolved;
- unsupported/stored reference roles;
- successful graph summary information.

Errors make the compiled IR invalid and can block runtime export. Warnings remain visible but are not necessarily fatal.

---

## Audio Tree Editing Tools

The Audio Tree window provides the following core operations:

- **Create Audio Tree**;
- **Add Child**;
- **Add Reference**;
- **Remove**;
- move item **Up** / **Down**;
- **Duplicate Branch**;
- **Export Tree**;
- **Import Tree**;
- **Validate Routing**;
- **Export Runtime**;
- **Go to Target** for references;
- inspector editing with **Apply Properties**.

### Duplicate Branch

Duplicating a branch:

- clones the selected branch;
- generates new IDs for copied nodes/references;
- remaps references whose targets are inside the copied branch;
- preserves references whose targets are outside the copied branch.

This allows reusable subtrees without accidental ID collisions.

### Export Tree / Import Tree

A selected Audio Tree branch can be exported as an XML fragment and later imported under another Audio Tree container.

Import handles ID collisions by regenerating colliding IDs and remapping internal references. External references are intentionally preserved where possible; unresolved references remain diagnosable rather than being silently redirected.

### Deletion and broken references

Deleting a referenced target preserves references so they can be reported as broken instead of silently disappearing. This makes damaged routing visible to the user and to validation.

---

## Save/Reopen and SVG Persistence

The SVG DOM is the source of truth for the authoring model. Node parameters, IDs, hierarchy, references, event mappings, automation curves, and other Audio Tree metadata are serialized into the SVG.

The final robustness work specifically covers stable IDs and reference integrity across normal editor persistence, branch duplication/import, and runtime compilation.

---

## Legacy Object-Level Audio Panel

The plugin also retains the older object-level Audio workflow for SVG elements with `data-sketsa-audio-*` attributes. This path is intentionally treated as legacy compatibility when an Audio Tree is not present.

Supported object-level metadata includes fields for:

- audio source path;
- autoplay;
- loop;
- volume;
- playback rate;
- start offset;
- event ID;
- mute;
- pan and position-derived panning;
- bus assignment;
- master/bus volume and mute;
- trigger source/type/event/action.

The legacy exporter packages local audio assets, deduplicates assets by SHA-256 digest, generates deterministic runtime IDs for duplicated SVG objects, embeds a data fallback for local-file use, and marks missing audio assets without making the complete SVG export impossible.

If a document contains both legacy object metadata and an Audio Tree, the Audio Tree takes precedence as the primary audio runtime.

---

## Architecture Summary

The final architecture is deliberately layered:

```text
Sketsa SVG document
      |
      |  SVG metadata: Audio Tree
      v
Java Audio Tree editor / validator
      |
      |  compile
      v
Audio Tree IR 1.5
      |
      |  export local assets
      v
Web Audio runtime 0.16.3 / contract 1.2
      |
      +--> AudioContext / Web Audio graph
      |
      +--> sketsa:runtime:event bus
                |
                +--> compatible companion runtimes / custom code
```

The Java plugin owns editor integration and authoring. The browser runtime owns Web Audio execution. Physics/Input interoperability is intentionally contract-based rather than implemented as direct Java plugin dependencies.

---

## Eight-Milestone Audio Tree Roadmap

The final Audio Tree is the result of the original eight-milestone roadmap:

1. **T1 - Tree Core**  
   Hierarchical tree, owned nodes, references, stable IDs, SVG serialization, save/reopen, broken references, Go to Target, editor history integration.

2. **T2 - Routing & Compilation**  
   Tree-to-Web-Audio compilation, child-to-parent routing, route/send references, routing validation and cycle detection.

3. **T3 - Core Audio Nodes**  
   Source, Gain, Stereo Pan, Filter, Compressor, Analyser and dedicated inspector parameters.

4. **T4 - Sends / Returns / Shared FX**  
   Parallel sends with amount, Return nodes, shared effects, Delay and Reverb.

5. **T5 - Modulation & Automation**  
   LFO, automation curves, modulation references and parameter-specific targeting.

6. **T6 - Runtime Integration**  
   Audio Tree promoted to the primary exported audio backend, unified public facade and one shared AudioContext; legacy audio retained as fallback.

7. **T7 - Interop**  
   Neutral runtime event bus, event/event-target references, parameter mapping, trigger actions and sidechain metadata without direct Input/Physics plugin dependencies.

8. **T8 - Robustness & Consolidation**  
   Branch duplication, ID/reference remapping, branch import/export, final validation hardening, repeated export cleanup, and cross-milestone regression coverage.

---

## Current Runtime Versions

The final package uses:

```text
NetBeans module specification: 0.16.3
Audio Tree runtime:            0.16.3
Runtime contract:              1.2
Audio Tree IR:                 1.5
Legacy audio runtime:          0.8.2
```

---

## Installation

1. Start Sketsa SVG Editor 9.1.
2. Open the NetBeans/Sketsa plugin manager.
3. Install `sketsa-audio-0.16.3.nbm` from this package.
4. Restart Sketsa when requested.
5. Open the Audio Tree window from the Sketsa window/actions menu.

The NBM is configured as a restart-requiring module.

---

## Building from Source

The archive includes the complete module source tree.

Required build environment:

- JDK capable of compiling Java 11 source;
- NetBeans Platform harness compatible with the Sketsa 9.1 platform;
- Sketsa SVG Editor 9.1 installation/cluster.

The project is a standard NetBeans module project. `nbproject/platform.properties` contains the original Windows platform identifiers/paths and can be overridden for another local environment.

Example conceptually:

```text
ant \
  -Dnbplatform.Sketsa_SVG_Editor_9.1.harness.dir=<netbeans-harness> \
  -Dnbplatform.Sketsa_SVG_Editor_9.1.netbeans.dest.dir=<Sketsa-9_1> \
  clean nbm
```

The generated NBM is placed under the module build output and can be copied/renamed for distribution.

---

## Package Contents

This distribution contains:

- complete Java source code;
- JavaScript legacy and Audio Tree runtimes;
- NetBeans module project files;
- module manifest and layer registration;
- license files;
- compiled NBM installer;
- build output/log;
- final T8 test SVG and instructions;
- this `README.md`.

---

## Important Implementation Notes / Boundaries

- The Audio Tree `source` node in this final roadmap is oscillator-based. Buffer/media-source work is not part of the certified original T1-T8 roadmap.
- The Reverb node uses a generated impulse response rather than loading a convolution asset from disk.
- Sidechain references are stored/validated and surfaced in the runtime snapshot as a contract, but are not treated as a universal Web Audio DSP sidechain input.
- Browser autoplay policies still apply; the exported **Enable Audio** control exists for this reason.
- Audio Tree is authoritative when present; legacy object-level audio is a fallback, not a second parallel graph.
- Companion runtime packaging is optional and controlled by **Include companion runtimes**. The currently implemented companion exporter handles Physics metadata/runtime packaging when present.
- No CDN is required for Audio Tree runtime export; generated runtime assets are local to the exported HTML package.

---

## License

See `LICENSE` and `legal/LICENSE.txt` included in the distribution.
