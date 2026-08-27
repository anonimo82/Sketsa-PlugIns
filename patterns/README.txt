SKETSA PATTERNS 0.3.0
======================

Private-application model.

The Pattern panel is only a draft editor.
Every Apply Fill / Apply Stroke creates a fresh immutable <pattern> definition
with a unique ID such as checker-1, checker-2, checker-3.

Later draft changes can never overwrite already-applied patterns.

Undo removes the exact private pattern created by that Apply and restores the
previous paint state. Redo restores both.
