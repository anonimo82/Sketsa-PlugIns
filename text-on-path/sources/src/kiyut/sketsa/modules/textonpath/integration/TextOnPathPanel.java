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
                SuggestionPopup.install(pathIdField, this::availablePathIds);

lookupResult = Utilities.actionsGlobalContext().lookupResult(SVGEditorCookie.class);
        lookupResult.addLookupListener(lookupListener);

        updateCanvasFromLookup();
        updateEnabledState();
    }

    private java.util.List<String> availablePathIds() {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<String>();
        Document doc = cachedSelected instanceof Element ? ((Element)cachedSelected).getOwnerDocument() : (cachedText != null ? cachedText.getOwnerDocument() : null);
        if (doc != null) {
            org.w3c.dom.NodeList paths = doc.getElementsByTagNameNS("http://www.w3.org/2000/svg", "path");
            if (paths.getLength()==0) paths=doc.getElementsByTagName("path");
            for(int i=0;i<paths.getLength();i++) if(paths.item(i) instanceof Element){String id=((Element)paths.item(i)).getAttribute("id");if(id!=null&&!id.trim().isEmpty())out.add(id.trim());}
        }
        return new java.util.ArrayList<String>(out);
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

        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, "Attach / Update Text Path");
        try {
            Element textPath = firstDirectChild(cachedText, "textPath");
            if (textPath == null) {
                textPath = document.createElementNS(SVG_NS, "textPath");
                textPath.setAttributeNS(null, "href", "#" + pathId);
                textPath.setAttributeNS(XLINK_NS, "xlink:href", "#" + pathId);
                textPath.setAttributeNS(null, "startOffset", offset);

                while (cachedText.hasChildNodes()) {
                    textPath.appendChild(cachedText.getFirstChild());
                }
                cachedText.appendChild(textPath);
            } else {
                /*
                 * Replace the live wrapper inside the Sketsa transaction.
                 * This preserves the Batik relayout behavior required by
                 * percentage offsets while keeping every DOM mutation in the
                 * native Undo/Redo history.
                 */
                Element replacement = document.createElementNS(SVG_NS, "textPath");
                org.w3c.dom.NamedNodeMap attrs = textPath.getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node a = attrs.item(i);
                    String name = a.getNodeName();
                    String ns = a.getNamespaceURI();
                    String local = a.getLocalName();
                    boolean controlled =
                            "href".equals(name)
                            || "startOffset".equals(name)
                            || (XLINK_NS.equals(ns) && "href".equals(local));
                    if (!controlled) replacement.setAttributeNS(ns, name, a.getNodeValue());
                }
                replacement.setAttributeNS(null, "href", "#" + pathId);
                replacement.setAttributeNS(XLINK_NS, "xlink:href", "#" + pathId);
                replacement.setAttributeNS(null, "startOffset", offset);
                while (textPath.hasChildNodes()) {
                    replacement.appendChild(textPath.getFirstChild());
                }
                cachedText.replaceChild(replacement, textPath);
                textPath = replacement;
            }
            cachedTextPath = textPath;
            undo.end();
        } catch (RuntimeException ex) {
            undo.cancel();
            status.setText("Could not attach/update text path: " + ex.getMessage());
            return;
        }

        canvas.refresh();
        selectLiveText();
        loadFields();
        updateEnabledState();
        status.setText("Text attached/updated.");
    }

    private void detach() {
        if (canvas == null || cachedText == null || cachedTextPath == null) return;

        Element textPath = cachedTextPath;
        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, "Detach Text from Path");
        try {
            while (textPath.hasChildNodes()) {
                Node child = textPath.getFirstChild();
                cachedText.insertBefore(child, textPath);
            }
            cachedText.removeChild(textPath);
            cachedTextPath = null;
            undo.end();
        } catch (RuntimeException ex) {
            undo.cancel();
            status.setText("Could not detach text path: " + ex.getMessage());
            return;
        }

        canvas.refresh();
        selectLiveText();
        loadFields();
        updateEnabledState();
        status.setText("Text detached; content preserved.");
    }

    private void setLiveText(Element text) {
        cachedText = text;
        cachedTextPath = firstDirectChild(text, "textPath");
        if (text instanceof SVGElement) cachedSelected = (SVGElement) text;
    }

    private void selectLiveText() {
        if (cachedText instanceof SVGElement) {
            cachedSelected = (SVGElement) cachedText;
            canvas.getCanvasSelection().setSelectionList(
                    Collections.singletonList(cachedSelected));
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
    /** Ensure every plugin edit is a separate Sketsa undo transaction. */
    private static void beginUndoTransaction(DOMUndoManager undo, String name) {
        // end() is a no-op when no transaction is open. If Sketsa left a
        // previous editor transaction pending, commit it before starting ours.
        undo.end();
        undo.start(name);
    }

}
