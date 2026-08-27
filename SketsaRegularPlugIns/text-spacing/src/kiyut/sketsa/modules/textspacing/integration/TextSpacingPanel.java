package kiyut.sketsa.modules.textspacing.integration;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import kiyut.sketsa.canvas.VectorCanvas;
import kiyut.sketsa.canvas.event.CanvasSelectionAdapter;
import kiyut.sketsa.canvas.event.CanvasSelectionEvent;
import kiyut.sketsa.cookies.SVGEditorCookie;
import kiyut.sketsa.undo.DOMUndoManager;
import kiyut.sketsa.util.DOMUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGStylable;
import org.w3c.dom.svg.SVGTextContentElement;

/**
 * Independent spacing controls hosted in the native Text Style TopComponent.
 *
 * This panel deliberately does not subclass TextStyleProperties and therefore
 * does not inherit its focus-driven element/tab enabled state.
 */
final class TextSpacingPanel extends JPanel {

    private final JSpinner letter =
            new JSpinner(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.5));
    private final JSpinner word =
            new JSpinner(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.5));
    private final JLabel status = new JLabel("Select one text object.");

    private final Lookup.Result<SVGEditorCookie> lookupResult;
    private final LookupListener lookupListener;
    private final SelectionHandler selectionHandler = new SelectionHandler();

    private VectorCanvas canvas;
    private SVGElement cachedTextElement;
    private boolean applying;
    private boolean loading;

    TextSpacingPanel() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Spacing"),
                BorderFactory.createEmptyBorder(2, 6, 5, 6)));

        configureSpinner(letter);
        configureSpinner(word);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        add(new JLabel("Letter spacing (px):"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(letter, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        add(new JLabel("Word spacing (px):"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(word, c);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(status, c);

        letter.addChangeListener(e -> applySpacing());
        word.addChangeListener(e -> applySpacing());

        lookupListener = (LookupEvent e) -> updateCanvasFromLookup();
        lookupResult = Utilities.actionsGlobalContext().lookupResult(SVGEditorCookie.class);
        lookupResult.addLookupListener(lookupListener);
        updateCanvasFromLookup();

        // Start enabled only when we actually have a cached text element.
        updateEnabledState();
    }

    private void configureSpinner(JSpinner spinner) {
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.##");
        spinner.setEditor(editor);
        JFormattedTextField field = editor.getTextField();
        field.setEditable(true);
        field.setColumns(7);
        field.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);

        // Arrow buttons must not steal focus from the SVG document.
        makeButtonsNonFocusable(spinner);

        field.addActionListener(e -> {
            try {
                spinner.commitEdit();
                applySpacing();
            } catch (java.text.ParseException ex) {
                status.setText("Invalid number.");
            }
        });
    }

    private void makeButtonsNonFocusable(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                b.setFocusable(false);
                b.setRequestFocusEnabled(false);
            }
            if (c instanceof Container) {
                makeButtonsNonFocusable((Container) c);
            }
        }
    }

    private void updateCanvasFromLookup() {
        Collection<? extends SVGEditorCookie> cookies = lookupResult.allInstances();

        // Important: an empty global lookup just means focus moved into this
        // panel. Keep the last document/canvas in that case.
        if (!cookies.isEmpty()) {
            SVGEditorCookie cookie = cookies.iterator().next();
            if (cookie.isOpened()) {
                setCanvas(cookie.getVectorCanvas());
            }
        }
    }

    private void setCanvas(VectorCanvas newCanvas) {
        if (canvas == newCanvas) {
            cacheSelection();
            return;
        }

        if (canvas != null) {
            canvas.getCanvasSelection().removeSelectionListener(selectionHandler);
        }

        canvas = newCanvas;
        cachedTextElement = null;

        if (canvas != null) {
            canvas.getCanvasSelection().addSelectionListener(selectionHandler);
            cacheSelection();
        }

        updateEnabledState();
    }

    private void cacheSelection() {
        if (canvas == null) {
            return;
        }

        List<SVGElement> list = canvas.getCanvasSelection().getSelectionList();

        // Preserve the last real text selection while focus moves to the panel.
        if (list == null || list.size() != 1) {
            return;
        }

        SVGElement e = list.get(0);
        if (e instanceof SVGTextContentElement && e instanceof SVGStylable) {
            cachedTextElement = e;
            loadFromElement(e);
        }

        updateEnabledState();
    }

    private void loadFromElement(SVGElement e) {
        loading = true;
        try {
            letter.setValue(readPx((SVGStylable)e, "letter-spacing"));
            word.setValue(readPx((SVGStylable)e, "word-spacing"));
        } finally {
            loading = false;
        }
    }

    private double readPx(SVGStylable e, String property) {
        /*
         * Sketsa DOMUtilities.updateProperty() writes either CSS style or a
         * presentation attribute according to CodeFormatOptions.isStylingCSS().
         * Read both forms so a selection refresh never resets the spinner to 0.
         */
        String value = e.getStyle().getPropertyValue(property);

        if (value == null || value.trim().isEmpty()) {
            if (e instanceof org.w3c.dom.Element) {
                value = ((org.w3c.dom.Element) e).getAttribute(property);
            }
        }

        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }

        value = value.trim();
        if (value.endsWith("px")) {
            value = value.substring(0, value.length() - 2).trim();
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private void updateEnabledState() {
        boolean enabled = canvas != null && cachedTextElement != null;
        letter.setEnabled(enabled);
        word.setEnabled(enabled);
        status.setText(enabled
                ? "Editing selected text object."
                : "Select exactly one text object.");
    }

    private void applySpacing() {
        if (loading || applying || canvas == null || cachedTextElement == null) {
            return;
        }

        if (!(cachedTextElement instanceof SVGStylable)) {
            return;
        }

        SVGStylable stylable = (SVGStylable) cachedTextElement;
        String letterValue = css(((Number) letter.getValue()).doubleValue());
        String wordValue = css(((Number) word.getValue()).doubleValue());

        applying = true;
        DOMUndoManager undo = canvas.getUndoManager();
        undo.start("Text Spacing");
        try {
            DOMUtilities.updateProperty(stylable, null, "letter-spacing", letterValue);
            DOMUtilities.updateProperty(stylable, null, "word-spacing", wordValue);

            /*
             * The DOM values are now stable/readable in both CSS-style and
             * presentation-attribute modes, so it is safe to ask Sketsa to
             * rebuild/repaint the canvas after the mutation.
             */
            canvas.refresh();
            status.setText("Spacing applied.");
        } finally {
            undo.end();
            applying = false;
        }
    }

    private String css(double value) {
        if (Math.abs(value) < 0.0000001) {
            return null;
        }
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value)) + "px";
        }
        return Double.toString(value) + "px";
    }

    private final class SelectionHandler extends CanvasSelectionAdapter {
        @Override
        public void valueChanged(CanvasSelectionEvent event) {
            List<SVGElement> list = event.getSelectionList();
            if (list != null && list.size() == 1) {
                SVGElement e = list.get(0);
                if (e instanceof SVGTextContentElement && e instanceof SVGStylable) {
                    cachedTextElement = e;
                    loadFromElement(e);
                } else {
                    cachedTextElement = null;
                }
            } else if (list != null && !list.isEmpty()) {
                cachedTextElement = null;
            }
            updateEnabledState();
        }
    }
}
