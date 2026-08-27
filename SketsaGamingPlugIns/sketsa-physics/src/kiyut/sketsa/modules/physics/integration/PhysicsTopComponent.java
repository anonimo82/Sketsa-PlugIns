package kiyut.sketsa.modules.physics.integration;

import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Independent, dockable and scrollable window for the Physics plugin.
 */
public final class PhysicsTopComponent extends TopComponent {

    private static final String PREFERRED_ID = "PhysicsTopComponent";
    private static PhysicsTopComponent instance;

    private PhysicsTopComponent() {
        setLayout(new BorderLayout());
        setName("Physics");
        setToolTipText("Author Matter.js physics properties, events and runtime export.");

        PhysicsPanel panel = new PhysicsPanel();
        JScrollPane scrollPane = new JScrollPane(
                panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    public static synchronized PhysicsTopComponent getDefault() {
        if (instance == null) {
            instance = new PhysicsTopComponent();
        }
        return instance;
    }

    public static synchronized PhysicsTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            return getDefault();
        }
        if (win instanceof PhysicsTopComponent) {
            return (PhysicsTopComponent) win;
        }
        Logger.getLogger(PhysicsTopComponent.class.getName()).warning(
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
            return PhysicsTopComponent.getDefault();
        }
    }
}
