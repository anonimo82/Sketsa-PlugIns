package kiyut.sketsa.modules.symbols.integration;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.reflect.Field;
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

final class SymbolsPanel extends JPanel {

    private static final String SVG_NS = "http://www.w3.org/2000/svg";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    private final JTextField idField = new JTextField("symbol1", 14);
    private final JTextField xField = new JTextField("0", 8);
    private final JTextField yField = new JTextField("0", 8);

    private final JButton createUpdateButton = new JButton("Create / Update Symbol");
    private final JButton insertUseButton = new JButton("Insert Use");
    private final JButton updateUseButton = new JButton("Update Use");
    private final JButton detachUseButton = new JButton("Detach Use");

    private final JLabel status = new JLabel("Select one SVG object.");

    private final Lookup.Result<SVGEditorCookie> lookupResult;
    private final LookupListener lookupListener;
    private final SelectionHandler selectionHandler = new SelectionHandler();

    private VectorCanvas canvas;
    private SVGElement cachedSelected;
    private Element cachedElement;
    private boolean loading;

    SymbolsPanel() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Symbol"),
                BorderFactory.createEmptyBorder(2, 6, 5, 6)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;

        addRow("Symbol ID:", idField, 0, c);
        addRow("Use X:", xField, 1, c);
        addRow("Use Y:", yField, 2, c);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        row1.add(createUpdateButton);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(row1, c);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        row2.add(insertUseButton);
        row2.add(updateUseButton);
        row2.add(detachUseButton);

        c.gridy = 4;
        add(row2, c);

        c.gridy = 5;
        add(status, c);

        createUpdateButton.addActionListener(e -> createOrUpdateSymbol());
        insertUseButton.addActionListener(e -> insertUse());
        updateUseButton.addActionListener(e -> updateUse());
        detachUseButton.addActionListener(e -> detachUse());

        lookupListener = (LookupEvent e) -> updateCanvasFromLookup();
                SuggestionPopup.install(idField, this::availableSymbolIds);

lookupResult = Utilities.actionsGlobalContext().lookupResult(SVGEditorCookie.class);
        lookupResult.addLookupListener(lookupListener);

