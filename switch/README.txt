SKETSA SWITCH 0.2.6
====================

Fix-only language-field editing regression.

The systemLanguage field now has a dirty state. Automatic DOM synchronization
is suspended while an uncommitted user edit exists, preventing the Undo/Redo
sync timer from overwriting text before Update Language is clicked.
