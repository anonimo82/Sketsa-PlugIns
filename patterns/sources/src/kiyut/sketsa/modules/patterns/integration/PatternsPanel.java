package kiyut.sketsa.modules.patterns.integration;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CompoundEdit;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGStylable;

final class PatternsPanel extends JPanel {

    private static final String SVG_NS = "http://www.w3.org/2000/svg";

    private static final String TYPE_VERTICAL = "Vertical stripes";
    private static final String TYPE_HORIZONTAL = "Horizontal stripes";
    private static final String TYPE_CHECKER = "Checkerboard";
    private static final String TYPE_DOTS = "Dots";
    private static final String TYPE_CUSTOM = "Custom / existing";

    private final JTextField idField = new JTextField("pattern1", 12);
    private final JComboBox<String> typeField = new JComboBox<>(
            new String[] {TYPE_VERTICAL, TYPE_HORIZONTAL, TYPE_CHECKER, TYPE_DOTS, TYPE_CUSTOM});

    private final JTextField colorAField = new JTextField("#ffffff", 8);
    private final JTextField colorBField = new JTextField("#808080", 8);
    private final JButton colorAButton = new JButton("Choose...");
    private final JButton colorBButton = new JButton("Choose...");

    private final JTextField xField = new JTextField("0", 6);
    private final JTextField yField = new JTextField("0", 6);
    private final JTextField widthField = new JTextField("20", 6);
    private final JTextField heightField = new JTextField("20", 6);
    private static final String MODE_ABSOLUTE = "Absolute (SVG units)";
    private static final String MODE_RELATIVE = "Relative (%)";

    private final JComboBox<String> unitsField =
            new JComboBox<>(new String[] {MODE_ABSOLUTE, MODE_RELATIVE});

    private final PatternPreview preview = new PatternPreview();

    private final JButton createUpdateButton = new JButton("Create / Update");
    private final JButton fillButton = new JButton("Apply Fill");
    private final JButton strokeButton = new JButton("Apply Stroke");
    private final JButton removeFillButton = new JButton("Remove Fill Pattern");
    private final JButton removeStrokeButton = new JButton("Remove Stroke Pattern");
    private final JLabel status = new JLabel("Select one SVG object.");

    private final Lookup.Result<SVGEditorCookie> lookupResult;
    private final LookupListener lookupListener;
    private final SelectionHandler selectionHandler = new SelectionHandler();

    private VectorCanvas canvas;
    private SVGElement cachedSelected;
    private Element cachedElement;

    // Draft pattern: never inserted into the SVG until Apply Fill/Stroke.
    private Element stagedPattern;
    private String stagedPatternId;

    private boolean loading;

    PatternsPanel() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Pattern"),
                BorderFactory.createEmptyBorder(2, 6, 5, 6)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        addRow("ID:", idField, row++, c);
        addRow("Type:", typeField, row++, c);

        JPanel ca = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        ca.add(colorAField);
        ca.add(colorAButton);
        addRow("Color A:", ca, row++, c);

        JPanel cb = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
        cb.add(colorBField);
        cb.add(colorBButton);
        addRow("Color B:", cb, row++, c);

        addRow("X:", xField, row++, c);
        addRow("Y:", yField, row++, c);
        addRow("Tile width:", widthField, row++, c);
        addRow("Tile height:", heightField, row++, c);
        addRow("Units:", unitsField, row++, c);

        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(preview, c);

        JPanel defsButtons = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        defsButtons.add(createUpdateButton);
        c.gridy = row++;
        add(defsButtons, c);

        JPanel applyButtons = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        applyButtons.add(fillButton);
        applyButtons.add(strokeButton);
        c.gridy = row++;
        add(applyButtons, c);

        JPanel removeButtons = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        removeButtons.add(removeFillButton);
        removeButtons.add(removeStrokeButton);
        c.gridy = row++;
        add(removeButtons, c);

        c.gridy = row;
        add(status, c);

