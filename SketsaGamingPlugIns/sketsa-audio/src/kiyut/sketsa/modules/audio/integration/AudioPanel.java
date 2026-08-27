package kiyut.sketsa.modules.audio.integration;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.Collection;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;
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
import org.w3c.dom.svg.SVGElement;

final class AudioPanel extends JPanel {
    private static final String ATTR_SRC = "data-sketsa-audio-src";
    private static final String ATTR_AUTOPLAY = "data-sketsa-audio-autoplay";
    private static final String ATTR_LOOP = "data-sketsa-audio-loop";
    private static final String ATTR_VOLUME = "data-sketsa-audio-volume";
    private static final String ATTR_PLAYBACK_RATE = "data-sketsa-audio-playback-rate";
    private static final String ATTR_START_OFFSET = "data-sketsa-audio-start-offset";
    private static final String ATTR_EVENT_ID = "data-sketsa-audio-event-id";
    private static final String ATTR_MUTE = "data-sketsa-audio-mute";
    private static final String ATTR_BUS = "data-sketsa-audio-bus";
    private static final String ATTR_MASTER_VOLUME = "data-sketsa-audio-master-volume";
    private static final String ATTR_MASTER_MUTE = "data-sketsa-audio-master-mute";
    private static final String ATTR_PAN = "data-sketsa-audio-pan";
    private static final String ATTR_PAN_MODE = "data-sketsa-audio-pan-mode";
    private static final String ATTR_TRIGGER_SOURCE = "data-sketsa-audio-trigger-source";
    private static final String ATTR_TRIGGER_TYPE = "data-sketsa-audio-trigger-type";
    private static final String ATTR_TRIGGER_EVENT_ID = "data-sketsa-audio-trigger-event-id";
    private static final String ATTR_TRIGGER_ACTION = "data-sketsa-audio-trigger-action";

