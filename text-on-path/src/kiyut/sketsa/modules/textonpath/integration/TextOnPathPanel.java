package kiyut.sketsa.modules.textonpath.integration;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CompoundEdit;
import java.lang.reflect.Field;
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

final class TextOnPathPanel extends JPanel {

    private static final String SVG_NS = "http://www.w3.org/2000/svg";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    private final JTextField pathIdField = new JTextField(14);
    private final JTextField offsetField = new JTextField("0", 8);
    private final JButton attachButton = new JButton("Attach / Update");
    private final JButton detachButton = new JButton("Detach");
    private final JLabel status = new JLabel("Select one text object.");

    private final Lookup.Result<SVGEditorCookie> lookupResult;
    private final LookupListener lookupListener;
    private final SelectionHandler selectionHandler = new SelectionHandler();

    private VectorCanvas canvas;
    private SVGElement cachedSelected;
    private Element cachedText;
    private Element cachedTextPath;
    private boolean loading;

    TextOnPathPanel() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Text on Path"),
                BorderFactory.createEmptyBorder(2, 6, 5, 6)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        add(new JLabel("Path ID:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(pathIdField, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        add(new JLabel("Start offset:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(offsetField, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        buttons.add(attachButton);
        buttons.add(detachButton);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(buttons, c);

        c.gridy = 3;
        add(status, c);

        attachButton.addActionListener(e -> attachOrUpdate());
        detachButton.addActionListener(e -> detach());
        pathIdField.addActionListener(e -> attachOrUpdate());
        offsetField.addActionListener(e -> attachOrUpdate());

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
            if (cookie.isOpened()) setCanvas(cookie.getVectorCanvas());
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
        cachedText = null;
        cachedTextPath = null;

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

        List<SVGElement> selection = canvas.getCanvasSelection().getSelectionList();
        if (selection == null || selection.size() != 1) {
            cachedSelected = null;
            cachedText = null;
            cachedTextPath = null;
            clearFields();
            updateEnabledState();
            return;
        }

        cachedSelected = selection.get(0);
        Element e = asElement(cachedSelected);
        resolveTextContext(e);
        loadFields();
        updateEnabledState();
    }

    private void resolveTextContext(Element e) {
        cachedText = null;
        cachedTextPath = null;

        if (e == null) return;

        if ("text".equals(localName(e))) {
            cachedText = e;
            cachedTextPath = firstDirectChild(e, "textPath");
            return;
        }

        if ("textPath".equals(localName(e))) {
            cachedTextPath = e;
            Node p = e.getParentNode();
            if (p instanceof Element && "text".equals(localName((Element)p))) {
                cachedText = (Element)p;
            }
            return;
        }

        // If Sketsa selected tspan/text child, walk up to <text>.
        Element current = e;
        while (current != null) {
            if ("textPath".equals(localName(current))) {
                cachedTextPath = current;
            }
            if ("text".equals(localName(current))) {
                cachedText = current;
                if (cachedTextPath == null) {
                    cachedTextPath = firstDirectChild(current, "textPath");
                }
                return;
            }
            Node p = current.getParentNode();
            current = p instanceof Element ? (Element)p : null;
        }
    }

    private Element firstDirectChild(Element parent, String wanted) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && wanted.equals(localName((Element)n))) {
                return (Element)n;
            }
        }
        return null;
    }

    private void loadFields() {
        loading = true;
        try {
            if (cachedText == null) {
                clearFieldsUnsafe();
                status.setText("Select one text object.");
                return;
            }

            if (cachedTextPath == null) {
                clearFieldsUnsafe();
                status.setText("Selected text is not on a path.");
                return;
            }

            String href = cachedTextPath.getAttribute("href");
            if (href == null || href.isEmpty()) {
                href = cachedTextPath.getAttributeNS(XLINK_NS, "href");
            }

            if (href != null && href.startsWith("#")) {
                href = href.substring(1);
            }

            pathIdField.setText(href == null ? "" : href);
            String offset = cachedTextPath.getAttribute("startOffset");
            offsetField.setText(offset == null || offset.isEmpty() ? "0" : offset);
            status.setText("Editing existing <textPath>.");
        } finally {
            loading = false;
        }
    }

    private void clearFields() {
        loading = true;
        try {
            clearFieldsUnsafe();
        } finally {
            loading = false;
        }
    }

    private void clearFieldsUnsafe() {
        pathIdField.setText("");
        offsetField.setText("0");
    }

    private void updateEnabledState() {
        boolean ok = canvas != null && cachedText != null;
        pathIdField.setEnabled(ok);
        offsetField.setEnabled(ok);
        attachButton.setEnabled(ok);
        detachButton.setEnabled(ok && cachedTextPath != null);
        if (!ok) status.setText("Select one text object.");
    }

    private Element findElementById(Document document, String id) {
        if (document == null || id == null || id.isEmpty()) return null;

        Element root = document.getDocumentElement();
        return findElementByIdRecursive(root, id);
    }

    private Element findElementByIdRecursive(Element e, String id) {
        if (id.equals(e.getAttribute("id"))) return e;

        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                Element found = findElementByIdRecursive((Element)n, id);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void attachOrUpdate() {
        if (loading || canvas == null || cachedText == null) return;

        String pathId = pathIdField.getText().trim();
        if (pathId.startsWith("#")) pathId = pathId.substring(1);

        if (pathId.isEmpty()) {
            status.setText("Enter the ID of a <path> element.");
            return;
        }

        Document document = cachedText.getOwnerDocument();
        Element path = findElementById(document, pathId);
        if (path == null || !"path".equals(localName(path))) {
            status.setText("No <path> with id '" + pathId + "' found.");
            return;
        }

        String offset = offsetField.getText().trim();
        if (offset.isEmpty()) offset = "0";

        Element before = (Element) cachedText.cloneNode(true);
        Element after = (Element) cachedText.cloneNode(true);

        Element afterTextPath = firstDirectChild(after, "textPath");

        if (afterTextPath == null) {
            Element textPath = document.createElementNS(SVG_NS, "textPath");
            textPath.setAttributeNS(null, "href", "#" + pathId);
            textPath.setAttributeNS(XLINK_NS, "xlink:href", "#" + pathId);
            textPath.setAttributeNS(null, "startOffset", offset);

            while (after.hasChildNodes()) {
                textPath.appendChild(after.getFirstChild());
            }
            after.appendChild(textPath);
        } else {
            /*
             * Replace the textPath wrapper itself instead of changing only
             * startOffset in place. Batik then rebuilds text-on-path layout
             * immediately and percentage offsets are visibly applied.
             */
            Element replacement = document.createElementNS(SVG_NS, "textPath");

            // Preserve all non-link/non-offset attributes.
            org.w3c.dom.NamedNodeMap attrs = afterTextPath.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node a = attrs.item(i);
                String name = a.getNodeName();
                String ns = a.getNamespaceURI();
                String local = a.getLocalName();

                boolean controlled =
                        "href".equals(name)
                        || "startOffset".equals(name)
                        || (XLINK_NS.equals(ns) && "href".equals(local));

                if (!controlled) {
                    replacement.setAttributeNS(ns, name, a.getNodeValue());
                }
            }

            replacement.setAttributeNS(null, "href", "#" + pathId);
            replacement.setAttributeNS(XLINK_NS, "xlink:href", "#" + pathId);
            replacement.setAttributeNS(null, "startOffset", offset);

            while (afterTextPath.hasChildNodes()) {
                replacement.appendChild(afterTextPath.getFirstChild());
            }
            after.replaceChild(replacement, afterTextPath);
        }

        replaceTextAndRegisterUndo(
                "Attach / Update Text Path", before, after);

        status.setText("Text attached/updated.");
    }

    private void detach() {
        if (canvas == null || cachedText == null || cachedTextPath == null) return;

        Element before = (Element) cachedText.cloneNode(true);
        Element after = (Element) cachedText.cloneNode(true);
        Element textPath = firstDirectChild(after, "textPath");

        if (textPath == null) {
            status.setText("No textPath to detach.");
            return;
        }

        while (textPath.hasChildNodes()) {
            Node child = textPath.getFirstChild();
            after.insertBefore(child, textPath);
        }
        after.removeChild(textPath);

        replaceTextAndRegisterUndo("Detach Text from Path", before, after);
        status.setText("Text detached; content preserved.");
    }

    private void replaceTextAndRegisterUndo(
            String name, Element beforeSnapshot, Element afterSnapshot) {

        if (cachedText == null || canvas == null) return;

        Node parent = cachedText.getParentNode();
        if (parent == null) {
            status.setText("Text object cannot be replaced.");
            return;
        }

        Element oldLive = cachedText;
        Element newLive = (Element) afterSnapshot.cloneNode(true);

        parent.replaceChild(newLive, oldLive);
        setLiveText(newLive);

        canvas.refresh();
        selectLiveText();

        DOMUndoManager undo = canvas.getUndoManager();
        undo.start(name);
        try {
            CompoundEdit compound = currentCompoundEdit(undo);
            compound.addEdit(new TextSnapshotEdit(
                    name,
                    parent,
                    beforeSnapshot,
                    afterSnapshot));
        } finally {
            undo.end();
        }

        loadFields();
        updateEnabledState();
    }

    private CompoundEdit currentCompoundEdit(DOMUndoManager undo) {
        try {
            Field f = DOMUndoManager.class.getDeclaredField("currentEntry");
            f.setAccessible(true);
            Object entry = f.get(undo);
            if (entry == null) {
                throw new IllegalStateException("DOMUndoManager has no current entry");
            }
            java.lang.reflect.Method m =
                    entry.getClass().getMethod("getCompoundEdit");
            return (CompoundEdit) m.invoke(entry);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                    "Cannot access Sketsa undo transaction", ex);
        }
    }

    private void setLiveText(Element text) {
        cachedText = text;
        cachedTextPath = firstDirectChild(text, "textPath");
        if (text instanceof SVGElement) {
            cachedSelected = (SVGElement) text;
        }
    }

    private void selectLiveText() {
        if (cachedText instanceof SVGElement) {
            cachedSelected = (SVGElement) cachedText;
            canvas.getCanvasSelection().setSelectionList(
                    Collections.singletonList(cachedSelected));
        }
    }

    private final class TextSnapshotEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final Node parent;
        private final Element before;
        private final Element after;
        private Element live;

        TextSnapshotEdit(
                String name, Node parent, Element before, Element after) {
            this.name = name;
            this.parent = parent;
            this.before = (Element) before.cloneNode(true);
            this.after = (Element) after.cloneNode(true);
            this.live = cachedText;
        }

        @Override
        public String getPresentationName() {
            return name;
        }

        @Override
        public void undo() {
            super.undo();
            Element restored = (Element) before.cloneNode(true);
            parent.replaceChild(restored, live);
            live = restored;
            setLiveText(restored);
            canvas.refresh();
            selectLiveText();
            loadFields();
            updateEnabledState();
        }

        @Override
        public void redo() {
            super.redo();
            Element restored = (Element) after.cloneNode(true);
            parent.replaceChild(restored, live);
            live = restored;
            setLiveText(restored);
            canvas.refresh();
            selectLiveText();
            loadFields();
            updateEnabledState();
        }
    }

    private final class SelectionHandler extends CanvasSelectionAdapter {
        @Override
        public void valueChanged(CanvasSelectionEvent event) {
            List<SVGElement> list = event.getSelectionList();

            if (list == null || list.size() != 1) {
                cachedSelected = null;
                cachedText = null;
                cachedTextPath = null;
            } else {
                cachedSelected = list.get(0);
                resolveTextContext(asElement(cachedSelected));
            }

            loadFields();
            updateEnabledState();
        }
    }
}
