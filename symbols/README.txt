SKETSA SYMBOLS 0.1.1
=====================

Fix-only build.

Existing <use> instances are now explicitly refreshed after a referenced
<symbol> definition is created/updated, and during Undo/Redo of that definition.

This addresses Batik rendering-cache behavior where the DOM changed correctly
but the visual instance stayed stale until Update Use was pressed.