    private final JTextField source = new JTextField();
    private final JButton browse = new JButton("Browse...");
    private final JCheckBox autoplay = new JCheckBox("Autoplay");
    private final JCheckBox loop = new JCheckBox("Loop");
    private final JSpinner volume = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 4.0, 0.05));
    private final JSpinner playbackRate = new JSpinner(new SpinnerNumberModel(1.0, 0.05, 8.0, 0.05));
    private final JSpinner startOffset = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 36000.0, 0.05));
    private final JTextField eventId = new JTextField();
    private final JCheckBox mute = new JCheckBox("Mute source");
    private final JSpinner pan = new JSpinner(new SpinnerNumberModel(0.0, -1.0, 1.0, 0.05));
    private final JCheckBox autoPan = new JCheckBox("Pan from SVG X");
    private final JTextField bus = new JTextField("main");
    private final JSpinner busVolume = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 4.0, 0.05));
    private final JCheckBox busMute = new JCheckBox("Mute bus");
    private final JSpinner masterVolume = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 4.0, 0.05));
    private final JCheckBox masterMute = new JCheckBox("Mute master");
    private final JTextField triggerSource = new JTextField("physics");
    private final JTextField triggerType = new JTextField("collisionStart");
    private final JTextField triggerEventId = new JTextField();
    private final JTextField triggerAction = new JTextField("play");
    private final JButton applyButton = new JButton("Apply Audio");
    private final JButton removeButton = new JButton("Remove Audio");
    private final JCheckBox includeCompanions = new JCheckBox("Include companion runtimes", true);
    private final JButton exportButton = new JButton("Export Runtime HTML...");
    private final JLabel status = new JLabel("Select exactly one SVG object.");
    private final JPanel form = new JPanel(new GridBagLayout());

    private final Lookup.Result<SVGEditorCookie> lookupResult;
    private final LookupListener lookupListener;
    private final SelectionHandler selectionHandler = new SelectionHandler();

    private VectorCanvas canvas;
    private SVGElement selected;
    private Document currentDocument;
    private boolean loading;

    AudioPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Audio / Web Audio API"));
        // Intentionally bounded + scrollable: Audio must never consume the whole Properties area.
        setPreferredSize(new Dimension(10, 300));
        setMinimumSize(new Dimension(10, 150));

        form.setBorder(BorderFactory.createEmptyBorder(2, 6, 5, 6));
        JScrollPane scroll = new JScrollPane(
                form,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 3, 2, 3);
        c.anchor = GridBagConstraints.WEST;
        int row = 0;

        JPanel srcRow = new JPanel(new BorderLayout(4,0));
        srcRow.add(source, BorderLayout.CENTER);
        srcRow.add(browse, BorderLayout.EAST);
        addRow(c, row++, "Source", srcRow);
        addWide(c, row++, autoplay);
        addWide(c, row++, loop);
        addRow(c, row++, "Volume", volume);
        addRow(c, row++, "Playback rate", playbackRate);
        addRow(c, row++, "Start offset (s)", startOffset);
        addRow(c, row++, "Event ID", eventId);
        addWide(c, row++, mute);
        addRow(c, row++, "Pan", pan);
        addWide(c, row++, autoPan);
        addRow(c, row++, "Bus", bus);
        addRow(c, row++, "Bus gain", busVolume);
        addWide(c, row++, busMute);
        addRow(c, row++, "Master gain", masterVolume);
        addWide(c, row++, masterMute);
        addRow(c, row++, "Trigger source", triggerSource);
        addRow(c, row++, "Trigger type", triggerType);
        addRow(c, row++, "Trigger event ID", triggerEventId);
        addRow(c, row++, "Trigger action", triggerAction);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(applyButton);
        buttons.add(removeButton);
        addWide(c, row++, buttons);
        addWide(c, row++, includeCompanions);
        addWide(c, row++, exportButton);
        addWide(c, row++, status);

        browse.addActionListener(e -> chooseAudio());
        applyButton.addActionListener(e -> applyAudio());
        removeButton.addActionListener(e -> removeAudio());
        exportButton.addActionListener(e -> exportRuntime());

        lookupResult = Utilities.actionsGlobalContext().lookupResult(SVGEditorCookie.class);
        lookupListener = (LookupEvent ev) -> updateCanvasFromLookup();
        lookupResult.addLookupListener(lookupListener);
        updateCanvasFromLookup();
        updateEnabledState();
    }

    private void addWide(GridBagConstraints c, int row, java.awt.Component component) {
        c.gridx=0; c.gridy=row; c.gridwidth=2; c.weightx=1; c.fill=GridBagConstraints.HORIZONTAL;
        form.add(component,c);
    }
    private void addRow(GridBagConstraints c, int row, String label, java.awt.Component component) {
        c.gridx=0; c.gridy=row; c.gridwidth=1; c.weightx=0; c.fill=GridBagConstraints.NONE;
        form.add(new JLabel(label),c);
        c.gridx=1; c.weightx=1; c.fill=GridBagConstraints.HORIZONTAL;
        form.add(component,c);
    }

    private void updateCanvasFromLookup() {
        Collection<? extends SVGEditorCookie> cookies = lookupResult.allInstances();
        if (cookies.isEmpty()) return;
        SVGEditorCookie cookie = cookies.iterator().next();
        if (cookie.isOpened()) setCanvas(cookie.getVectorCanvas());
    }

    private void setCanvas(VectorCanvas newCanvas) {
        if (canvas == newCanvas) { cacheSelection(); return; }
        if (canvas != null) canvas.getCanvasSelection().removeSelectionListener(selectionHandler);
        canvas = newCanvas; selected=null; currentDocument=null;
        if (canvas != null) {
            canvas.getCanvasSelection().addSelectionListener(selectionHandler);
            cacheSelection();
        }
        updateEnabledState();
    }

    private void cacheSelection() {
        if (canvas == null) return;
        List<SVGElement> selection = canvas.getCanvasSelection().getSelectionList();
        if (selection == null || selection.size() != 1) {
            selected=null; updateEnabledState(); return;
        }
        selected=selection.get(0);
        if (selected instanceof Element) {
            currentDocument=((Element)selected).getOwnerDocument();
            loadFields((Element)selected);
        }
        updateEnabledState();
    }

    private void loadFields(Element e) {
        loading=true;
        try {
            source.setText(e.getAttribute(ATTR_SRC));
            autoplay.setSelected("true".equalsIgnoreCase(e.getAttribute(ATTR_AUTOPLAY)));
            loop.setSelected("true".equalsIgnoreCase(e.getAttribute(ATTR_LOOP)));
            setSpinner(volume, e.getAttribute(ATTR_VOLUME), 1.0);
            setSpinner(playbackRate, e.getAttribute(ATTR_PLAYBACK_RATE), 1.0);
            setSpinner(startOffset, e.getAttribute(ATTR_START_OFFSET), 0.0);
            eventId.setText(e.getAttribute(ATTR_EVENT_ID));
            mute.setSelected("true".equalsIgnoreCase(e.getAttribute(ATTR_MUTE)));
            setSpinner(pan, e.getAttribute(ATTR_PAN), 0.0);
            autoPan.setSelected("svg-x".equalsIgnoreCase(e.getAttribute(ATTR_PAN_MODE)));
            triggerSource.setText(e.hasAttribute(ATTR_TRIGGER_SOURCE) ? e.getAttribute(ATTR_TRIGGER_SOURCE) : "physics");
            triggerType.setText(e.hasAttribute(ATTR_TRIGGER_TYPE) ? e.getAttribute(ATTR_TRIGGER_TYPE) : "collisionStart");
            triggerEventId.setText(e.getAttribute(ATTR_TRIGGER_EVENT_ID));
            triggerAction.setText(e.hasAttribute(ATTR_TRIGGER_ACTION) ? e.getAttribute(ATTR_TRIGGER_ACTION) : "play");
            String busId = normalizeBusId(e.getAttribute(ATTR_BUS));
            bus.setText(busId);
            Element root = currentDocument == null ? null : currentDocument.getDocumentElement();
            if (root != null) {
                setSpinner(masterVolume, root.getAttribute(ATTR_MASTER_VOLUME), 1.0);
                masterMute.setSelected("true".equalsIgnoreCase(root.getAttribute(ATTR_MASTER_MUTE)));
                setSpinner(busVolume, root.getAttribute(busAttr(busId, "volume")), 1.0);
                busMute.setSelected("true".equalsIgnoreCase(root.getAttribute(busAttr(busId, "mute"))));
            }
            status.setText(source.getText().trim().isEmpty() ? "Selected object has no audio metadata." : "Audio metadata loaded.");
        } finally { loading=false; }
    }

    private static String normalizeBusId(String value) {
        String s=value==null?"":value.trim();
        if(s.isEmpty()) return "main";
        s=s.replaceAll("[^A-Za-z0-9_-]+","-").replaceAll("^-+|-+$","");
        return s.isEmpty()?"main":s;
    }

    private static String busAttr(String busId, String property) {
        return "data-sketsa-audio-bus-"+normalizeBusId(busId)+"-"+property;
    }

    private static void setSpinner(JSpinner spinner, String value, double fallback) {
        try { spinner.setValue(value == null || value.isEmpty() ? fallback : Double.parseDouble(value)); }
        catch (NumberFormatException ex) { spinner.setValue(fallback); }
    }

    private void updateEnabledState() {
        boolean hasElement=selected instanceof Element;
        applyButton.setEnabled(hasElement);
        removeButton.setEnabled(hasElement && ((Element)selected).hasAttribute(ATTR_SRC));
        exportButton.setEnabled(currentDocument!=null);
        if (!hasElement) status.setText("Select exactly one SVG object.");
    }

    private void chooseAudio() {
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Choose audio file");
        chooser.setFileFilter(new FileNameExtensionFilter("Audio files", "wav","mp3","ogg","m4a","aac","flac"));
        if (chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) source.setText(chooser.getSelectedFile().getAbsolutePath());
    }

    private void applyAudio() {
        if (loading || canvas==null || !(selected instanceof Element)) return;
        Element e=(Element)selected;
        String src=source.getText().trim();
        if (src.isEmpty()) { status.setText("Choose an audio source first."); return; }
        DOMUndoManager undo=canvas.getUndoManager();
        undo.start("Apply Audio");
        try {
            Element replacement=(Element)e.cloneNode(true);
            replacement.setAttribute(ATTR_SRC,src);
            replacement.setAttribute(ATTR_AUTOPLAY,Boolean.toString(autoplay.isSelected()));
            replacement.setAttribute(ATTR_LOOP,Boolean.toString(loop.isSelected()));
            replacement.setAttribute(ATTR_VOLUME,String.valueOf(((Number)volume.getValue()).doubleValue()));
            replacement.setAttribute(ATTR_PLAYBACK_RATE,String.valueOf(((Number)playbackRate.getValue()).doubleValue()));
            replacement.setAttribute(ATTR_START_OFFSET,String.valueOf(((Number)startOffset.getValue()).doubleValue()));
            replacement.setAttribute(ATTR_EVENT_ID,eventId.getText().trim());
            replacement.setAttribute(ATTR_MUTE,Boolean.toString(mute.isSelected()));
            replacement.setAttribute(ATTR_PAN,String.valueOf(((Number)pan.getValue()).doubleValue()));
            replacement.setAttribute(ATTR_PAN_MODE,autoPan.isSelected()?"svg-x":"manual");
            replacement.setAttribute(ATTR_TRIGGER_SOURCE,triggerSource.getText().trim());
            replacement.setAttribute(ATTR_TRIGGER_TYPE,triggerType.getText().trim());
            replacement.setAttribute(ATTR_TRIGGER_EVENT_ID,triggerEventId.getText().trim());
            replacement.setAttribute(ATTR_TRIGGER_ACTION,triggerAction.getText().trim());
            String busId=normalizeBusId(bus.getText());
            replacement.setAttribute(ATTR_BUS,busId);
            Element root=currentDocument==null?null:currentDocument.getDocumentElement();
            if(root!=null){
                root.setAttribute(ATTR_MASTER_VOLUME,String.valueOf(((Number)masterVolume.getValue()).doubleValue()));
                root.setAttribute(ATTR_MASTER_MUTE,Boolean.toString(masterMute.isSelected()));
                root.setAttribute(busAttr(busId,"volume"),String.valueOf(((Number)busVolume.getValue()).doubleValue()));
                root.setAttribute(busAttr(busId,"mute"),Boolean.toString(busMute.isSelected()));
            }
            if (e.getParentNode()==null) throw new IllegalStateException("Selected object has no parent.");
            e.getParentNode().replaceChild(replacement,e);
            selected=replacement instanceof SVGElement ? (SVGElement)replacement : null;
            currentDocument=replacement.getOwnerDocument();
            undo.end();
            status.setText("Audio metadata applied.");
        } catch(RuntimeException ex) {
            undo.cancel();
            status.setText("Could not apply audio: "+ex.getMessage());
        }
        updateEnabledState();
    }

    private void removeAudio() {
        if (canvas==null || !(selected instanceof Element)) return;
        Element e=(Element)selected;
        DOMUndoManager undo=canvas.getUndoManager();
        undo.start("Remove Audio");
        try {
            Element replacement=(Element)e.cloneNode(true);
            for (String a : new String[]{ATTR_SRC,ATTR_AUTOPLAY,ATTR_LOOP,ATTR_VOLUME,ATTR_PLAYBACK_RATE,ATTR_START_OFFSET,ATTR_EVENT_ID,ATTR_MUTE,ATTR_PAN,ATTR_PAN_MODE,ATTR_BUS,ATTR_TRIGGER_SOURCE,ATTR_TRIGGER_TYPE,ATTR_TRIGGER_EVENT_ID,ATTR_TRIGGER_ACTION}) replacement.removeAttribute(a);
            if (e.getParentNode()==null) throw new IllegalStateException("Selected object has no parent.");
            e.getParentNode().replaceChild(replacement,e);
            selected=replacement instanceof SVGElement ? (SVGElement)replacement : null;
            currentDocument=replacement.getOwnerDocument();
            undo.end();
            status.setText("Audio metadata removed.");
        } catch(RuntimeException ex) {
            undo.cancel();
            status.setText("Could not remove audio: "+ex.getMessage());
        }
        updateEnabledState();
    }

    private void exportRuntime() {
        if (currentDocument==null) return;
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Export Sketsa Audio Runtime");
        chooser.setFileFilter(new FileNameExtensionFilter("HTML file","html","htm"));
        chooser.setSelectedFile(new File("sketsa-audio.html"));
        if (chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        File file=chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".html") && !file.getName().toLowerCase().endsWith(".htm"))
            file=new File(file.getParentFile(),file.getName()+".html");
        try {
            RuntimeHtmlExporter.export(currentDocument,file,includeCompanions.isSelected());
            status.setText("Exported runtime package: "+file.getName());
        } catch(Exception ex) {
            status.setText("Export failed: "+ex.getMessage());
        }
    }

    private final class SelectionHandler extends CanvasSelectionAdapter {
        @Override public void valueChanged(CanvasSelectionEvent event) { cacheSelection(); }
    }
}
