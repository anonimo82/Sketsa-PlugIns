package kiyut.sketsa.modules.audio.integration;

import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/** Installs the plugin as an independent dockable TopComponent instead of embedding it in another panel. */
public final class AudioIntegrator {
    private AudioIntegrator() {}

    public static boolean install() {
        try {
            WindowManager wm = WindowManager.getDefault();
            AudioTopComponent panel = AudioTopComponent.getDefault();
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
