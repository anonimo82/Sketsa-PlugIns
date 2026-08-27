package kiyut.sketsa.modules.audio.integration;

import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Independent, dockable and scrollable window for the Audio plugin.
 */
public final class AudioTopComponent extends TopComponent {

    private static final String PREFERRED_ID = "AudioTopComponent";
    private static AudioTopComponent instance;

    private AudioTopComponent() {
        setLayout(new BorderLayout());
        setName("Audio");
        setToolTipText("Author Web Audio behavior and runtime bindings.");

        AudioPanel panel = new AudioPanel();
        JScrollPane scrollPane = new JScrollPane(
                panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    public static synchronized AudioTopComponent getDefault() {
        if (instance == null) {
            instance = new AudioTopComponent();
        }
        return instance;
    }

    public static synchronized AudioTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            return getDefault();
        }
        if (win instanceof AudioTopComponent) {
            return (AudioTopComponent) win;
        }
        Logger.getLogger(AudioTopComponent.class.getName()).warning(
                "Multiple TopComponents use id " + PREFERRED_ID + "; using module singleton.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    @Override
    public Object writeReplace() {
        return new ResolvableHelper();
    }

    private static final class ResolvableHelper implements Serializable {
        private static final long serialVersionUID = 1L;
        public Object readResolve() {
            return AudioTopComponent.getDefault();
        }
    }
}
