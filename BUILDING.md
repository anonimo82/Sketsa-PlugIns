# Building the Sketsa Plug-ins

This document describes the common build environment for all plug-ins in this repository.

## Supported reference environment

The plug-ins were designed for the following stack:

- **JDK 11 (64-bit)**
- **Apache NetBeans IDE 11.3**
- **Sketsa SVG Editor 9.1**
- NetBeans Platform / Harness supplied with NetBeans 11.3

The projects are normal **NetBeans Module** projects. They are not JavaScript plug-ins. Audio, Input and Physics contain JavaScript only as browser runtime code generated/copied by their Java authoring modules.

## Recommended Windows paths

The historic/reference setup uses:

```text
C:\Program Files\NetBeans-11.3\netbeans
C:\Program Files\Kiyut\Sketsa-9_1
```

Using these paths allows the supplied `nbproject/platform.properties` files to work with little or no editing.

## 1. Install JDK 11

Verify:

```bat
java -version
javac -version
```

Both should report Java 11.

Do not build these modules with a newer bytecode target. The resulting class files must remain compatible with the Java 11 runtime used by Sketsa.

## 2. Install NetBeans 11.3

Confirm that the Harness exists:

```text
C:\Program Files\NetBeans-11.3\netbeans\harness
```

## 3. Install Sketsa 9.1

Confirm that the application contains at least:

```text
C:\Program Files\Kiyut\Sketsa-9_1\platform
C:\Program Files\Kiyut\Sketsa-9_1\sketsa
```

The Sketsa installation acts as the NetBeans Platform against which the modules compile.

## 4. Register the Sketsa platform in NetBeans

In NetBeans:

1. Open **Tools → NetBeans Platforms**.
2. Click **Add Platform**.
3. Select the Sketsa 9.1 installation directory:
   ```text
   C:\Program Files\Kiyut\Sketsa-9_1
   ```
4. Name it, for example:
   ```text
   Sketsa SVG Editor 9.1
   ```
5. Use the Harness from NetBeans 11.3:
   ```text
   C:\Program Files\NetBeans-11.3\netbeans\harness
   ```

The supplied projects use the platform key:

```properties
nbplatform.active=Sketsa_SVG_Editor_9.1
```

If your NetBeans platform has a different internal name, update `nbproject/platform.properties` accordingly.

## 5. Platform properties

A typical `nbproject/platform.properties` is:

```properties
nbplatform.active=Sketsa_SVG_Editor_9.1
nbplatform.Sketsa_SVG_Editor_9.1.harness.dir=C:\\Program Files\\NetBeans-11.3\\netbeans\\harness
nbplatform.Sketsa_SVG_Editor_9.1.netbeans.dest.dir=C:\\Program Files\\Kiyut\\Sketsa-9_1
```

Do not solve missing NetBeans/Sketsa classes by adding random JAR files manually. Missing classes such as `org.openide.nodes.Node` normally indicate that the project platform or `project.xml` dependency list is incomplete/misconfigured.

## 6. Build a plug-in in NetBeans

Open one plug-in directory as a NetBeans Module project, for example:

```text
SketsaRegularPlugIns\links
```

or:

```text
SketsaGamingPlugIns\sketsa-physics
```

Then:

1. Select **JDK 11** for the project/platform.
2. Right-click the project.
3. Choose **Clean and Build**.
4. Confirm:
   ```text
   BUILD SUCCESSFUL
   ```
5. Right-click the project again.
6. Choose **Create NBM**.

`Clean and Build` compiles the module, but **Create NBM** is the explicit action that creates the installable NetBeans Module package.

The generated NBM is normally found in the project build/output area.

## 7. Build from the command line

The projects use the NetBeans Ant Harness. If `ant` is available:

```bat
cd /d C:\path\to\SketsaPlugIns\SketsaRegularPlugIns\links
ant clean build
```

To build the NBM, use the Harness target exposed by the NetBeans module project, commonly:

```bat
ant nbm
```

Depending on local NetBeans configuration, the IDE's **Create NBM** command is the most reliable way to invoke the exact packaging target.

If `ant` is not globally installed, use the Ant/Harness bundled with NetBeans through the IDE.

## 8. Required module dependencies

The individual `nbproject/project.xml` files declare the exact dependencies. Common dependencies include:

- `kiyut.sketsa`
- `kiyut.sketsa.modules.batik`
- `org.openide.awt`
- `org.openide.modules`
- `org.openide.nodes`
- `org.openide.util`
- `org.openide.util.lookup`
- `org.openide.util.ui`
- `org.openide.windows`

Do not remove dependencies merely because a specific source file does not appear to import them; NetBeans module metadata and transitive Sketsa APIs may still require them.

## 9. Independent tool windows

With this repository revision, all plug-ins use NetBeans `TopComponent` windows. Their UI registration is stored in each module's `layer.xml`, with the window component settings and a **Window** menu action.

If a panel does not appear after installing an NBM:

1. restart Sketsa;
2. look under the **Window** menu;
3. inspect Sketsa's NetBeans user log;
4. verify that the NBM version is newer than the installed module;
5. verify Java/module dependencies.

The plug-ins no longer need to modify `PropertiesTopComponent`.

## 10. Installing a locally built NBM

Use Sketsa's plug-in manager when possible:

1. open the plug-in manager;
2. add the generated `.nbm`;
3. install it;
4. restart Sketsa.

For development, NetBeans Module project run/install facilities may also be used if the project is configured against the Sketsa platform.

## 11. User module location and logs

A typical Sketsa user module location is:

```text
%APPDATA%\sketsa\9.1\modules
```

The application log/user directory is also under the Sketsa user profile. Check the log first when a module installs but does not load.

## 12. Release checklist

Before publishing an NBM:

- compile with JDK 11;
- verify the module specification version;
- verify `manifest.mf` and `nbproject/project.xml`;
- run the plug-in's included SVG regression/autotest where available;
- verify the independent window opens from **Window**;
- resize the window and verify scrolling;
- create and install the NBM;
- restart Sketsa and repeat a smoke test;
- keep the matching source and NBM together in the release.

## 13. Repository distribution

This archive already contains prebuilt NBM files in:

```text
dist\nbm
```

and next to each corresponding source project. The source remains the authoritative implementation; the bundled NBM files are convenience builds for the reference Sketsa 9.1 / Java 11 environment.
