package kiyut.sketsa.modules.input.integration;

import java.awt.BorderLayout;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.util.Collection;
import kiyut.sketsa.cookies.SVGEditorCookie;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Independent, dockable and scrollable window for the Input plugin.
 */
public final class InputTopComponent extends TopComponent {

    private static final String PREFERRED_ID = "InputTopComponent";
    private static InputTopComponent instance;

    /** Keep NetBeans Undo/Redo bound to the last active SVG editor while this plugin has focus. */
    private final Lookup.Result<SVGEditorCookie> editorCookies;
    private SVGEditorCookie editorCookie;
    private UndoRedo editorUndoRedo;

    private InputTopComponent() {
        editorCookies = Utilities.actionsGlobalContext().lookupResult(SVGEditorCookie.class);
        editorCookies.addLookupListener(ev -> refreshEditorUndoRedo());
        refreshEditorUndoRedo();

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
    public UndoRedo getUndoRedo() {
        refreshEditorUndoRedo();
        return editorUndoRedo != null ? editorUndoRedo : super.getUndoRedo();
    }

    @Override
    protected void componentActivated() {
        super.componentActivated();
        refreshEditorUndoRedo();
    }

    private void refreshEditorUndoRedo() {
        Collection<? extends SVGEditorCookie> active = editorCookies.allInstances();
        if (!active.isEmpty()) {
            SVGEditorCookie cookie = active.iterator().next();
            if (cookie != editorCookie) {
                editorCookie = cookie;
                editorUndoRedo = new FocusUndoRedo(this, cookie.getUndoRedo());
            }
        } else if (editorCookie != null && !editorCookie.isOpened()) {
            editorCookie = null;
            editorUndoRedo = null;
        }
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
    /**
     * Sketsa's editor UndoRedo delegate does not notify NetBeans after undo()/redo().
     * Relay normal changes and explicitly fire after undo/redo so Redo becomes
     * enabled immediately while focus remains in this plugin TopComponent.
     */
    private static final class RefreshingUndoRedo implements UndoRedo {
        private final UndoRedo delegate;
        private final javax.swing.event.EventListenerList listeners = new javax.swing.event.EventListenerList();
        private final javax.swing.event.ChangeListener relay = e -> fireChange();

        RefreshingUndoRedo(UndoRedo delegate) {
            this.delegate = delegate;
            if (delegate != null) delegate.addChangeListener(relay);
        }

        @Override public boolean canUndo() { return delegate != null && delegate.canUndo(); }
        @Override public boolean canRedo() { return delegate != null && delegate.canRedo(); }
        @Override public void undo() throws javax.swing.undo.CannotUndoException {
            if (delegate == null) throw new javax.swing.undo.CannotUndoException();
            delegate.undo();
            fireChange();
        }
        @Override public void redo() throws javax.swing.undo.CannotRedoException {
            if (delegate == null) throw new javax.swing.undo.CannotRedoException();
            delegate.redo();
            fireChange();
        }
        @Override public String getUndoPresentationName() { return delegate == null ? "" : delegate.getUndoPresentationName(); }
        @Override public String getRedoPresentationName() { return delegate == null ? "" : delegate.getRedoPresentationName(); }
        @Override public void addChangeListener(javax.swing.event.ChangeListener l) { listeners.add(javax.swing.event.ChangeListener.class, l); }
        @Override public void removeChangeListener(javax.swing.event.ChangeListener l) { listeners.remove(javax.swing.event.ChangeListener.class, l); }
        private void fireChange() {
            javax.swing.event.ChangeEvent e = new javax.swing.event.ChangeEvent(this);
            for (javax.swing.event.ChangeListener l : listeners.getListeners(javax.swing.event.ChangeListener.class)) l.stateChanged(e);
        }
    }

}
