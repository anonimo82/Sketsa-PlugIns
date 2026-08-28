package kiyut.sketsa.modules.switcher.integration;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.lang.reflect.Field;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import kiyut.sketsa.canvas.VectorCanvas;
import kiyut.sketsa.canvas.event.CanvasSelectionAdapter;
import kiyut.sketsa.canvas.event.CanvasSelectionEvent;
import kiyut.sketsa.cookies.SVGEditorCookie;
import kiyut.sketsa.undo.DOMUndoManager;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGElement;

final class SwitchPanel extends JPanel {

    private static final String SVG_NS = "http://www.w3.org/2000/svg";

    private final JTextField languageField = new JTextField("en", 12);

    private final JRadioButton simEn = new JRadioButton("en");
    private final JRadioButton simIt = new JRadioButton("it");
    private final JRadioButton simFr = new JRadioButton("fr");
    private final JRadioButton simCustom = new JRadioButton("Custom");
    private final JTextField simCustomField = new JTextField("de", 6);

    private final JLabel simulationResult = new JLabel("Simulation: select a switch.");

    private final JButton wrapButton = new JButton("Wrap in Switch");
    private final JButton addAlternativeButton = new JButton("Add Alternative");
    private final JButton updateConditionButton = new JButton("Update Language");
    private final JButton removeAlternativeButton = new JButton("Remove Alternative");
    private final JButton extractButton = new JButton("Extract from Switch");

    private final JLabel status = new JLabel("Select one SVG object.");

    /*
     * Native Sketsa Undo/Redo changes the DOM without necessarily generating
     * a new canvas-selection event. Keep the inspector synchronized with the
     * selected alternative's real systemLanguage value.
     */
    private final javax.swing.Timer domSyncTimer;
    private boolean languageDirty;

    private final Lookup.Result<SVGEditorCookie> lookupResult;
    private final LookupListener lookupListener;
    private final SelectionHandler selectionHandler = new SelectionHandler();

    private VectorCanvas canvas;
    private SVGElement cachedSelected;
    private Element cachedElement;
    private Element simulatedSwitch;
    private Element simulatedClone;
    private String simulatedSwitchDisplay;
    private boolean loading;

