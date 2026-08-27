Sketsa Audio 0.8.2 — Joint Physics + Audio Export Consolidation

Purpose
-------
This is a consolidation-only release. It does not add new Audio or Physics behavior.
It adds one export option to the Audio Properties panel:

    Include companion runtimes

When enabled, the Audio exporter inspects the SVG. If Physics metadata is present,
it includes the certified Physics 0.9.5 runtime and Matter.js 0.20.0 assets in the same
HTML export, alongside Audio 0.8.2.

Runtime order
-------------
Audio runtime is loaded before Physics so Audio is already listening to
sketsa:runtime:event when Physics starts the simulation.

The existing neutral contract is unchanged:

    Physics collisionStart
        -> sketsa:runtime:event
        -> Audio binding
        -> sketsa:audio:action
        -> play

Test
----
Open SKETSA_PHYSICS_AUDIO_CONSOLIDATION_AUTOTEST.svg in Sketsa.
Export from the Audio panel with "Include companion runtimes" checked.
Open the exported HTML and wait for AUTOTEST PASS.

The test checks only features already introduced and certified in Physics 0.9.5 and
Audio 0.8.1: both public APIs, real collision event, neutral bus, Audio action/play,
local assets, and runtime metadata.

Note
----
Matter.js follows the existing Physics P7+ behavior: it is copied from the verified
local Physics cache; if the cache is not populated yet, the first Physics-capable
export may require one Internet connection to download and SHA-512 verify Matter.js.
