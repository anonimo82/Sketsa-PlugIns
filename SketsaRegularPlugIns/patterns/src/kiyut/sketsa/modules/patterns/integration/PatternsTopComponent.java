package kiyut.sketsa.modules.patterns.integration;

import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Independent, dockable and scrollable window for the Patterns plugin.
 */
public final class PatternsTopComponent extends TopComponent {

    private static final String PREFERRED_ID = "PatternsTopComponent";
    private static PatternsTopComponent instance;

    private PatternsTopComponent() {
        setLayout(new BorderLayout());
        setName("Patterns");
        setToolTipText("Create and manage SVG pattern resources.");

        PatternsPanel panel = new PatternsPanel();
        JScrollPane scrollPane = new JScrollPane(
                panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    public static synchronized PatternsTopComponent getDefault() {
        if (instance == null) {
            instance = new PatternsTopComponent();
        }
        return instance;
    }

    public static synchronized PatternsTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            return getDefault();
        }
        if (win instanceof PatternsTopComponent) {
            return (PatternsTopComponent) win;
        }
        Logger.getLogger(PatternsTopComponent.class.getName()).warning(
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
            return PatternsTopComponent.getDefault();
        }
    }
}