    SwitchPanel() {
        super(new GridBagLayout());

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Switch"),
                BorderFactory.createEmptyBorder(2, 6, 5, 6)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        add(new JLabel("systemLanguage:"), c);

        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(languageField, c);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        row1.add(wrapButton);
        row1.add(addAlternativeButton);

        c.gridx = 0; c.gridy = 1; c.gridwidth = 2;
        c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(row1, c);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        row2.add(updateConditionButton);
        row2.add(removeAlternativeButton);
        row2.add(extractButton);

        c.gridy = 2;
        add(row2, c);

        ButtonGroup simGroup = new ButtonGroup();
        simGroup.add(simEn);
        simGroup.add(simIt);
        simGroup.add(simFr);
        simGroup.add(simCustom);
        simEn.setSelected(true);

        JPanel simRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        simRow.setBorder(BorderFactory.createTitledBorder("Simulate language"));
        simRow.add(simEn);
        simRow.add(simIt);
        simRow.add(simFr);
        simRow.add(simCustom);
        simRow.add(simCustomField);

        c.gridy = 3;
        add(simRow, c);

        c.gridy = 4;
        add(simulationResult, c);

        c.gridy = 5;
        add(status, c);

        wrapButton.addActionListener(e -> wrapInSwitch());
        addAlternativeButton.addActionListener(e -> addAlternative());
        updateConditionButton.addActionListener(e -> updateLanguage());
        removeAlternativeButton.addActionListener(e -> removeAlternative());
        extractButton.addActionListener(e -> extractFromSwitch());

        domSyncTimer = new javax.swing.Timer(250, e -> syncFromDOM());
        domSyncTimer.setRepeats(true);
        domSyncTimer.start();

        languageField.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
            private void changed() {
                if (!loading) {
                    languageDirty = true;
                }
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                changed();
            }
        });

        simEn.addActionListener(e -> updateSimulation());
        simIt.addActionListener(e -> updateSimulation());
        simFr.addActionListener(e -> updateSimulation());
        simCustom.addActionListener(e -> updateSimulation());
        simCustomField.addActionListener(e -> updateSimulation());

        lookupListener = (LookupEvent e) -> updateCanvasFromLookup();
        lookupResult = Utilities.actionsGlobalContext().lookupResult(SVGEditorCookie.class);
        lookupResult.addLookupListener(lookupListener);

        updateCanvasFromLookup();
        updateEnabledState();
    }

    private void updateCanvasFromLookup() {
        Collection<? extends SVGEditorCookie> cookies = lookupResult.allInstances();

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
        restoreSimulation();
        if (canvas == newCanvas) {
            cacheSelection();
            return;
        }

        if (canvas != null) {
            canvas.getCanvasSelection().removeSelectionListener(selectionHandler);
        }

        canvas = newCanvas;
        cachedSelected = null;
        cachedElement = null;

        if (canvas != null) {
            canvas.getCanvasSelection().addSelectionListener(selectionHandler);
            cacheSelection();
        }

        updateEnabledState();
    }

    private Element asElement(SVGElement svg) {
        return svg instanceof Element ? (Element)svg : null;
    }

    private String localName(Element e) {
        if (e == null) return "";
        String n = e.getLocalName();
        return (n == null || n.isEmpty()) ? e.getTagName() : n;
    }

    private void cacheSelection() {
        if (canvas == null) return;

        List<SVGElement> list = canvas.getCanvasSelection().getSelectionList();

        if (list == null || list.size() != 1) {
            cachedSelected = null;
            cachedElement = null;
            updateEnabledState();
            return;
        }

        cachedSelected = list.get(0);
        cachedElement = asElement(cachedSelected);

        loadFromSelection();
        updateEnabledState();
    }

    private Element parentSwitch(Element e) {
        if (e == null) return null;
        Node p = e.getParentNode();
        if (p instanceof Element && "switch".equals(localName((Element)p))) {
            return (Element)p;
        }
        return null;
    }

    private void loadFromSelection() {
        if (cachedElement == null) return;

        loading = true;
        try {
            Element sw = parentSwitch(cachedElement);

            if (sw != null) {
                languageField.setText(cachedElement.getAttribute("systemLanguage"));
                status.setText("Selected switch alternative.");
            } else if ("switch".equals(localName(cachedElement))) {
                languageField.setText("");
                status.setText("Selected <switch> container.");
            } else {
                languageField.setText("");
                status.setText("Selected object can be wrapped in <switch>.");
            }
        } finally {
            loading = false;
            languageDirty = false;
        }

        updateSimulation();
    }

    private void syncFromDOM() {
        if (loading || cachedElement == null
                || languageField.isFocusOwner()
                || languageDirty) {
            return;
        }

        Element sw = parentSwitch(cachedElement);
        if (sw == null) {
            return;
        }

        String domLanguage = cachedElement.getAttribute("systemLanguage");
        String uiLanguage = languageField.getText();

        if (domLanguage == null) domLanguage = "";
        if (uiLanguage == null) uiLanguage = "";

        if (!domLanguage.equals(uiLanguage)) {
            loading = true;
            try {
                languageField.setText(domLanguage);
            } finally {
                loading = false;
                languageDirty = false;
                }

            /*
             * Rebuild the current visual simulation because Undo/Redo may have
             * changed which branch matches the simulated language.
             */
            updateSimulation();
        }
    }

    private void updateEnabledState() {
        boolean selected = canvas != null && cachedElement != null;
        boolean selectedSwitch = selected && "switch".equals(localName(cachedElement));
        boolean alternative = selected && parentSwitch(cachedElement) != null;

        languageField.setEnabled(selected);

        wrapButton.setEnabled(selected && !selectedSwitch && !alternative);
        addAlternativeButton.setEnabled(selected && (selectedSwitch || alternative));
        updateConditionButton.setEnabled(alternative);
        removeAlternativeButton.setEnabled(alternative);
        extractButton.setEnabled(alternative);

        if (!selected) {
            status.setText("Select exactly one SVG object.");
        }
    }

    private String simulatedLanguage() {
        if (simIt.isSelected()) return "it";
        if (simFr.isSelected()) return "fr";
        if (simCustom.isSelected()) return simCustomField.getText().trim();
        return "en";
    }

    private Element currentSwitch() {
        if (cachedElement == null) return null;
        if ("switch".equals(localName(cachedElement))) return cachedElement;
        return parentSwitch(cachedElement);
    }

    private void updateSimulation() {
        restoreSimulation();

        Element sw = currentSwitch();
        if (sw == null) {
            simulationResult.setText("Simulation: select a <switch> or one of its alternatives.");
            if (canvas != null) canvas.refresh();
            return;
        }

        String lang = simulatedLanguage();
        Element fallback = null;
        Element match = null;
        int index = 0;
        int matchIndex = -1;
        int fallbackIndex = -1;

        for (Node n = sw.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element)) continue;

            Element child = (Element)n;
            String cond = child.getAttribute("systemLanguage");

            if (cond == null || cond.trim().isEmpty()) {
                if (fallback == null) {
                    fallback = child;
                    fallbackIndex = index;
                }
            } else if (matchesLanguage(cond, lang)) {
                match = child;
                matchIndex = index;
                break;
            }

            index++;
        }

        Element active = match != null ? match : fallback;
        int activeIndex = match != null ? matchIndex : fallbackIndex;

        Node parent = sw.getParentNode();
        if (parent == null) {
            simulationResult.setText("Simulation: switch has no parent.");
            return;
        }

        /*
         * Batik decides <switch> branches internally from its user-agent
         * language and did not reliably react to display changes on children.
         *
         * For preview we therefore bypass <switch> rendering completely:
         * - temporarily hide the real <switch>
         * - clone the branch that our simulator selected
         * - insert that clone immediately after the <switch>
         *
         * The clone is marked and stripped of systemLanguage so Batik renders
         * it unconditionally.
         */
        simulatedSwitch = sw;
        simulatedSwitchDisplay = sw.getAttribute("display");

        boolean oldInProgress = suppressUndo(true);
        try {
            sw.setAttribute("display", "none");

            if (active != null) {
                Element clone = (Element) active.cloneNode(true);
                clone.removeAttribute("systemLanguage");
                clone.setAttribute("data-sketsa-switch-preview", "true");

                /*
                 * Avoid duplicate top-level IDs during the temporary preview.
                 * Descendant IDs are kept because they may be referenced internally.
                 */
                clone.removeAttribute("id");

                Node next = sw.getNextSibling();
                if (next != null) {
                    parent.insertBefore(clone, next);
                } else {
                    parent.appendChild(clone);
                }

                simulatedClone = clone;
            }
        } finally {
            suppressUndo(oldInProgress);
        }

        if (active == null) {
            simulationResult.setText(
                    "Simulation [" + lang + "]: no matching branch and no fallback.");
        } else {
            String id = active.getAttribute("id");
            String label = (id == null || id.isEmpty())
                    ? "child " + (activeIndex + 1)
                    : "#" + id;

            String cond = active.getAttribute("systemLanguage");
            if (cond == null || cond.trim().isEmpty()) {
                simulationResult.setText(
                        "Simulation [" + lang + "]: fallback -> " + label);
            } else {
                simulationResult.setText(
                        "Simulation [" + lang + "]: active -> " + label
                        + " (" + cond + ")");
            }
        }

        if (canvas != null) {
            canvas.refresh();
        }
    }

    private void restoreSimulation() {
        boolean oldInProgress = suppressUndo(true);
        try {
            if (simulatedClone != null && simulatedClone.getParentNode() != null) {
                simulatedClone.getParentNode().removeChild(simulatedClone);
            }

            if (simulatedSwitch != null) {
                if (simulatedSwitchDisplay == null || simulatedSwitchDisplay.isEmpty()) {
                    simulatedSwitch.removeAttribute("display");
                } else {
                    simulatedSwitch.setAttribute("display", simulatedSwitchDisplay);
                }
            }

            simulatedClone = null;
            simulatedSwitch = null;
            simulatedSwitchDisplay = null;
        } finally {
            suppressUndo(oldInProgress);
        }
    }

    private boolean matchesLanguage(String condition, String requested) {
        if (condition == null || requested == null) return false;

        String req = requested.trim().toLowerCase(java.util.Locale.ROOT);
        if (req.isEmpty()) return false;

        String[] tokens = condition.split(",");

        for (String token : tokens) {
            String value = token.trim().toLowerCase(java.util.Locale.ROOT);
            if (value.isEmpty()) continue;

            /*
             * SVG systemLanguage matching is prefix-based for language tags:
             * "en" matches "en" and "en-US"; "en-US" matches only that branch.
             * For this local simulator we accept either exact match or a
             * requested language that starts with condition + "-".
             */
            if (req.equals(value) || req.startsWith(value + "-")) {
                return true;
            }
        }

        return false;
    }

    private void wrapInSwitch() {
        restoreSimulation();

        if (canvas == null || cachedElement == null) return;
        if ("switch".equals(localName(cachedElement)) || parentSwitch(cachedElement) != null) return;

        Node parent = cachedElement.getParentNode();
        if (parent == null) {
            status.setText("Cannot wrap this object.");
            return;
        }

        Document document = cachedElement.getOwnerDocument();
        Element sw = document.createElementNS(SVG_NS, "switch");

        String language = languageField.getText().trim();

        beginUndoTransaction(canvas.getUndoManager(), "Wrap in Switch");
        try {
            parent.replaceChild(sw, cachedElement);
            sw.appendChild(cachedElement);

            if (!language.isEmpty()) {
                cachedElement.setAttribute("systemLanguage", language);
            } else {
                cachedElement.removeAttribute("systemLanguage");
            }
        } finally {
            canvas.getUndoManager().end();
        }

        canvas.refresh();
        selectElement(cachedElement);
        updateSimulation();
        status.setText("Wrapped selected object in <switch>.");
    }

    private void addAlternative() {
        restoreSimulation();

        if (canvas == null || cachedElement == null) return;

        Element sw;
        Element source;

        if ("switch".equals(localName(cachedElement))) {
            sw = cachedElement;
            source = firstElementChild(sw);
            if (source == null) {
                status.setText("Switch has no source child to clone.");
                return;
            }
        } else {
            sw = parentSwitch(cachedElement);
            source = cachedElement;
        }

        if (sw == null || source == null) {
            status.setText("Select a <switch> or one of its alternatives.");
            return;
        }

        Element clone = (Element)source.cloneNode(true);
        clone.removeAttribute("id");

        String language = languageField.getText().trim();
        if (language.isEmpty()) {
            clone.removeAttribute("systemLanguage");
        } else {
            clone.setAttribute("systemLanguage", language);
        }

        beginUndoTransaction(canvas.getUndoManager(), "Add Switch Alternative");
        try {
            sw.appendChild(clone);
        } finally {
            canvas.getUndoManager().end();
        }

        canvas.refresh();
        selectElement(clone);
        updateSimulation();
        status.setText("Alternative added to <switch>.");
    }

    private void updateLanguage() {
        restoreSimulation();

        if (canvas == null || cachedElement == null) return;
        if (parentSwitch(cachedElement) == null) return;

        String after = languageField.getText().trim();
        final Element target = cachedElement;
        final boolean hadBefore = target.hasAttribute("systemLanguage");
        final String before = target.getAttribute("systemLanguage");
        final boolean hadAfter = !after.isEmpty();

        if (hadBefore == hadAfter && before.equals(after)) {
            languageDirty = false;
            status.setText(after.isEmpty()
                    ? "Language condition removed."
                    : "systemLanguage = " + after);
            return;
        }

        DOMUndoManager undo = canvas.getUndoManager();
        beginExplicitUndoTransaction(undo, "Update Switch Language");
        try {
            boolean oldInProgress = suppressUndo(true);
            try {
                setAttributeState(target, "systemLanguage", hadAfter, after);
            } finally {
                suppressUndo(oldInProgress);
            }

            addExplicitUndoEdit(undo, new AbstractUndoableEdit() {
                @Override public void undo() throws CannotUndoException {
                    super.undo();
                    setAttributeState(target, "systemLanguage", hadBefore, before);
                    languageDirty = false;
                    canvas.refresh();
                }

                @Override public void redo() throws CannotRedoException {
                    super.redo();
                    setAttributeState(target, "systemLanguage", hadAfter, after);
                    languageDirty = false;
                    canvas.refresh();
                }
            });
            undo.end();
        } catch (RuntimeException ex) {
            undo.cancel();
            throw ex;
        }

        languageDirty = false;
        canvas.refresh();
        updateSimulation();
        status.setText(after.isEmpty()
                ? "Language condition removed."
                : "systemLanguage = " + after);
    }

    private void removeAlternative() {
        restoreSimulation();

        if (canvas == null || cachedElement == null) return;

        Element sw = parentSwitch(cachedElement);
        if (sw == null) return;

        Element removed = cachedElement;

        beginUndoTransaction(canvas.getUndoManager(), "Remove Switch Alternative");
        try {
            sw.removeChild(removed);
        } finally {
            canvas.getUndoManager().end();
        }

        canvas.refresh();

        Element first = firstElementChild(sw);
        if (first != null) {
            selectElement(first);
        } else {
            selectElement(sw);
        }

        updateSimulation();
        status.setText("Alternative removed.");
    }

    private void extractFromSwitch() {
        restoreSimulation();

        if (canvas == null || cachedElement == null) return;

        Element sw = parentSwitch(cachedElement);
        if (sw == null) return;

        Node parent = sw.getParentNode();
        if (parent == null) {
            status.setText("Cannot extract this alternative.");
            return;
        }

        Element extracted = cachedElement;
        Node switchNext = sw.getNextSibling();

        beginUndoTransaction(canvas.getUndoManager(), "Extract from Switch");
        try {
            extracted.removeAttribute("systemLanguage");
            sw.removeChild(extracted);

            if (switchNext != null) {
                parent.insertBefore(extracted, switchNext);
            } else {
                parent.appendChild(extracted);
            }

            if (firstElementChild(sw) == null) {
                parent.removeChild(sw);
            }
        } finally {
            canvas.getUndoManager().end();
        }

        canvas.refresh();
        selectElement(extracted);
        updateSimulation();
        status.setText("Alternative extracted from <switch>.");
    }

    private Element firstElementChild(Element parent) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) return (Element)n;
        }
        return null;
    }

    private void selectElement(Element element) {
        if (element instanceof SVGElement) {
            cachedElement = element;
            cachedSelected = (SVGElement)element;
            canvas.getCanvasSelection().setSelectionList(
                    Collections.singletonList(cachedSelected));
        }
    }

    private final class SelectionHandler extends CanvasSelectionAdapter {
        @Override
        public void valueChanged(CanvasSelectionEvent event) {
            restoreSimulation();
            List<SVGElement> list = event.getSelectionList();

            if (list == null || list.size() != 1) {
                cachedSelected = null;
                cachedElement = null;
                languageDirty = false;
            } else {
                cachedSelected = list.get(0);
                cachedElement = asElement(cachedSelected);
            }

            if (cachedElement != null) {
                loadFromSelection();
            }

            updateEnabledState();
        }
    }
    private boolean suppressUndo(boolean value) {
        if (canvas == null) return false;
        try {
            DOMUndoManager undo = canvas.getUndoManager();
            Field f = DOMUndoManager.class.getDeclaredField("inProgress");
            f.setAccessible(true);
            boolean old = f.getBoolean(undo);
            f.setBoolean(undo, value);
            return old;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not suppress preview undo recording", ex);
        }
    }


    private static void setAttributeState(Element element, String name, boolean present, String value) {
        if (present) element.setAttribute(name, value == null ? "" : value);
        else element.removeAttribute(name);
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
