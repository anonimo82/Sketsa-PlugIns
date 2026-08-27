package kiyut.sketsa.modules.symbols.integration;

import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Independent, dockable and scrollable window for the Symbols plugin.
 */
public final class SymbolsTopComponent extends TopComponent {

    private static final String PREFERRED_ID = "SymbolsTopComponent";
    private static SymbolsTopComponent instance;

    private SymbolsTopComponent() {
        setLayout(new BorderLayout());
        setName("Symbols");
        setToolTipText("Create, inspect and reuse SVG symbols.");

        SymbolsPanel panel = new SymbolsPanel();
        JScrollPane scrollPane = new JScrollPane(
                panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    public static synchronized SymbolsTopComponent getDefault() {
        if (instance == null) {
            instance = new SymbolsTopComponent();
        }
        return instance;
    }

    public static synchronized SymbolsTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            return getDefault();
        }
        if (win instanceof SymbolsTopComponent) {
            return (SymbolsTopComponent) win;
        }
        Logger.getLogger(SymbolsTopComponent.class.getName()).warning(
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
            return SymbolsTopComponent.getDefault();
        }
    }
}
