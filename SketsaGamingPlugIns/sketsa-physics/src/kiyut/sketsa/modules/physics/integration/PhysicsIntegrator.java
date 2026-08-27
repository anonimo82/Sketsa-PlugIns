package kiyut.sketsa.modules.physics.integration;

import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/** Installs the plugin as an independent dockable TopComponent instead of embedding it in another panel. */
public final class PhysicsIntegrator {
    private PhysicsIntegrator() {}

    public static boolean install() {
        try {
            WindowManager wm = WindowManager.getDefault();
            PhysicsTopComponent panel = PhysicsTopComponent.getDefault();
            if (!panel.isOpened()) {
                TopComponent anchor = wm.findTopComponent("PropertiesTopComponent");
                Mode mode = anchor == null ? null : wm.findMode(anchor);
                if (mode != null) mode.dockInto(panel);
                panel.open();
            }
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
