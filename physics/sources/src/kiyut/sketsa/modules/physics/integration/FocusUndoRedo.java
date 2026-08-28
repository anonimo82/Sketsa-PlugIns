package kiyut.sketsa.modules.physics.integration;

import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.event.ItemEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.JTextComponent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import org.openide.awt.UndoRedo;

/**
 * Focus-aware Undo/Redo bridge for plugin panels.
 *
 * All editable controls in one plugin panel share ONE local history. This is
 * intentional: moving from one field to another must not make earlier control
 * edits disappear from Ctrl+Z/Ctrl+Y. Document undo is used only when focus is
 * outside an editable plugin control.
 */
final class FocusUndoRedo implements UndoRedo {
    static final String KEY = "sketsa.plugin.localUndoRedo";
    static final String USER_EDIT_KEY = "sketsa.plugin.userEdit";
    private static final String TREE_WATCH_KEY = "sketsa.plugin.undoTreeWatch";

    private final JComponent root;
    private final UndoRedo document;
    private final boolean installLocalControls;
    private final LocalUndoRedo local = new LocalUndoRedo();
    private final EventListenerList listeners = new EventListenerList();
    private final ChangeListener relay = e -> fireChange();
    private final PropertyChangeListener focusRelay = e -> fireChange();

    FocusUndoRedo(JComponent root, UndoRedo document) { this(root, document, true); }

    FocusUndoRedo(JComponent root, UndoRedo document, boolean installLocalControls) {
        this.root = root;
        this.document = document == null ? UndoRedo.NONE : document;
        this.installLocalControls = installLocalControls;
        local.addChangeListener(relay);
        if (installLocalControls) installRecursively(root);
        this.document.addChangeListener(relay);
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addPropertyChangeListener("focusOwner", focusRelay);
    }

    static void userSetText(JTextComponent field, String value) {
        if (field == null) return;
        field.putClientProperty(USER_EDIT_KEY, Boolean.TRUE);
        try { field.setText(value == null ? "" : value); }
        finally { field.putClientProperty(USER_EDIT_KEY, null); }
        field.requestFocusInWindow();
    }

    private void installRecursively(Component c) {
        if (c instanceof JComponent) installOne((JComponent)c);
        if (c instanceof Container) {
            watchContainer((Container)c);
            for (Component child : ((Container)c).getComponents()) installRecursively(child);
        }
    }

    /**
     * Keep Undo support complete when plugin panels create editors lazily.
     * Several Sketsa plugin UIs add combo/spinner/table editors after the
     * TopComponent Undo bridge has already been constructed; without this
     * listener those controls never receive an UndoableEditListener.
     */
    private void watchContainer(Container container) {
        if (!(container instanceof JComponent)) return;
        JComponent jc = (JComponent)container;
        if (Boolean.TRUE.equals(jc.getClientProperty(TREE_WATCH_KEY))) return;
        jc.putClientProperty(TREE_WATCH_KEY, Boolean.TRUE);
        container.addContainerListener(new ContainerAdapter() {
            @Override public void componentAdded(ContainerEvent e) {
                installRecursively(e.getChild());
                fireChange();
            }
        });
    }

    private void installOne(JComponent c) {
        if (c.getClientProperty(KEY) != null) return;
        if (c instanceof JTextComponent) installText((JTextComponent)c);
        else if (c instanceof JSpinner) installSpinner((JSpinner)c);
        else if (c instanceof JComboBox) installCombo((JComboBox<?>)c);
        else if (c instanceof JSlider) installSlider((JSlider)c);
        else if (c instanceof javax.swing.JToggleButton) installToggle((AbstractButton)c);
    }

    private void installText(JTextComponent field) {
        field.putClientProperty(KEY, local);
        field.getDocument().addUndoableEditListener(e -> {
            if (local.replaying) return;
            boolean explicit = Boolean.TRUE.equals(field.getClientProperty(USER_EDIT_KEY));
            if (explicit || field.isFocusOwner()) local.addEdit(e.getEdit());
        });
    }

    private void installSpinner(JSpinner spinner) {
        spinner.putClientProperty(KEY, local);
        final Object[] previous = { spinner.getValue() };
        spinner.addChangeListener(e -> {
            Object now = spinner.getValue(), old = previous[0];
            previous[0] = now;
            if (local.replaying || Objects.equals(old, now) || !focusWithin(spinner)) return;
            local.addValueEdit(old, now, v -> { spinner.setValue(v); previous[0] = v; });
        });
    }

