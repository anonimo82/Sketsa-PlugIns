package kiyut.sketsa.modules.textonpath.integration;

import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Independent, dockable and scrollable window for the Text on Path plugin.
 */
public final class TextOnPathTopComponent extends TopComponent {

    private static final String PREFERRED_ID = "TextOnPathTopComponent";
    private static TextOnPathTopComponent instance;

    private TextOnPathTopComponent() {
        setLayout(new BorderLayout());
        setName("Text on Path");
        setToolTipText("Attach SVG text to paths and edit textPath settings.");

        TextOnPathPanel panel = new TextOnPathPanel();
        JScrollPane scrollPane = new JScrollPane(
                panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    public static synchronized TextOnPathTopComponent getDefault() {
        if (instance == null) {
            instance = new TextOnPathTopComponent();
        }
        return instance;
    }

    public static synchronized TextOnPathTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            return getDefault();
        }
        if (win instanceof TextOnPathTopComponent) {
            return (TextOnPathTopComponent) win;
        }
        Logger.getLogger(TextOnPathTopComponent.class.getName()).warning(
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
            return TextOnPathTopComponent.getDefault();
        }
    }
}
