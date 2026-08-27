package kiyut.sketsa.modules.input.integration;

import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Independent, dockable and scrollable window for the Input plugin.
 */
public final class InputTopComponent extends TopComponent {

    private static final String PREFERRED_ID = "InputTopComponent";
    private static InputTopComponent instance;

    private InputTopComponent() {
        setLayout(new BorderLayout());
        setName("Input");
        setToolTipText("Author keyboard, pointer, touch, gamepad and on-screen input mappings.");

        InputPanel panel = new InputPanel();
        JScrollPane scrollPane = new JScrollPane(
                panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    public static synchronized InputTopComponent getDefault() {
        if (instance == null) {
            instance = new InputTopComponent();
        }
        return instance;
    }

    public static synchronized InputTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            return getDefault();
        }
        if (win instanceof InputTopComponent) {
            return (InputTopComponent) win;
        }
        Logger.getLogger(InputTopComponent.class.getName()).warning(
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
            return InputTopComponent.getDefault();
        }
    }
}
