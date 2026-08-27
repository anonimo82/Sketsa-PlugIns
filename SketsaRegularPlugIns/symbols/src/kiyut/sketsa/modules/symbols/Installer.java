package kiyut.sketsa.modules.symbols;

import org.openide.modules.ModuleInstall;

/**
 * Module lifecycle hook.
 *
 * The plugin UI is registered as an independent NetBeans TopComponent
 * through layer.xml; no controls are injected into Sketsa's Properties window.
 */
public final class Installer extends ModuleInstall {
    @Override
    public void restored() {
        // Window-system registration is handled by the module layer.
    }
}
