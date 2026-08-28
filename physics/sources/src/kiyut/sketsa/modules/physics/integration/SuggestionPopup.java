package kiyut.sketsa.modules.physics.integration;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.function.Supplier;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

final class SuggestionPopup {
    private SuggestionPopup() {}

    static void install(JTextField field, Supplier<? extends Collection<String>> valuesSupplier) {
        field.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                SwingUtilities.invokeLater(() -> show(field, valuesSupplier));
            }
        });
    }

    private static void show(JTextField field, Supplier<? extends Collection<String>> valuesSupplier) {
        Collection<String> supplied;
        try { supplied = valuesSupplier.get(); } catch (RuntimeException ex) { return; }
        if (supplied == null) return;
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String value : supplied) {
            if (value != null && !value.trim().isEmpty()) values.add(value.trim());
        }
        if (values.isEmpty()) return;
        JPopupMenu popup = new JPopupMenu();
        for (String value : values) {
            JMenuItem item = new JMenuItem(value);
            item.addActionListener(ev -> FocusUndoRedo.userSetText(field, value));
            popup.add(item);
        }
        popup.show(field, 0, field.getHeight());
    }
}
