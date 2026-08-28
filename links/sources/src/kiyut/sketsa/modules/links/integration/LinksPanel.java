package kiyut.sketsa.modules.links.integration;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
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

final class LinksPanel extends JPanel {

    private static final String SVG_NS = "http://www.w3.org/2000/svg";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    private final JTextField hrefField = new JTextField(18);
    private final JComboBox<String> targetField =
            new JComboBox<>(new String[] {"", "_self", "_blank", "_parent", "_top"});
    private final JTextField titleField = new JTextField(18);
    private final JButton applyButton = new JButton("Create / Update");
    private final JButton removeButton = new JButton("Remove Link");
    private final JLabel status = new JLabel("Select one SVG object.");

    private final Lookup.Result<SVGEditorCookie> lookupResult;
    private final LookupListener lookupListener;
    private final SelectionHandler selectionHandler = new SelectionHandler();

    private VectorCanvas canvas;
    private SVGElement cachedSelected;
    private Element cachedLink;
    private boolean loading;

    LinksPanel() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Link"),
                BorderFactory.createEmptyBorder(2, 6, 5, 6)));

        targetField.setEditable(true);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        add(new JLabel("URL:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(hrefField, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        add(new JLabel("Target:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(targetField, c);

        c.gridx = 0; c.gridy = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        add(new JLabel("Title:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        add(titleField, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        buttons.add(applyButton);
        buttons.add(removeButton);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(buttons, c);

        c.gridy = 4;
        add(status, c);

        applyButton.addActionListener(e -> applyLink());
        removeButton.addActionListener(e -> removeLink());
        hrefField.addActionListener(e -> applyLink());
        titleField.addActionListener(e -> applyLink());
        targetField.addActionListener(e -> { if (!loading) applyLink(); });

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
        cachedLink = null;
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
            cachedLink = null;
            clearFields();
            updateEnabledState();
            return;
        }

        cachedSelected = selection.get(0);
        cachedLink = findLink(asElement(cachedSelected));
        loadFields();
        updateEnabledState();
    }

    private Element findLink(Element element) {
        Element current = element;
        while (current != null) {
            String local = current.getLocalName();
            if (local == null || local.isEmpty()) local = current.getTagName();
            if ("a".equals(local) && SVG_NS.equals(current.getNamespaceURI())) {
                return current;
            }
            Node parent = current.getParentNode();
            current = parent instanceof Element ? (Element)parent : null;
        }
        return null;
    }

    private void loadFields() {
        loading = true;
        try {
            if (cachedLink == null) {
                clearFieldsUnsafe();
                status.setText(cachedSelected == null
                        ? "Select one SVG object."
                        : "Selected object is not linked.");
                return;
            }

            String href = cachedLink.getAttribute("href");
            if (href == null || href.isEmpty()) {
                href = cachedLink.getAttributeNS(XLINK_NS, "href");
            }
            hrefField.setText(href == null ? "" : href);
            targetField.setSelectedItem(cachedLink.getAttribute("target"));
            titleField.setText(cachedLink.getAttribute("title"));
            status.setText("Editing existing <a> link.");
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
        hrefField.setText("");
        targetField.setSelectedItem("");
        titleField.setText("");
    }

    private void updateEnabledState() {
        boolean enabled = canvas != null && cachedSelected != null && asElement(cachedSelected) != null;
        hrefField.setEnabled(enabled);
        targetField.setEnabled(enabled);
        titleField.setEnabled(enabled);
        applyButton.setEnabled(enabled);
        removeButton.setEnabled(enabled && cachedLink != null);
        if (!enabled) status.setText("Select exactly one SVG object.");
    }

    private void applyLink() {
        if (loading || canvas == null || cachedSelected == null) return;

        Element selectedElement = asElement(cachedSelected);
        if (selectedElement == null) {
            status.setText("Selection is not a DOM element.");
            return;
        }

        String href = hrefField.getText().trim();
        if (href.isEmpty()) {
            status.setText("Enter a URL or fragment such as #symbol1.");
            return;
        }

        String target = "";
        Object targetValue = targetField.getEditor().getItem();
        if (targetValue != null) target = targetValue.toString().trim();
        String title = titleField.getText().trim();

        DOMUndoManager undo = canvas.getUndoManager();
        boolean creating = cachedLink == null;
        beginUndoTransaction(undo, creating ? "Create Link" : "Update Link");

        try {
            Element link = cachedLink;

            if (link == null) {
                Document document = selectedElement.getOwnerDocument();
                link = document.createElementNS(SVG_NS, "a");

                Node parent = selectedElement.getParentNode();
                if (parent == null) {
                    undo.cancel();
                    status.setText("Selected object cannot be wrapped.");
                    return;
                }

                setLinkAttributes(link, href, target, title);

                parent.replaceChild(link, selectedElement);
                link.appendChild(selectedElement);
                cachedLink = link;
            } else {
                /*
                 * Sketsa's DOMUndoManager reliably records structural DOM
                 * mutations. Attribute-only changes on an existing <a> were
                 * not producing a usable Undo entry, so update the link by
                 * replacing its wrapper while preserving the SAME child nodes.
                 *
                 * Attribute setup happens while the replacement wrapper is
                 * detached. The actual document change is then structural and
                 * therefore participates in Sketsa Undo/Redo.
                 */
                Node parent = link.getParentNode();
                if (parent == null) {
                    undo.cancel();
                    status.setText("Existing link cannot be updated.");
                    return;
                }

                Document document = link.getOwnerDocument();
                Element replacement = document.createElementNS(SVG_NS, "a");

                copyNonLinkAttributes(link, replacement);
                setLinkAttributes(replacement, href, target, title);

                parent.replaceChild(replacement, link);

                while (link.hasChildNodes()) {
                    replacement.appendChild(link.getFirstChild());
                }

                cachedLink = replacement;
            }

        } finally {
            undo.end();
        }

        canvas.refresh();
        canvas.getCanvasSelection().setSelectionList(Collections.singletonList(cachedSelected));

        cachedLink = findLink(selectedElement);
        loadFields();
        updateEnabledState();
        status.setText("Link created/updated.");
    }

    private void setLinkAttributes(
            Element link, String href, String target, String title) {

        link.setAttributeNS(null, "href", href);
        link.setAttributeNS(XLINK_NS, "xlink:href", href);

        if (target.isEmpty()) {
            link.removeAttribute("target");
        } else {
            link.setAttributeNS(null, "target", target);
        }

        if (title.isEmpty()) {
            link.removeAttribute("title");
        } else {
            link.setAttributeNS(null, "title", title);
        }
    }

    private void copyNonLinkAttributes(Element source, Element destination) {
        org.w3c.dom.NamedNodeMap attributes = source.getAttributes();

        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            String namespace = attribute.getNamespaceURI();
            String local = attribute.getLocalName();
            String name = attribute.getNodeName();

            boolean isHref =
                    "href".equals(name)
                    || (XLINK_NS.equals(namespace) && "href".equals(local));

            boolean replacedField =
                    isHref || "target".equals(name) || "title".equals(name);

            if (replacedField) {
                continue;
            }

            if (namespace == null) {
                destination.setAttribute(name, attribute.getNodeValue());
            } else {
                destination.setAttributeNS(
                        namespace, name, attribute.getNodeValue());
            }
        }
    }

    private void removeLink() {
        if (canvas == null || cachedSelected == null || cachedLink == null) return;

        Node parent = cachedLink.getParentNode();
        if (parent == null) {
            status.setText("Link cannot be removed.");
            return;
        }

        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, "Remove Link");
        try {
            while (cachedLink.hasChildNodes()) {
                Node child = cachedLink.getFirstChild();
                parent.insertBefore(child, cachedLink);
            }
            parent.removeChild(cachedLink);
        } finally {
            undo.end();
        }

        cachedLink = null;
        canvas.refresh();
        canvas.getCanvasSelection().setSelectionList(Collections.singletonList(cachedSelected));
        clearFields();
        updateEnabledState();
        status.setText("Link removed; content preserved.");
    }

    private final class SelectionHandler extends CanvasSelectionAdapter {
        @Override
        public void valueChanged(CanvasSelectionEvent event) {
            List<SVGElement> list = event.getSelectionList();
            if (list == null || list.size() != 1) {
                cachedSelected = null;
                cachedLink = null;
            } else {
                cachedSelected = list.get(0);
                cachedLink = findLink(asElement(cachedSelected));
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
