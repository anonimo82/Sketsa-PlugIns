package kiyut.sketsa.modules.audio.integration;

import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.logging.Logger;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/** Independent dockable Audio Tree editor, modeled after Sketsa's DOM Editor. */
public final class AudioTreeTopComponent extends TopComponent {
    private static final String PREFERRED_ID = "AudioTreeTopComponent";
    private static AudioTreeTopComponent instance;

    private AudioTreeTopComponent() {
        setLayout(new BorderLayout());
        setName("Audio Tree");
        setToolTipText("Hierarchical Audio authoring with stable references.");
        add(new AudioTreePanel(), BorderLayout.CENTER);
    }

    public static synchronized AudioTreeTopComponent getDefault() {
        if (instance == null) instance = new AudioTreeTopComponent();
        return instance;
    }

    public static synchronized AudioTreeTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) return getDefault();
        if (win instanceof AudioTreeTopComponent) return (AudioTreeTopComponent) win;
        Logger.getLogger(AudioTreeTopComponent.class.getName()).warning(
                "Multiple TopComponents use id " + PREFERRED_ID + "; using module singleton.");
        return getDefault();
    }

    @Override public int getPersistenceType() { return TopComponent.PERSISTENCE_ALWAYS; }
    @Override protected String preferredID() { return PREFERRED_ID; }
    @Override public Object writeReplace() { return new ResolvableHelper(); }

    private static final class ResolvableHelper implements Serializable {
        private static final long serialVersionUID = 1L;
        public Object readResolve() { return AudioTreeTopComponent.getDefault(); }
    }
}
