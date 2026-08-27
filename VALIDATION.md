# Validation Summary

The repository consolidation package was validated before archiving.

## Source compilation

All ten module source trees compile successfully with:

```text
javac --release 11
```

against the supplied Sketsa 9.1 / NetBeans public API JAR set.

Representative independent TopComponent classes report Java class-file **major version 55** (Java 11).

## NBM validation

All ten current NBMs were checked for:

- valid ZIP/NBM structure;
- embedded module JAR;
- matching module specification version;
- module layer declaration;
- layer resource present in the JAR;
- referenced `.settings` / `.wstcref` resources present;
- referenced TopComponent implementation class present;
- Apache-2.0 license metadata.

## UI integration validation

The source was checked so the nine formerly embedded panels are registered through their own NetBeans `TopComponent` and Window-menu action. Their startup `Installer` classes no longer inject controls into `PropertiesTopComponent`.

Animation retains its pre-existing independent `AnimationTopComponent` and its scrollable timeline/inspector UI.

## Note

This validation is a build/package/static integration validation. Final visual/docking behavior should still be smoke-tested in the target Sketsa 9.1 installation after installing the NBMs, because the NetBeans window layout is persisted in the user's Sketsa profile.
