package kiyut.sketsa.modules.textspacing.integration;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.List;
import java.lang.reflect.Field;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
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
import org.w3c.dom.Element;
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

    VectorCanvas getCanvasForUndo() {
        return canvas;
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
        String value = "";
        if (e instanceof org.w3c.dom.Element) {
            org.w3c.dom.Element element = (org.w3c.dom.Element) e;
            String style = element.getAttribute("style");
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                    "(?i)(?:^|;)\\s*" + java.util.regex.Pattern.quote(property)
                    + "\\s*:\\s*([^;]+)").matcher(style == null ? "" : style);
            if (matcher.find()) value = matcher.group(1).trim();
            if (value.isEmpty()) value = element.getAttribute(property);
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
        if (!(cachedTextElement instanceof Element)) {
            return;
        }

        final Element element = (Element) cachedTextElement;
        final SpacingState before = SpacingState.capture(element);
        String letterValue = css(((Number) letter.getValue()).doubleValue());
        String wordValue = css(((Number) word.getValue()).doubleValue());
        boolean cssMode = kiyut.sketsa.options.CodeFormatOptions.getInstance().isStylingCSS();

        applying = true;
        DOMUndoManager undo = canvas.getUndoManager();
        beginExplicitUndoTransaction(undo, "Text Spacing");
        try {
            boolean oldInProgress = setUndoInProgress(undo, true);
            try {
                updateSpacingProperty(element, "letter-spacing", letterValue, cssMode);
                updateSpacingProperty(element, "word-spacing", wordValue, cssMode);
            } finally {
                setUndoInProgress(undo, oldInProgress);
            }

            final SpacingState after = SpacingState.capture(element);
            if (before.equals(after)) {
                undo.cancel();
                return;
            }

            addExplicitUndoEdit(undo, new AbstractUndoableEdit() {
                @Override public void undo() throws CannotUndoException {
                    super.undo();
                    before.apply(element);
                    syncAfterUndoRedo();
                    canvas.refresh();
                }

                @Override public void redo() throws CannotRedoException {
                    super.redo();
                    after.apply(element);
                    syncAfterUndoRedo();
                    canvas.refresh();
                }
            });
            undo.end();

            canvas.refresh();
            status.setText("Spacing applied.");
        } catch (RuntimeException ex) {
            undo.cancel();
            status.setText("Could not apply spacing: " + ex.getMessage());
        } finally {
            applying = false;
        }
    }


    /** Keep the UI controls aligned with the DOM after document Undo/Redo. */
    private void syncControlsFromElement() {
        if (cachedTextElement != null) {
            loadFromElement(cachedTextElement);
        }
    }

    /**
     * Canvas Undo/Redo may complete outside the spinner event sequence. Re-read
     * the actual DOM on the Swing event queue so the visible numeric values
     * always match the restored text attributes.
     */
    void syncAfterUndoRedo() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (!applying) syncControlsFromElement();
        });
    }

    private static final class SpacingState {
        final boolean hasStyle;
        final String style;
        final boolean hasLetter;
        final String letter;
        final boolean hasWord;
        final String word;

        SpacingState(boolean hasStyle, String style, boolean hasLetter, String letter,
                boolean hasWord, String word) {
            this.hasStyle = hasStyle; this.style = style;
            this.hasLetter = hasLetter; this.letter = letter;
            this.hasWord = hasWord; this.word = word;
        }

        static SpacingState capture(Element e) {
            return new SpacingState(
                    e.hasAttribute("style"), e.getAttribute("style"),
                    e.hasAttribute("letter-spacing"), e.getAttribute("letter-spacing"),
                    e.hasAttribute("word-spacing"), e.getAttribute("word-spacing"));
        }

        void apply(Element e) {
            set(e, "style", hasStyle, style);
            set(e, "letter-spacing", hasLetter, letter);
            set(e, "word-spacing", hasWord, word);
        }

        private static void set(Element e, String name, boolean present, String value) {
            if (present) e.setAttribute(name, value == null ? "" : value);
            else e.removeAttribute(name);
        }

        @Override public boolean equals(Object o) {
            if (!(o instanceof SpacingState)) return false;
            SpacingState x = (SpacingState)o;
            return hasStyle == x.hasStyle && hasLetter == x.hasLetter && hasWord == x.hasWord
                    && java.util.Objects.equals(style, x.style)
                    && java.util.Objects.equals(letter, x.letter)
                    && java.util.Objects.equals(word, x.word);
        }

        @Override public int hashCode() {
            return java.util.Objects.hash(hasStyle, style, hasLetter, letter, hasWord, word);
        }
    }

    private static void updateSpacingProperty(Element e, String property, String value, boolean cssMode) {
        if (cssMode) {
            String style = removeStyleProperty(e.getAttribute("style"), property);
            if (value != null) {
                if (!style.isEmpty() && !style.endsWith(";")) style += ";";
                style += property + ":" + value;
            }
            if (style.trim().isEmpty()) e.removeAttribute("style");
            else e.setAttribute("style", style);
            e.removeAttribute(property);
        } else {
            if (value == null) e.removeAttribute(property);
            else e.setAttribute(property, value);

            String style = removeStyleProperty(e.getAttribute("style"), property);
            if (style.trim().isEmpty()) e.removeAttribute("style");
            else e.setAttribute("style", style);
        }
    }

    private static String removeStyleProperty(String style, String property) {
        if (style == null || style.trim().isEmpty()) return "";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(?i)(^|;)\\s*" + java.util.regex.Pattern.quote(property)
                + "\\s*:[^;]*(?=;|$)");
        String cleaned = p.matcher(style).replaceAll("$1");
        cleaned = cleaned.replaceAll(";\\s*;", ";").trim();
        while (cleaned.startsWith(";")) cleaned = cleaned.substring(1).trim();
        while (cleaned.endsWith(";")) cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        return cleaned;
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

    private static boolean setUndoInProgress(DOMUndoManager undo, boolean value) {
        try {
            Field f = DOMUndoManager.class.getDeclaredField("inProgress");
            f.setAccessible(true);
            boolean old = f.getBoolean(undo);
            f.setBoolean(undo, value);
            return old;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not control Sketsa undo recording", ex);
        }
    }

    private static void beginExplicitUndoTransaction(DOMUndoManager undo, String name) {
        try {
            Field current = DOMUndoManager.class.getDeclaredField("currentEntry");
            current.setAccessible(true);
            if (current.get(undo) != null) undo.end();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not inspect Sketsa undo transaction", ex);
        }
        undo.start(name);
    }

    private static void addExplicitUndoEdit(DOMUndoManager undo, javax.swing.undo.UndoableEdit edit) {
        try {
            Field current = DOMUndoManager.class.getDeclaredField("currentEntry");
            current.setAccessible(true);
            Object entry = current.get(undo);
            if (entry == null) throw new IllegalStateException("No active Sketsa undo transaction");
            java.lang.reflect.Method getter = entry.getClass().getDeclaredMethod("getCompoundEdit");
            getter.setAccessible(true);
            CompoundEdit compound = (CompoundEdit)getter.invoke(entry);
            compound.addEdit(edit);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not add explicit Sketsa undo edit", ex);
        }
    }
    /** Ensure every plugin edit is a separate Sketsa undo transaction. */
    private static void beginUndoTransaction(DOMUndoManager undo, String name) {
        // end() is a no-op when no transaction is open. If Sketsa left a
        // previous editor transaction pending, commit it before starting ours.
        undo.end();
        undo.start(name);
    }
}