        createUpdateButton.addActionListener(e -> createOrUpdatePattern());
        fillButton.addActionListener(e -> applyPaint("fill"));
        strokeButton.addActionListener(e -> applyPaint("stroke"));
        removeFillButton.addActionListener(e -> removePatternPaint("fill"));
        removeStrokeButton.addActionListener(e -> removePatternPaint("stroke"));

        typeField.addActionListener(e -> {
            if (!loading) {
                updateManagedControls();
                preview.repaint();
            }
        });

        unitsField.addActionListener(e -> {
            if (!loading) {
                convertFieldsForMode();
                preview.repaint();
            }
        });

        colorAButton.addActionListener(e -> chooseColor(colorAField, "Choose Pattern Color A"));
        colorBButton.addActionListener(e -> chooseColor(colorBField, "Choose Pattern Color B"));

        java.awt.event.ActionListener previewAction = e -> preview.repaint();
        colorAField.addActionListener(previewAction);
        colorBField.addActionListener(previewAction);
        widthField.addActionListener(previewAction);
        heightField.addActionListener(previewAction);

        FocusAdapter focusPreview = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                preview.repaint();
            }
        };
        colorAField.addFocusListener(focusPreview);
        colorBField.addFocusListener(focusPreview);
        widthField.addFocusListener(focusPreview);
        heightField.addFocusListener(focusPreview);

        lookupListener = (LookupEvent e) -> updateCanvasFromLookup();
        lookupResult = Utilities.actionsGlobalContext().lookupResult(SVGEditorCookie.class);
        lookupResult.addLookupListener(lookupListener);

        updateCanvasFromLookup();
        updateManagedControls();
        updateEnabledState();
    }

    private void addRow(String label, java.awt.Component field, int row, GridBagConstraints c) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        c.weightx = 0; c.fill = GridBagConstraints.NONE;
        add(new JLabel(label), c);

        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(field, c);
    }

    private void chooseColor(JTextField target, String title) {
        Color initial = parseColor(target.getText(), Color.GRAY);
        Color chosen = JColorChooser.showDialog(this, title, initial);
        if (chosen != null) {
            target.setText(toHex(chosen));
            preview.repaint();
        }
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private Color parseColor(String value, Color fallback) {
        try {
            String v = value == null ? "" : value.trim();
            if (v.matches("#[0-9a-fA-F]{6}")) {
                return new Color(Integer.parseInt(v.substring(1), 16));
            }
            if (v.matches("#[0-9a-fA-F]{3}")) {
                int r = Integer.parseInt(v.substring(1, 2) + v.substring(1, 2), 16);
                int g = Integer.parseInt(v.substring(2, 3) + v.substring(2, 3), 16);
                int b = Integer.parseInt(v.substring(3, 4) + v.substring(3, 4), 16);
                return new Color(r, g, b);
            }
        } catch (RuntimeException ex) {
            // use fallback
        }
        return fallback;
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

    private void setCanvas(VectorCanvas newCanvas) {
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

    private void cacheSelection() {
        if (canvas == null) return;

        List<SVGElement> selection = canvas.getCanvasSelection().getSelectionList();
        if (selection == null || selection.size() != 1) {
            cachedSelected = null;
            cachedElement = null;
            updateEnabledState();
            return;
        }

        cachedSelected = selection.get(0);
        cachedElement = asElement(cachedSelected);

        loadPatternFromSelection();
        updateEnabledState();
    }

    private void loadPatternFromSelection() {
        if (cachedElement == null) return;

        String fill = getProperty(cachedElement, "fill");
        String stroke = getProperty(cachedElement, "stroke");

        String id = extractPatternId(fill);
        if (id == null) id = extractPatternId(stroke);

        if (id != null) {
            Element pattern = findPattern(cachedElement.getOwnerDocument(), id);
            if (pattern != null) {
                loadPatternFields(pattern);
                status.setText("Pattern #" + id + " detected on selection.");
                return;
            }
        }

        status.setText("Selected object has no detected pattern paint.");
    }

    private String getProperty(Element e, String property) {
        if (e instanceof SVGStylable) {
            String v = ((SVGStylable)e).getStyle().getPropertyValue(property);
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        String attr = e.getAttribute(property);
        return attr == null ? "" : attr.trim();
    }

    private String extractPatternId(String paint) {
        if (paint == null) return null;
        String v = paint.trim();
        if (v.startsWith("url(#") && v.endsWith(")")) {
            return v.substring(5, v.length() - 1);
        }
        return null;
    }

    private void loadPatternFields(Element pattern) {
        loading = true;
        try {
            idField.setText(pattern.getAttribute("id"));

            String domUnits = valueOr(pattern.getAttribute("patternUnits"), "userSpaceOnUse");
            boolean relative = "objectBoundingBox".equals(domUnits);

            unitsField.setSelectedItem(relative ? MODE_RELATIVE : MODE_ABSOLUTE);

            if (relative) {
                xField.setText(toPercent(valueOr(pattern.getAttribute("x"), "0")));
                yField.setText(toPercent(valueOr(pattern.getAttribute("y"), "0")));
                widthField.setText(toPercent(valueOr(pattern.getAttribute("width"), "0.2")));
                heightField.setText(toPercent(valueOr(pattern.getAttribute("height"), "0.2")));
            } else {
                xField.setText(valueOr(pattern.getAttribute("x"), "0"));
                yField.setText(valueOr(pattern.getAttribute("y"), "0"));
                widthField.setText(valueOr(pattern.getAttribute("width"), "20"));
                heightField.setText(valueOr(pattern.getAttribute("height"), "20"));
            }

            String type = pattern.getAttribute("data-sketsa-pattern-type");
            String a = pattern.getAttribute("data-sketsa-color-a");
            String b = pattern.getAttribute("data-sketsa-color-b");

            if (type == null || type.isEmpty()) {
                typeField.setSelectedItem(TYPE_CUSTOM);
                colorAField.setText("#ffffff");
                colorBField.setText("#808080");
            } else {
                typeField.setSelectedItem(displayType(type));
                colorAField.setText(valueOr(a, "#ffffff"));
                colorBField.setText(valueOr(b, "#808080"));
            }
        } finally {
            loading = false;
        }

        updateManagedControls();
        preview.repaint();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private boolean isRelativeMode() {
        return MODE_RELATIVE.equals(unitsField.getSelectedItem());
    }

    private String toPercent(String domValue) {
        try {
            String v = domValue.trim();
            if (v.endsWith("%")) {
                return v;
            }
            double d = Double.parseDouble(v);
            return formatNumber(d * 100.0) + "%";
        } catch (RuntimeException ex) {
            return domValue;
        }
    }

    private String relativeToDom(String uiValue, String fallbackPercent) {
        String v = valueOr(uiValue, fallbackPercent).trim();
        try {
            if (v.endsWith("%")) {
                double p = Double.parseDouble(v.substring(0, v.length() - 1).trim());
                return formatNumber(p / 100.0);
            }

            /*
             * In Relative mode, a bare number is interpreted as percent too.
             * Example: "25" means 25% -> 0.25 in the SVG DOM.
             */
            double p = Double.parseDouble(v);
            return formatNumber(p / 100.0);
        } catch (RuntimeException ex) {
            return relativeToDom(fallbackPercent, fallbackPercent);
        }
    }

    private String formatNumber(double d) {
        if (Math.abs(d - Math.rint(d)) < 0.0000001) {
            return Long.toString(Math.round(d));
        }
        String s = String.format(java.util.Locale.US, "%.6f", d);
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private void convertFieldsForMode() {
        if (isRelativeMode()) {
            xField.setText(normalizePercentField(xField.getText(), "0%"));
            yField.setText(normalizePercentField(yField.getText(), "0%"));
            widthField.setText(normalizePercentField(widthField.getText(), "20%"));
            heightField.setText(normalizePercentField(heightField.getText(), "20%"));
        } else {
            xField.setText(percentToAbsoluteDisplay(xField.getText(), "0"));
            yField.setText(percentToAbsoluteDisplay(yField.getText(), "0"));
            widthField.setText(percentToAbsoluteDisplay(widthField.getText(), "20"));
            heightField.setText(percentToAbsoluteDisplay(heightField.getText(), "20"));
        }
    }

    private String normalizePercentField(String value, String fallback) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) return fallback;
        if (v.endsWith("%")) return v;

        try {
            /*
             * When switching from Absolute to Relative, reuse the visible
             * number as a percentage for predictable UI behavior.
             */
            Double.parseDouble(v);
            return v + "%";
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private String percentToAbsoluteDisplay(String value, String fallback) {
        String v = value == null ? "" : value.trim();
        if (v.endsWith("%")) {
            v = v.substring(0, v.length() - 1).trim();
        }
        try {
            Double.parseDouble(v);
            return v;
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private String displayType(String key) {
        if ("vertical".equals(key)) return TYPE_VERTICAL;
        if ("horizontal".equals(key)) return TYPE_HORIZONTAL;
        if ("checker".equals(key)) return TYPE_CHECKER;
        if ("dots".equals(key)) return TYPE_DOTS;
        return TYPE_CUSTOM;
    }

    private String typeKey() {
        Object selected = typeField.getSelectedItem();
        String value = selected == null ? TYPE_VERTICAL : selected.toString();
        if (TYPE_HORIZONTAL.equals(value)) return "horizontal";
        if (TYPE_CHECKER.equals(value)) return "checker";
        if (TYPE_DOTS.equals(value)) return "dots";
        if (TYPE_CUSTOM.equals(value)) return "custom";
        return "vertical";
    }

    private boolean isManagedType() {
        return !"custom".equals(typeKey());
    }

    private void updateManagedControls() {
        boolean managed = isManagedType();
        colorAField.setEnabled(managed);
        colorBField.setEnabled(managed);
        colorAButton.setEnabled(managed);
        colorBButton.setEnabled(managed);
        preview.repaint();
    }

    private void updateEnabledState() {
        boolean enabled = canvas != null && cachedElement != null;

        idField.setEnabled(enabled);
        typeField.setEnabled(enabled);
        xField.setEnabled(enabled);
        yField.setEnabled(enabled);
        widthField.setEnabled(enabled);
        heightField.setEnabled(enabled);
        unitsField.setEnabled(enabled);
        createUpdateButton.setEnabled(enabled);
        fillButton.setEnabled(enabled);
        strokeButton.setEnabled(enabled);

        boolean fillPattern = enabled && extractPatternId(getProperty(cachedElement, "fill")) != null;
        boolean strokePattern = enabled && extractPatternId(getProperty(cachedElement, "stroke")) != null;

        removeFillButton.setEnabled(fillPattern);
        removeStrokeButton.setEnabled(strokePattern);

        if (!enabled) status.setText("Select exactly one SVG object.");
        updateManagedControls();
    }

    private Element ensureDefs(Document document) {
        Element root = document.getDocumentElement();

        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                Element e = (Element)n;
                String local = e.getLocalName();
                if (local == null) local = e.getTagName();
                if ("defs".equals(local)) return e;
            }
        }

        Element defs = document.createElementNS(SVG_NS, "defs");
        root.insertBefore(defs, root.getFirstChild());
        return defs;
    }

    private Element findPattern(Document document, String id) {
        if (document == null || id == null || id.isEmpty()) return null;
        return PatternSnapshot.findStatic(document.getDocumentElement(), id);
    }

    private void createOrUpdatePattern() {
        if (loading || canvas == null || cachedElement == null) return;

        String id = idField.getText().trim();
        if (id.startsWith("#")) id = id.substring(1);

        if (id.isEmpty()) {
            status.setText("Pattern ID is required.");
            return;
        }

        if (TYPE_CUSTOM.equals(typeField.getSelectedItem())) {
            status.setText("Choose an editable pattern type before preparing a draft.");
            return;
        }

        String colorA = normalizeColorText(colorAField.getText(), "#ffffff");
        String colorB = normalizeColorText(colorBField.getText(), "#808080");
        colorAField.setText(colorA);
        colorBField.setText(colorB);

        Document document = cachedElement.getOwnerDocument();

        /*
         * IMPORTANT: draft only. This element is detached and therefore cannot
         * affect any existing url(#id) user in the document.
         */
        stagedPattern = buildManagedPattern(document, id, colorA, colorB);
        stagedPatternId = id;

        status.setText("Draft #" + id + " prepared. Choose Apply Fill or Apply Stroke.");
        preview.repaint();
    }

    private Element buildManagedPattern(
            Document document, String id, String colorA, String colorB) {

        Element pattern = document.createElementNS(SVG_NS, "pattern");
        pattern.setAttribute("id", id);

        if (isRelativeMode()) {
            pattern.setAttribute("x", relativeToDom(xField.getText(), "0%"));
            pattern.setAttribute("y", relativeToDom(yField.getText(), "0%"));
            pattern.setAttribute("width", relativeToDom(widthField.getText(), "20%"));
            pattern.setAttribute("height", relativeToDom(heightField.getText(), "20%"));
            pattern.setAttribute("patternUnits", "objectBoundingBox");
        } else {
            pattern.setAttribute("x", valueOr(xField.getText(), "0"));
            pattern.setAttribute("y", valueOr(yField.getText(), "0"));
            pattern.setAttribute("width", valueOr(widthField.getText(), "20"));
            pattern.setAttribute("height", valueOr(heightField.getText(), "20"));
            pattern.setAttribute("patternUnits", "userSpaceOnUse");
        }

        pattern.setAttribute("viewBox", "0 0 1 1");
        pattern.setAttribute("preserveAspectRatio", "none");
        pattern.setAttribute("data-sketsa-pattern-type", typeKey());
        pattern.setAttribute("data-sketsa-color-a", colorA);
        pattern.setAttribute("data-sketsa-color-b", colorB);

        regenerateManagedContent(document, pattern, typeKey(), colorA, colorB);
        return pattern;
    }

    private String normalizeColorText(String value, String fallback) {
        String v = value == null ? "" : value.trim();
        if (v.matches("#[0-9a-fA-F]{6}") || v.matches("#[0-9a-fA-F]{3}")) {
            return v.toLowerCase();
        }
        return fallback;
    }

    private void regenerateManagedContent(
            Document document, Element pattern, String type,
            String colorA, String colorB) {

        while (pattern.hasChildNodes()) {
            pattern.removeChild(pattern.getFirstChild());
        }

        /*
         * Managed pattern content lives in the normalized viewBox 0 0 1 1.
         * These numbers therefore describe fractions of ONE tile, not
         * percentages of the outer SVG viewport.
         */
        if ("vertical".equals(type)) {
            appendRect(document, pattern, "0", "0", "1", "1", colorA);
            appendRect(document, pattern, "0", "0", "0.5", "1", colorB);
            return;
        }

        if ("horizontal".equals(type)) {
            appendRect(document, pattern, "0", "0", "1", "1", colorA);
            appendRect(document, pattern, "0", "0", "1", "0.5", colorB);
            return;
        }

        if ("checker".equals(type)) {
            appendRect(document, pattern, "0", "0", "1", "1", colorA);
            appendRect(document, pattern, "0", "0", "0.5", "0.5", colorB);
            appendRect(document, pattern, "0.5", "0.5", "0.5", "0.5", colorB);
            return;
        }

        if ("dots".equals(type)) {
            appendRect(document, pattern, "0", "0", "1", "1", colorA);
            Element circle = document.createElementNS(SVG_NS, "circle");
            circle.setAttribute("cx", "0.5");
            circle.setAttribute("cy", "0.5");
            circle.setAttribute("r", "0.25");
            circle.setAttribute("fill", colorB);
            pattern.appendChild(circle);
        }
    }

    private void appendRect(Document document, Element parent,
                            String x, String y, String w, String h, String fill) {
        Element rect = document.createElementNS(SVG_NS, "rect");
        rect.setAttribute("x", x);
        rect.setAttribute("y", y);
        rect.setAttribute("width", w);
        rect.setAttribute("height", h);
        rect.setAttribute("fill", fill);
        parent.appendChild(rect);
    }

    private void applyPaint(String property) {
        if (canvas == null || cachedElement == null) return;

        String baseId = idField.getText().trim();
        if (baseId.startsWith("#")) baseId = baseId.substring(1);
        if (baseId.isEmpty()) { status.setText("Pattern ID is required."); return; }

        Document document = cachedElement.getOwnerDocument();
        Element draft;
        if (TYPE_CUSTOM.equals(typeField.getSelectedItem())) {
            Element existing = findPattern(document, baseId);
            if (existing == null) { status.setText("No existing custom pattern #" + baseId + " found."); return; }
            draft = (Element) existing.cloneNode(true);
        } else {
            String colorA = normalizeColorText(colorAField.getText(), "#ffffff");
            String colorB = normalizeColorText(colorBField.getText(), "#808080");
            draft = buildManagedPattern(document, baseId, colorA, colorB);
        }

        String uniqueId = nextUniquePatternId(document, baseId);
        Element appliedPattern = (Element) draft.cloneNode(true);
        appliedPattern.setAttribute("id", uniqueId);

        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, "fill".equals(property) ? "Apply Pattern Fill" : "Apply Pattern Stroke");
        try {
            Element defs = ensureDefs(document);
            defs.appendChild(appliedPattern);
            if (cachedElement instanceof SVGStylable) {
                DOMUtilities.updateProperty((SVGStylable) cachedElement, null, property, "url(#" + uniqueId + ")");
            } else {
                cachedElement.setAttribute(property, "url(#" + uniqueId + ")");
            }
            undo.end();
        } catch (RuntimeException ex) {
            undo.cancel();
            status.setText("Could not apply pattern: " + ex.getMessage());
            return;
        }

        canvas.refresh();
        status.setText("Applied private pattern #" + uniqueId + " as " + property + ".");
        updateEnabledState();
    }


    private String nextUniquePatternId(Document document, String baseId) {
        String base = (baseId == null || baseId.trim().isEmpty())
                ? "pattern"
                : baseId.trim();

        int i = 1;
        String candidate;
        do {
            candidate = base + "-" + i++;
        } while (findPattern(document, candidate) != null);

        return candidate;
    }

    private void removePatternPaint(String property) {
        if (canvas == null || cachedElement == null) return;
        String current = getProperty(cachedElement, property);
        if (extractPatternId(current) == null) {
            status.setText("Selected " + property + " is not a pattern.");
            return;
        }

        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, "fill".equals(property) ? "Remove Pattern Fill" : "Remove Pattern Stroke");
        try {
            if (cachedElement instanceof SVGStylable) {
                DOMUtilities.updateProperty((SVGStylable) cachedElement, null, property, "");
            } else {
                cachedElement.removeAttribute(property);
            }
            undo.end();
        } catch (RuntimeException ex) {
            undo.cancel();
            status.setText("Could not remove pattern: " + ex.getMessage());
            return;
        }

        canvas.refresh();
        status.setText("Pattern removed from " + property + "; definition preserved.");
        updateEnabledState();
    }


    private final class PatternPreview extends JPanel {
        PatternPreview() {
            setPreferredSize(new Dimension(260, 110));
            setMinimumSize(new Dimension(180, 90));
            setBorder(BorderFactory.createTitledBorder("Preview"));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D)g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int left = 10;
                int top = 22;
                int pw = Math.max(1, getWidth() - 20);
                int ph = Math.max(1, getHeight() - 32);

                if (!isManagedType()) {
                    g2.setColor(getForeground());
                    g2.drawString("Custom pattern: content not generated by this editor.",
                            left + 6, top + 25);
                    return;
                }

                Color a = parseColor(colorAField.getText(), Color.WHITE);
                Color b = parseColor(colorBField.getText(), Color.GRAY);

                int tw = previewSize(widthField.getText(), 20);
                int th = previewSize(heightField.getText(), 20);
                tw = Math.max(6, Math.min(80, tw));
                th = Math.max(6, Math.min(80, th));

                String type = typeKey();

                for (int y = top; y < top + ph; y += th) {
                    for (int x = left; x < left + pw; x += tw) {
                        paintTile(g2, x, y, tw, th, type, a, b);
                    }
                }

                g2.setColor(getForeground());
                g2.drawRect(left, top, pw - 1, ph - 1);
            } finally {
                g2.dispose();
            }
        }

        private int previewSize(String text, int fallback) {
            try {
                double d = Double.parseDouble(text.trim().replace("%", ""));
                if (d > 0) return (int)Math.round(d);
            } catch (RuntimeException ex) {
                // fallback
            }
            return fallback;
        }

        private void paintTile(Graphics2D g2, int x, int y, int w, int h,
                               String type, Color a, Color b) {
            g2.setColor(a);
            g2.fillRect(x, y, w, h);

            g2.setColor(b);
            if ("vertical".equals(type)) {
                g2.fillRect(x, y, Math.max(1, w / 2), h);
            } else if ("horizontal".equals(type)) {
                g2.fillRect(x, y, w, Math.max(1, h / 2));
            } else if ("checker".equals(type)) {
                int hw = Math.max(1, w / 2);
                int hh = Math.max(1, h / 2);
                g2.fillRect(x, y, hw, hh);
                g2.fillRect(x + hw, y + hh, w - hw, h - hh);
            } else if ("dots".equals(type)) {
                int diameter = Math.max(2, Math.min(w, h) / 2);
                g2.fillOval(
                        x + (w - diameter) / 2,
                        y + (h - diameter) / 2,
                        diameter, diameter);
            }
        }
    }

    private static final class PrivatePatternApplyEdit
            extends AbstractUndoableEdit {

        private static final long serialVersionUID = 1L;

        private final Element pattern;
        private final Node defsParent;
        private final Element target;
        private final PaintState before;
        private final PaintState after;
        private final VectorCanvas canvas;
        private final String name;

        PrivatePatternApplyEdit(
                Element pattern,
                Element target,
                PaintState before,
                PaintState after,
                VectorCanvas canvas,
                String name) {

            this.pattern = pattern;
            this.defsParent = pattern.getParentNode();
            this.target = target;
            this.before = before;
            this.after = after;
            this.canvas = canvas;
            this.name = name;
        }

        @Override
        public String getPresentationName() {
            return name;
        }

        @Override
        public void undo() {
            super.undo();

            before.apply(target);

            if (pattern.getParentNode() != null) {
                pattern.getParentNode().removeChild(pattern);
            }

            canvas.refresh();
        }

        @Override
        public void redo() {
            super.redo();

            if (pattern.getParentNode() == null) {
                defsParent.appendChild(pattern);
            }

            after.apply(target);
            canvas.refresh();
        }
    }

    private static final class PaintState {
        final String style;
        final String fill;
        final String stroke;

        PaintState(String style, String fill, String stroke) {
            this.style = style == null ? "" : style;
            this.fill = fill == null ? "" : fill;
            this.stroke = stroke == null ? "" : stroke;
        }

        static PaintState capture(Element e) {
            return new PaintState(
                    e.getAttribute("style"),
                    e.getAttribute("fill"),
                    e.getAttribute("stroke"));
        }

        void apply(Element e) {
            setAttr(e, "style", style);
            setAttr(e, "fill", fill);
            setAttr(e, "stroke", stroke);
        }

        private static void setAttr(Element e, String name, String value) {
            if (value == null || value.isEmpty()) e.removeAttribute(name);
            else e.setAttribute(name, value);
        }
    }

    private static final class PaintEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;

        private final Element target;
        private final PaintState before;
        private final PaintState after;
        private final VectorCanvas canvas;
        private final String name;

        PaintEdit(Element target, PaintState before, PaintState after,
                  VectorCanvas canvas, String name) {
            this.target = target;
            this.before = before;
            this.after = after;
            this.canvas = canvas;
            this.name = name;
        }

        @Override
        public String getPresentationName() { return name; }

        @Override
        public void undo() {
            super.undo();
            before.apply(target);
            canvas.refresh();
        }

        @Override
        public void redo() {
            super.redo();
            after.apply(target);
            canvas.refresh();
        }
    }

    private static final class PatternSnapshot {
        final Element patternClone;
        final boolean existed;

        PatternSnapshot(Element patternClone, boolean existed) {
            this.patternClone = patternClone;
            this.existed = existed;
        }

        static PatternSnapshot capture(Document document, String id) {
            Element found = findStatic(document.getDocumentElement(), id);
            return new PatternSnapshot(
                    found == null ? null : (Element)found.cloneNode(true),
                    found != null);
        }

        static Element findStatic(Element e, String id) {
            String local = e.getLocalName();
            if (local == null) local = e.getTagName();

            if ("pattern".equals(local) && id.equals(e.getAttribute("id"))) {
                return e;
            }

            for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element) {
                    Element result = findStatic((Element)n, id);
                    if (result != null) return result;
                }
            }
            return null;
        }
    }

    private static final class PatternDefinitionEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;

        private final Document document;
        private final String id;
        private final PatternSnapshot before;
        private final PatternSnapshot after;
        private final VectorCanvas canvas;

        PatternDefinitionEdit(Document document, String id,
                              PatternSnapshot before, PatternSnapshot after,
                              VectorCanvas canvas) {
            this.document = document;
            this.id = id;
            this.before = before;
            this.after = after;
            this.canvas = canvas;
        }

        @Override
        public String getPresentationName() {
            return before.existed ? "Update Pattern" : "Create Pattern";
        }

        @Override
        public void undo() {
            super.undo();
            applySnapshot(before);
            canvas.refresh();
        }

        @Override
        public void redo() {
            super.redo();
            applySnapshot(after);
            canvas.refresh();
        }

        private void applySnapshot(PatternSnapshot snapshot) {
            Element root = document.getDocumentElement();
            Element current = PatternSnapshot.findStatic(root, id);

            if (!snapshot.existed) {
                if (current != null && current.getParentNode() != null) {
                    current.getParentNode().removeChild(current);
                }
                return;
            }

            Element clone = (Element)snapshot.patternClone.cloneNode(true);

            if (current != null && current.getParentNode() != null) {
                current.getParentNode().replaceChild(clone, current);
                return;
            }

            Element defs = null;
            for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element) {
                    Element e = (Element)n;
                    String local = e.getLocalName();
                    if (local == null) local = e.getTagName();
                    if ("defs".equals(local)) {
                        defs = e;
                        break;
                    }
                }
            }

            if (defs == null) {
                defs = document.createElementNS(SVG_NS, "defs");
                root.insertBefore(defs, root.getFirstChild());
            }
            defs.appendChild(clone);
        }
    }

    /** Register a custom edit inside Sketsa's DOM transaction, not Swing UndoManager's private edit list. */
    private void registerUndoEdit(String name, AbstractUndoableEdit edit) {
        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, name);
        try {
            Field f = DOMUndoManager.class.getDeclaredField("currentEntry");
            f.setAccessible(true);
            Object entry = f.get(undo);
            if (entry == null) throw new IllegalStateException("Sketsa undo transaction was not created");
            java.lang.reflect.Method m = entry.getClass().getMethod("getCompoundEdit");
            CompoundEdit compound = (CompoundEdit)m.invoke(entry);
            if (!compound.addEdit(edit)) throw new IllegalStateException("Could not register undo edit");
            undo.end();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            undo.cancel();
            throw new IllegalStateException("Could not register Sketsa undo edit", ex);
        }
    }

    private final class SelectionHandler extends CanvasSelectionAdapter {
        @Override
        public void valueChanged(CanvasSelectionEvent event) {
            List<SVGElement> list = event.getSelectionList();

            if (list == null || list.size() != 1) {
                cachedSelected = null;
                cachedElement = null;
            } else {
                cachedSelected = list.get(0);
                cachedElement = asElement(cachedSelected);
            }

            if (cachedElement != null) loadPatternFromSelection();
            updateEnabledState();
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
