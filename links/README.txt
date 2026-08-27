SKETSA LINKS 0.1.1
===================

Target: Sketsa 9.1 / NetBeans 11.3 / JDK 11

0.1.1 is a fix-only build.

Fix:
Existing-link updates are now represented as structural replacement of the
<a> wrapper, preserving the original child nodes. This allows Sketsa's
DOMUndoManager to record URL / Target / Title edits for Undo/Redo.

No feature additions.