        updateCanvasFromLookup();
        updateEnabledState();
    }

    private void addRow(String label, java.awt.Component field, int row, GridBagConstraints c) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        c.weightx = 0; c.fill = GridBagConstraints.NONE;
        add(new JLabel(label), c);

        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(field, c);
    }

    private java.util.List<String> availableSymbolIds() {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<String>();
        Document doc = cachedSelected instanceof Element ? ((Element)cachedSelected).getOwnerDocument() : (cachedElement != null ? cachedElement.getOwnerDocument() : null);
        if (doc != null) {
            org.w3c.dom.NodeList syms=doc.getElementsByTagNameNS("http://www.w3.org/2000/svg", "symbol");
            if(syms.getLength()==0)syms=doc.getElementsByTagName("symbol");
            for(int i=0;i<syms.getLength();i++) if(syms.item(i) instanceof Element){String id=((Element)syms.item(i)).getAttribute("id");if(id!=null&&!id.trim().isEmpty())out.add(id.trim());}
        }
        return new java.util.ArrayList<String>(out);
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
            cachedElement = null;
            updateEnabledState();
            return;
        }

        cachedSelected = selection.get(0);
        cachedElement = asElement(cachedSelected);

        loadFromSelection();
        updateEnabledState();
    }

    private void loadFromSelection() {
        if (cachedElement == null) return;

        loading = true;
        try {
            if ("use".equals(localName(cachedElement))) {
                String href = cachedElement.getAttribute("href");
                if (href == null || href.isEmpty()) {
                    href = cachedElement.getAttributeNS(XLINK_NS, "href");
                }
                if (href != null && href.startsWith("#")) {
                    href = href.substring(1);
                }

                idField.setText(href == null ? "" : href);
                xField.setText(valueOr(cachedElement.getAttribute("x"), "0"));
                yField.setText(valueOr(cachedElement.getAttribute("y"), "0"));
                status.setText("Editing selected <use> instance.");
            } else {
                status.setText("Selected object can be stored as a symbol.");
            }
        } finally {
            loading = false;
        }
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private void updateEnabledState() {
        boolean selected = canvas != null && cachedElement != null;
        boolean isUse = selected && "use".equals(localName(cachedElement));

        idField.setEnabled(selected);
        xField.setEnabled(selected);
        yField.setEnabled(selected);

        createUpdateButton.setEnabled(selected && !isUse);
        insertUseButton.setEnabled(selected);
        updateUseButton.setEnabled(isUse);
        detachUseButton.setEnabled(isUse);

        if (!selected) status.setText("Select exactly one SVG object.");
    }

    private Element ensureDefs(Document document) {
        Element root = document.getDocumentElement();

        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                Element e = (Element)n;
                if ("defs".equals(localName(e))) return e;
            }
        }

        Element defs = document.createElementNS(SVG_NS, "defs");
        root.insertBefore(defs, root.getFirstChild());
        return defs;
    }

    private Element findSymbol(Document document, String id) {
        return findSymbolRecursive(document.getDocumentElement(), id);
    }

    private Element findSymbolRecursive(Element e, String id) {
        if ("symbol".equals(localName(e)) && id.equals(e.getAttribute("id"))) {
            return e;
        }

        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                Element found = findSymbolRecursive((Element)n, id);
                if (found != null) return found;
            }
        }

        return null;
    }

    private void createOrUpdateSymbol() {
        if (loading || canvas == null || cachedElement == null) return;
        if ("use".equals(localName(cachedElement))) return;

        String id = idField.getText().trim();
        if (id.startsWith("#")) id = id.substring(1);
        if (id.isEmpty()) { status.setText("Symbol ID is required."); return; }

        Document document = cachedElement.getOwnerDocument();
        Element existing = findSymbol(document, id);
        Element symbol = document.createElementNS(SVG_NS, "symbol");
        symbol.setAttribute("id", id);
        Element clone = (Element) cachedElement.cloneNode(true);
        clone.removeAttribute("id");
        symbol.appendChild(clone);

        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, existing == null ? "Create Symbol" : "Update Symbol");
        try {
            if (existing == null) ensureDefs(document).appendChild(symbol);
            else existing.getParentNode().replaceChild(symbol, existing);
            undo.end();
        } catch (RuntimeException ex) {
            undo.cancel();
            status.setText("Could not create/update symbol: " + ex.getMessage());
            return;
        }

        refreshUseInstances(document.getDocumentElement(), id);
        canvas.refresh();
        status.setText(existing == null ? "Symbol #" + id + " created." : "Symbol #" + id + " updated.");
    }


    private void refreshUseInstances(Element root, String symbolId) {
        if ("use".equals(localName(root))) {
            String href = root.getAttribute("href");
            String xlinkHref = root.getAttributeNS(XLINK_NS, "href");

            boolean matches =
                    ("#" + symbolId).equals(href)
                    || ("#" + symbolId).equals(xlinkHref);

            if (matches) {
                /*
                 * Clear/restore both forms. This does not change semantics;
                 * it only creates a DOM mutation that invalidates Batik's
                 * cached reference/rendering.
                 */
                if (href != null && !href.isEmpty()) {
                    root.removeAttribute("href");
                    root.setAttribute("href", href);
                }

                if (xlinkHref != null && !xlinkHref.isEmpty()) {
                    root.removeAttributeNS(XLINK_NS, "href");
                    root.setAttributeNS(XLINK_NS, "xlink:href", xlinkHref);
                }
            }
        }

        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                refreshUseInstances((Element)n, symbolId);
            }
        }
    }

    private void insertUse() {
        if (canvas == null || cachedElement == null) return;
        String id = idField.getText().trim();
        if (id.startsWith("#")) id = id.substring(1);
        if (id.isEmpty()) { status.setText("Symbol ID is required."); return; }

        Document document = cachedElement.getOwnerDocument();
        if (findSymbol(document, id) == null) {
            status.setText("No <symbol> with id '" + id + "' found.");
            return;
        }

        Element use = document.createElementNS(SVG_NS, "use");
        use.setAttribute("href", "#" + id);
        use.setAttributeNS(XLINK_NS, "xlink:href", "#" + id);
        use.setAttribute("x", valueOr(xField.getText(), "0"));
        use.setAttribute("y", valueOr(yField.getText(), "0"));
        Node parent = cachedElement.getParentNode();
        if (parent == null) { status.setText("Cannot insert instance here."); return; }
        Node next = cachedElement.getNextSibling();

        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, "Insert Symbol Instance");
        try {
            if (next == null) parent.appendChild(use); else parent.insertBefore(use, next);
            undo.end();
        } catch (RuntimeException ex) {
            undo.cancel();
            status.setText("Could not insert symbol instance: " + ex.getMessage());
            return;
        }
        canvas.refresh();
        selectElement(use);
        status.setText("Inserted <use> of #" + id + ".");
    }


    private void updateUse() {
        if (canvas == null || cachedElement == null || !"use".equals(localName(cachedElement))) return;
        String id = idField.getText().trim();
        if (id.startsWith("#")) id = id.substring(1);
        if (id.isEmpty() || findSymbol(cachedElement.getOwnerDocument(), id) == null) {
            status.setText("Referenced symbol does not exist.");
            return;
        }

        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, "Update Symbol Instance");
        try {
            cachedElement.setAttribute("href", "#" + id);
            cachedElement.setAttributeNS(XLINK_NS, "xlink:href", "#" + id);
            cachedElement.setAttribute("x", valueOr(xField.getText(), "0"));
            cachedElement.setAttribute("y", valueOr(yField.getText(), "0"));
            undo.end();
        } catch (RuntimeException ex) {
            undo.cancel();
            status.setText("Could not update symbol instance: " + ex.getMessage());
            return;
        }
        canvas.refresh();
        status.setText("Use instance updated.");
    }


    private void detachUse() {
        if (canvas == null || cachedElement == null || !"use".equals(localName(cachedElement))) return;
        String href = cachedElement.getAttribute("href");
        if (href == null || href.isEmpty()) href = cachedElement.getAttributeNS(XLINK_NS, "href");
        if (href != null && href.startsWith("#")) href = href.substring(1);

        Element symbol = findSymbol(cachedElement.getOwnerDocument(), href);
        if (symbol == null) { status.setText("Referenced symbol cannot be found."); return; }
        Node parent = cachedElement.getParentNode();
        if (parent == null) { status.setText("Cannot detach this instance."); return; }

        Document document = cachedElement.getOwnerDocument();
        Element group = document.createElementNS(SVG_NS, "g");
        String x = valueOr(cachedElement.getAttribute("x"), "0");
        String y = valueOr(cachedElement.getAttribute("y"), "0");
        if (!"0".equals(x) || !"0".equals(y)) group.setAttribute("transform", "translate(" + x + " " + y + ")");
        for (Node n = symbol.getFirstChild(); n != null; n = n.getNextSibling()) group.appendChild(n.cloneNode(true));

        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, "Detach Symbol Instance");
        try {
            parent.replaceChild(group, cachedElement);
            undo.end();
        } catch (RuntimeException ex) {
            undo.cancel();
            status.setText("Could not detach symbol instance: " + ex.getMessage());
            return;
        }
        canvas.refresh();
        selectElement(group);
        status.setText("Use detached; symbol definition preserved.");
    }


    private void selectElement(Element element) {
        if (element instanceof SVGElement) {
            cachedElement = element;
            cachedSelected = (SVGElement)element;
            canvas.getCanvasSelection().setSelectionList(
                    Collections.singletonList(cachedSelected));
        }
    }

    private static final class UseState {
        final String href;
        final String xlinkHref;
        final String x;
        final String y;

        UseState(String href, String xlinkHref, String x, String y) {
            this.href = href == null ? "" : href;
            this.xlinkHref = xlinkHref == null ? "" : xlinkHref;
            this.x = x == null ? "" : x;
            this.y = y == null ? "" : y;
        }

        static UseState capture(Element e) {
            return new UseState(
                    e.getAttribute("href"),
                    e.getAttributeNS(XLINK_NS, "href"),
                    e.getAttribute("x"),
                    e.getAttribute("y"));
        }

        void apply(Element e) {
            setOrRemove(e, null, "href", href);
            setOrRemove(e, XLINK_NS, "xlink:href", xlinkHref);
            setOrRemove(e, null, "x", x);
            setOrRemove(e, null, "y", y);
        }

        private static void setOrRemove(
                Element e, String ns, String qname, String value) {
            if (value == null || value.isEmpty()) {
                if (ns == null) {
                    e.removeAttribute(qname);
                } else {
                    String local = qname.contains(":")
                            ? qname.substring(qname.indexOf(':') + 1)
                            : qname;
                    e.removeAttributeNS(ns, local);
                }
            } else {
                e.setAttributeNS(ns, qname, value);
            }
        }
    }

    private static final class UseStateEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;

        private final Element use;
        private final UseState before;
        private final UseState after;
        private final VectorCanvas canvas;

        UseStateEdit(Element use, UseState before, UseState after, VectorCanvas canvas) {
            this.use = use;
            this.before = before;
            this.after = after;
            this.canvas = canvas;
        }

        @Override
        public String getPresentationName() {
            return "Update Symbol Use";
        }

        @Override
        public void undo() {
            super.undo();
            before.apply(use);
            canvas.refresh();
        }

        @Override
        public void redo() {
            super.redo();
            after.apply(use);
            canvas.refresh();
        }
    }

    private static final class InsertUseEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;

        private final Node parent;
        private final Element use;
        private final Node next;
        private final VectorCanvas canvas;

        InsertUseEdit(Node parent, Element use, Node next, VectorCanvas canvas) {
            this.parent = parent;
            this.use = use;
            this.next = next;
            this.canvas = canvas;
        }

        @Override
        public String getPresentationName() {
            return "Insert Symbol Use";
        }

        @Override
        public void undo() {
            super.undo();
            if (use.getParentNode() != null) {
                use.getParentNode().removeChild(use);
            }
            canvas.refresh();
        }

        @Override
        public void redo() {
            super.redo();
            if (next != null && next.getParentNode() == parent) {
                parent.insertBefore(use, next);
            } else {
                parent.appendChild(use);
            }
            canvas.refresh();
        }
    }

    private static final class DetachUseEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;

        private final Node parent;
        private final Element useSnapshot;
        private final Element group;
        private final Node next;
        private final VectorCanvas canvas;
        private Element liveUse;

        DetachUseEdit(Node parent, Element useSnapshot, Element group,
                      Node next, VectorCanvas canvas) {
            this.parent = parent;
            this.useSnapshot = useSnapshot;
            this.group = group;
            this.next = next;
            this.canvas = canvas;
        }

        @Override
        public String getPresentationName() {
            return "Detach Symbol Use";
        }

        @Override
        public void undo() {
            super.undo();

            liveUse = (Element)useSnapshot.cloneNode(true);

            if (group.getParentNode() == parent) {
                parent.replaceChild(liveUse, group);
            } else if (next != null && next.getParentNode() == parent) {
                parent.insertBefore(liveUse, next);
            } else {
                parent.appendChild(liveUse);
            }

            canvas.refresh();
        }

        @Override
        public void redo() {
            super.redo();

            if (liveUse != null && liveUse.getParentNode() == parent) {
                parent.replaceChild(group, liveUse);
            }

            canvas.refresh();
        }
    }

    private static final class SymbolDefinitionEdit extends AbstractUndoableEdit {
        private static final long serialVersionUID = 1L;

        private final Document document;
        private final String id;
        private final Element before;
        private final Element after;
        private final VectorCanvas canvas;
        private final boolean existedBefore;

        SymbolDefinitionEdit(
                Document document, String id,
                Element before, Element after,
                VectorCanvas canvas, boolean existedBefore) {
            this.document = document;
            this.id = id;
            this.before = before;
            this.after = after;
            this.canvas = canvas;
            this.existedBefore = existedBefore;
        }

        @Override
        public String getPresentationName() {
            return existedBefore ? "Update Symbol" : "Create Symbol";
        }

        @Override
        public void undo() {
            super.undo();
            apply(before);
            refreshUses();
            canvas.refresh();
        }

        @Override
        public void redo() {
            super.redo();
            apply(after);
            refreshUses();
            canvas.refresh();
        }

        private void refreshUses() {
            Element root = document.getDocumentElement();
            refreshUsesRecursive(root);
        }

        private void refreshUsesRecursive(Element e) {
            String local = e.getLocalName();
            if (local == null) local = e.getTagName();

            if ("use".equals(local)) {
                String href = e.getAttribute("href");
                String xlinkHref = e.getAttributeNS(XLINK_NS, "href");

                if (("#" + id).equals(href) || ("#" + id).equals(xlinkHref)) {
                    if (href != null && !href.isEmpty()) {
                        e.removeAttribute("href");
                        e.setAttribute("href", href);
                    }
                    if (xlinkHref != null && !xlinkHref.isEmpty()) {
                        e.removeAttributeNS(XLINK_NS, "href");
                        e.setAttributeNS(XLINK_NS, "xlink:href", xlinkHref);
                    }
                }
            }

            for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element) {
                    refreshUsesRecursive((Element)n);
                }
            }
        }

        private void apply(Element snapshot) {
            Element root = document.getDocumentElement();
            Element current = find(root, id);

            if (snapshot == null) {
                if (current != null && current.getParentNode() != null) {
                    current.getParentNode().removeChild(current);
                }
                return;
            }

            Element clone = (Element)snapshot.cloneNode(true);

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

        private Element find(Element e, String wantedId) {
            String local = e.getLocalName();
            if (local == null) local = e.getTagName();

            if ("symbol".equals(local) && wantedId.equals(e.getAttribute("id"))) {
                return e;
            }

            for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element) {
                    Element found = find((Element)n, wantedId);
                    if (found != null) return found;
                }
            }

            return null;
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

            if (cachedElement != null) {
                loadFromSelection();
            }

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