    private void installCombo(JComboBox<?> combo) {
        combo.putClientProperty(KEY, local);
        final Object[] previous = { combo.getSelectedItem() };
        combo.addActionListener(e -> {
            Object now = combo.getSelectedItem(), old = previous[0];
            previous[0] = now;
            if (local.replaying || Objects.equals(old, now)) return;
            if (!(focusWithin(combo) || combo.isPopupVisible())) return;
            local.addValueEdit(old, now, v -> {
                ((JComboBox)combo).setSelectedItem(v);
                previous[0] = v;
            });
        });
    }

    private void installToggle(AbstractButton button) {
        button.putClientProperty(KEY, local);
        final boolean[] previous = { button.isSelected() };
        button.addItemListener(e -> {
            boolean now = e.getStateChange() == ItemEvent.SELECTED, old = previous[0];
            previous[0] = now;
            if (local.replaying || old == now || !focusWithin(button)) return;
            local.addValueEdit(Boolean.valueOf(old), Boolean.valueOf(now), v -> {
                boolean b = ((Boolean)v).booleanValue();
                button.setSelected(b);
                previous[0] = b;
            });
        });
    }

    private void installSlider(JSlider slider) {
        slider.putClientProperty(KEY, local);
        final int[] previous = { slider.getValue() };
        slider.addChangeListener(e -> {
            int now = slider.getValue(), old = previous[0];
            previous[0] = now;
            if (local.replaying || old == now || !focusWithin(slider)) return;
            local.addValueEdit(Integer.valueOf(old), Integer.valueOf(now), v -> {
                int n = ((Integer)v).intValue();
                slider.setValue(n);
                previous[0] = n;
            });
        });
    }

    private static boolean focusWithin(Component c) {
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focus == c || (focus != null && c instanceof Container
                && SwingUtilities.isDescendingFrom(focus, (Container)c));
    }

    private boolean focusInEditableControl() {
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focus == null || !SwingUtilities.isDescendingFrom(focus, root)) return false;
        Component p = focus;
        while (p != null) {
            if (p instanceof JTextComponent || p instanceof JSpinner
                    || p instanceof JComboBox || p instanceof JSlider
                    || p instanceof javax.swing.JToggleButton) return true;
            if (p == root) break;
            p = p.getParent();
        }
        return false;
    }

    private UndoRedo active() {
        if (installLocalControls) installRecursively(root); // catches lazy editors
        if (installLocalControls && focusInEditableControl()) return local;
        return document;
    }

    @Override public boolean canUndo() { return active().canUndo(); }
    @Override public boolean canRedo() { return active().canRedo(); }
    @Override public void undo() throws CannotUndoException { active().undo(); fireChange(); }
    @Override public void redo() throws CannotRedoException { active().redo(); fireChange(); }
    @Override public String getUndoPresentationName() { return active().getUndoPresentationName(); }
    @Override public String getRedoPresentationName() { return active().getRedoPresentationName(); }
    @Override public void addChangeListener(ChangeListener l) { listeners.add(ChangeListener.class, l); }
    @Override public void removeChangeListener(ChangeListener l) { listeners.remove(ChangeListener.class, l); }

    private void fireChange() {
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : listeners.getListeners(ChangeListener.class)) l.stateChanged(e);
    }

    private interface ValueSetter { void set(Object value); }

    private static final class LocalUndoRedo extends UndoManager implements UndoRedo {
        private final EventListenerList listeners = new EventListenerList();
        private boolean replaying;

        @Override public synchronized boolean addEdit(UndoableEdit edit) {
            boolean ok = super.addEdit(edit);
            if (ok) fireChange();
            return ok;
        }

        void addValueEdit(Object oldValue, Object newValue, ValueSetter setter) {
            addEdit(new AbstractUndoableEdit() {
                @Override public void undo() throws CannotUndoException {
                    super.undo();
                    replaying = true;
                    try { setter.set(oldValue); } finally { replaying = false; }
                }
                @Override public void redo() throws CannotRedoException {
                    super.redo();
                    replaying = true;
                    try { setter.set(newValue); } finally { replaying = false; }
                }
            });
        }

        @Override public synchronized void undo() throws CannotUndoException {
            replaying = true;
            try { super.undo(); } finally { replaying = false; }
            fireChange();
        }

        @Override public synchronized void redo() throws CannotRedoException {
            replaying = true;
            try { super.redo(); } finally { replaying = false; }
            fireChange();
        }

        @Override public void addChangeListener(ChangeListener l) { listeners.add(ChangeListener.class, l); }
        @Override public void removeChangeListener(ChangeListener l) { listeners.remove(ChangeListener.class, l); }
        @Override public String getUndoPresentationName() { return canUndo() ? "Undo control edit" : ""; }
        @Override public String getRedoPresentationName() { return canRedo() ? "Redo control edit" : ""; }

        private void fireChange() {
            ChangeEvent e = new ChangeEvent(this);
            for (ChangeListener l : listeners.getListeners(ChangeListener.class)) l.stateChanged(e);
        }
    }
}
