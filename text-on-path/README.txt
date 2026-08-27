SKETSA TEXT ON PATH 0.1.1
==========================

Fix-only build for:
- startOffset visual update
- isolated Undo/Redo for Attach/Update/Detach

The implementation now swaps complete <text> snapshots and adds one explicit
undo edit to Sketsa DOMUndoManager.
