package kiyut.sketsa.modules.switcher.integration;

import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Independent, dockable and scrollable window for the Switch plugin.
 */
public final class SwitchTopComponent extends TopComponent {

    private static final String PREFERRED_ID = "SwitchTopComponent";
    private static SwitchTopComponent instance;

    private SwitchTopComponent() {
        setLayout(new BorderLayout());
        setName("Switch");
        setToolTipText("Author and edit SVG switch elements.");

        SwitchPanel panel = new SwitchPanel();
        JScrollPane scrollPane = new JScrollPane(
                panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    public static synchronized SwitchTopComponent getDefault() {
        if (instance == null) {
            instance = new SwitchTopComponent();
        }
        return instance;
    }

    public static synchronized SwitchTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            return getDefault();
        }
        if (win instanceof SwitchTopComponent) {
            return (SwitchTopComponent) win;
        }
        Logger.getLogger(SwitchTopComponent.class.getName()).warning(
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
            return SwitchTopComponent.getDefault();
        }
    }
}
