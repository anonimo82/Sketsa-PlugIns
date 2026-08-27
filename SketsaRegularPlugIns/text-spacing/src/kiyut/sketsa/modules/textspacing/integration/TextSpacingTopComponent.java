package kiyut.sketsa.modules.textspacing.integration;

import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Independent, dockable and scrollable window for the Text Spacing plugin.
 */
public final class TextSpacingTopComponent extends TopComponent {

    private static final String PREFERRED_ID = "TextSpacingTopComponent";
    private static TextSpacingTopComponent instance;

    private TextSpacingTopComponent() {
        setLayout(new BorderLayout());
        setName("Text Spacing");
        setToolTipText("Edit SVG text spacing and typography attributes.");

        TextSpacingPanel panel = new TextSpacingPanel();
        JScrollPane scrollPane = new JScrollPane(
                panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    public static synchronized TextSpacingTopComponent getDefault() {
        if (instance == null) {
            instance = new TextSpacingTopComponent();
        }
        return instance;
    }

    public static synchronized TextSpacingTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            return getDefault();
        }
        if (win instanceof TextSpacingTopComponent) {
            return (TextSpacingTopComponent) win;
        }
        Logger.getLogger(TextSpacingTopComponent.class.getName()).warning(
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
            return TextSpacingTopComponent.getDefault();
        }
    }
}
