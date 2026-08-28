package kiyut.sketsa.modules.audio.integration;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import kiyut.sketsa.canvas.VectorCanvas;
import kiyut.sketsa.cookies.SVGEditorCookie;
import kiyut.sketsa.undo.DOMUndoManager;
import kiyut.sketsa.undo.DOMUndoableEdit;
import java.lang.reflect.Field;
import kiyut.swing.tree.dom.DOMTree;
import kiyut.swing.tree.dom.DOMTreeModel;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.traversal.NodeFilter;

/**
 * Hierarchical authoring model for Audio.  The UI deliberately reuses
 * Sketsa's DOMTree/DOMTreeModel infrastructure used by the DOM Editor.
 *
 * Ownership is represented by the DOM hierarchy. Non-hierarchical audio
 * connections are represented by first-class reference elements that point
 * to stable IDs.
 */
final class AudioTreePanel extends JPanel {

    static final String AUDIO_TREE_NS = "urn:sketsa:audio-tree:1";
    static final String META_ID = "sketsa-audio-tree";
    static final String META_VERSION = "1.0";

    private final DOMTree tree = new DOMTree();
    private final JLabel status = new JLabel("Open an SVG document to edit its Audio Tree.");
    private final JTextArea validationMessages = new JTextArea(6, 40);
    private final JButton createTree = new JButton("Create Audio Tree");
    private final JButton addChild = new JButton("Add Child");
    private final JButton addReference = new JButton("Add Reference");
    private final JButton remove = new JButton("Remove");
    private final JButton moveUp = new JButton("Up");
    private final JButton moveDown = new JButton("Down");
    private final JButton validateRouting = new JButton("Validate Routing");
    private final JButton exportRuntime = new JButton("Export Runtime");
    private final JButton duplicateBranch = new JButton("Duplicate Branch");
    private final JButton exportTree = new JButton("Export Tree");
    private final JButton importTree = new JButton("Import Tree");

    private final JTextField stableId = new JTextField();
    private final JTextField label = new JTextField();
    private final JComboBox<String> type = new JComboBox<>(new String[]{
        "master", "bus", "return", "source", "gain", "pan", "filter", "compressor", "analyser", "delay", "reverb", "lfo", "effect", "group", "automation"
    });
    private final JComboBox<String> referenceRole = new JComboBox<>(new String[]{
        "route", "send", "sidechain", "modulation", "event", "event-target"
    });
    private final JTextField targetId = new JTextField();
    private final JTextField referenceAmount = new JTextField("1.0");
    private final JTextField referenceTargetParam = new JTextField("gain");
    private final JTextField referenceEvent = new JTextField();
    private final JComboBox<String> referenceAction = new JComboBox<>(new String[]{"trigger", "start", "stop", "toggle", "set-param"});
    private final JTextField referenceScale = new JTextField("1.0");
    private final JTextField referenceOffset = new JTextField("0.0");
    private final JTextField referenceMin = new JTextField();
    private final JTextField referenceMax = new JTextField();

    // T3 core-node parameters. The inspector shows only the fields relevant
    // to the currently selected node type, while the DOM remains the source
    // of truth for persistence/Undo/Redo.
    private final JComboBox<String> sourceWaveform = new JComboBox<>(new String[]{"sine", "square", "sawtooth", "triangle"});
    private final JTextField sourceFrequency = new JTextField("440");
    private final JTextField sourceLevel = new JTextField("0.05");
    private final JTextField gainValue = new JTextField("1.0");
    private final JTextField panValue = new JTextField("0.0");
    private final JComboBox<String> filterType = new JComboBox<>(new String[]{"lowpass", "highpass", "bandpass", "lowshelf", "highshelf", "peaking", "notch", "allpass"});
    private final JTextField filterFrequency = new JTextField("1200");
    private final JTextField filterQ = new JTextField("0.707");
    private final JTextField filterGain = new JTextField("0");
    private final JTextField compressorThreshold = new JTextField("-24");
    private final JTextField compressorKnee = new JTextField("30");
    private final JTextField compressorRatio = new JTextField("12");
    private final JTextField compressorAttack = new JTextField("0.003");
    private final JTextField compressorRelease = new JTextField("0.25");
    private final JComboBox<String> analyserFftSize = new JComboBox<>(new String[]{"32", "64", "128", "256", "512", "1024", "2048", "4096", "8192", "16384", "32768"});
    private final JTextField analyserSmoothing = new JTextField("0.8");
    private final JTextField returnGain = new JTextField("1.0");
    private final JTextField delayTime = new JTextField("0.28");
    private final JTextField reverbDuration = new JTextField("1.5");
    private final JTextField reverbDecay = new JTextField("2.0");
    private final JComboBox<String> lfoWaveform = new JComboBox<>(new String[]{"sine", "square", "sawtooth", "triangle"});
    private final JTextField lfoFrequency = new JTextField("2.0");
    private final JTextField lfoDepth = new JTextField("0.25");
    private final JTextField automationCurve = new JTextField("0:0,1:1");
    private final Map<String,JPanel> parameterRows = new LinkedHashMap<>();
    private final JButton applyProperties = new JButton("Apply Properties");
    private final JButton goToTarget = new JButton("Go to Target");

    private final Lookup.Result<SVGEditorCookie> lookupResult;
    private final LookupListener lookupListener;
    private VectorCanvas canvas;
    private SVGDocument document;
    private Element selectedModelElement;
    private boolean loading;

    AudioTreePanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new AudioTreeRenderer());
        ((DOMTreeModel) tree.getModel()).setNodeFilter(new AudioTreeFilter());
        tree.addTreeSelectionListener(e -> updateSelectedModelElement());
        tree.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) goToReferenceTarget();
            }
        });

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        toolbar.add(createTree);
        toolbar.add(addChild);
        toolbar.add(addReference);
        toolbar.add(remove);
        toolbar.add(moveUp);
        toolbar.add(moveDown);
        toolbar.add(duplicateBranch);
        toolbar.add(exportTree);
        toolbar.add(importTree);
        toolbar.add(validateRouting);
        toolbar.add(exportRuntime);

        JPanel inspector = buildInspector();
        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.getVerticalScrollBar().setUnitIncrement(18);
        JScrollPane inspectorScroll = new JScrollPane(inspector);
        inspectorScroll.getVerticalScrollBar().setUnitIncrement(18);
        inspectorScroll.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, inspectorScroll);
        split.setResizeWeight(0.62);
        split.setContinuousLayout(true);

        JPanel north = new JPanel(new BorderLayout(4, 4));
        north.add(toolbar, BorderLayout.CENTER);
        north.add(status, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        validationMessages.setEditable(false);
        validationMessages.setLineWrap(false);
        validationMessages.setText("Validation Messages\nNo validation has been run yet.");
        JScrollPane validationScroll = new JScrollPane(validationMessages);
        validationScroll.setBorder(BorderFactory.createTitledBorder("Validation Messages"));
        validationScroll.getVerticalScrollBar().setUnitIncrement(18);
        add(validationScroll, BorderLayout.SOUTH);

        createTree.addActionListener(e -> createAudioTree());
        addChild.addActionListener(e -> addOwnedNode());
        addReference.addActionListener(e -> addReferenceNode());
        remove.addActionListener(e -> removeSelected());
        moveUp.addActionListener(e -> moveSelected(-1));
        moveDown.addActionListener(e -> moveSelected(1));
        duplicateBranch.addActionListener(e -> duplicateSelectedBranch());
        exportTree.addActionListener(e -> exportSelectedTree());
        importTree.addActionListener(e -> importTreeFragment());
        applyProperties.addActionListener(e -> applySelectedProperties());
        goToTarget.addActionListener(e -> goToReferenceTarget());
        type.addActionListener(e -> { if (!loading) updateParameterVisibility(); });
        referenceRole.addActionListener(e -> { if (!loading) updateButtons(); });
        referenceAction.addActionListener(e -> { if (!loading) updateButtons(); });
        validateRouting.addActionListener(e -> validateRouting());
        exportRuntime.addActionListener(e -> exportTreeRuntime());

        stableId.setEditable(false);

        lookupResult = Utilities.actionsGlobalContext().lookupResult(SVGEditorCookie.class);
        lookupListener = (LookupEvent ev) -> updateCanvasFromLookup();
        lookupResult.addLookupListener(lookupListener);
        updateCanvasFromLookup();
        updateButtons();
    }

    private JPanel buildInspector() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Audio Tree item"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        int row = 0;
        addInspectorRow(p, c, row++, "Stable ID", stableId);
        addInspectorRow(p, c, row++, "Label", label);
        addInspectorRow(p, c, row++, "Type", type);
        addInspectorRow(p, c, row++, "Reference role", referenceRole);
        addInspectorRow(p, c, row++, "Target ID", targetId);
        addInspectorRow(p, c, row++, "Amount / depth", referenceAmount);
        addInspectorRow(p, c, row++, "Target parameter", referenceTargetParam);
        addInspectorRow(p, c, row++, "Event name", referenceEvent);
        addInspectorRow(p, c, row++, "Event action", referenceAction);
        addInspectorRow(p, c, row++, "Event scale", referenceScale);
        addInspectorRow(p, c, row++, "Event offset", referenceOffset);
        addInspectorRow(p, c, row++, "Event min", referenceMin);
        addInspectorRow(p, c, row++, "Event max", referenceMax);
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        p.add(buildNodeParameterPanel(), c);
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        p.add(applyProperties, c);
        c.gridy = row++;
        p.add(goToTarget, c);
        c.gridy = row; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        p.add(new JPanel(), c);
        return p;
    }

    private JPanel buildNodeParameterPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("Core node parameters"));
        addParameterRow(p, "source.waveform", "Waveform", sourceWaveform);
        addParameterRow(p, "source.frequency", "Frequency (Hz)", sourceFrequency);
        addParameterRow(p, "source.level", "Output level", sourceLevel);
        addParameterRow(p, "gain.value", "Gain", gainValue);
        addParameterRow(p, "pan.value", "Stereo pan", panValue);
        addParameterRow(p, "filter.type", "Filter type", filterType);
        addParameterRow(p, "filter.frequency", "Frequency (Hz)", filterFrequency);
        addParameterRow(p, "filter.q", "Q", filterQ);
        addParameterRow(p, "filter.gain", "Filter gain (dB)", filterGain);
        addParameterRow(p, "compressor.threshold", "Threshold (dB)", compressorThreshold);
        addParameterRow(p, "compressor.knee", "Knee (dB)", compressorKnee);
        addParameterRow(p, "compressor.ratio", "Ratio", compressorRatio);
        addParameterRow(p, "compressor.attack", "Attack (s)", compressorAttack);
        addParameterRow(p, "compressor.release", "Release (s)", compressorRelease);
        addParameterRow(p, "analyser.fft", "FFT size", analyserFftSize);
        addParameterRow(p, "analyser.smoothing", "Smoothing", analyserSmoothing);
        addParameterRow(p, "return.gain", "Return gain", returnGain);
        addParameterRow(p, "delay.time", "Delay time (s)", delayTime);
        addParameterRow(p, "reverb.duration", "Impulse duration (s)", reverbDuration);
        addParameterRow(p, "reverb.decay", "Decay", reverbDecay);
        addParameterRow(p, "lfo.waveform", "LFO waveform", lfoWaveform);
        addParameterRow(p, "lfo.frequency", "LFO frequency (Hz)", lfoFrequency);
        addParameterRow(p, "lfo.depth", "LFO depth", lfoDepth);
        addParameterRow(p, "automation.curve", "Automation curve", automationCurve);
        return p;
    }

    private void addParameterRow(JPanel parent, String key, String name, java.awt.Component value) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        JLabel l = new JLabel(name);
        l.setPreferredSize(new java.awt.Dimension(110, l.getPreferredSize().height));
        row.add(l, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);
        row.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        parameterRows.put(key, row);
        parent.add(row);
    }

    private void updateParameterVisibility() {
        String t = selectedModelElement != null && "node".equals(modelLocalName(selectedModelElement))
                ? String.valueOf(type.getSelectedItem()) : "";
        for (Map.Entry<String,JPanel> entry : parameterRows.entrySet()) {
            String key = entry.getKey();
            boolean show = ("source".equals(t) && key.startsWith("source."))
                    || ("gain".equals(t) && key.startsWith("gain."))
                    || ("pan".equals(t) && key.startsWith("pan."))
                    || ("filter".equals(t) && key.startsWith("filter."))
                    || ("compressor".equals(t) && key.startsWith("compressor."))
                    || ("analyser".equals(t) && key.startsWith("analyser."))
                    || ("return".equals(t) && key.startsWith("return."))
                    || ("delay".equals(t) && key.startsWith("delay."))
                    || ("reverb".equals(t) && key.startsWith("reverb."))
                    || ("lfo".equals(t) && key.startsWith("lfo."))
                    || ("automation".equals(t) && key.startsWith("automation."));
            entry.getValue().setVisible(show);
        }
        revalidate();
        repaint();
    }

    private static void addInspectorRow(JPanel p, GridBagConstraints c, int row, String name, java.awt.Component value) {
        c.gridy = row; c.gridx = 0; c.gridwidth = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        p.add(new JLabel(name), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        p.add(value, c);
    }

    private void updateCanvasFromLookup() {
        Collection<? extends SVGEditorCookie> cookies = lookupResult.allInstances();
        // When focus moves from the SVG editor to this independent TopComponent,
        // NetBeans' global action context can temporarily contain no SVGEditorCookie.
        // Do not detach from the last valid canvas in that case: doing so left the
        // visible DOM tree stale while disabling every editing command. This mirrors
        // the behaviour already used by the stable Audio inspector.
        if (cookies.isEmpty()) return;
        SVGEditorCookie cookie = cookies.iterator().next();
        if (!cookie.isOpened()) return;
        setCanvas(cookie.getVectorCanvas());
    }

    private void setCanvas(VectorCanvas newCanvas) {
        canvas = newCanvas;
        SVGDocument newDocument = canvas == null ? null : canvas.getSVGDocument();
        if (document != newDocument) {
            document = newDocument;
            selectedModelElement = null;
            tree.setDocument(document);
            expandAudioTree();
        }
        status.setText(document == null
                ? "Open an SVG document to edit its Audio Tree."
                : (findMetadataRoot() == null ? "No Audio Tree metadata yet." : "Audio Tree loaded from SVG metadata."));
        updateButtons();
    }

    private void expandAudioTree() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
        });
    }

    private void updateSelectedModelElement() {
        Node node = null;
        TreePath path = tree.getSelectionPath();
        if (path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode) {
            Object user = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (user instanceof Node) node = (Node) user;
        }
        if (node == null) node = tree.getLastSelectedDOMNode();
        selectedModelElement = node instanceof Element && isAudioTreeModelElement((Element) node)
                ? (Element) node : null;
        loadInspector();
        updateButtons();
        if (selectedModelElement != null) {
            status.setText("Selected: " + displayName(selectedModelElement) + " [" + selectedModelElement.getAttribute("id") + "]");
        }
    }

    private void loadInspector() {
        loading = true;
        try {
            if (selectedModelElement == null) {
                stableId.setText(""); label.setText(""); targetId.setText(""); referenceAmount.setText("1.0"); referenceTargetParam.setText("gain"); referenceEvent.setText(""); referenceAction.setSelectedItem("trigger"); referenceScale.setText("1.0"); referenceOffset.setText("0.0"); referenceMin.setText(""); referenceMax.setText("");
                type.setSelectedItem("group"); referenceRole.setSelectedItem("route");
                loadNodeParameterDefaults(null);
                return;
            }
            stableId.setText(selectedModelElement.getAttribute("id"));
            label.setText(selectedModelElement.getAttribute("label"));
            String local = modelLocalName(selectedModelElement);
            if ("node".equals(local)) {
                type.setSelectedItem(defaultString(selectedModelElement.getAttribute("type"), "group"));
                targetId.setText("");
                referenceAmount.setText("1.0");
                referenceTargetParam.setText("gain");
                referenceEvent.setText(""); referenceAction.setSelectedItem("trigger"); referenceScale.setText("1.0"); referenceOffset.setText("0.0"); referenceMin.setText(""); referenceMax.setText("");
                referenceRole.setSelectedItem("route");
                loadNodeParameterDefaults(selectedModelElement);
            } else if ("reference".equals(local)) {
                referenceRole.setSelectedItem(defaultString(selectedModelElement.getAttribute("role"), "route"));
                targetId.setText(selectedModelElement.getAttribute("targetId"));
                referenceAmount.setText(defaultString(selectedModelElement.getAttribute("amount"), "1.0"));
                referenceTargetParam.setText(defaultString(selectedModelElement.getAttribute("targetParam"), "gain"));
                referenceEvent.setText(selectedModelElement.getAttribute("event"));
                referenceAction.setSelectedItem(defaultString(selectedModelElement.getAttribute("action"), "trigger"));
                referenceScale.setText(defaultString(selectedModelElement.getAttribute("scale"), "1.0"));
                referenceOffset.setText(defaultString(selectedModelElement.getAttribute("offset"), "0.0"));
                referenceMin.setText(selectedModelElement.getAttribute("min"));
                referenceMax.setText(selectedModelElement.getAttribute("max"));
            }
        } finally {
            loading = false;
        }
        updateParameterVisibility();
    }

    private void loadNodeParameterDefaults(Element e) {
        sourceWaveform.setSelectedItem(attr(e, "waveform", "sine"));
        sourceFrequency.setText(attr(e, "frequency", "440"));
        sourceLevel.setText(attr(e, "level", "0.05"));
        gainValue.setText(attr(e, "gain", "1.0"));
        panValue.setText(attr(e, "pan", "0.0"));
        filterType.setSelectedItem(attr(e, "filterType", "lowpass"));
        filterFrequency.setText(attr(e, "filterFrequency", "1200"));
        filterQ.setText(attr(e, "filterQ", "0.707"));
        filterGain.setText(attr(e, "filterGain", "0"));
        compressorThreshold.setText(attr(e, "threshold", "-24"));
        compressorKnee.setText(attr(e, "knee", "30"));
        compressorRatio.setText(attr(e, "ratio", "12"));
        compressorAttack.setText(attr(e, "attack", "0.003"));
        compressorRelease.setText(attr(e, "release", "0.25"));
        analyserFftSize.setSelectedItem(attr(e, "fftSize", "2048"));
        analyserSmoothing.setText(attr(e, "smoothing", "0.8"));
        returnGain.setText(attr(e, "returnGain", "1.0"));
        delayTime.setText(attr(e, "delayTime", "0.28"));
        reverbDuration.setText(attr(e, "reverbDuration", "1.5"));
        reverbDecay.setText(attr(e, "reverbDecay", "2.0"));
        lfoWaveform.setSelectedItem(attr(e, "waveform", "sine"));
        lfoFrequency.setText(attr(e, "frequency", "2.0"));
        lfoDepth.setText(attr(e, "depth", "0.25"));
        automationCurve.setText(attr(e, "curve", "0:0,1:1"));
    }

    private static String attr(Element e, String name, String fallback) {
        return e == null ? fallback : defaultString(e.getAttribute(name), fallback);
    }

    private void updateButtons() {
        boolean hasDoc = document != null;
        boolean hasRoot = findMetadataRoot() != null;
        boolean selected = selectedModelElement != null;
        boolean isReference = selected && "reference".equals(modelLocalName(selectedModelElement));
        createTree.setEnabled(hasDoc && !hasRoot);
        addChild.setEnabled(hasDoc && hasRoot);
        addReference.setEnabled(hasDoc && hasRoot);
        remove.setEnabled(selected);
        moveUp.setEnabled(selected);
        moveDown.setEnabled(selected);
        duplicateBranch.setEnabled(selected && "node".equals(modelLocalName(selectedModelElement)));
        exportTree.setEnabled(hasDoc && hasRoot && selected);
        importTree.setEnabled(hasDoc && hasRoot);
        applyProperties.setEnabled(selected);
        goToTarget.setEnabled(isReference && !selectedModelElement.getAttribute("targetId").isEmpty());
        type.setEnabled(selected && "node".equals(modelLocalName(selectedModelElement)));
        referenceRole.setEnabled(isReference);
        targetId.setEnabled(isReference);
        referenceAmount.setEnabled(isReference && ("send".equals(String.valueOf(referenceRole.getSelectedItem())) || "modulation".equals(String.valueOf(referenceRole.getSelectedItem()))));
        String rr=String.valueOf(referenceRole.getSelectedItem());
        boolean eventRole="event".equals(rr)||"event-target".equals(rr);
        referenceTargetParam.setEnabled(isReference && ("modulation".equals(rr) || (eventRole && "set-param".equals(String.valueOf(referenceAction.getSelectedItem())))));
        referenceEvent.setEnabled(isReference && eventRole);
        referenceAction.setEnabled(isReference && eventRole);
        boolean mapped=isReference && eventRole && "set-param".equals(String.valueOf(referenceAction.getSelectedItem()));
        referenceScale.setEnabled(mapped); referenceOffset.setEnabled(mapped); referenceMin.setEnabled(mapped); referenceMax.setEnabled(mapped);
        validateRouting.setEnabled(hasDoc && hasRoot);
        exportRuntime.setEnabled(hasDoc && hasRoot);
    }

    private void createAudioTree() {
        if (document == null || findMetadataRoot() != null) return;
        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, "Create Audio Tree");
        try {
            Element svg = document.getDocumentElement();
            Element metadata = document.createElementNS("http://www.w3.org/2000/svg", "metadata");
            metadata.setAttribute("id", META_ID);
            metadata.setAttribute("data-sketsa-audio-tree-version", META_VERSION);
            metadata.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:sat", AUDIO_TREE_NS);
            Element project = document.createElementNS(AUDIO_TREE_NS, "sat:tree");
            project.setAttribute("id", stableId("tree"));
            project.setAttribute("label", "Audio");
            Element master = newOwnedNode("master", "Master");
            project.appendChild(master);
            metadata.appendChild(project);
            svg.insertBefore(metadata, svg.getFirstChild());
            undo.end();
            tree.setDocument(document);
            selectElement(master);
            expandAudioTree();
            status.setText("Audio Tree created. IDs are stable and persistent.");
        } catch (RuntimeException ex) {
            undo.cancel();
            status.setText("Could not create Audio Tree: " + ex.getMessage());
        }
    }

    private void addOwnedNode() {
        if (document == null) return;
        Element parent = selectedContainer();
        if (parent == null) return;
        String chosen = (String) JOptionPane.showInputDialog(this, "Node type", "Add Audio Tree node",
                JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"bus", "return", "source", "gain", "pan", "filter", "compressor", "analyser", "delay", "reverb", "lfo", "effect", "group", "automation"}, "bus");
        if (chosen == null) return;
        String name = JOptionPane.showInputDialog(this, "Label", capitalize(chosen));
        if (name == null) return;
        Element node = newOwnedNode(chosen, name.trim().isEmpty() ? capitalize(chosen) : name.trim());
        mutate("Add Audio Tree Node", () -> parent.appendChild(node));
        tree.setDocument(document);
        selectElement(node);
        expandAudioTree();
    }

    private void addReferenceNode() {
        if (document == null) return;
        Element parent = selectedContainer();
        if (parent == null) return;
        List<Element> targets = allOwnedNodes();
        if (targets.isEmpty()) {
            status.setText("Create at least one owned node before adding a reference.");
            return;
        }
        String[] choices = new String[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            Element e = targets.get(i);
            choices[i] = displayName(e) + "  [" + e.getAttribute("id") + "]";
        }
        String choice = (String) JOptionPane.showInputDialog(this, "Target", "Add Audio Reference",
                JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
        if (choice == null) return;
        Element target = targets.get(java.util.Arrays.asList(choices).indexOf(choice));
        String role = (String) JOptionPane.showInputDialog(this, "Reference role", "Add Audio Reference",
                JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"route", "send", "sidechain", "modulation", "event", "event-target"}, "send");
        if (role == null) return;
        Element ref = document.createElementNS(AUDIO_TREE_NS, "sat:reference");
        ref.setAttribute("id", stableId("ref"));
        ref.setAttribute("label", displayName(target));
        ref.setAttribute("role", role);
        ref.setAttribute("targetId", target.getAttribute("id"));
        if ("send".equals(role) || "modulation".equals(role)) ref.setAttribute("amount", "1.0");
        if ("modulation".equals(role)) ref.setAttribute("targetParam", "gain");
        if ("event".equals(role) || "event-target".equals(role)) { ref.setAttribute("event", "trigger"); ref.setAttribute("action", "trigger"); }
        mutate("Add Audio Reference", () -> parent.appendChild(ref));
        tree.setDocument(document);
        selectElement(ref);
        expandAudioTree();
    }

    private void removeSelected() {
        if (selectedModelElement == null || selectedModelElement.getParentNode() == null) return;
        Element victim = selectedModelElement;
        String id = victim.getAttribute("id");
        int refs = "reference".equals(modelLocalName(victim)) ? 0 : countReferencesTo(id);
        if (refs > 0) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "This node is referenced " + refs + " time(s). Removing it will leave visible broken references. Continue?",
                    "Referenced Audio Tree node", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) return;
        }
        removeWithReliableUndo(victim, "Remove Audio Tree Item");
        selectedModelElement = null;
        tree.setDocument(document);
        expandAudioTree();
        status.setText(refs > 0 ? "Node removed; broken references were preserved." : "Audio Tree item removed.");
    }


    /**
     * Register the removal explicitly before detaching the node. Sketsa 9.1's
     * DOM mutation listener can observe DOMNodeRemoved too late for a reliable
     * parent/next-sibling snapshot, so the native mutation edit is suppressed
     * for this operation and replaced by one pre-captured DOMUndoableEdit.
     * This keeps the transaction visible to Sketsa's normal Undo/Redo actions.
     */
    private void removeWithReliableUndo(Element victim, String name) {
        if (canvas == null || victim == null) return;
        Node parent = victim.getParentNode();
        if (parent == null) return;
        DOMUndoManager undo = canvas.getUndoManager();
        DOMUndoableEdit reliable = new DOMUndoableEdit(
                "DOMNodeRemoved", victim, parent, null, null, null, (short) 0);
        beginUndoTransaction(undo, name);
        boolean suppressed = false;
        try {
            appendCurrentUndoEdit(undo, reliable);
            setUndoInProgress(undo, true);
            suppressed = true;
            parent.removeChild(victim);
            setUndoInProgress(undo, false);
            suppressed = false;
            undo.end();
        } catch (RuntimeException ex) {
            if (suppressed) setUndoInProgress(undo, false);
            undo.cancel();
            throw ex;
        }
    }

    private static void appendCurrentUndoEdit(DOMUndoManager undo, DOMUndoableEdit edit) {
        try {
            Field f = DOMUndoManager.class.getDeclaredField("currentEntry");
            f.setAccessible(true);
            DOMUndoManager.Entry entry = (DOMUndoManager.Entry) f.get(undo);
            if (entry == null || !entry.getCompoundEdit().addEdit(edit)) {
                throw new IllegalStateException("Unable to register reliable DOM removal undo edit");
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access Sketsa DOM undo transaction", ex);
        }
    }

    private static void setUndoInProgress(DOMUndoManager undo, boolean value) {
        try {
            Field f = DOMUndoManager.class.getDeclaredField("inProgress");
            f.setAccessible(true);
            f.setBoolean(undo, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to control Sketsa DOM undo recording", ex);
        }
    }

    private void duplicateSelectedBranch() {
        if (document == null || selectedModelElement == null || !"node".equals(modelLocalName(selectedModelElement))) return;
        Element source = selectedModelElement;
        Node parent = source.getParentNode();
        if (parent == null) return;
        Element copy = (Element) source.cloneNode(true);
        Map<String,String> nodeIdMap = new HashMap<>();
        Set<String> used = allModelIds();
        remapClonedIds(copy, nodeIdMap, used);
        remapClonedReferenceTargets(copy, nodeIdMap);
        mutate("Duplicate Audio Tree Branch", () -> {
            Node next = source.getNextSibling();
            if (next == null) parent.appendChild(copy); else parent.insertBefore(copy, next);
        });
        tree.setDocument(document);
        selectElement(copy);
        expandAudioTree();
        status.setText("Branch duplicated. Internal references remapped; external references preserved.");
    }

    private Set<String> allModelIds() {
        Set<String> out = new HashSet<>();
        Element metadata = findMetadataRoot();
        if (metadata != null) collectModelIds(metadata, out);
        return out;
    }

    private static void collectModelIds(Node n, Set<String> out) {
        if (n instanceof Element && isAudioTreeModelElement((Element)n)) {
            String id=((Element)n).getAttribute("id").trim(); if(!id.isEmpty()) out.add(id);
        }
        NodeList kids=n.getChildNodes();
        for(int i=0;i<kids.getLength();i++) collectModelIds(kids.item(i),out);
    }

    private void remapClonedIds(Node n, Map<String,String> nodeIdMap, Set<String> used) {
        if (n instanceof Element) {
            Element e=(Element)n;
            if (isAudioTreeModelElement(e) && ("node".equals(modelLocalName(e)) || "reference".equals(modelLocalName(e)))) {
                String old=e.getAttribute("id").trim();
                String kind="reference".equals(modelLocalName(e))?"ref":defaultString(e.getAttribute("type"),"node");
                String fresh=uniqueStableId(kind,used);
                e.setAttribute("id",fresh);
                if ("node".equals(modelLocalName(e)) && !old.isEmpty()) nodeIdMap.put(old,fresh);
            }
        }
        NodeList kids=n.getChildNodes();
        for(int i=0;i<kids.getLength();i++) remapClonedIds(kids.item(i),nodeIdMap,used);
    }

    private void remapClonedReferenceTargets(Node n, Map<String,String> nodeIdMap) {
        if(n instanceof Element){
            Element e=(Element)n;
            if(isAudioTreeModelElement(e) && "reference".equals(modelLocalName(e))){
                String target=e.getAttribute("targetId").trim();
                if(nodeIdMap.containsKey(target)) e.setAttribute("targetId",nodeIdMap.get(target));
            }
        }
        NodeList kids=n.getChildNodes();
        for(int i=0;i<kids.getLength();i++) remapClonedReferenceTargets(kids.item(i),nodeIdMap);
    }

    private String uniqueStableId(String kind, Set<String> used) {
        String id;
        do { id=stableId(kind); } while(used.contains(id));
        used.add(id); return id;
    }

    private void exportSelectedTree() {
        if(document==null || selectedModelElement==null) return;
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Export Audio Tree / Branch");
        chooser.setFileFilter(new FileNameExtensionFilter("Audio Tree XML", "xml", "sat"));
        chooser.setSelectedFile(new File("sketsa-audio-tree-fragment.xml"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        File file=chooser.getSelectedFile();
        if(!file.getName().toLowerCase().matches(".*\\.(xml|sat)$")) file=new File(file.getParentFile(),file.getName()+".xml");
        try{
            String xml=serializeModelFragment(selectedModelElement);
            Files.write(file.toPath(),xml.getBytes(StandardCharsets.UTF_8));
            status.setText("Exported Audio Tree fragment: "+file.getName());
        }catch(Exception ex){status.setText("Tree export failed: "+ex.getMessage());}
    }

    private String serializeModelFragment(Element selected) throws Exception {
        Document out=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root=out.createElementNS(AUDIO_TREE_NS,"sat:fragment");
        root.setAttribute("version","1.0");
        root.setAttributeNS("http://www.w3.org/2000/xmlns/","xmlns:sat",AUDIO_TREE_NS);
        out.appendChild(root);
        if("tree".equals(modelLocalName(selected))){
            NodeList kids=selected.getChildNodes();
            for(int i=0;i<kids.getLength();i++) if(kids.item(i) instanceof Element && isAudioTreeModelElement((Element)kids.item(i))) root.appendChild(out.importNode(kids.item(i),true));
        }else root.appendChild(out.importNode(selected,true));
        Transformer t=TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"no"); t.setOutputProperty(OutputKeys.INDENT,"yes");
        StringWriter w=new StringWriter(); t.transform(new DOMSource(out),new StreamResult(w)); return w.toString();
    }

    private void importTreeFragment() {
        if(document==null) return;
        Element parent=selectedContainer(); if(parent==null) return;
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Import Audio Tree / Branch");
        chooser.setFileFilter(new FileNameExtensionFilter("Audio Tree XML", "xml", "sat"));
        if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        try{
            DocumentBuilderFactory f=DocumentBuilderFactory.newInstance(); f.setNamespaceAware(true);
            Document incoming=f.newDocumentBuilder().parse(chooser.getSelectedFile());
            Element root=incoming.getDocumentElement();
            List<Element> imported=new ArrayList<>();
            if("fragment".equals(modelLocalName(root)) || "tree".equals(modelLocalName(root))){
                NodeList kids=root.getChildNodes();
                for(int i=0;i<kids.getLength();i++) if(kids.item(i) instanceof Element && isImportableModel((Element)kids.item(i))) imported.add((Element)document.importNode(kids.item(i),true));
            }else if(isImportableModel(root)) imported.add((Element)document.importNode(root,true));
            if(imported.isEmpty()) throw new IllegalArgumentException("No Audio Tree nodes/references found in fragment.");
            Set<String> used=allModelIds(); Map<String,String> nodeMap=new HashMap<>();
            for(Element e:imported) remapImportedCollisions(e,nodeMap,used);
            for(Element e:imported) remapClonedReferenceTargets(e,nodeMap);
            mutate("Import Audio Tree Fragment", () -> { for(Element e:imported) parent.appendChild(e); });
            tree.setDocument(document); expandAudioTree(); selectElement(imported.get(0));
            status.setText("Imported Audio Tree fragment. Colliding IDs remapped; external references preserved.");
        }catch(Exception ex){status.setText("Tree import failed: "+ex.getMessage());}
    }

    private static boolean isImportableModel(Element e){
        String local=modelLocalName(e); return "node".equals(local)||"reference".equals(local);
    }

    private void remapImportedCollisions(Node n, Map<String,String> nodeMap, Set<String> used){
        if(n instanceof Element){
            Element e=(Element)n;
            if(isImportableModel(e)){
                String old=e.getAttribute("id").trim();
                if(old.isEmpty() || used.contains(old)){
                    String kind="reference".equals(modelLocalName(e))?"ref":defaultString(e.getAttribute("type"),"node");
                    String fresh=uniqueStableId(kind,used); e.setAttribute("id",fresh);
                    if("node".equals(modelLocalName(e)) && !old.isEmpty()) nodeMap.put(old,fresh);
                }else used.add(old);
            }
        }
        NodeList kids=n.getChildNodes(); for(int i=0;i<kids.getLength();i++) remapImportedCollisions(kids.item(i),nodeMap,used);
    }

    private void moveSelected(int direction) {
        if (selectedModelElement == null) return;
        Node parent = selectedModelElement.getParentNode();
        if (parent == null) return;
        Node sibling = direction < 0 ? previousModelSibling(selectedModelElement) : nextModelSibling(selectedModelElement);
        if (sibling == null) return;
        Element moving = selectedModelElement;
        mutate(direction < 0 ? "Move Audio Tree Item Up" : "Move Audio Tree Item Down", () -> {
            if (direction < 0) parent.insertBefore(moving, sibling);
            else parent.insertBefore(sibling, moving);
        });
        tree.setDocument(document);
        selectElement(moving);
        expandAudioTree();
    }

    private static Node previousModelSibling(Node n) {
        Node p = n.getPreviousSibling();
        while (p != null && !(p instanceof Element && AUDIO_TREE_NS.equals(p.getNamespaceURI()))) p = p.getPreviousSibling();
        return p;
    }

    private static Node nextModelSibling(Node n) {
        Node p = n.getNextSibling();
        while (p != null && !(p instanceof Element && AUDIO_TREE_NS.equals(p.getNamespaceURI()))) p = p.getNextSibling();
        return p;
    }

    private void applySelectedProperties() {
        if (loading || selectedModelElement == null) return;
        Element e = selectedModelElement;
        mutate("Edit Audio Tree Item", () -> {
            e.setAttribute("label", label.getText().trim());
            if ("node".equals(modelLocalName(e))) {
                String nodeType = String.valueOf(type.getSelectedItem());
                e.setAttribute("type", nodeType);
                applyNodeParameters(e, nodeType);
            } else if ("reference".equals(modelLocalName(e))) {
                e.setAttribute("role", String.valueOf(referenceRole.getSelectedItem()));
                e.setAttribute("targetId", targetId.getText().trim());
                if ("send".equals(String.valueOf(referenceRole.getSelectedItem())) || "modulation".equals(String.valueOf(referenceRole.getSelectedItem()))) e.setAttribute("amount", referenceAmount.getText().trim());
                else e.removeAttribute("amount");
                String rr=String.valueOf(referenceRole.getSelectedItem());
                boolean er="event".equals(rr)||"event-target".equals(rr);
                if ("modulation".equals(rr) || (er && "set-param".equals(String.valueOf(referenceAction.getSelectedItem())))) e.setAttribute("targetParam", referenceTargetParam.getText().trim());
                else e.removeAttribute("targetParam");
                if(er){
                    e.setAttribute("event",referenceEvent.getText().trim()); e.setAttribute("action",String.valueOf(referenceAction.getSelectedItem()));
                    if("set-param".equals(String.valueOf(referenceAction.getSelectedItem()))){
                        e.setAttribute("scale",referenceScale.getText().trim()); e.setAttribute("offset",referenceOffset.getText().trim());
                        setOrRemove(e,"min",referenceMin.getText().trim()); setOrRemove(e,"max",referenceMax.getText().trim());
                    }else{e.removeAttribute("scale");e.removeAttribute("offset");e.removeAttribute("min");e.removeAttribute("max");}
                }else{e.removeAttribute("event");e.removeAttribute("action");e.removeAttribute("scale");e.removeAttribute("offset");e.removeAttribute("min");e.removeAttribute("max");}
            }
        });
        tree.repaint();
        updateButtons();
        status.setText("Audio Tree item updated. Stable ID unchanged.");
    }

    private static void setOrRemove(Element e,String name,String value){if(value==null||value.isEmpty())e.removeAttribute(name);else e.setAttribute(name,value);}

    private void applyNodeParameters(Element e, String nodeType) {
        String[] attrs = {"waveform","frequency","level","gain","pan","filterType","filterFrequency","filterQ","filterGain",
            "threshold","knee","ratio","attack","release","fftSize","smoothing","returnGain","delayTime","reverbDuration","reverbDecay","depth","curve"};
        for (String a : attrs) e.removeAttribute(a);
        if ("source".equals(nodeType)) {
            e.setAttribute("waveform", String.valueOf(sourceWaveform.getSelectedItem()));
            e.setAttribute("frequency", sourceFrequency.getText().trim());
            e.setAttribute("level", sourceLevel.getText().trim());
        } else if ("gain".equals(nodeType)) {
            e.setAttribute("gain", gainValue.getText().trim());
        } else if ("pan".equals(nodeType)) {
            e.setAttribute("pan", panValue.getText().trim());
        } else if ("filter".equals(nodeType)) {
            e.setAttribute("filterType", String.valueOf(filterType.getSelectedItem()));
            e.setAttribute("filterFrequency", filterFrequency.getText().trim());
            e.setAttribute("filterQ", filterQ.getText().trim());
            e.setAttribute("filterGain", filterGain.getText().trim());
        } else if ("compressor".equals(nodeType)) {
            e.setAttribute("threshold", compressorThreshold.getText().trim());
            e.setAttribute("knee", compressorKnee.getText().trim());
            e.setAttribute("ratio", compressorRatio.getText().trim());
            e.setAttribute("attack", compressorAttack.getText().trim());
            e.setAttribute("release", compressorRelease.getText().trim());
        } else if ("analyser".equals(nodeType)) {
            e.setAttribute("fftSize", String.valueOf(analyserFftSize.getSelectedItem()));
            e.setAttribute("smoothing", analyserSmoothing.getText().trim());
        } else if ("return".equals(nodeType)) {
            e.setAttribute("returnGain", returnGain.getText().trim());
        } else if ("delay".equals(nodeType)) {
            e.setAttribute("delayTime", delayTime.getText().trim());
        } else if ("reverb".equals(nodeType)) {
            e.setAttribute("reverbDuration", reverbDuration.getText().trim());
            e.setAttribute("reverbDecay", reverbDecay.getText().trim());
        } else if ("lfo".equals(nodeType)) {
            e.setAttribute("waveform", String.valueOf(lfoWaveform.getSelectedItem()));
            e.setAttribute("frequency", lfoFrequency.getText().trim());
            e.setAttribute("depth", lfoDepth.getText().trim());
        } else if ("automation".equals(nodeType)) {
            e.setAttribute("curve", automationCurve.getText().trim());
        }
    }

    private void validateRouting() {
        if (document == null) return;
        AudioTreeCompiler.Result result = AudioTreeCompiler.compile(document);
        showValidationMessages(result);
        if (result.isValid()) {
            status.setText("Routing valid: " + result.nodes.size() + " nodes, " + result.connections.size() + " connections"
                    + (result.warnings.isEmpty() ? "." : "; " + result.warnings.size() + " warning(s). See Validation Messages."));
        } else {
            status.setText("Routing INVALID: " + result.errors.size() + " error(s). See Validation Messages.");
            JOptionPane.showMessageDialog(this, String.join("\n", result.errors), "Audio Tree routing errors", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showValidationMessages(AudioTreeCompiler.Result result) {
        StringBuilder out = new StringBuilder();
        out.append("Errors: ").append(result.errors.size())
           .append("   Warnings: ").append(result.warnings.size())
           .append("   Info: ").append(result.infos.size()).append('\n');
        if (result.errors.isEmpty() && result.warnings.isEmpty()) out.append("OK: no errors or warnings.\n");
        for (String message : result.errors) out.append("ERROR: ").append(message).append('\n');
        for (String message : result.warnings) out.append("WARNING: ").append(message).append('\n');
        for (String message : result.infos) out.append("INFO: ").append(message).append('\n');
        validationMessages.setText(out.toString());
        validationMessages.setCaretPosition(0);
    }

    private void exportTreeRuntime() {
        if (document == null) return;
        AudioTreeCompiler.Result result = AudioTreeCompiler.compile(document);
        showValidationMessages(result);
        if (!result.isValid()) {
            status.setText("Export blocked: routing is invalid.");
            JOptionPane.showMessageDialog(this, String.join("\n", result.errors), "Audio Tree routing errors", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Sketsa Audio Tree Runtime");
        chooser.setFileFilter(new FileNameExtensionFilter("HTML file", "html", "htm"));
        chooser.setSelectedFile(new File("sketsa-audio-tree.html"));
        JCheckBox includeCompanions = new JCheckBox("Include companion runtimes", true);
        includeCompanions.setToolTipText("Package compatible companion runtimes (for example Physics) when metadata is present.");
        JPanel exportOptions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        exportOptions.setBorder(BorderFactory.createTitledBorder("Runtime options"));
        exportOptions.add(includeCompanions);
        chooser.setAccessory(exportOptions);
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        String lower = file.getName().toLowerCase();
        if (!lower.endsWith(".html") && !lower.endsWith(".htm")) file = new File(file.getParentFile(), file.getName() + ".html");
        try {
            RuntimeHtmlExporter.export(document, file, includeCompanions.isSelected());
            status.setText("Exported Audio Tree runtime: " + file.getName());
        } catch (Exception ex) {
            status.setText("Export failed: " + ex.getMessage());
        }
    }

    private void goToReferenceTarget() {
        if (selectedModelElement == null || !"reference".equals(modelLocalName(selectedModelElement))) return;
        Element target = findOwnedNodeById(selectedModelElement.getAttribute("targetId"));
        if (target == null) {
            status.setText("Broken reference: target ID not found.");
            return;
        }
        selectElement(target);
        status.setText("Jumped to referenced node: " + displayName(target));
    }

    private void selectElement(Element element) {
        DOMTreeModel model = (DOMTreeModel) tree.getModel();
        TreeNode treeNode = model.getTreeNode(element);
        if (treeNode == null) return;
        TreePath path = new TreePath(((DefaultMutableTreeNode) treeNode).getPath());
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }

    private Element selectedContainer() {
        if (selectedModelElement != null && "node".equals(modelLocalName(selectedModelElement))) return selectedModelElement;
        if (selectedModelElement != null && "tree".equals(modelLocalName(selectedModelElement))) return selectedModelElement;
        if (selectedModelElement != null && selectedModelElement.getParentNode() instanceof Element) {
            Element parent = (Element) selectedModelElement.getParentNode();
            if (isAudioTreeModelElement(parent)) return parent;
        }
        Element metadata = findMetadataRoot();
        if (metadata == null) return null;
        NodeList children = metadata.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element && AUDIO_TREE_NS.equals(n.getNamespaceURI()) && "tree".equals(n.getLocalName())) return (Element) n;
        }
        return null;
    }

    private Element newOwnedNode(String nodeType, String nodeLabel) {
        Element e = document.createElementNS(AUDIO_TREE_NS, "sat:node");
        e.setAttribute("id", stableId(nodeType));
        e.setAttribute("type", nodeType);
        e.setAttribute("label", nodeLabel);
        if ("source".equals(nodeType)) { e.setAttribute("waveform", "sine"); e.setAttribute("frequency", "440"); e.setAttribute("level", "0.05"); }
        else if ("gain".equals(nodeType)) e.setAttribute("gain", "1.0");
        else if ("pan".equals(nodeType)) e.setAttribute("pan", "0.0");
        else if ("filter".equals(nodeType)) { e.setAttribute("filterType", "lowpass"); e.setAttribute("filterFrequency", "1200"); e.setAttribute("filterQ", "0.707"); e.setAttribute("filterGain", "0"); }
        else if ("compressor".equals(nodeType)) { e.setAttribute("threshold", "-24"); e.setAttribute("knee", "30"); e.setAttribute("ratio", "12"); e.setAttribute("attack", "0.003"); e.setAttribute("release", "0.25"); }
        else if ("analyser".equals(nodeType)) { e.setAttribute("fftSize", "2048"); e.setAttribute("smoothing", "0.8"); }
        return e;
    }

    private String stableId(String kind) {
        return "audio-" + kind + "-" + UUID.randomUUID().toString();
    }

    private Element findMetadataRoot() {
        if (document == null) return null;
        Element byId = document.getElementById(META_ID);
        if (byId != null && "metadata".equals(modelLocalName(byId))) return byId;
        NodeList list = document.getElementsByTagNameNS("http://www.w3.org/2000/svg", "metadata");
        for (int i = 0; i < list.getLength(); i++) {
            Element e = (Element) list.item(i);
            if (META_ID.equals(e.getAttribute("id"))) return e;
        }
        return null;
    }

    private List<Element> allOwnedNodes() {
        List<Element> out = new ArrayList<>();
        Element metadata = findMetadataRoot();
        if (metadata != null) collectOwnedNodes(metadata, out);
        return out;
    }

    private static void collectOwnedNodes(Node n, List<Element> out) {
        if (n instanceof Element && isAudioTreeModelElement((Element) n) && "node".equals(modelLocalName((Element) n))) out.add((Element) n);
        NodeList children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) collectOwnedNodes(children.item(i), out);
    }

    private Element findOwnedNodeById(String id) {
        if (id == null || id.isEmpty()) return null;
        for (Element e : allOwnedNodes()) if (id.equals(e.getAttribute("id"))) return e;
        return null;
    }

    private int countReferencesTo(String id) {
        if (id == null || id.isEmpty()) return 0;
        Element metadata = findMetadataRoot();
        return metadata == null ? 0 : countReferencesRecursive(metadata, id);
    }

    private static int countReferencesRecursive(Node n, String id) {
        int count = 0;
        if (n instanceof Element && isAudioTreeModelElement((Element) n) && "reference".equals(modelLocalName((Element) n))
                && id.equals(((Element) n).getAttribute("targetId"))) count++;
        NodeList children = n.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) count += countReferencesRecursive(children.item(i), id);
        return count;
    }

    private void mutate(String name, Runnable operation) {
        if (canvas == null) return;
        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, name);
        try {
            operation.run();
            undo.end();
        } catch (RuntimeException ex) {
            undo.cancel();
            throw ex;
        }
    }

    private static String modelLocalName(Element e) {
        if (e == null) return "";
        String local = e.getLocalName();
        if (local != null && !local.isEmpty()) return local;
        String name = e.getTagName();
        int colon = name == null ? -1 : name.indexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : defaultString(name, "");
    }

    private static boolean isAudioTreeModelElement(Element e) {
        if (e == null) return false;
        String local = modelLocalName(e);
        if (!("tree".equals(local) || "node".equals(local) || "reference".equals(local))) return false;
        if (AUDIO_TREE_NS.equals(e.getNamespaceURI())) return true;
        Node p = e.getParentNode();
        while (p instanceof Element) {
            Element pe = (Element) p;
            if (META_ID.equals(pe.getAttribute("id")) && "metadata".equals(modelLocalName(pe))) return true;
            p = p.getParentNode();
        }
        return false;
    }

    private static String defaultString(String s, String fallback) {
        return s == null || s.trim().isEmpty() ? fallback : s;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "Node";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String displayName(Element e) {
        String label = e.getAttribute("label");
        return label == null || label.trim().isEmpty() ? defaultString(e.getAttribute("type"), modelLocalName(e)) : label;
    }

    private final class AudioTreeFilter implements NodeFilter {
        @Override public short acceptNode(Node n) {
            if (n.getNodeType() == Node.DOCUMENT_NODE) return FILTER_ACCEPT;
            if (n instanceof Element) {
                Element e = (Element) n;
                if ("metadata".equals(modelLocalName(e)) && META_ID.equals(e.getAttribute("id"))) return FILTER_ACCEPT;
                if (isAudioTreeModelElement(e)) return FILTER_ACCEPT;
                return FILTER_SKIP;
            }
            return FILTER_REJECT;
        }
    }

    private final class AudioTreeRenderer extends DefaultTreeCellRenderer {
        @Override public java.awt.Component getTreeCellRendererComponent(JTree treeComponent, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(treeComponent, value, sel, expanded, leaf, row, hasFocus);
            if (!(value instanceof DefaultMutableTreeNode)) return this;
            Object u = ((DefaultMutableTreeNode) value).getUserObject();
            if (!(u instanceof Node)) return this;
            Node n = (Node) u;
            if (n.getNodeType() == Node.DOCUMENT_NODE) {
                setText("Audio Tree");
            } else if (n instanceof Element) {
                Element e = (Element) n;
                if ("metadata".equals(modelLocalName(e)) && META_ID.equals(e.getAttribute("id"))) {
                    setText("Audio Tree Metadata");
                } else if (isAudioTreeModelElement(e) && "tree".equals(modelLocalName(e))) {
                    setText(defaultString(e.getAttribute("label"), "Audio"));
                } else if (isAudioTreeModelElement(e) && "node".equals(modelLocalName(e))) {
                    setText(displayName(e) + "  <" + defaultString(e.getAttribute("type"), "node") + ">  [" + shortId(e.getAttribute("id")) + "]");
                } else if (isAudioTreeModelElement(e) && "reference".equals(modelLocalName(e))) {
                    Element target = findOwnedNodeById(e.getAttribute("targetId"));
                    String targetName = target == null ? "BROKEN: " + e.getAttribute("targetId") : displayName(target);
                    setText((target == null ? "! " : "↗ ") + defaultString(e.getAttribute("role"), "reference") + " → " + targetName);
                }
            }
            return this;
        }
    }

    private static String shortId(String id) {
        if (id == null) return "";
        return id.length() <= 12 ? id : id.substring(0, 12) + "…";
    }
    /** Ensure every plugin edit is a separate Sketsa undo transaction. */
    private static void beginUndoTransaction(DOMUndoManager undo, String name) {
        // end() is a no-op when no transaction is open. If Sketsa left a
        // previous editor transaction pending, commit it before starting ours.
        undo.end();
        undo.start(name);
    }

}
