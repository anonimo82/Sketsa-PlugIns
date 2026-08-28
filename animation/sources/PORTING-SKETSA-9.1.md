# Animation Editor 1.0.0 — SMIL M5 Complete

Target: Sketsa SVG Editor 9.1 / Java 11 / Batik 1.10.

This completes the planned SMIL redesign:
M1 core tracks and GVT preview
M2 mature timeline/keyframes
M3 easing and timing
M4 motion and discrete state
M5 sequencing and final timing controls

M5 adds Track ID, free Begin expressions, syncbase timing, event timing storage,
End, RepeatDur and Restart. Deterministic clock/syncbase timing is previewed
locally; event-triggered Begin values are preserved as native SMIL and are not
invented by numeric scrub before the event occurs.
