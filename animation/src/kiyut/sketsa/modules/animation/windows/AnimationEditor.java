package kiyut.sketsa.modules.animation.windows;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.AWTEventListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import kiyut.sketsa.undo.DOMUndoManager;
import kiyut.sketsa.canvas.CanvasSelection;
import kiyut.sketsa.canvas.VectorCanvas;
import kiyut.sketsa.canvas.event.CanvasSelectionAdapter;
import kiyut.sketsa.canvas.event.CanvasSelectionEvent;
import kiyut.sketsa.modules.animation.timeline.SMILTrack;
import kiyut.sketsa.modules.animation.timeline.Timeline;
import kiyut.sketsa.modules.animation.timeline.TimelineModel;
import org.apache.batik.bridge.SVGAnimationEngine;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.ShapeNode;
import org.apache.batik.gvt.ShapePainter;
import org.apache.batik.gvt.FillShapePainter;
import org.apache.batik.gvt.StrokeShapePainter;
import org.apache.batik.gvt.CompositeShapePainter;
import org.apache.batik.gvt.CompositeGraphicsNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGAnimationElement;

public class AnimationEditor extends JPanel {

    private static final String SVG_NS = "http://www.w3.org/2000/svg";
    private static final int TIME_SCALE = 100; // hundredths of a second

    protected final Timeline timeline = new Timeline();
    protected VectorCanvas canvas;
    private final CanvasSelectionHandler canvasSelectionHandler = new CanvasSelectionHandler();
    private final PropertyChangeListener canvasPropertyChangeListener;

    private final JButton playButton = new JButton("▶");
    private final JButton jumpStartButton = new JButton("|<");
    private final JButton jumpEndButton = new JButton(">|");
    private final JButton addTrackButton = new JButton("+ Track");
    private final JButton addKeyButton = new JButton("+ Key");
    private final JButton deleteKeyButton = new JButton("− Key");
    private final JButton deleteTrackButton = new JButton("− Track");
    private final JButton zoomOutButton = new JButton("−");
    private final JButton zoomInButton = new JButton("+");
    private final JSlider timeSlider = new JSlider(0, 60 * TIME_SCALE, 0);
    private final JLabel currentTimeLabel = new JLabel("0.00s");

    private final JLabel trackLabel = new JLabel("No track");
    private final JTextField trackIdField = new JTextField("", 8);
    private final JTextField durationField = new JTextField("5", 6);
    private final JTextField beginField = new JTextField("0s", 12);
    private final JTextField endField = new JTextField("", 10);
    private final JTextField repeatField = new JTextField("1", 7);
    private final JTextField repeatDurField = new JTextField("", 8);
    private final JComboBox<String> restartCombo = new JComboBox<String>(
            new String[]{"always", "whenNotActive", "never"});
    private final JComboBox<String> fillModeCombo = new JComboBox<String>(new String[]{"freeze", "remove"});
    private final JComboBox<String> calcModeCombo = new JComboBox<String>(
            new String[]{"linear", "discrete", "spline", "paced"});
    private final JComboBox<String> additiveCombo =
            new JComboBox<String>(new String[]{"replace", "sum"});
    private final JComboBox<String> accumulateCombo =
            new JComboBox<String>(new String[]{"none", "sum"});
    private final JComboBox<String> easingCombo = new JComboBox<String>(new String[]{"Custom", "Linear", "Ease In", "Ease Out", "Ease In-Out"});
    private final JTextField keySplinesField = new JTextField("", 18);
    private final JTextField motionPathField = new JTextField("", 10);
    private final JComboBox<String> motionRotateCombo = new JComboBox<String>(
            new String[]{"auto", "auto-reverse", "0", "90", "180", "270"});
    private final JComboBox<String> motionAnchorCombo = new JComboBox<String>(
            new String[]{"Center", "Local origin", "Custom", "SVG native (import)"});
    private final JTextField motionAnchorXField = new JTextField("0", 6);
    private final JTextField motionAnchorYField = new JTextField("0", 6);
    private final JTextField keyTimeField = new JTextField("", 6);
    private final JTextField keyValueField = new JTextField("", 12);
    private final JButton applyInspectorButton = new JButton("Apply");
    private final JLabel previewStatusLabel = new JLabel("Preview: idle");

    private boolean paused = true;
    private final Timer playbackTimer;
    private final Timer scrubTimer;
    private long playbackStartNanos;
    private long lastEventCanvasRefreshNanos;
    private float playbackStartTime;
    private boolean internalSliderUpdate;
    private boolean preserveTimelineSelection;
    private final Map<String, List<Float>> eventTriggerHistory =
            new HashMap<String, List<Float>>();
    private float timingEvaluationSeconds = 0f;
    private final AWTEventListener canvasEventListener = new AWTEventListener() {
        @Override public void eventDispatched(AWTEvent event) {
            if (!(event instanceof MouseEvent) || canvas == null) return;
            MouseEvent me = (MouseEvent)event;
            Object src = me.getSource();
            if (!(src instanceof Component)) return;
            Component c = (Component)src;

            boolean insideCanvas = c == canvas || javax.swing.SwingUtilities.isDescendingFrom(c, canvas);
            if (!insideCanvas) return;

            switch (me.getID()) {
                case MouseEvent.MOUSE_CLICKED:
                    triggerPreviewEvent("click");
                    break;
                case MouseEvent.MOUSE_ENTERED:
                    triggerPreviewEvent("mouseover");
                    break;
                case MouseEvent.MOUSE_EXITED:
                    triggerPreviewEvent("mouseout");
                    break;
                case MouseEvent.MOUSE_PRESSED:
                    triggerPreviewEvent("mousedown");
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    triggerPreviewEvent("mouseup");
                    break;
                default:
                    break;
            }
        }
    };

    /*
     * Diagnostic preview state. This build intentionally reports what Sketsa's
     * CanvasModel returns and whether direct GVT mutation succeeds.
     */
    private Element previewTarget;
    private GraphicsNode previewGraphicsNode;
    private AffineTransform previewBaseTransform;
    private Composite previewBaseComposite;
    private final List<FillPaintState> previewFillPaints = new ArrayList<FillPaintState>();
    private double previewBaseX;
    private double previewBaseY;
    private double previewBaseCenterX;
    private double previewBaseCenterY;
    private boolean previewBaseCenterValid;

    /*
     * Preview state for animated elements that are not the current timeline
     * selection. The timeline remains single-object for editing, but playback
     * must evaluate all animated objects in the document simultaneously.
     */
    private final Map<Element, List<TransformOp>> documentBaseTransformOps =
            new java.util.IdentityHashMap<Element, List<TransformOp>>();
    private final Map<Element, Boolean> documentBaseVisibility =
            new java.util.IdentityHashMap<Element, Boolean>();
    private final Map<Element, Shape> documentBaseShapes =
            new java.util.IdentityHashMap<Element, Shape>();
    private final Map<Element, Boolean> documentVisibilityBootstrapped =
            new java.util.IdentityHashMap<Element, Boolean>();
    private final Map<Element, Composite> documentBaseComposites =
            new java.util.IdentityHashMap<Element, Composite>();
    private final Map<Element, Point2D.Double> documentMotionAnchors =
            new java.util.IdentityHashMap<Element, Point2D.Double>();
    private int runtimeVisibilityTracks;
    private int runtimeVisibilityResolved;
    private int runtimeMotionTracks;
    private int runtimeMotionResolved;
    private int runtimeOpacityTracks;
    private int runtimeGenericAnimateTracks;
    private int runtimeGenericAnimateHandled;

    public AnimationEditor() {
        super(new BorderLayout());

        canvasPropertyChangeListener = new PropertyChangeListener() {
            @Override public void propertyChange(PropertyChangeEvent evt) {
                if (canvas == null) return;
                updateTimeline(canvas.getCanvasSelection().getSelectionList());

                /*
                 * M5 1.6.3:
                 * Sketsa Properties can change presentation attributes while
                 * Animation Editor is open. Re-evaluate on the next Swing turn
                 * so newly-created painters (notably stroke) are picked up
                 * without requiring a manual scrub.
                 */
                final float stableSeconds = sliderSeconds();
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (canvas != null) setCurrentTime(stableSeconds);
                });
            }
        };

        buildUI();
        SuggestionPopup.install(repeatField, () -> java.util.Arrays.asList("indefinite"));
        SuggestionPopup.install(repeatDurField, () -> java.util.Arrays.asList("indefinite"));
        SuggestionPopup.install(motionPathField, this::availableMotionPaths);

        /*
         * 16 ms gives the local GVT runtime roughly 60 fps. Event-timed
         * tracks intentionally use repaint() rather than VectorCanvas.refresh()
         * to stay isolated from Batik native event SMIL, so the old 40 ms
         * cadence (25 fps) was visibly stepped in tests F/G.
         */
        playbackTimer = new Timer(16, e -> updatePlaybackFrame());
        playbackTimer.setCoalesce(true);

        scrubTimer = new Timer(40, e -> {
            if (canvas == null || !timeSlider.getValueIsAdjusting()) {
                ((Timer)e.getSource()).stop();
                return;
            }
            setCurrentTime(sliderSeconds());
        });
        scrubTimer.setCoalesce(true);

        timeSlider.addChangeListener((ChangeListener)e -> {
            if (canvas == null || internalSliderUpdate) return;
            if (timeSlider.getValueIsAdjusting()) {
                if (!paused) pausePlayback();
                if (!scrubTimer.isRunning()) scrubTimer.start();
            } else {
                scrubTimer.stop();
                setCurrentTime(sliderSeconds());
            }
        });

        timeline.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refreshInspector();
        });

        timeline.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = timeline.rowAtPoint(e.getPoint());
                int col = timeline.columnAtPoint(e.getPoint());
                if (row == 0 && col == 0 && e.getClickCount() == 1) {
                    timeline.getTimelineModel().toggleExpanded();
                    return;
                }
                if (row > 0 && col == 1) {
                    java.awt.Rectangle cell = timeline.getCellRect(row, col, true);
                    int xInCell = e.getX() - cell.x;
                    int idx = timeline.keyIndexAt(row, xInCell);
                    timeline.setSelectedKeyIndex(idx);

                    float seconds;
                    SMILTrack clickedTrack = timeline.getTimelineModel().getTrackAtModelRow(row);
                    if (idx >= 0 && clickedTrack != null && idx < clickedTrack.getKeyTimes().size()) {
                        seconds = clickedTrack.getKeyTimes().get(idx) * clickedTrack.getDurationSeconds();
                    } else {
                        seconds = timeline.secondsAtX(xInCell);
                    }
                    setSliderSeconds(seconds);
                    setCurrentTime(seconds);
                    refreshInspector();
                }
            }
        });

        playButton.addActionListener(e -> { if (paused) startPlayback(); else pausePlayback(); });
        jumpStartButton.addActionListener(e -> jumpToStart());
        jumpEndButton.addActionListener(e -> jumpToEnd());
        zoomInButton.addActionListener(e -> changeZoom(true));
        zoomOutButton.addActionListener(e -> changeZoom(false));
        addTrackButton.addActionListener(e -> showAddTrackMenu());
        addKeyButton.addActionListener(e -> addKeyframe());
        deleteKeyButton.addActionListener(e -> deleteKeyframe());
        deleteTrackButton.addActionListener(e -> deleteTrack());
        applyInspectorButton.addActionListener(e -> applyInspector());
        easingCombo.addActionListener(e -> applyEasingPreset());
        calcModeCombo.addActionListener(e -> updateTimingFieldState());
        motionAnchorCombo.addActionListener(e -> updateMotionAnchorFieldState());

        updateButtons();
    }

    private void buildUI() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(playButton);
        bar.add(jumpStartButton);
        bar.add(jumpEndButton);
        bar.addSeparator();
        bar.add(addTrackButton);
        bar.add(addKeyButton);
        bar.add(deleteKeyButton);
        bar.add(deleteTrackButton);
        bar.addSeparator();
        bar.add(new JLabel(" Zoom "));
        bar.add(zoomOutButton);
        bar.add(zoomInButton);

        JPanel north = new JPanel(new BorderLayout());
        north.add(bar, BorderLayout.WEST);
        north.add(timeSlider, BorderLayout.CENTER);
        JPanel timeReadout = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        timeReadout.add(new JLabel("Time:"));
        timeReadout.add(currentTimeLabel);
        north.add(timeReadout, BorderLayout.EAST);
        add(north, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(timeline);

        JPanel inspector = new JPanel(new GridLayout(6, 1, 4, 2));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("Track:")); row1.add(trackLabel);
        row1.add(new JLabel(" ID:")); row1.add(trackIdField);
        row1.add(new JLabel(" Duration(s):")); row1.add(durationField);
        row1.add(new JLabel(" Begin:")); row1.add(beginField);
        row1.add(new JLabel(" End:")); row1.add(endField);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel("Repeat:")); row2.add(repeatField);
        row2.add(new JLabel(" RepeatDur:")); row2.add(repeatDurField);
        row2.add(new JLabel(" Fill:")); row2.add(fillModeCombo);
        row2.add(new JLabel(" Restart:")); row2.add(restartCombo);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row3.add(new JLabel("Calc Mode:")); row3.add(calcModeCombo);
        row3.add(new JLabel(" Additive:")); row3.add(additiveCombo);
        row3.add(new JLabel(" Accumulate:")); row3.add(accumulateCombo);
        row3.add(new JLabel(" Easing:")); row3.add(easingCombo);
        row3.add(new JLabel(" Key Splines:")); row3.add(keySplinesField);

        JPanel row4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row4.add(new JLabel("Motion Path (#id or inline):")); row4.add(motionPathField);
        row4.add(new JLabel(" Motion Rotate:")); row4.add(motionRotateCombo);
        row4.add(new JLabel("   Key time(s):"));
        keyTimeField.setEditable(true); row4.add(keyTimeField);
        row4.add(new JLabel(" Value:")); row4.add(keyValueField);
        row4.add(applyInspectorButton);

        JPanel row5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row5.add(new JLabel("Motion Anchor:")); row5.add(motionAnchorCombo);
        row5.add(new JLabel(" X:")); row5.add(motionAnchorXField);
        row5.add(new JLabel(" Y:")); row5.add(motionAnchorYField);

        JPanel row6 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row6.add(previewStatusLabel);

        inspector.add(row1); inspector.add(row2); inspector.add(row3);
        inspector.add(row4); inspector.add(row5); inspector.add(row6);

        /*
         * Keep the inspector reachable even when Animation Editor is docked in
         * a short area (for example while DOM Editor is open below it).
         * A fixed vertical JSplitPane divider could push the entire inspector
         * out of view. The timeline now owns the center and the inspector is a
         * scrollable, bounded SOUTH area.
         */
        JScrollPane inspectorScroll = new JScrollPane(
                inspector,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        inspectorScroll.setBorder(null);
        inspectorScroll.setPreferredSize(new Dimension(100, 194));
        inspectorScroll.setMinimumSize(new Dimension(0, 72));

        JPanel center = new JPanel(new BorderLayout());
        center.add(scroll, BorderLayout.CENTER);
        center.add(inspectorScroll, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
    }

    private java.util.List<String> availableMotionPaths() {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<String>();
        Element selected = selectedSVGElement();
        Document doc = selected == null ? null : selected.getOwnerDocument();
        if (doc == null) return new java.util.ArrayList<String>(out);
        org.w3c.dom.NodeList paths = doc.getElementsByTagNameNS("http://www.w3.org/2000/svg", "path");
        if (paths.getLength() == 0) paths = doc.getElementsByTagName("path");
        for (int i = 0; i < paths.getLength(); i++) {
            org.w3c.dom.Node n = paths.item(i);
            if (n instanceof Element) {
                String id = ((Element)n).getAttribute("id");
                if (id != null && !id.trim().isEmpty()) out.add("#" + id.trim());
            }
        }
        return new java.util.ArrayList<String>(out);
    }

    private void showAddTrackMenu() {
        JPopupMenu menu = new JPopupMenu();
        addTrackItem(menu, "Geometry / x", "x");
        addTrackItem(menu, "Geometry / y", "y");
        addTrackItem(menu, "Geometry / cx", "cx");
        addTrackItem(menu, "Geometry / cy", "cy");
        addTrackItem(menu, "Geometry / r", "r");
        addTrackItem(menu, "Geometry / width", "width");
        addTrackItem(menu, "Geometry / height", "height");
        addTrackItem(menu, "Geometry / path d", "d");
        menu.addSeparator();
        addTrackItem(menu, "Appearance / opacity", "opacity");
        addTrackItem(menu, "Appearance / fill", "fill");
        addTrackItem(menu, "Appearance / fill-opacity", "fill-opacity");
        addTrackItem(menu, "Appearance / stroke", "stroke");
        addTrackItem(menu, "Appearance / stroke-opacity", "stroke-opacity");
        addTrackItem(menu, "Appearance / stroke-width", "stroke-width");
        addTrackItem(menu, "State / visibility", "visibility");
        menu.addSeparator();
        addTrackItem(menu, "Transform / translate", "translate");
        addTrackItem(menu, "Transform / scale", "scale");
        addTrackItem(menu, "Transform / rotate", "rotate");
        addTrackItem(menu, "Transform / skewX", "skewX");
        addTrackItem(menu, "Transform / skewY", "skewY");
        menu.addSeparator();
        addTrackItem(menu, "Motion / Motion Path", "Motion Path");
        menu.addSeparator();
        addTrackItem(menu, "Generic / Numeric…", "__generic_numeric__");
        addTrackItem(menu, "Generic / Color…", "__generic_color__");
        addTrackItem(menu, "Generic / Discrete…", "__generic_discrete__");
        menu.addSeparator();
        addTrackItem(menu, "State / Set x", "Set x");
        addTrackItem(menu, "State / Set y", "Set y");
        addTrackItem(menu, "State / Set opacity", "Set opacity");
        addTrackItem(menu, "State / Set fill", "Set fill");
        addTrackItem(menu, "State / Set visibility", "Set visibility");
        menu.show(addTrackButton, 0, addTrackButton.getHeight());
    }


    private void addTrackItem(JPopupMenu menu, String label, String track) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> addTrack(track));
        menu.add(item);
    }

    private Element selectedSVGElement() {
        /*
         * Primary source: the object currently attached to the timeline.
         */
        SVGElement e = timeline.getSVGElement();
        if (e instanceof Element) return (Element)e;

        /*
         * Fallback for reopened/imported SMIL documents.
         *
         * Sketsa can briefly clear the CanvasSelection while the DOM/timeline
         * still contains the selected animation row. In that state the old
         * implementation returned null and the preview status became
         * "no target/canvas", so Motion Path could never move even with a
         * correct Motion Path ID.
         *
         * A SMIL animation element is a child of the SVG element it animates,
         * therefore recover the target directly from the selected track.
         */
        SMILTrack track = selectedTrack();
        if (track != null) {
            Element anim = track.getAnimationElement();
            if (anim != null) {
                org.w3c.dom.Node parent = anim.getParentNode();
                if (parent instanceof Element) return (Element)parent;
            }
        }

        /*
         * Last fallback: if exactly one track exists, its parent is still an
         * unambiguous target even when the row selection was transiently lost.
         */
        List<SMILTrack> tracks = timeline.getTimelineModel().getTracks();
        if (tracks.size() == 1) {
            Element anim = tracks.get(0).getAnimationElement();
            if (anim != null) {
                org.w3c.dom.Node parent = anim.getParentNode();
                if (parent instanceof Element) return (Element)parent;
            }
        }

        return null;
    }

    private List<Element> authoringTargets() {
        List<Element> out = new ArrayList<Element>();
        if (canvas != null) {
            try {
                List<SVGElement> selected =
                        canvas.getCanvasSelection().getSelectionList();
                if (selected != null) {
                    for (SVGElement e : selected) {
                        if (e instanceof Element) out.add((Element)e);
                    }
                }
            } catch (RuntimeException ex) { }
        }
        if (out.isEmpty()) {
            Element one = selectedSVGElement();
            if (one != null) out.add(one);
        }
        return out;
    }

    private boolean isTransformTrackName(String type) {
        return "translate".equals(type) || "scale".equals(type)
                || "rotate".equals(type) || "skewX".equals(type)
                || "skewY".equals(type);
    }

    private String initialTransformValue(Element target, String type) {
        List<TransformOp> ops = parseTransformOps(target.getAttribute("transform"));
        int idx = findTransformOp(ops, type);
        if (idx >= 0) return joinNumbers(ops.get(idx).p);

        if ("translate".equals(type)) return "0 0";
        if ("scale".equals(type)) return "1 1";

        if ("rotate".equals(type)) {
            Point2D.Double center = transformPivotCenter(target);
            return "0 " + trimDouble(center.x) + " " + trimDouble(center.y);
        }

        return "0";
    }

    private Point2D.Double transformPivotCenter(Element target) {
        if (target == null) return new Point2D.Double(0d, 0d);

        /*
         * M5 1.6.6:
         * Editor-authored transforms use the visual object's LOCAL center as
         * their common pivot. GraphicsNode bounds are local user-space bounds,
         * so an outer authored translate/rotate does not move this pivot.
         */
        try {
            if (canvas != null && target instanceof SVGElement) {
                GraphicsNode node =
                        canvas.getModel().getGraphicsNode((SVGElement)target);
                if (node != null) {
                    Rectangle2D b = node.getBounds();
                    if (b != null) {
                        return new Point2D.Double(
                                b.getCenterX(), b.getCenterY());
                    }
                }
            }
        } catch (RuntimeException ex) { }

        String tag = localName(target);
        if ("circle".equals(tag) || "ellipse".equals(tag)) {
            return new Point2D.Double(
                    parseNumericAttribute(target, "cx", 0d),
                    parseNumericAttribute(target, "cy", 0d));
        }
        if ("rect".equals(tag) || "image".equals(tag)) {
            double x = parseNumericAttribute(target, "x", 0d);
            double y = parseNumericAttribute(target, "y", 0d);
            double w = parseNumericAttribute(target, "width", 0d);
            double h = parseNumericAttribute(target, "height", 0d);
            return new Point2D.Double(x + w/2d, y + h/2d);
        }
        if ("line".equals(tag)) {
            double x1 = parseNumericAttribute(target, "x1", 0d);
            double y1 = parseNumericAttribute(target, "y1", 0d);
            double x2 = parseNumericAttribute(target, "x2", 0d);
            double y2 = parseNumericAttribute(target, "y2", 0d);
            return new Point2D.Double((x1+x2)/2d, (y1+y2)/2d);
        }
        return new Point2D.Double(0d, 0d);
    }

    private boolean needsPivotHelpers(String type) {
        return "scale".equals(type)
                || "skewX".equals(type)
                || "skewY".equals(type);
    }

    private void createPivotHelpers(
            Element target, Element main, String type) {
        if (target == null || main == null || !needsPivotHelpers(type)) return;

        Document doc = target.getOwnerDocument();
        Point2D.Double pivot = transformPivotCenter(target);
        String owner = "pivot-" + java.util.UUID.randomUUID().toString();

        main.setAttribute("data-sketsa-pivot-owner", owner);
        main.setAttribute("data-sketsa-pivot-x", trimDouble(pivot.x));
        main.setAttribute("data-sketsa-pivot-y", trimDouble(pivot.y));

        Element before = doc.createElementNS(SVG_NS, "animateTransform");
        before.setAttribute("attributeName", "transform");
        before.setAttribute("type", "translate");
        before.setAttribute("values",
                trimDouble(pivot.x) + " " + trimDouble(pivot.y)
                + ";" + trimDouble(pivot.x) + " " + trimDouble(pivot.y));
        before.setAttribute("keyTimes", "0;1");
        before.setAttribute("additive", "sum");
        before.setAttribute("data-sketsa-pivot-helper", "true");
        before.setAttribute("data-sketsa-pivot-owner", owner);

        Element after = doc.createElementNS(SVG_NS, "animateTransform");
        after.setAttribute("attributeName", "transform");
        after.setAttribute("type", "translate");
        after.setAttribute("values",
                trimDouble(-pivot.x) + " " + trimDouble(-pivot.y)
                + ";" + trimDouble(-pivot.x) + " " + trimDouble(-pivot.y));
        after.setAttribute("keyTimes", "0;1");
        after.setAttribute("additive", "sum");
        after.setAttribute("data-sketsa-pivot-helper", "true");
        after.setAttribute("data-sketsa-pivot-owner", owner);

        target.insertBefore(before, main);
        org.w3c.dom.Node next = main.getNextSibling();
        if (next != null) target.insertBefore(after, next);
        else target.appendChild(after);

        syncPivotHelpers(new SMILTrack(main, type, "animateTransform"));
    }

    private void syncPivotHelpers(SMILTrack track) {
        if (track == null || !"animateTransform".equals(track.getKind())) return;
        Element main = track.getAnimationElement();
        String owner = main.getAttribute("data-sketsa-pivot-owner");
        if (owner == null || owner.trim().isEmpty()) return;

        org.w3c.dom.Node parent = main.getParentNode();
        if (!(parent instanceof Element)) return;

        org.w3c.dom.NodeList children = parent.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (!(n instanceof Element)) continue;
            Element e = (Element)n;
            if (!"true".equals(e.getAttribute("data-sketsa-pivot-helper"))) continue;
            if (!owner.equals(e.getAttribute("data-sketsa-pivot-owner"))) continue;

            copyTimingAttribute(main, e, "dur");
            copyTimingAttribute(main, e, "begin");
            copyTimingAttribute(main, e, "end");
            copyTimingAttribute(main, e, "repeatCount");
            copyTimingAttribute(main, e, "repeatDur");
            copyTimingAttribute(main, e, "restart");
            copyTimingAttribute(main, e, "fill");
            e.setAttribute("calcMode", "linear");
            e.setAttribute("additive", "sum");
        }
    }

    private void copyTimingAttribute(Element source, Element dest, String name) {
        if (source.hasAttribute(name)) dest.setAttribute(name, source.getAttribute(name));
        else dest.removeAttribute(name);
    }

    private void removePivotHelpers(Element main) {
        if (main == null) return;
        String owner = main.getAttribute("data-sketsa-pivot-owner");
        if (owner == null || owner.trim().isEmpty()) return;

        org.w3c.dom.Node parent = main.getParentNode();
        if (!(parent instanceof Element)) return;

        List<org.w3c.dom.Node> remove = new ArrayList<org.w3c.dom.Node>();
        org.w3c.dom.NodeList children = parent.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (!(n instanceof Element)) continue;
            Element e = (Element)n;
            if ("true".equals(e.getAttribute("data-sketsa-pivot-helper"))
                    && owner.equals(e.getAttribute("data-sketsa-pivot-owner"))) {
                remove.add(e);
            }
        }
        for (org.w3c.dom.Node n : remove) parent.removeChild(n);
    }

    private List<SMILTrack> linkedMultiEditTracks(SMILTrack selected) {
        List<SMILTrack> out = new ArrayList<SMILTrack>();
        if (selected == null) return out;

        Element anim = selected.getAnimationElement();
        if (anim == null) {
            out.add(selected);
            return out;
        }

        String group = anim.getAttribute("data-sketsa-multi-edit-group");
        if (group == null || group.trim().isEmpty()) {
            out.add(selected);
            return out;
        }

        Document doc = anim.getOwnerDocument();
        if (doc == null || doc.getDocumentElement() == null) {
            out.add(selected);
            return out;
        }

        collectLinkedMultiEditTracks(
                doc.getDocumentElement(), group.trim(), out);

        if (out.isEmpty()) out.add(selected);
        return out;
    }

    private void collectLinkedMultiEditTracks(
            Element root, String group, List<SMILTrack> out) {
        if (root == null) return;

        String local = localName(root);
        if (isAnimationElementName(local)
                && group.equals(root.getAttribute(
                        "data-sketsa-multi-edit-group"))) {
            String name;
            String kind = local;
            if ("animateTransform".equals(local)) {
                name = root.getAttribute("type");
                if (name == null || name.trim().isEmpty()) name = "transform";
            } else if ("animateMotion".equals(local)) {
                name = "Motion Path";
            } else if ("set".equals(local)) {
                name = "Set " + root.getAttribute("attributeName");
            } else {
                name = root.getAttribute("attributeName");
            }
            out.add(new SMILTrack(root, name, kind));
        }

        org.w3c.dom.NodeList children = root.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (n instanceof Element) {
                collectLinkedMultiEditTracks((Element)n, group, out);
            }
        }
    }

    private Element createAuthoredTrack(
            Element target, String type, String motionSource,
            String forcedInitial, boolean genericDiscrete) {
        Document doc = target.getOwnerDocument();
        Element anim;
        String initial = "";

        if (type.startsWith("Set ")) {
            String attr = type.substring(4).trim();
            anim = doc.createElementNS(SVG_NS, "set");
            anim.setAttribute("attributeName", attr);
            anim.setAttribute("to", forcedInitial == null
                    ? initialValue(target, attr) : forcedInitial);
            anim.setAttribute("dur", "1s");
        } else if ("Motion Path".equals(type)) {
            anim = doc.createElementNS(SVG_NS, "animateMotion");
            anim.setAttribute("rotate", "auto");
            anim.setAttribute("dur", "5s");
        } else if (isTransformTrackName(type)) {
            anim = doc.createElementNS(SVG_NS, "animateTransform");
            anim.setAttribute("attributeName", "transform");
            anim.setAttribute("type", type);
            initial = initialTransformValue(target, type);
            anim.setAttribute("values", initial + ";" + initial);
            anim.setAttribute("keyTimes", "0;1");
            anim.setAttribute("dur", "5s");
            anim.setAttribute("calcMode", "linear");
        } else {
            anim = doc.createElementNS(SVG_NS, "animate");
            anim.setAttribute("attributeName", type);
            initial = forcedInitial == null ? initialValue(target, type) : forcedInitial;
            if ("d".equals(type) && (initial == null || initial.trim().isEmpty())) {
                initial = target.getAttribute("d");
            }
            if ("visibility".equals(type)
                    && (initial == null || initial.trim().isEmpty())) {
                initial = "visible";
            }
            anim.setAttribute("values", initial + ";" + initial);
            anim.setAttribute("keyTimes", "0;1");
            anim.setAttribute("dur", "5s");
            anim.setAttribute("calcMode", genericDiscrete ? "discrete" : "linear");
        }

        anim.setAttribute("fill", "freeze");
        anim.setAttribute("data-sketsa-track", type);
        target.appendChild(anim);

        if (isTransformTrackName(type) && needsPivotHelpers(type)) {
            createPivotHelpers(target, anim, type);
        }

        if ("Motion Path".equals(type)) {
            SMILTrack mt = new SMILTrack(anim, "Motion Path", "animateMotion");
            setMotionAnchorMode(mt, "center", 0d, 0d);
            String source = motionSource == null ? "" : motionSource.trim();
            String id = source.startsWith("#") ? source.substring(1) : source;
            if (findElementById(target.getOwnerDocument().getDocumentElement(), id) != null) {
                mt.setMotionPathId(id);
            } else {
                mt.setInlineMotionPath(source);
            }
        }
        return anim;
    }

    private void addTrack(String requestedType) {
        restorePreviewBase();
        if (canvas == null) return;

        List<Element> targets = authoringTargets();
        if (targets.isEmpty()) return;

        String type = requestedType;
        String forcedInitial = null;
        boolean genericDiscrete = "__generic_discrete__".equals(requestedType);

        if (requestedType.startsWith("__generic_")) {
            String attr = JOptionPane.showInputDialog(
                    this, "SVG attribute / CSS property name:", "");
            if (attr == null) return;
            attr = attr.trim();
            if (attr.isEmpty()) return;
            type = attr;

            String def = initialValue(targets.get(0), type);
            String val = JOptionPane.showInputDialog(
                    this, "Initial value for '" + type + "':", def);
            if (val == null) return;
            forcedInitial = val.trim();
        }

        String motionSource = null;
        if ("Motion Path".equals(type)) {
            motionSource = JOptionPane.showInputDialog(
                    this,
                    "Motion source: #pathId, pathId, or inline SVG path data:",
                    "");
            if (motionSource == null) return;
            motionSource = motionSource.trim();
            if (motionSource.isEmpty()) return;

            String id = motionSource.startsWith("#")
                    ? motionSource.substring(1) : motionSource;
            boolean inline = motionSource.matches("(?i)\\s*[mM].*");
            if (!inline && findElementById(
                    targets.get(0).getOwnerDocument().getDocumentElement(),
                    id) == null) {
                JOptionPane.showMessageDialog(this,
                        "No matching path ID and value is not inline path data.");
                return;
            }
        }

        if (type.startsWith("Set ")) {
            String attr = type.substring(4).trim();
            String val = JOptionPane.showInputDialog(
                    this, "Value for " + attr + ":",
                    initialValue(targets.get(0), attr));
            if (val == null || val.trim().isEmpty()) return;
            forcedInitial = val.trim();
        }

        for (Element target : targets) {
            org.w3c.dom.NodeList children = target.getChildNodes();
            for (int i=0; i<children.getLength(); i++) {
                org.w3c.dom.Node n = children.item(i);
                if (!(n instanceof Element)) continue;
                Element e = (Element)n;
                String local = localName(e);
                if ("true".equals(e.getAttribute("data-sketsa-pivot-helper"))) {
                    continue;
                }
                boolean same = false;
                if ("animateTransform".equals(local) && isTransformTrackName(type)) {
                    same = type.equals(e.getAttribute("type"));
                } else if ("animateMotion".equals(local)
                        && "Motion Path".equals(type)) {
                    same = true;
                } else if ("set".equals(local) && type.startsWith("Set ")) {
                    same = type.substring(4).trim().equals(
                            e.getAttribute("attributeName"));
                } else if ("animate".equals(local)) {
                    same = type.equals(e.getAttribute("attributeName"));
                }
                if (same) {
                    JOptionPane.showMessageDialog(this,
                            "At least one selected object already has track '"
                            + type + "'.");
                    return;
                }
            }
        }

        List<Element> created = new ArrayList<Element>();
        final String multiEditGroup = targets.size() > 1
                ? "multi-" + java.util.UUID.randomUUID().toString()
                : "";
        beginUndoTransaction(canvas.getUndoManager(), 
                targets.size() > 1
                        ? "Add SMIL Track " + type + " to "
                            + targets.size() + " objects"
                        : "Add SMIL Track " + type);
        try {
            for (Element target : targets) {
                Element createdTrack = createAuthoredTrack(
                        target, type, motionSource,
                        forcedInitial, genericDiscrete);
                if (!multiEditGroup.isEmpty()) {
                    createdTrack.setAttribute(
                            "data-sketsa-multi-edit-group",
                            multiEditGroup);
                }
                created.add(createdTrack);
            }
        } finally {
            canvas.getUndoManager().end();
        }

        for (Element anim : created) {
            if (!"set".equals(localName(anim))) activateNewAnimation(anim);
        }

        canvas.refresh();
        if (!targets.isEmpty() && targets.get(0) instanceof SVGElement) {
            timeline.setSVGElement((SVGElement)targets.get(0));
        } else {
            timeline.getTimelineModel().refresh();
        }
        selectTrackByName(type);
        setCurrentTime(sliderSeconds());
        refreshInspector();
        updateButtons();
    }


    private Element ensureMotionPathReference(Element animateMotion, String pathId) {
        if (animateMotion == null) return null;
        String id = pathId == null ? "" : pathId.trim();
        if (id.isEmpty()) return null;

        Element mpath = null;
        org.w3c.dom.NodeList children = animateMotion.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (!(n instanceof Element)) continue;
            Element e = (Element)n;
            String local = e.getLocalName();
            if (local == null) local = e.getTagName();
            if ("mpath".equals(local) || local.endsWith(":mpath")) {
                mpath = e;
                break;
            }
        }

        if (mpath == null) {
            mpath = animateMotion.getOwnerDocument().createElementNS(SVG_NS, "mpath");
            animateMotion.appendChild(mpath);
        }

        String ref = "#" + id;
        // SVG 2 href
        mpath.setAttributeNS(null, "href", ref);
        // SVG 1.1 compatibility
        mpath.setAttributeNS("http://www.w3.org/1999/xlink", "xlink:href", ref);

        animateMotion.setAttribute("data-sketsa-motion-path-id", id);
        return mpath;
    }

    private void activateNewAnimation(Element anim) {
        if (canvas == null || anim == null) return;

        /*
         * Batik's animation engine is already running by the time the editor
         * inserts a new <animate>/<animateTransform>. A dynamically inserted
         * SMIL element can exist in the DOM without an active begin instance.
         *
         * Reset to the document origin and explicitly begin the new element so
         * subsequent setCurrentTime() calls can evaluate it while scrubbing.
         */
        SVGAnimationEngine eng = canvas.getAnimationEngine();
        if (eng == null) return;

        if (!eng.hasStarted()) {
            eng.start(0);
        }

        eng.setCurrentTime(0f);

        if (anim instanceof SVGAnimationElement) {
            try {
                ((SVGAnimationElement)anim).beginElement();
            } catch (RuntimeException ex) {
                // Keep the DOM valid even if a renderer refuses dynamic begin.
            }
        }

        setSliderSeconds(0f);

        canvas.refresh();
    }

    private String initialValue(Element target, String attr) {
        String v = target.getAttribute(attr);
        if (!v.isEmpty()) return v;
        if ("opacity".equals(attr)) return "1";
        if ("fill".equals(attr)) return "#000000";
        return "0";
    }

    private void selectTrackByName(String name) {
        TimelineModel m = timeline.getTimelineModel();
        List<SMILTrack> ts = m.getTracks();
        for (int i=0; i<ts.size(); i++) {
            if (name.equals(ts.get(i).getName())) {
                timeline.setRowSelectionInterval(i+1, i+1);
                timeline.setSelectedKeyIndex(-1);
                break;
            }
        }
    }

    private SMILTrack selectedTrack() {
        int row = timeline.getSelectedRow();
        return timeline.getTimelineModel().getTrackAtModelRow(row);
    }

    private float sliderSeconds() {
        return timeSlider.getValue() / (float)TIME_SCALE;
    }

    private void setSliderSeconds(float seconds) {
        float max = timeSlider.getMaximum() / (float)TIME_SCALE;
        seconds = Math.max(0f, Math.min(max, seconds));
        int value = Math.round(seconds * TIME_SCALE);
        internalSliderUpdate = true;
        try {
            timeSlider.setValue(value);
        } finally {
            internalSliderUpdate = false;
        }
        currentTimeLabel.setText(String.format(java.util.Locale.US, "%.2fs", seconds));
        timeline.setCurrentTime(seconds);
    }

    private void addKeyframe() {
        restorePreviewBase();
        SMILTrack track = selectedTrack();
        if (canvas == null || track == null) {
            JOptionPane.showMessageDialog(this, "Select a track first.");
            return;
        }

        float dur = track.getDurationSeconds();
        float seconds = Math.max(0f, Math.min(dur, sliderSeconds()));
        float keyTime = seconds / dur;

        List<Float> times = new ArrayList<Float>(track.getKeyTimes());
        List<String> values = new ArrayList<String>(track.getValues());
        while (values.size() < times.size()) values.add(values.isEmpty() ? "0" : values.get(values.size()-1));

        String suggested = nearestValue(times, values, keyTime);
        String value = JOptionPane.showInputDialog(this,
                "Value at " + SMILTrack.trim(seconds) + "s:", suggested);
        if (value == null) return;

        int replace = -1;
        for (int i=0; i<times.size(); i++) {
            if (Math.abs(times.get(i) - keyTime) < 0.0005f) { replace = i; break; }
        }

        int selectedIndex;
        if (replace >= 0) {
            values.set(replace, value.trim());
            selectedIndex = replace;
        } else {
            int pos = 0;
            while (pos < times.size() && times.get(pos) < keyTime) pos++;
            times.add(pos, keyTime);
            values.add(pos, value.trim());
            selectedIndex = pos;
        }

        List<SMILTrack> editTracks = linkedMultiEditTracks(track);
        beginUndoTransaction(canvas.getUndoManager(), 
                editTracks.size() > 1
                        ? "Add SMIL Keyframe to multiple objects"
                        : "Add SMIL Keyframe");
        try {
            for (SMILTrack editTrack : editTracks) {
                editTrack.setKeys(
                        new ArrayList<Float>(times),
                        new ArrayList<String>(values));
                normalizeSplineCount(editTrack);
            }
        } finally {
            canvas.getUndoManager().end();
        }

        applyEditorPreview(sliderSeconds());
        canvas.refresh();
        timeline.repaint();
        timeline.setSelectedKeyIndex(selectedIndex);
        refreshInspector();
    }

    private String nearestValue(List<Float> times, List<String> values, float keyTime) {
        if (values.isEmpty()) return "0";
        int best = 0;
        float dist = Float.MAX_VALUE;
        for (int i=0; i<Math.min(times.size(), values.size()); i++) {
            float d = Math.abs(times.get(i) - keyTime);
            if (d < dist) { dist = d; best = i; }
        }
        return values.get(best);
    }

    private void deleteKeyframe() {
        restorePreviewBase();
        SMILTrack track = selectedTrack();
        int idx = timeline.getSelectedKeyIndex();
        if (canvas == null || track == null || idx < 0) return;

        List<Float> times = new ArrayList<Float>(track.getKeyTimes());
        List<String> values = new ArrayList<String>(track.getValues());
        if (times.size() <= 2) {
            JOptionPane.showMessageDialog(this, "A track keeps at least two endpoint keys.");
            return;
        }
        if (idx >= times.size() || idx >= values.size()) return;

        times.remove(idx);
        values.remove(idx);

        List<SMILTrack> editTracks = linkedMultiEditTracks(track);
        beginUndoTransaction(canvas.getUndoManager(), 
                editTracks.size() > 1
                        ? "Delete SMIL Keyframe from multiple objects"
                        : "Delete SMIL Keyframe");
        try {
            for (SMILTrack editTrack : editTracks) {
                editTrack.setKeys(
                        new ArrayList<Float>(times),
                        new ArrayList<String>(values));
                normalizeSplineCount(editTrack);
            }
        } finally {
            canvas.getUndoManager().end();
        }

        timeline.setSelectedKeyIndex(-1);
        finishTrackEdit(track);
        timeline.repaint();
    }

    private void deleteTrack() {
        restorePreviewBase();
        SMILTrack track = selectedTrack();
        if (canvas == null || track == null) return;

        Element anim = track.getAnimationElement();
        Element target = anim.getParentNode() instanceof Element
                ? (Element)anim.getParentNode() : selectedSVGElement();

        if (JOptionPane.showConfirmDialog(this, "Delete track '" + track.getName() + "'?",
                "Animation", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        /*
         * M5 1.6.5:
         * Stop a possible native Batik instance before removing the DOM node.
         * Otherwise a deleted event/clock animation can leave renderer state
         * alive until the next time change.
         */
        stopNativeAnimationInstance(track);

        beginUndoTransaction(canvas.getUndoManager(), "Delete SMIL Track");
        try {
            removePivotHelpers(anim);
            if (anim.getParentNode() != null) anim.getParentNode().removeChild(anim);
        } finally {
            canvas.getUndoManager().end();
        }

        /*
         * M5 1.6.1:
         * Removing a SMIL track must also remove its live GVT contribution.
         *
         * A deleted animateTransform could disappear correctly from the DOM
         * and timeline while its last preview matrix remained installed on the
         * existing GraphicsNode. A normal canvas.refresh() is not sufficient
         * in every Batik/Sketsa dynamic-update state.
         *
         * Drop all cached runtime bases for this target, rebuild from the
         * authored DOM, explicitly restore the authored transform/visibility,
         * and finally evaluate any SMIL tracks that still remain on the target.
         */
        resetRuntimeVisualStateAfterTrackDelete(target);

        timeline.getTimelineModel().refresh();
        timeline.clearSelection();
        timeline.setSelectedKeyIndex(-1);
        refreshInspector();
        updateButtons();
    }

    private void resetRuntimeVisualStateAfterTrackDelete(Element target) {
        if (canvas == null) return;

        /*
         * M5 1.6.5 — GLOBAL DELETE RESET
         *
         * Track deletion is renderer state invalidation, not a special case for
         * transform or stroke. Every animation kind can mutate live GVT state:
         * transforms, motion, primitive geometry, path d, opacity, fill/stroke,
         * visibility and generic properties.
         *
         * Therefore deletion now invalidates the complete runtime cache and
         * rebuilds the visual target from authored SVG before re-evaluating all
         * remaining SMIL at the current editor time.
         */
        documentBaseTransformOps.clear();
        documentBaseVisibility.clear();
        documentBaseShapes.clear();
        documentVisibilityBootstrapped.clear();
        documentBaseComposites.clear();
        documentMotionAnchors.clear();

        previewTarget = null;
        previewGraphicsNode = null;
        previewBaseTransform = null;
        previewBaseComposite = null;
        previewFillPaints.clear();

        final float stableSeconds = sliderSeconds();

        canvas.refresh();
        restoreAuthoredVisualState(target);

        /*
         * Re-evaluate the entire document, not only the edited target. This is
         * important when the deleted track participated in timing/composition
         * with other objects.
         */
        setCurrentTime(stableSeconds);

        /*
         * Batik/Sketsa may replace GraphicsNodes asynchronously after refresh.
         * A second authoritative pass on the next Swing turn guarantees that
         * no deleted animation contribution survives until manual scrub.
         */
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (canvas == null) return;
            canvas.refresh();
            restoreAuthoredVisualState(target);
            setCurrentTime(stableSeconds);

            /*
             * setCurrentTime() refreshes again and may expose stale native
             * visibility state from Batik's previous animation instance.
             * Reassert authored state once more as the last operation.
             */
            restoreAuthoredVisualState(target);
            canvas.repaint();
        });
    }

    private void restoreAuthoredVisualState(Element target) {
        if (canvas == null || target == null || !(target instanceof SVGElement)) {
            return;
        }

        GraphicsNode node = null;
        try {
            node = canvas.getModel().getGraphicsNode((SVGElement)target);
        } catch (RuntimeException ex) {
            node = null;
        }
        if (node == null) return;

        // ----- Transform / Motion base -----
        try {
            List<TransformOp> authoredOps =
                    parseTransformOps(target.getAttribute("transform"));
            node.setTransform(toAffineTransform(authoredOps));
        } catch (RuntimeException ex) { }

        // ----- Visibility -----
        try {
            String authoredVisibility =
                    underlyingValueForAttribute(target, "visibility");
            boolean visible = !"hidden".equals(authoredVisibility)
                    && !"collapse".equals(authoredVisibility);

            /*
             * M5 1.6.7:
             * Visibility runtime propagates the computed state through the GVT
             * subtree. Restoring only the root GraphicsNode after track deletion
             * is therefore insufficient: descendants can remain hidden until a
             * later scrub/refresh. Restore the complete inherited subtree from
             * the authored/static SVG state.
             */
            applyInheritedVisibility(node, visible, true);
        } catch (RuntimeException ex) { }

        // ----- Opacity -----
        try {
            float opacity = clamp01(parseFloatOr(
                    underlyingValueForAttribute(target, "opacity"), 1f));
            node.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, opacity));
        } catch (RuntimeException ex) { }

        if (!(node instanceof ShapeNode)) return;
        ShapeNode shapeNode = (ShapeNode)node;

        // ----- Primitive geometry / path d -----
        try {
            Shape authoredShape = authoredShapeForTarget(target);
            if (authoredShape != null) shapeNode.setShape(authoredShape);
        } catch (RuntimeException ex) { }

        // ----- Fill / stroke presentation -----
        ShapePainter painter = shapeNode.getShapePainter();
        List<FillShapePainter> fills = new ArrayList<FillShapePainter>();
        List<StrokeShapePainter> strokes = new ArrayList<StrokeShapePainter>();
        collectPaintAdapters(painter, fills, strokes);

        try {
            String fillRaw = underlyingValueForAttribute(target, "fill");
            Paint fill = "none".equalsIgnoreCase(fillRaw)
                    ? null : parseColorPaint(fillRaw);
            float fillAlpha = clamp01(parseFloatOr(
                    underlyingValueForAttribute(target, "fill-opacity"), 1f));
            for (FillShapePainter fp : fills) {
                fp.setPaint(fill == null ? null : withPaintAlpha(fill, fillAlpha));
            }
        } catch (RuntimeException ex) { }

        try {
            String strokeRaw = underlyingValueForAttribute(target, "stroke");
            Paint strokePaint = "none".equalsIgnoreCase(strokeRaw)
                    ? null : parseColorPaint(strokeRaw);
            float strokeAlpha = clamp01(parseFloatOr(
                    underlyingValueForAttribute(target, "stroke-opacity"), 1f));
            float width = Math.max(0f, parseFloatOr(
                    underlyingValueForAttribute(target, "stroke-width"), 1f));

            for (StrokeShapePainter sp : strokes) {
                sp.setPaint(strokePaint == null
                        ? null : withPaintAlpha(strokePaint, strokeAlpha));

                Stroke oldStroke = sp.getStroke();
                if (oldStroke instanceof BasicStroke) {
                    BasicStroke bs = (BasicStroke)oldStroke;
                    int cap = lineCap(
                            underlyingValueForAttribute(target, "stroke-linecap"),
                            bs.getEndCap());
                    int join = lineJoin(
                            underlyingValueForAttribute(target, "stroke-linejoin"),
                            bs.getLineJoin());
                    float miter = Math.max(1f, parseFloatOr(
                            underlyingValueForAttribute(
                                    target, "stroke-miterlimit"),
                            bs.getMiterLimit()));
                    float phase = parseFloatOr(
                            underlyingValueForAttribute(
                                    target, "stroke-dashoffset"),
                            bs.getDashPhase());
                    try {
                        sp.setStroke(new BasicStroke(
                                width, cap, join, miter,
                                bs.getDashArray(), phase));
                    } catch (IllegalArgumentException ex) {
                        sp.setStroke(new BasicStroke(width));
                    }
                } else {
                    sp.setStroke(new BasicStroke(width));
                }
            }
        } catch (RuntimeException ex) { }
    }

    private Shape authoredShapeForTarget(Element target) {
        String tag = localName(target);

        if ("path".equals(tag)) {
            String d = target.getAttribute("d");
            if (d != null && !d.trim().isEmpty()) {
                return parsePathData(d.trim());
            }
            return null;
        }

        if ("circle".equals(tag)) {
            double cx = parseNumericAttribute(target, "cx", 0d);
            double cy = parseNumericAttribute(target, "cy", 0d);
            double r = Math.max(0d, parseNumericAttribute(target, "r", 0d));
            return new Ellipse2D.Double(cx-r, cy-r, r*2d, r*2d);
        }

        if ("ellipse".equals(tag)) {
            double cx = parseNumericAttribute(target, "cx", 0d);
            double cy = parseNumericAttribute(target, "cy", 0d);
            double rx = Math.max(0d, parseNumericAttribute(target, "rx", 0d));
            double ry = Math.max(0d, parseNumericAttribute(target, "ry", 0d));
            return new Ellipse2D.Double(cx-rx, cy-ry, rx*2d, ry*2d);
        }

        if ("rect".equals(tag)) {
            double x = parseNumericAttribute(target, "x", 0d);
            double y = parseNumericAttribute(target, "y", 0d);
            double w = Math.max(0d, parseNumericAttribute(target, "width", 0d));
            double h = Math.max(0d, parseNumericAttribute(target, "height", 0d));
            double rx = Math.max(0d, parseNumericAttribute(target, "rx", 0d));
            double ry = Math.max(0d, parseNumericAttribute(target, "ry", 0d));

            if (rx > 0d || ry > 0d) {
                if (rx == 0d) rx = ry;
                if (ry == 0d) ry = rx;
                rx = Math.min(rx, w/2d);
                ry = Math.min(ry, h/2d);
                return new RoundRectangle2D.Double(
                        x, y, w, h, rx*2d, ry*2d);
            }
            return new Rectangle2D.Double(x, y, w, h);
        }

        if ("line".equals(tag)) {
            double x1 = parseNumericAttribute(target, "x1", 0d);
            double y1 = parseNumericAttribute(target, "y1", 0d);
            double x2 = parseNumericAttribute(target, "x2", 0d);
            double y2 = parseNumericAttribute(target, "y2", 0d);
            return new Line2D.Double(x1, y1, x2, y2);
        }

        return null;
    }

    private void updateMotionAnchorFieldState() {
        SMILTrack track = selectedTrack();
        boolean motion = track != null && track.isMotionTrack();
        motionAnchorCombo.setEnabled(motion);

        boolean custom = motion
                && "Custom".equals(String.valueOf(
                        motionAnchorCombo.getSelectedItem()));
        motionAnchorXField.setEnabled(custom);
        motionAnchorYField.setEnabled(custom);
    }

    private boolean isImportedNativeMotionTrack(SMILTrack track) {
        if (track == null || !track.isMotionTrack()) return false;
        Element anim = track.getAnimationElement();
        if (anim == null) return false;
        return !anim.hasAttribute("data-sketsa-motion-anchor");
    }

    private String motionAnchorMode(SMILTrack track) {
        if (track == null || !track.isMotionTrack()) return "native";
        Element anim = track.getAnimationElement();
        if (anim == null) return "native";

        String raw = anim.getAttribute("data-sketsa-motion-anchor");
        if ("center".equals(raw) || "origin".equals(raw)
                || "custom".equals(raw) || "native".equals(raw)) {
            return raw;
        }

        /*
         * IMPORT FALLBACK:
         * An imported SVG that did not pass through Animation Editor has no
         * Sketsa anchor metadata. Preserve standard/native SVG animateMotion
         * semantics rather than silently changing the file to center-anchor.
         */
        return "native";
    }

    private void setMotionAnchorMode(
            SMILTrack track, String mode, double x, double y) {
        if (track == null || !track.isMotionTrack()) return;
        Element anim = track.getAnimationElement();
        if (anim == null) return;

        String m = mode == null ? "native" : mode.trim();
        if (!"center".equals(m) && !"origin".equals(m)
                && !"custom".equals(m) && !"native".equals(m)) {
            m = "native";
        }

        anim.setAttribute("data-sketsa-motion-anchor", m);
        if ("custom".equals(m)) {
            anim.setAttribute("data-sketsa-motion-anchor-x", trimDouble(x));
            anim.setAttribute("data-sketsa-motion-anchor-y", trimDouble(y));
        } else {
            anim.removeAttribute("data-sketsa-motion-anchor-x");
            anim.removeAttribute("data-sketsa-motion-anchor-y");
        }

        documentMotionAnchors.remove(
                anim.getParentNode() instanceof Element
                        ? (Element)anim.getParentNode() : null);
    }

    private void updateTimingFieldState() {
        SMILTrack selected = selectedTrack();
        boolean hasTrack = selected != null;
        boolean motion = hasTrack && selected.isMotionTrack();
        boolean setTrack = hasTrack && selected.isSetTrack();
        boolean spline = hasTrack && !motion && !setTrack && "spline".equals(calcModeCombo.getSelectedItem());
        keySplinesField.setEnabled(spline);
        easingCombo.setEnabled(hasTrack && !motion && !setTrack);
        calcModeCombo.setEnabled(hasTrack && !motion && !setTrack);
    }

    private void applyEasingPreset() {
        if (selectedTrack() == null) return;
        String preset = String.valueOf(easingCombo.getSelectedItem());
        if ("Custom".equals(preset)) return;

        if ("Linear".equals(preset)) {
            calcModeCombo.setSelectedItem("linear");
            keySplinesField.setText("");
        } else {
            calcModeCombo.setSelectedItem("spline");
            if ("Ease In".equals(preset)) {
                keySplinesField.setText(repeatSplineForIntervals("0.42 0 1 1"));
            } else if ("Ease Out".equals(preset)) {
                keySplinesField.setText(repeatSplineForIntervals("0 0 0.58 1"));
            } else if ("Ease In-Out".equals(preset)) {
                keySplinesField.setText(repeatSplineForIntervals("0.42 0 0.58 1"));
            }
        }
        updateTimingFieldState();
    }

    private String repeatSplineForIntervals(String spline) {
        SMILTrack track = selectedTrack();
        int intervals = track == null ? 1 : Math.max(1, track.getKeyTimes().size() - 1);
        StringBuilder out = new StringBuilder();
        for (int i=0; i<intervals; i++) {
            if (i > 0) out.append(';');
            out.append(spline);
        }
        return out.toString();
    }

    private boolean validRepeatCount(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty() || "indefinite".equals(v)) return true;
        try { return Float.parseFloat(v) > 0f; }
        catch (RuntimeException ex) { return false; }
    }

    private void normalizeSplineCount(SMILTrack track) {
        if (track == null || !"spline".equals(track.getCalcMode())) return;
        int intervals = Math.max(1, track.getKeyTimes().size() - 1);
        String raw = track.getKeySplines();
        String base = "0.42 0 0.58 1";
        if (raw != null && !raw.trim().isEmpty()) {
            String[] groups = raw.split(";");
            if (groups.length > 0 && groups[0].trim().split("\\s+").length == 4) {
                base = groups[0].trim();
            }
        }
        StringBuilder out = new StringBuilder();
        for (int i=0; i<intervals; i++) {
            if (i > 0) out.append(';');
            out.append(base);
        }
        track.setKeySplines(out.toString());
    }

    private boolean validKeySplines(String raw, int intervals) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return false;
        String[] groups = s.split(";");
        if (groups.length != intervals) return false;
        for (String group : groups) {
            String[] nums = group.trim().split("\\s+");
            if (nums.length != 4) return false;
            try {
                for (String n : nums) {
                    float v = Float.parseFloat(n);
                    if (v < 0f || v > 1f) return false;
                }
            } catch (RuntimeException ex) {
                return false;
            }
        }
        return true;
    }

    private void refreshInspector() {
        SMILTrack track = selectedTrack();
        int idx = timeline.getSelectedKeyIndex();
        boolean hasTrack = track != null;
        boolean motionTrack = hasTrack && track.isMotionTrack();
        boolean setTrack = hasTrack && track.isSetTrack();
        trackLabel.setText(hasTrack ? track.getName() : "No track");
        trackIdField.setEnabled(hasTrack);
        durationField.setEnabled(hasTrack);
        beginField.setEnabled(hasTrack);
        endField.setEnabled(hasTrack);
        repeatField.setEnabled(hasTrack);
        repeatDurField.setEnabled(hasTrack);
        restartCombo.setEnabled(hasTrack);
        fillModeCombo.setEnabled(hasTrack);
        calcModeCombo.setEnabled(hasTrack);
        additiveCombo.setEnabled(hasTrack && !setTrack && !motionTrack);
        accumulateCombo.setEnabled(hasTrack && !setTrack && !motionTrack);
        easingCombo.setEnabled(hasTrack);
        applyInspectorButton.setEnabled(hasTrack);
        motionPathField.setEnabled(motionTrack);
        motionRotateCombo.setEnabled(motionTrack);
        motionAnchorCombo.setEnabled(motionTrack);
        keyTimeField.setEnabled(hasTrack && !motionTrack && !setTrack && idx >= 0);
        keyValueField.setEnabled((hasTrack && !motionTrack && !setTrack && idx >= 0) || setTrack);
        deleteKeyButton.setEnabled(hasTrack && !motionTrack && !setTrack && idx >= 0);
        deleteTrackButton.setEnabled(hasTrack);

        if (!hasTrack) {
            keyTimeField.setEnabled(false);
            trackIdField.setText("");
            durationField.setText("");
            beginField.setText("");
            endField.setText("");
            repeatField.setText("");
            repeatDurField.setText("");
            keySplinesField.setText("");
            keySplinesField.setEnabled(false);
            additiveCombo.setSelectedItem("replace");
            accumulateCombo.setSelectedItem("none");
            additiveCombo.setEnabled(false);
            accumulateCombo.setEnabled(false);
            motionPathField.setText("");
            motionPathField.setEnabled(false);
            motionRotateCombo.setEnabled(false);
            motionAnchorCombo.setSelectedItem("SVG native (import)");
            motionAnchorCombo.setEnabled(false);
            motionAnchorXField.setText("0");
            motionAnchorYField.setText("0");
            motionAnchorXField.setEnabled(false);
            motionAnchorYField.setEnabled(false);
            keyTimeField.setText("");
            keyValueField.setText("");
            return;
        }

        trackIdField.setText(track.getTrackId());
        durationField.setText(SMILTrack.trim(track.getDurationSeconds()));
        beginField.setText(track.getBeginRaw());
        endField.setText(track.getEndRaw());
        repeatField.setText(track.getRepeatCount());
        repeatDurField.setText(track.getRepeatDur());
        restartCombo.setSelectedItem(track.getRestart());
        fillModeCombo.setSelectedItem(track.getFillMode());
        calcModeCombo.setSelectedItem(track.getCalcMode());
        additiveCombo.setSelectedItem(track.getAdditive());
        accumulateCombo.setSelectedItem(track.getAccumulate());
        keySplinesField.setText(track.getKeySplines());
        easingCombo.setSelectedItem("Custom");
        updateTimingFieldState();
        if (motionTrack) {
            String motionId = track.getMotionPathId();
            motionPathField.setText(
                    motionId != null && !motionId.trim().isEmpty()
                            ? "#" + motionId.trim()
                            : track.getInlineMotionPath());
            motionRotateCombo.setSelectedItem(track.getMotionRotate());

            String anchorMode = motionAnchorMode(track);
            if ("center".equals(anchorMode)) {
                motionAnchorCombo.setSelectedItem("Center");
            } else if ("origin".equals(anchorMode)) {
                motionAnchorCombo.setSelectedItem("Local origin");
            } else if ("custom".equals(anchorMode)) {
                motionAnchorCombo.setSelectedItem("Custom");
            } else {
                motionAnchorCombo.setSelectedItem("SVG native (import)");
            }

            Element motionAnim = track.getAnimationElement();
            motionAnchorXField.setText(
                    motionAnim == null ? "0"
                            : motionAnim.getAttribute("data-sketsa-motion-anchor-x"));
            motionAnchorYField.setText(
                    motionAnim == null ? "0"
                            : motionAnim.getAttribute("data-sketsa-motion-anchor-y"));
            if (motionAnchorXField.getText().trim().isEmpty()) {
                motionAnchorXField.setText("0");
            }
            if (motionAnchorYField.getText().trim().isEmpty()) {
                motionAnchorYField.setText("0");
            }
            updateMotionAnchorFieldState();

            keyTimeField.setText("");
            keyValueField.setText("");
            keyTimeField.setEnabled(false);
            keyValueField.setEnabled(false);
            return;
        } else {
            motionPathField.setText("");
        }

        if (setTrack) {
            keyTimeField.setText("");
            keyTimeField.setEnabled(false);
            keyValueField.setText(track.getSetValue());
            keyValueField.setEnabled(true);
            calcModeCombo.setEnabled(false);
            additiveCombo.setEnabled(false);
            accumulateCombo.setEnabled(false);
            easingCombo.setEnabled(false);
            keySplinesField.setEnabled(false);
            return;
        }

        List<Float> times = track.getKeyTimes();
        List<String> values = track.getValues();
        if (idx >= 0 && idx < times.size()) {
            float seconds = times.get(idx) * track.getDurationSeconds();
            keyTimeField.setText(SMILTrack.trim(seconds));
            keyValueField.setText(idx < values.size() ? values.get(idx) : "");
        } else {
            keyTimeField.setText("");
            keyValueField.setText("");
        }
    }

    private void finishTrackEdit(SMILTrack track) {
        Element target = selectedSVGElement();
        Element animationElement = track == null ? null : track.getAnimationElement();
        preserveTimelineSelection = true;
        try {
            /*
             * M5 1.6.2:
             * Editing a transform must use the same complete runtime path as
             * scrub/Play. The old selected-only pass handled rotate/x/y but not
             * scale/skew/translate composition, then refresh() could briefly
             * expose a stale/native GVT matrix and move the edited object.
             */
            setCurrentTime(sliderSeconds());
            restoreTrackSelection(target, animationElement);

            /*
             * Batik may replace a GraphicsNode asynchronously after DOM edits.
             * Re-apply on the next Swing turn so the final visible state is
             * always derived from the authored DOM + current SMIL time.
             */
            final float stableSeconds = sliderSeconds();
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (canvas == null) return;
                setCurrentTime(stableSeconds);
                restoreTrackSelection(target, animationElement);
            });
        } finally {
            preserveTimelineSelection = false;
        }
    }

    private void applyInspector() {
        restorePreviewBase();
        SMILTrack track = selectedTrack();
        if (canvas == null || track == null) return;

        float oldDur = track.getDurationSeconds();
        float dur;
        try {
            dur = Float.parseFloat(durationField.getText().trim());
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Invalid duration.");
            return;
        }
        if (dur <= 0) {
            JOptionPane.showMessageDialog(this, "Duration must be greater than zero.");
            return;
        }

        String trackId = trackIdField.getText().trim();
        if (!trackId.isEmpty() && !trackId.matches("[A-Za-z_][A-Za-z0-9_.-]*")) {
            JOptionPane.showMessageDialog(this, "Invalid Track ID.");
            return;
        }
        if (!trackId.isEmpty() && !trackIdAvailable(track, trackId)) {
            JOptionPane.showMessageDialog(this, "Another animation track already uses id='" + trackId + "'.");
            return;
        }

        String beginRaw = beginField.getText().trim();
        if (beginRaw.isEmpty()) beginRaw = "0s";
        if (!validTimingExpression(beginRaw, false)) {
            JOptionPane.showMessageDialog(this, "Invalid Begin. Use a clock value, event, or syncbase such as move.end+0.5s.");
            return;
        }

        String endRaw = endField.getText().trim();
        if (!endRaw.isEmpty() && !validTimingExpression(endRaw, true)) {
            JOptionPane.showMessageDialog(this, "Invalid End timing expression.");
            return;
        }

        String repeatDur = repeatDurField.getText().trim();
        if (!repeatDur.isEmpty() && !validClockOrIndefinite(repeatDur)) {
            JOptionPane.showMessageDialog(this, "RepeatDur must be a clock value or 'indefinite'.");
            return;
        }
        String restart = String.valueOf(restartCombo.getSelectedItem());

        if (isEventTiming(beginRaw)) {
            stopNativeAnimationInstance(track);
            eventTriggerHistory.clear();
        }

        String repeat = repeatField.getText().trim();
        if (repeat.isEmpty()) repeat = "1";
        if (!validRepeatCount(repeat)) {
            JOptionPane.showMessageDialog(this, "Repeat must be a positive number or 'indefinite'.");
            return;
        }

        String fillMode = String.valueOf(fillModeCombo.getSelectedItem());
        String calcMode = String.valueOf(calcModeCombo.getSelectedItem());
        String additive = String.valueOf(additiveCombo.getSelectedItem());
        String accumulate = String.valueOf(accumulateCombo.getSelectedItem());
        String splines = keySplinesField.getText().trim();

        if (track.isSetTrack()) {
            String setValue = keyValueField.getText().trim();
            if (setValue.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Set value cannot be empty.");
                return;
            }
            List<SMILTrack> editTracks = linkedMultiEditTracks(track);
            beginUndoTransaction(canvas.getUndoManager(), 
                    editTracks.size() > 1
                            ? "Edit SMIL Set Track on multiple objects"
                            : "Edit SMIL Set Track");
            try {
                for (SMILTrack editTrack : editTracks) {
                    editTrack.setDurationSeconds(dur);
                    editTrack.setBeginRaw(beginRaw);
                    if (editTrack == track) editTrack.setTrackId(trackId);
                    editTrack.setEndRaw(endRaw);
                    editTrack.setRepeatDur(repeatDur);
                    editTrack.setRestart(restart);
                    editTrack.setRepeatCount(repeat);
                    editTrack.setFillMode(fillMode);
                    editTrack.setSetValue(setValue);
                }
            } finally {
                canvas.getUndoManager().end();
            }
            finishTrackEdit(track);
            timeline.repaint();
            return;
        }

        if (track.isMotionTrack()) {
            String source = motionPathField.getText().trim();
            if (source.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Motion path must be #id, id, or inline SVG path data.");
                return;
            }
            String id = source.startsWith("#") ? source.substring(1) : source;
            Element root = selectedSVGElement().getOwnerDocument().getDocumentElement();
            Element ref = findElementById(root, id);
            boolean inline = source.matches("(?i)\\s*[mM].*");
            if (ref == null && !inline) {
                JOptionPane.showMessageDialog(this,
                        "Motion path must reference an existing ID or contain inline path data.");
                return;
            }
            String motionRotate = String.valueOf(motionRotateCombo.getSelectedItem()).trim();

            String anchorUi = String.valueOf(
                    motionAnchorCombo.getSelectedItem()).trim();
            String anchorMode = "native";
            if ("Center".equals(anchorUi)) anchorMode = "center";
            else if ("Local origin".equals(anchorUi)) anchorMode = "origin";
            else if ("Custom".equals(anchorUi)) anchorMode = "custom";
            else if ("SVG native (import)".equals(anchorUi)) {
                /*
                 * M5 1.6.10:
                 * Native is a compatibility fallback for imported/unmarked
                 * animateMotion, not an editor-authored anchor mode. Selecting
                 * it on a track created by Animation Editor can double an
                 * existing static placement transform and produce surprising
                 * offsets. Keep it only while the track is genuinely imported.
                 */
                if (!isImportedNativeMotionTrack(track)) {
                    JOptionPane.showMessageDialog(this,
                            "SVG native is reserved for imported Motion tracks. "
                            + "Use Center, Local origin, or Custom for editor-authored Motion.");
                    return;
                }
                anchorMode = "native";
            }

            double anchorX = 0d;
            double anchorY = 0d;
            if ("custom".equals(anchorMode)) {
                try {
                    anchorX = Double.parseDouble(
                            motionAnchorXField.getText().trim());
                    anchorY = Double.parseDouble(
                            motionAnchorYField.getText().trim());
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Custom Motion Anchor requires numeric X and Y.");
                    return;
                }
            }

            List<SMILTrack> editTracks = linkedMultiEditTracks(track);
            beginUndoTransaction(canvas.getUndoManager(), 
                    editTracks.size() > 1
                            ? "Edit SMIL Motion Track on multiple objects"
                            : "Edit SMIL Motion Track");
            try {
                for (SMILTrack editTrack : editTracks) {
                    editTrack.setDurationSeconds(dur);
                    editTrack.setBeginRaw(beginRaw);
                    if (editTrack == track) editTrack.setTrackId(trackId);
                    editTrack.setEndRaw(endRaw);
                    editTrack.setRepeatDur(repeatDur);
                    editTrack.setRestart(restart);
                    editTrack.setRepeatCount(repeat);
                    editTrack.setFillMode(fillMode);
                    editTrack.setCalcMode("linear");
                    editTrack.setKeySplines("");
                    if (ref != null) editTrack.setMotionPathId(id);
                    else editTrack.setInlineMotionPath(source);
                    editTrack.setMotionRotate(motionRotate);
                    if ("native".equals(anchorMode)
                            && isImportedNativeMotionTrack(editTrack)) {
                        Element motionAnim = editTrack.getAnimationElement();
                        motionAnim.removeAttribute("data-sketsa-motion-anchor");
                        motionAnim.removeAttribute("data-sketsa-motion-anchor-x");
                        motionAnim.removeAttribute("data-sketsa-motion-anchor-y");
                        Element motionTarget =
                                motionAnim.getParentNode() instanceof Element
                                        ? (Element)motionAnim.getParentNode() : null;
                        if (motionTarget != null) {
                            documentMotionAnchors.remove(motionTarget);
                        }
                    } else {
                        setMotionAnchorMode(
                                editTrack, anchorMode, anchorX, anchorY);
                    }
                }
            } finally {
                canvas.getUndoManager().end();
            }
            finishTrackEdit(track);
            timeline.repaint();
            return;
        }

        int idx = timeline.getSelectedKeyIndex();
        List<Float> times = new ArrayList<Float>(track.getKeyTimes());
        List<String> values = new ArrayList<String>(track.getValues());
        while (values.size() < times.size()) values.add("");

        int newSelectedIndex = idx;

        if (idx >= 0 && idx < times.size()) {
            float keySeconds;
            try {
                keySeconds = Float.parseFloat(keyTimeField.getText().trim());
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "Invalid key time.");
                return;
            }

            keySeconds = Math.max(0f, Math.min(dur, keySeconds));
            float newNormalized = dur <= 0f ? 0f : keySeconds / dur;

            // Prevent two distinct keys from occupying the same instant.
            for (int i=0; i<times.size(); i++) {
                if (i == idx) continue;
                if (Math.abs(times.get(i) - newNormalized) < 0.00005f) {
                    JOptionPane.showMessageDialog(this,
                            "Another keyframe already exists at "
                            + SMILTrack.trim(keySeconds) + "s.");
                    return;
                }
            }

            String newValue = keyValueField.getText().trim();

            times.remove(idx);
            values.remove(idx);

            int pos = 0;
            while (pos < times.size() && times.get(pos) < newNormalized) pos++;
            times.add(pos, newNormalized);
            values.add(pos, newValue);
            newSelectedIndex = pos;
        }

        int intervalCount = Math.max(1, times.size() - 1);
        if ("spline".equals(calcMode) && !validKeySplines(splines, intervalCount)) {
            JOptionPane.showMessageDialog(this,
                    "Spline mode requires exactly one cubic-bezier (4 values 0..1) per key interval.");
            return;
        }

        List<SMILTrack> editTracks = linkedMultiEditTracks(track);
        beginUndoTransaction(canvas.getUndoManager(), 
                editTracks.size() > 1
                        ? "Edit SMIL Track on multiple objects"
                        : "Edit SMIL Track");
        try {
            for (SMILTrack editTrack : editTracks) {
                editTrack.setDurationSeconds(dur);
                editTrack.setBeginRaw(beginRaw);
                if (editTrack == track) editTrack.setTrackId(trackId);
                editTrack.setEndRaw(endRaw);
                editTrack.setRepeatDur(repeatDur);
                editTrack.setRestart(restart);
                editTrack.setRepeatCount(repeat);
                editTrack.setFillMode(fillMode);
                editTrack.setCalcMode(calcMode);
                editTrack.setAdditive(additive);
                editTrack.setAccumulate(accumulate);
                if ("spline".equals(calcMode)) {
                    editTrack.setKeySplines(splines);
                } else {
                    editTrack.setKeySplines("");
                }
                if (idx >= 0) {
                    editTrack.setKeys(
                            new ArrayList<Float>(times),
                            new ArrayList<String>(values));
                    normalizeSplineCount(editTrack);
                }
                syncPivotHelpers(editTrack);
            }
        } finally {
            canvas.getUndoManager().end();
        }

        // Keep the selected key visible after duration/time edits.
        timeline.setSelectedKeyIndex(newSelectedIndex);
        if (newSelectedIndex >= 0 && newSelectedIndex < times.size()) {
            float newSeconds = times.get(newSelectedIndex) * dur;
            setSliderSeconds(newSeconds);
        } else if (oldDur != dur && sliderSeconds() > dur) {
            setSliderSeconds(dur);
        }

        finishTrackEdit(track);
        timeline.repaint();
        refreshInspector();
    }

    private void changeZoom(boolean in) {
        float current = sliderSeconds();
        int max = timeline.getMaximum();
        max = in ? Math.max(5, max / 2) : Math.min(600, max * 2);
        timeline.setMaximum(max);
        timeSlider.setMaximum(max * TIME_SCALE);
        setSliderSeconds(Math.min(current, max));
    }

    private void jumpToStart() {
        if (!paused) pausePlayback();
        setSliderSeconds(0f);
        setCurrentTime(0f);
        refreshInspector();
        timeline.repaint();
    }

    private float lastAuthoredInstantSeconds() {
        float last = 0f;
        java.util.List<SMILTrack> tracks = timeline.getTimelineModel().getTracks();
        for (SMILTrack t : tracks) {
            if (t == null) continue;
            java.util.List<Float> keyTimes = t.getKeyTimes();
            if (keyTimes == null || keyTimes.isEmpty()) continue;

            /*
             * Jump to the last actual key marker authored on the visible
             * timeline, not to the timeline zoom/range and not merely to a
             * track duration. getKeyTimes() also supplies the implicit evenly
             * spaced keys used by SMIL values/from-to tracks.
             */
            Float kt = keyTimes.get(keyTimes.size() - 1);
            if (kt != null) {
                last = Math.max(last, kt.floatValue() * t.getDurationSeconds());
            }
        }
        return last;
    }

    private void jumpToEnd() {
        if (!paused) pausePlayback();
        float end = lastAuthoredInstantSeconds();
        if (end <= 0f) end = Math.max(0f, sliderSeconds());
        setSliderSeconds(end);
        setCurrentTime(end);
        refreshInspector();
        timeline.repaint();
    }

    private boolean hasMotionTracks() {
        for (SMILTrack t : timeline.getTimelineModel().getTracks()) {
            if (t.isMotionTrack()) return true;
        }
        return false;
    }

    private boolean hasEventTimedTracks() {
        if (canvas == null) return false;
        try {
            org.w3c.dom.svg.SVGDocument doc = canvas.getSVGDocument();
            return doc != null && doc.getDocumentElement() != null
                    && hasEventTimingInSubtree(doc.getDocumentElement());
        } catch (RuntimeException ex) {
            for (SMILTrack t : timeline.getTimelineModel().getTracks()) {
                if (isEventTiming(t.getBeginRaw())
                        || isEventTiming(t.getEndRaw())) return true;
            }
            return false;
        }
    }

    private boolean hasRecordedEventTriggers() {
        for (List<Float> history : eventTriggerHistory.values()) {
            if (history != null && !history.isEmpty()) return true;
        }
        return false;
    }

    private boolean hasEventTimingInSubtree(Element root) {
        if (root == null) return false;
        String local = localName(root);
        if (isAnimationElementName(local)) {
            String begin = root.getAttribute("begin");
            String end = root.getAttribute("end");
            if (isEventTiming(begin) || isEventTiming(end)) return true;
        }
        org.w3c.dom.NodeList children = root.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (n instanceof Element
                    && hasEventTimingInSubtree((Element)n)) return true;
        }
        return false;
    }

    private boolean shouldRefreshEventCanvasNow() {
        if (paused) return true;
        long now = System.nanoTime();
        if (lastEventCanvasRefreshNanos == 0L
                || now - lastEventCanvasRefreshNanos >= 50_000_000L) {
            lastEventCanvasRefreshNanos = now;
            return true;
        }
        return false;
    }

    private void stopNativeEventAnimationsDocumentWide() {
        if (canvas == null) return;
        try {
            org.w3c.dom.svg.SVGDocument doc = canvas.getSVGDocument();
            if (doc == null || doc.getDocumentElement() == null) return;
            stopNativeEventAnimationsInSubtree(doc.getDocumentElement());
        } catch (RuntimeException ex) {
            // Local runtime remains authoritative.
        }
    }

    private void stopNativeEventAnimationsInSubtree(Element root) {
        if (root == null) return;

        String local = localName(root);
        if (isAnimationElementName(local)) {
            String begin = root.getAttribute("begin");
            if (isEventTiming(begin)) {
                SMILTrack t = trackFromAnimationElement(root);
                if (t != null) stopNativeAnimationInstance(t);
            }
        }

        org.w3c.dom.NodeList children = root.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (n instanceof Element) {
                stopNativeEventAnimationsInSubtree((Element)n);
            }
        }
    }

    private void setCurrentTime(float seconds) {
        if (canvas == null) return;

        timingEvaluationSeconds = Math.max(0f, seconds);

        /*
         * Local GVT preview is authoritative.
         *
         * Event-timed tracks need stronger isolation: a VectorCanvas.refresh()
         * after our local pass can let Batik re-apply a stale/native SMIL
         * instance and visually start the animation even though click/mouseover
         * has not occurred. Therefore event-timed objects are repainted, not
         * refreshed, after the local evaluation.
         */
        applyEditorPreview(seconds);
        applyDocumentSMILPreview(seconds);
        /*
         * Motion Path must use VectorCanvas.refresh(), exactly as in the
         * certified M4 implementation. Direct GraphicsNode.setTransform()
         * updates the GVT node, but a plain Swing repaint is not sufficient
         * for Sketsa's renderer to invalidate/repaint the changed GVT geometry
         * reliably. That is why Preview could show changing motion=x,y values
         * while the object stayed still or disappeared.
         *
         * Event-timed tracks remain isolated with repaint(), because refresh()
         * can wake Batik's native event SMIL state.
         */
        /*
         * A document may CONTAIN event-timed tracks while the user is testing
         * ordinary clock/syncbase animation. Merely having "begin=click"
         * somewhere in the SVG must not disable VectorCanvas.refresh() for the
         * whole document: ShapeNode geometry changes need that invalidation in
         * Sketsa, otherwise ordinary animations can appear completely static.
         *
         * Event isolation is only required after a local event instance has
         * actually been recorded. Before the first event, refresh is safe and
         * necessary. After an event fires, repaint-only preserves the certified
         * local event isolation from 1.0.3.
         */
        if (hasEventTimedTracks() && hasRecordedEventTriggers()) {
            /*
             * Direct ShapeNode geometry mutation (x/cx/etc.) is not always
             * invalidated by Swing repaint alone in Sketsa/Batik. That is why
             * event animation could change only when another mouse click
             * happened.
             *
             * Use a controlled VectorCanvas.refresh() cadence for event-mode
             * rendering. Immediately after refresh, stop any native Batik
             * event animation instances again and re-apply the local
             * document-wide runtime. This preserves event isolation while also
             * invalidating GVT geometry so Play updates continuously.
             */
            if (shouldRefreshEventCanvasNow()) {
                canvas.refresh();
                stopNativeEventAnimationsDocumentWide();
                applyDocumentSMILPreview(seconds);
            } else {
                applyDocumentSMILPreview(seconds);
            }
            canvas.repaint();
        } else {
            /*
             * refresh() can re-apply authored DOM state after the first local
             * GVT pass. That is essential for Motion invalidation, but for
             * visibility="hidden" targets it can immediately hide again a node
             * that the document runtime just made visible.
             *
             * Run a second document-wide pass after refresh so visibility,
             * transforms and path morphs are the final rendered state for the
             * requested editor time.
             */
            canvas.refresh();
            applyDocumentSMILPreview(seconds);
            canvas.repaint();

            /*
             * 1.6.24 - finite fill=freeze final-frame pinning.
             *
             * VectorCanvas.refresh() may rebuild the Batik GVT asynchronously.
             * For a finite repeated animation exactly at/after its active end,
             * Batik can therefore publish its native repeat-boundary geometry
             * after the editor's authoritative local pass. The visible symptom
             * is a fill=freeze object snapping away from its authored final
             * value (e.g. x=800) even though normalizedTrackTime() correctly
             * evaluates the frozen sample as 1.0.
             *
             * Only when at least one finite frozen animation has actually
             * completed, schedule one EDT pass after the refresh has had a
             * chance to publish its rebuilt nodes. Reacquiring the nodes inside
             * applyDocumentSMILPreview() then pins the final frozen state on the
             * current GVT rather than on the pre-refresh node.
             */
            if (hasCompletedFiniteFrozenTrack(seconds)) {
                final float frozenSeconds = seconds;
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        if (canvas == null) return;
                        applyDocumentSMILPreview(frozenSeconds);
                        canvas.repaint();
                    }
                });
            }
        }
    }

    private boolean hasCompletedFiniteFrozenTrack(float seconds) {
        if (canvas == null) return false;
        try {
            org.w3c.dom.svg.SVGDocument doc = canvas.getSVGDocument();
            if (doc == null || doc.getDocumentElement() == null) return false;
            return hasCompletedFiniteFrozenTrackRecursive(
                    doc.getDocumentElement(), seconds);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean hasCompletedFiniteFrozenTrackRecursive(
            Element element, float seconds) {
        if (element == null) return false;

        String local = localName(element);
        if (isAnimationElementName(local)
                && "freeze".equals(new SMILTrack(
                        element,
                        "animateTransform".equals(local)
                                ? element.getAttribute("type")
                                : element.getAttribute("attributeName"),
                        local).getFillMode())) {
            SMILTrack track = trackFromAnimationElement(element);
            if (track != null) {
                float begin = resolveBeginSeconds(track);
                if (!Float.isNaN(begin)) {
                    float dur = track.getDurationSeconds();
                    float active = computeActiveDuration(track, dur, begin);
                    if (!Float.isInfinite(active)
                            && seconds + 0.00001f >= begin + active) {
                        return true;
                    }
                }
            }
        }

        org.w3c.dom.NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (n instanceof Element
                    && hasCompletedFiniteFrozenTrackRecursive(
                            (Element)n, seconds)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnimateTransformChild(Element target) {
        if (target == null) return false;
        org.w3c.dom.NodeList children = target.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (!(n instanceof Element)) continue;
            if ("animateTransform".equals(localName((Element)n))) return true;
        }
        return false;
    }

    private void capturePreviewBase(Element target) {
        if (target == null || canvas == null) return;
        if (previewTarget == target && previewGraphicsNode != null) return;

        restorePreviewBase();
        previewTarget = target;

        try {
            previewGraphicsNode = canvas.getModel().getGraphicsNode((SVGElement)target);
        } catch (RuntimeException ex) {
            previewGraphicsNode = null;
            setPreviewStatus("node=ERROR " + ex.getClass().getSimpleName());
        }

        if (previewGraphicsNode == null) {
            setPreviewStatus("node=null");
            return;
        }

        AffineTransform tx = previewGraphicsNode.getTransform();

        /*
         * Critical Motion import rule:
         *
         * For an SVG element with no authored transform="" attribute, any
         * transform exposed by the live GVT node while animateMotion is present
         * is renderer/runtime state, not the object's static base transform.
         * Keeping that GVT transform as previewBaseTransform makes the local
         * Motion evaluator compound on top of Batik's native motion and can
         * move an otherwise-correct point outside the viewport.
         *
         * Therefore a Motion target with no DOM transform starts from identity.
         * Non-Motion tracks retain the original GVT base behavior.
         */
        boolean motionTarget = hasMotionTracks();
        boolean transformAnimatedTarget = hasAnimateTransformChild(target);
        String authoredTransform = target.getAttribute("transform");

        /*
         * M5 1.6.2:
         * During authoring, the live Batik GraphicsNode can already contain the
         * animation value from the previous preview frame. Capturing that live
         * matrix as the new "base" makes subsequent edits compound transforms,
         * and in some refresh states the object can jump toward (0,0).
         *
         * For targets with animateTransform, the static base is authoritative
         * in the SVG DOM. Rebuild that base from transform="" instead of from
         * transient GVT state. Motion-without-static-transform keeps its
         * certified identity-base rule.
         */
        if (transformAnimatedTarget) {
            previewBaseTransform = toAffineTransform(
                    parseTransformOps(authoredTransform));
        } else if (motionTarget
                && (authoredTransform == null || authoredTransform.trim().isEmpty())) {
            previewBaseTransform = new AffineTransform();
        } else {
            previewBaseTransform = tx == null
                    ? new AffineTransform() : new AffineTransform(tx);
        }

        previewBaseComposite = previewGraphicsNode.getComposite();
        previewBaseX = previewCoordinateBase(target, true);
        previewBaseY = previewCoordinateBase(target, false);

        /*
         * Capture the motion anchor exactly once from the unmodified GVT node.
         * getBounds() must NOT be queried again after Motion has already moved
         * the live node: depending on Batik's node implementation, the returned
         * bounds can reflect the current transformed position. Subtracting that
         * moving center on every frame compounds the delta and can throw the
         * object outside the viewport even though motion=x,y is correct.
         */
        Rectangle2D baseBounds = previewGraphicsNode.getBounds();
        if (baseBounds != null) {
            previewBaseCenterX = baseBounds.getCenterX();
            previewBaseCenterY = baseBounds.getCenterY();
            previewBaseCenterValid = true;
        } else {
            previewBaseCenterX = 0d;
            previewBaseCenterY = 0d;
            previewBaseCenterValid = false;
        }

        /*
         * Keep the GVT base exactly as Sketsa exposes it.
         * The previous imported-motion neutralization inverted a synthetic
         * t=0 motion transform and could throw the object far outside the
         * viewport. The reported motion coordinates themselves are already
         * correct, so no base inversion is required here.
         */
        previewFillPaints.clear();
        if (previewGraphicsNode instanceof ShapeNode) {
            collectFillPainters(((ShapeNode)previewGraphicsNode).getShapePainter());
        }

        setPreviewStatus("node=" + previewGraphicsNode.getClass().getSimpleName()
                + " | baseX=" + trimDouble(previewBaseX)
                + " | baseY=" + trimDouble(previewBaseY)
                + " | fillPainters=" + previewFillPaints.size());
    }

    private List<FillShapePainter> currentFillPainters(GraphicsNode node) {
        List<FillShapePainter> result = new ArrayList<FillShapePainter>();
        if (node instanceof ShapeNode) {
            collectCurrentFillPainters(((ShapeNode)node).getShapePainter(), result);
        }
        return result;
    }

    private void collectCurrentFillPainters(ShapePainter painter, List<FillShapePainter> result) {
        if (painter == null) return;
        if (painter instanceof FillShapePainter) {
            result.add((FillShapePainter)painter);
            return;
        }
        if (painter instanceof CompositeShapePainter) {
            CompositeShapePainter composite = (CompositeShapePainter)painter;
            for (int i=0; i<composite.getShapePainterCount(); i++) {
                collectCurrentFillPainters(composite.getShapePainter(i), result);
            }
        }
    }

    private void collectFillPainters(ShapePainter painter) {
        if (painter == null) return;
        if (painter instanceof FillShapePainter) {
            FillShapePainter fill = (FillShapePainter)painter;
            previewFillPaints.add(new FillPaintState(fill, fill.getPaint()));
            return;
        }
        if (painter instanceof CompositeShapePainter) {
            CompositeShapePainter composite = (CompositeShapePainter)painter;
            for (int i=0; i<composite.getShapePainterCount(); i++) {
                collectFillPainters(composite.getShapePainter(i));
            }
        }
    }

    private void restorePreviewBase() {
        if (previewGraphicsNode != null) {
            try {
                previewGraphicsNode.setTransform(
                        previewBaseTransform == null ? null : new AffineTransform(previewBaseTransform));
                previewGraphicsNode.setComposite(previewBaseComposite);
                for (FillPaintState state : previewFillPaints) {
                    state.painter.setPaint(state.paint);
                }
            } catch (RuntimeException ex) {
                // Old renderer node may already be invalid after document rebuild.
            }
        }
        previewTarget = null;
        previewGraphicsNode = null;
        previewBaseTransform = null;
        previewBaseComposite = null;
        previewBaseCenterX = 0d;
        previewBaseCenterY = 0d;
        previewBaseCenterValid = false;
        previewFillPaints.clear();
    }

    private SMILTrack findRotateTrack() {
        for (SMILTrack t : timeline.getTimelineModel().getTracks()) {
            if ("rotate".equals(t.getName())) return t;
        }
        return null;
    }

    private boolean rotateUsesLocalOriginPivot(SMILTrack track, Element target) {
        if (track == null || target == null) return false;
        if (!"animateTransform".equals(track.getKind())) return false;

        /*
         * Native animateTransform type="rotate" values that specify only an
         * angle rotate around the current user-space origin. In the attached
         * clock SVG that origin is moved to (150,150) by the element's static
         * transform="translate(150,150) rotate(0)".
         *
         * Therefore the local preview must append rotate(angle) to the already
         * captured base transform, i.e. rotate around local (0,0). Rotating
         * around the rendered line's bounds center changes the pivot and makes
         * the hand orbit incorrectly around its own midpoint.
         */
        String authored = target.getAttribute("transform");
        if (authored == null || authored.trim().isEmpty()) return false;

        String by = track.getByRaw();
        String from = track.getFromRaw();
        String to = track.getToRaw();

        // by="360" (or plain numeric from/to) contains angle only, no cx/cy.
        if (by != null && !by.trim().isEmpty()) {
            return by.trim().matches("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?");
        }
        if (from != null && !from.trim().isEmpty()
                && to != null && !to.trim().isEmpty()) {
            return from.trim().matches("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?")
                    && to.trim().matches("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?");
        }
        return false;
    }

    private static final class GenericSample {
        final String value;
        final boolean active;
        GenericSample(String value, boolean active) {
            this.value = value;
            this.active = active;
        }
    }

    private static final class TransformOp {
        final String type;
        final double[] p;

        TransformOp(String type, double[] p) {
            this.type = type;
            this.p = p;
        }

        TransformOp copy() {
            return new TransformOp(type, p.clone());
        }
    }

    private void applyDocumentSMILPreview(float seconds) {
        if (canvas == null) return;

        runtimeVisibilityTracks = 0;
        runtimeVisibilityResolved = 0;
        runtimeMotionTracks = 0;
        runtimeMotionResolved = 0;
        runtimeOpacityTracks = 0;
        runtimeGenericAnimateTracks = 0;
        runtimeGenericAnimateHandled = 0;

        try {
            org.w3c.dom.svg.SVGDocument doc = canvas.getSVGDocument();
            if (doc == null || doc.getDocumentElement() == null) return;
            applyDocumentSMILPreviewRecursive(doc.getDocumentElement(), seconds);
        } catch (RuntimeException ex) {
            // The selected-target legacy preview remains available as fallback.
        }
    }

    private void applyDocumentSMILPreviewRecursive(Element element, float seconds) {
        if (element == null) return;

        if (element instanceof SVGElement) {
            applyElementSMILRuntime(element, seconds);
        }

        org.w3c.dom.NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (!(n instanceof Element)) continue;

            Element child = (Element)n;
            String local = localName(child);

            // SMIL metadata nodes are evaluated through their visual parent.
            if ("animate".equals(local)
                    || "animateTransform".equals(local)
                    || "animateMotion".equals(local)
                    || "set".equals(local)
                    || "mpath".equals(local)) {
                continue;
            }
            applyDocumentSMILPreviewRecursive(child, seconds);
        }
    }

    private String localName(Element e) {
        String local = e.getLocalName();
        if (local == null || local.isEmpty()) local = e.getTagName();
        int colon = local.indexOf(':');
        if (colon >= 0) local = local.substring(colon + 1);
        return local;
    }

    private GraphicsNode resolveRuntimeGraphicsNode(
            Element target, boolean hasVisibilityAnimation) {
        GraphicsNode node = null;
        try {
            node = canvas.getModel().getGraphicsNode((SVGElement)target);
        } catch (RuntimeException ex) {
            node = null;
        }

        if (node != null || !hasVisibilityAnimation) return node;

        /*
         * Batik/Sketsa may not create a usable GVT node for an element whose
         * authored visibility is "hidden" when the document is first built.
         * Live chatbot.svg uses exactly this pattern: hidden groups become
         * visible later via animate attributeName="visibility".
         *
         * Bootstrap the rendering node once by temporarily exposing the
         * element, refreshing the bridge, then restoring the authored DOM
         * value. Runtime visibility itself remains a GVT-only property, so
         * the SVG source is not left modified by preview.
         */
        if (Boolean.TRUE.equals(documentVisibilityBootstrapped.get(target))) {
            return null;
        }

        String authored = target.getAttribute("visibility");
        boolean hadAttribute = target.hasAttribute("visibility");

        if (!"hidden".equals(authored) && !"collapse".equals(authored)) {
            documentVisibilityBootstrapped.put(target, Boolean.TRUE);
            return null;
        }

        try {
            target.setAttribute("visibility", "visible");
            canvas.refresh();

            node = canvas.getModel().getGraphicsNode((SVGElement)target);
        } catch (RuntimeException ex) {
            node = null;
        } finally {
            try {
                if (hadAttribute) target.setAttribute("visibility", authored);
                else target.removeAttribute("visibility");
            } catch (RuntimeException ex) { }
        }

        documentVisibilityBootstrapped.put(target, Boolean.TRUE);
        return node;
    }

    private void applyElementSMILRuntime(Element target, float seconds) {
        org.w3c.dom.NodeList children = target.getChildNodes();
        List<SMILTrack> transformTracks = new ArrayList<SMILTrack>();
        List<SMILTrack> motionTracks = new ArrayList<SMILTrack>();
        List<SMILTrack> visibilityTracks = new ArrayList<SMILTrack>();
        List<SMILTrack> setVisibilityTracks = new ArrayList<SMILTrack>();
        List<SMILTrack> opacityTracks = new ArrayList<SMILTrack>();
        List<SMILTrack> pathTracks = new ArrayList<SMILTrack>();
        List<SMILTrack> genericAnimateTracks = new ArrayList<SMILTrack>();

        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (!(n instanceof Element)) continue;
            Element e = (Element)n;
            String local = localName(e);

            if ("animateTransform".equals(local)) {
                String type = e.getAttribute("type");
                String name = type == null || type.trim().isEmpty()
                        ? "transform" : type.trim();
                transformTracks.add(new SMILTrack(e, name, "animateTransform"));
            } else if ("animateMotion".equals(local)) {
                motionTracks.add(new SMILTrack(e, "Motion Path", "animateMotion"));
            } else if ("animate".equals(local)
                    && "visibility".equals(e.getAttribute("attributeName"))) {
                visibilityTracks.add(new SMILTrack(e, "visibility", "animate"));
            } else if ("set".equals(local)
                    && "visibility".equals(e.getAttribute("attributeName"))) {
                setVisibilityTracks.add(new SMILTrack(e, "Set visibility", "set"));
            } else if ("animate".equals(local)
                    && "opacity".equals(e.getAttribute("attributeName"))) {
                opacityTracks.add(new SMILTrack(e, "opacity", "animate"));
            } else if ("animate".equals(local)
                    && "d".equals(e.getAttribute("attributeName"))) {
                pathTracks.add(new SMILTrack(e, "d", "animate"));
            } else if ("animate".equals(local)) {
                String attr = e.getAttribute("attributeName");
                if (attr != null && !attr.trim().isEmpty()) {
                    genericAnimateTracks.add(
                            new SMILTrack(e, attr.trim(), "animate"));
                }
            }
        }

        if (transformTracks.isEmpty()
                && motionTracks.isEmpty()
                && visibilityTracks.isEmpty()
                && setVisibilityTracks.isEmpty()
                && opacityTracks.isEmpty()
                && pathTracks.isEmpty()
                && genericAnimateTracks.isEmpty()) return;

        runtimeVisibilityTracks += visibilityTracks.size() + setVisibilityTracks.size();
        runtimeMotionTracks += motionTracks.size();
        runtimeOpacityTracks += opacityTracks.size();
        runtimeGenericAnimateTracks += genericAnimateTracks.size();

        GraphicsNode node = resolveRuntimeGraphicsNode(
                target, !visibilityTracks.isEmpty() || !setVisibilityTracks.isEmpty());
        if (node == null) return;
        if (!visibilityTracks.isEmpty() || !setVisibilityTracks.isEmpty()) runtimeVisibilityResolved += visibilityTracks.size() + setVisibilityTracks.size();

        if (!transformTracks.isEmpty() || !motionTracks.isEmpty()) {
            List<TransformOp> baseOps = documentBaseTransformOps.get(target);
            if (baseOps == null) {
                baseOps = parseTransformOps(target.getAttribute("transform"));
                documentBaseTransformOps.put(target, copyTransformOps(baseOps));
            }

            List<TransformOp> effective = copyTransformOps(baseOps);

            for (SMILTrack track : transformTracks) {
                String type = track.getName();
                int baseIndex = findTransformOp(effective, type);
                TransformOp reference = baseIndex >= 0 ? effective.get(baseIndex) : null;

                double[] value = evaluateTransformTrack(
                        track, seconds, target, reference);
                if (value == null) continue;

                TransformOp animated = new TransformOp(type, value);

                if ("sum".equals(track.getAdditive())) {
                    /*
                     * Additive animateTransform contributes another transform
                     * in document order instead of replacing the underlying
                     * transform operation.
                     */
                    effective.add(animated);
                } else {
                    if (baseIndex >= 0) effective.set(baseIndex, animated);
                    else effective.add(animated);
                }
            }

            AffineTransform baseTransform = toAffineTransform(effective);
            AffineTransform combined = new AffineTransform(baseTransform);

            /*
             * SVG animateMotion placement must not be distorted by the
             * target's own authored transform. For example:
             *
             *   transform="scale(.9)"
             *   animateMotion path="M80 ... "
             *
             * must place the element origin at x=80 and then scale the marker,
             * not scale the path position to x=72.
             *
             * Therefore Motion is the outer placement transform:
             *
             *   Motion * authored/animateTransform
             *
             * rather than:
             *
             *   authored/animateTransform * Motion
             */
            for (SMILTrack track : motionTracks) {
                MotionSample sample = evaluateMotionTrack(track, seconds, target);
                if (sample == null) continue;

                runtimeMotionResolved++;

                AffineTransform motion = new AffineTransform();

                /*
                 * There are two valid Motion authoring conventions in files
                 * handled by Animation Editor:
                 *
                 * 1) Native/imported SVG animateMotion:
                 *    the path places the element's local origin.
                 *
                 * 2) Animation Editor-created/bound Motion tracks:
                 *    historically the user binds an already-positioned object
                 *    to a visible path and expects the object's visual center
                 *    to sit on that route (certified M4 behavior).
                 *
                 * setMotionPathId() leaves the custom
                 * data-sketsa-motion-path-id attribute present even when its
                 * value is cleared, so attribute presence is a reliable marker
                 * for editor-owned Motion semantics.
                 */
                Point2D.Double anchor = editorMotionAnchor(
                        track, target, node);

                motion.translate(sample.x, sample.y);
                if (sample.rotate) {
                    motion.rotate(Math.toRadians(sample.angleDegrees));
                }
                if (anchor != null) {
                    motion.translate(-anchor.x, -anchor.y);
                }

                motion.concatenate(combined);
                combined = motion;
            }

            try {
                node.setTransform(combined);
            } catch (RuntimeException ex) {
                // Renderer may replace the live node between frames.
            }
        }

        if (!pathTracks.isEmpty() && node instanceof ShapeNode) {
            ShapeNode shapeNode = (ShapeNode)node;

            if (!documentBaseShapes.containsKey(target)) {
                try {
                    Shape baseShape = shapeNode.getShape();
                    if (baseShape != null) {
                        documentBaseShapes.put(target, cloneShape(baseShape));
                    }
                } catch (RuntimeException ex) {
                    // Geometry runtime can still attempt direct d parsing.
                }
            }

            Shape animatedShape = null;
            for (SMILTrack track : pathTracks) {
                String d = evaluatePathTrack(track, seconds);
                if (d == null) continue;
                try {
                    animatedShape = parsePathData(d);
                } catch (RuntimeException ex) {
                    animatedShape = null;
                }
            }

            try {
                if (animatedShape != null) {
                    shapeNode.setShape(animatedShape);
                } else {
                    Shape baseShape = documentBaseShapes.get(target);
                    if (baseShape != null) shapeNode.setShape(cloneShape(baseShape));
                }
            } catch (RuntimeException ex) {
                // Keep the rest of the document-wide runtime alive.
            }
        }

        if (!genericAnimateTracks.isEmpty()) {
            Map<String, String> values = new LinkedHashMap<String, String>();

            for (SMILTrack track : genericAnimateTracks) {
                String attr = track.getName();

                /*
                 * 1.6.27 - restore the underlying value whenever a generic
                 * animation is inactive.
                 *
                 * Before this fix, evaluateGenericAnimateTrack() correctly
                 * returned null before begin and after fill=remove, but the
                 * document-wide GVT runtime then simply skipped the attribute.
                 * The last geometry written into the live ShapeNode therefore
                 * remained visible. A rect animated x=120->760 with begin=1s
                 * and fill=remove could stay at x=760 after rewind to t=0 and
                 * only start behaving again once t reached 1s.
                 *
                 * Seed each animated attribute with its authored/underlying
                 * value on every frame. Active tracks then replace or add to
                 * that base in document order. This gives SMIL remove semantics
                 * and also restores the pre-begin interval without mutating DOM.
                 */
                if (!values.containsKey(attr)) {
                    values.put(attr,
                            underlyingValueForAttribute(target, attr));
                }

                String value = evaluateGenericAnimateTrack(
                        track, seconds, target, attr);
                if (value == null) continue;

                if ("sum".equals(track.getAdditive())
                        && isAdditiveNumericAttribute(attr)) {
                    String base = values.get(attr);
                    value = sumNumericStrings(base, value);
                }
                values.put(attr, value);
            }

            if (!values.isEmpty()) {
                runtimeGenericAnimateHandled +=
                        applyGenericAnimateValues(target, node, values);
            }
        }

        if (!opacityTracks.isEmpty()) {
            if (!documentBaseComposites.containsKey(target)) {
                try {
                    documentBaseComposites.put(target, node.getComposite());
                } catch (RuntimeException ex) {
                    documentBaseComposites.put(target, null);
                }
            }

            Float opacity = null;
            for (SMILTrack track : opacityTracks) {
                String value = evaluateGenericAnimateTrack(
                        track, seconds, target, "opacity");
                if (value == null) continue;
                try {
                    float v = Float.parseFloat(value.trim());
                    if ("sum".equals(track.getAdditive())) {
                        float base = opacity == null
                                ? parseFloatOr(
                                    underlyingValueForAttribute(target, "opacity"), 1f)
                                : opacity.floatValue();
                        opacity = Float.valueOf(base + v);
                    } else {
                        opacity = Float.valueOf(v);
                    }
                } catch (RuntimeException ex) { }
            }

            try {
                if (opacity != null) {
                    float a = Math.max(0f, Math.min(1f, opacity.floatValue()));
                    node.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, a));
                } else {
                    node.setComposite(documentBaseComposites.get(target));
                }
            } catch (RuntimeException ex) {
                // Keep the remaining document runtime active.
            }
        }

        if (!visibilityTracks.isEmpty() || !setVisibilityTracks.isEmpty()) {
            Boolean baseVisible = documentBaseVisibility.get(target);
            if (baseVisible == null) {
                String authored = target.getAttribute("visibility");
                baseVisible = Boolean.valueOf(
                        !"hidden".equals(authored) && !"collapse".equals(authored));
                documentBaseVisibility.put(target, baseVisible);
            }

            Boolean visible = null;
            for (SMILTrack track : visibilityTracks) {
                String value = evaluateGenericAnimateTrack(
                        track, seconds, target, "visibility");
                if (value == null) continue;
                visible = Boolean.valueOf(
                        !"hidden".equals(value) && !"collapse".equals(value));
            }

            /*
             * 1.6.26 - <set attributeName="visibility"> is a discrete
             * visibility animation too. It must participate in the same GVT
             * visibility adapter as <animate attributeName="visibility">.
             * Apply set tracks after continuous visibility tracks so their
             * active interval overrides the underlying/current value exactly
             * as a discrete SMIL state. fill="remove" naturally restores the
             * base/continuous visibility when evaluateSetTrack() returns null.
             */
            for (SMILTrack track : setVisibilityTracks) {
                String value = evaluateSetTrack(track, seconds);
                if (value == null) continue;
                visible = Boolean.valueOf(
                        !"hidden".equals(value) && !"collapse".equals(value));
            }

            try {
                boolean requested = visible == null
                        ? baseVisible.booleanValue()
                        : visible.booleanValue();

                /*
                 * SVG visibility is inherited. Batik stores the computed
                 * visibility on descendant GraphicsNodes too. Therefore making
                 * only the parent CompositeGraphicsNode visible is not enough
                 * after a group was authored visibility="hidden": its children
                 * may still remain individually invisible.
                 *
                 * Propagate the parent's runtime visibility through the GVT
                 * subtree. Child SVG elements with their own explicit hidden/
                 * collapse state are kept hidden here and, if they have their
                 * own SMIL visibility track, will be evaluated later by the
                 * document recursion.
                 */
                applyInheritedVisibility(node, requested, true);
            } catch (RuntimeException ex) {
                // Keep transform preview alive if visibility application fails.
            }
        }
    }

    private void applyInheritedVisibility(
            GraphicsNode node, boolean parentVisible, boolean animatedRoot) {
        if (node == null) return;

        boolean ownVisible = parentVisible;

        /*
         * IMPORTANT:
         * The animated root itself MUST be allowed to override its authored
         * visibility="hidden". That authored value is precisely the base value
         * being animated. Only descendants should preserve their own explicit
         * hidden/collapse state until their individual SMIL track is evaluated.
         */
        if (!animatedRoot) {
            try {
                SVGElement element = canvas.getModel().getSVGElement(node);
                if (element != null) {
                    String authored = ((Element)element).getAttribute("visibility");
                    if ("hidden".equals(authored) || "collapse".equals(authored)) {
                        ownVisible = false;
                    }
                }
            } catch (RuntimeException ex) {
                // If reverse lookup is unavailable, inherit the parent state.
            }
        }

        try {
            node.setVisible(ownVisible);
        } catch (RuntimeException ex) { }

        if (node instanceof CompositeGraphicsNode) {
            try {
                java.util.List children =
                        ((CompositeGraphicsNode)node).getChildren();
                for (Object child : children) {
                    if (child instanceof GraphicsNode) {
                        applyInheritedVisibility(
                                (GraphicsNode)child, ownVisible, false);
                    }
                }
            } catch (RuntimeException ex) {
                // Root visibility has still been applied.
            }
        }
    }

    private List<TransformOp> copyTransformOps(List<TransformOp> src) {
        List<TransformOp> out = new ArrayList<TransformOp>();
        for (TransformOp op : src) out.add(op.copy());
        return out;
    }

    private int findTransformOp(List<TransformOp> ops, String type) {
        for (int i = 0; i < ops.size(); i++) {
            if (type.equals(ops.get(i).type)) return i;
        }
        return -1;
    }

    private List<TransformOp> parseTransformOps(String raw) {
        List<TransformOp> out = new ArrayList<TransformOp>();
        if (raw == null || raw.trim().isEmpty()) return out;

        Matcher m = Pattern.compile("([A-Za-z]+)\\s*\\(([^)]*)\\)").matcher(raw);
        while (m.find()) {
            String type = m.group(1);
            double[] p = parseNumberList(m.group(2));
            if (p != null) out.add(new TransformOp(type, normalizeTransformParams(type, p)));
        }
        return out;
    }

    private double[] parseNumberList(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return new double[0];

        String[] parts = s.split("[,\\s]+");
        double[] out = new double[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                out[i] = Double.parseDouble(parts[i]);
            }
            return out;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private double[] normalizeTransformParams(String type, double[] p) {
        if ("translate".equals(type)) {
            if (p.length == 0) return new double[]{0d, 0d};
            if (p.length == 1) return new double[]{p[0], 0d};
            return new double[]{p[0], p[1]};
        }
        if ("scale".equals(type)) {
            if (p.length == 0) return new double[]{1d, 1d};
            if (p.length == 1) return new double[]{p[0], p[0]};
            return new double[]{p[0], p[1]};
        }
        if ("rotate".equals(type)) {
            if (p.length >= 3) return new double[]{p[0], p[1], p[2]};
            if (p.length >= 1) return new double[]{p[0]};
            return new double[]{0d};
        }
        if ("skewX".equals(type) || "skewY".equals(type)) {
            return p.length == 0 ? new double[]{0d} : new double[]{p[0]};
        }
        if ("matrix".equals(type)) {
            if (p.length >= 6) {
                return new double[]{p[0],p[1],p[2],p[3],p[4],p[5]};
            }
            return new double[]{1d,0d,0d,1d,0d,0d};
        }
        return p.clone();
    }

    private AffineTransform toAffineTransform(List<TransformOp> ops) {
        AffineTransform tx = new AffineTransform();

        for (TransformOp op : ops) {
            double[] p = op.p;
            if ("matrix".equals(op.type) && p.length >= 6) {
                tx.concatenate(new AffineTransform(
                        p[0], p[1], p[2], p[3], p[4], p[5]));
            } else if ("translate".equals(op.type) && p.length >= 2) {
                tx.translate(p[0], p[1]);
            } else if ("scale".equals(op.type) && p.length >= 2) {
                tx.scale(p[0], p[1]);
            } else if ("rotate".equals(op.type) && p.length >= 1) {
                if (p.length >= 3) {
                    tx.rotate(Math.toRadians(p[0]), p[1], p[2]);
                } else {
                    tx.rotate(Math.toRadians(p[0]));
                }
            } else if ("skewX".equals(op.type) && p.length >= 1) {
                tx.shear(Math.tan(Math.toRadians(p[0])), 0d);
            } else if ("skewY".equals(op.type) && p.length >= 1) {
                tx.shear(0d, Math.tan(Math.toRadians(p[0])));
            }
        }
        return tx;
    }

    private double[] evaluateTransformTrack(
            SMILTrack track, float seconds, Element target, TransformOp baseOp) {
        String type = track.getName();
        List<String> rawValues = new ArrayList<String>();

        Element anim = track.getAnimationElement();
        String valuesRaw = anim.getAttribute("values");
        String fromRaw = track.getFromRaw();
        String toRaw = track.getToRaw();
        String byRaw = track.getByRaw();

        if (valuesRaw != null && !valuesRaw.trim().isEmpty()) {
            for (String p : valuesRaw.split(";")) rawValues.add(p.trim());
        } else if (!fromRaw.isEmpty() && !toRaw.isEmpty()) {
            rawValues.add(fromRaw);
            rawValues.add(toRaw);
        } else if (!fromRaw.isEmpty() && !byRaw.isEmpty()) {
            double[] start = parseNumberList(fromRaw);
            double[] by = parseNumberList(byRaw);
            if (start == null || by == null) return null;
            start = normalizeTransformParams(type, start);
            by = normalizeTransformParams(type, by);
            int n = Math.max(start.length, by.length);
            double[] startN = expandTransformParams(type, start, n);
            double[] byN = expandTransformParams(type, by, n);
            double[] finish = new double[n];
            for (int i=0; i<n; i++) finish[i] = startN[i] + byN[i];
            rawValues.add(joinNumbers(startN));
            rawValues.add(joinNumbers(finish));
        } else if (!toRaw.isEmpty()) {
            double[] start;
            if (baseOp != null) start = normalizeTransformParams(type, baseOp.p);
            else start = neutralTransformParams(type);
            rawValues.add(joinNumbers(start));
            rawValues.add(toRaw);
        } else if (!byRaw.isEmpty()) {
            double[] by = parseNumberList(byRaw);
            if (by == null) return null;
            by = normalizeTransformParams(type, by);

            double[] start;
            if (baseOp != null) start = normalizeTransformParams(type, baseOp.p);
            else start = neutralTransformParams(type);

            int n = Math.max(start.length, by.length);
            double[] startN = expandTransformParams(type, start, n);
            double[] byN = expandTransformParams(type, by, n);
            double[] finish = new double[n];
            for (int i=0; i<n; i++) finish[i] = startN[i] + byN[i];
            rawValues.add(joinNumbers(startN));
            rawValues.add(joinNumbers(finish));
        }

        if (rawValues.isEmpty()) return null;

        List<double[]> values = new ArrayList<double[]>();
        for (String raw : rawValues) {
            double[] parsed = parseNumberList(raw);
            if (parsed == null) return null;
            values.add(normalizeTransformParams(type, parsed));
        }

        int dims = 0;
        for (double[] v : values) dims = Math.max(dims, v.length);
        if (dims == 0) return null;

        double rotatePivotX = 0d;
        double rotatePivotY = 0d;
        boolean rotatePivotKnown = false;
        if ("rotate".equals(type) && dims >= 3) {
            for (double[] v : values) {
                if (v.length >= 3) {
                    rotatePivotX = v[1];
                    rotatePivotY = v[2];
                    rotatePivotKnown = true;
                    break;
                }
            }
        }

        for (int i=0; i<values.size(); i++) {
            double[] v = values.get(i);
            if ("rotate".equals(type)
                    && dims >= 3
                    && rotatePivotKnown
                    && v.length == 1) {
                values.set(i, new double[]{
                    v[0], rotatePivotX, rotatePivotY
                });
            } else {
                values.set(i, expandTransformParams(type, v, dims));
            }
        }

        List<Float> times;
        if ("paced".equals(track.getCalcMode())) {
            times = pacedTransformTimes(values);
        } else {
            times = track.getKeyTimes();
            if (times.size() != values.size()) times = evenlySpacedTimes(values.size());
        }

        float normalized = normalizedTrackTime(track, seconds);
        if (Float.isNaN(normalized)) return null;

        double[] out;
        if (normalized <= times.get(0)) {
            out = values.get(0).clone();
        } else if (normalized >= times.get(times.size()-1)) {
            out = values.get(values.size()-1).clone();
        } else {
            int left = findInterval(times, normalized);
            float a = times.get(left);
            float b = times.get(left + 1);
            float t = (b-a) == 0f ? 0f : (normalized-a)/(b-a);

            String calcMode = track.getCalcMode();
            if ("discrete".equals(calcMode)) t = 0f;
            else if ("spline".equals(calcMode)) {
                t = applySpline(track.getKeySplines(), left, t);
            }

            double[] v0 = values.get(left);
            double[] v1 = values.get(left + 1);
            out = new double[dims];
            for (int i=0; i<dims; i++) {
                out[i] = v0[i] + (v1[i] - v0[i]) * t;
            }
        }

        if ("sum".equals(track.getAccumulate())) {
            int iteration = completedRepeatIterations(track, seconds);
            if (iteration > 0 && values.size() >= 2) {
                double[] first = values.get(0);
                double[] last = values.get(values.size()-1);
                for (int i=0; i<out.length; i++) {
                    out[i] += (last[i] - first[i]) * iteration;
                }
            }
        }

        return out;
    }

    private double[] neutralTransformParams(String type) {
        if ("scale".equals(type)) return new double[]{1d,1d};
        if ("translate".equals(type)) return new double[]{0d,0d};
        if ("matrix".equals(type)) return new double[]{1d,0d,0d,1d,0d,0d};
        return new double[]{0d};
    }

    private List<Float> pacedTransformTimes(List<double[]> values) {
        List<Float> times = new ArrayList<Float>();
        if (values == null || values.size() <= 1) {
            times.add(0f);
            return times;
        }
        double total = 0d;
        double[] seg = new double[values.size()-1];
        for (int i=0; i<seg.length; i++) {
            seg[i] = vectorDistance(values.get(i), values.get(i+1));
            total += seg[i];
        }
        times.add(0f);
        if (total <= 0.0000001d) return evenlySpacedTimes(values.size());
        double walked = 0d;
        for (int i=0; i<seg.length; i++) {
            walked += seg[i];
            times.add((float)(walked/total));
        }
        return times;
    }

    private double vectorDistance(double[] a, double[] b) {
        int n = Math.min(a.length, b.length);
        double sum = 0d;
        for (int i=0; i<n; i++) {
            double d = b[i]-a[i];
            sum += d*d;
        }
        return Math.sqrt(sum);
    }

    private double[] expandTransformParams(String type, double[] p, int n) {
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            if (i < p.length) out[i] = p[i];
            else if ("scale".equals(type)) out[i] = p.length > 0 ? p[0] : 1d;
            else out[i] = 0d;
        }
        return out;
    }

    private String joinNumbers(double[] v) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            if (i > 0) b.append(' ');
            b.append(SMILTrack.trim((float)v[i]));
        }
        return b.toString();
    }

    private List<Float> evenlySpacedTimes(int count) {
        List<Float> out = new ArrayList<Float>();
        if (count <= 1) {
            out.add(0f);
            return out;
        }
        for (int i = 0; i < count; i++) {
            out.add((float)i / (float)(count - 1));
        }
        return out;
    }

    private int findInterval(List<Float> times, float normalized) {
        for (int i = 0; i < times.size() - 1; i++) {
            if (normalized >= times.get(i) && normalized <= times.get(i+1)) {
                return i;
            }
        }
        return Math.max(0, times.size() - 2);
    }

    private float normalizedTrackTime(SMILTrack track, float seconds) {
        float dur = track.getDurationSeconds();
        float begin = resolveBeginSeconds(track);
        if (Float.isNaN(begin)) return Float.NaN;

        float local = seconds - begin;
        if (local < 0f) return Float.NaN;

        float activeDuration = computeActiveDuration(track, dur, begin);
        if (local >= activeDuration) {
            if (!"freeze".equals(track.getFillMode())) return Float.NaN;

            /*
             * A finite repeatDur/end may truncate the active duration in the
             * middle of a simple iteration. fill=freeze must preserve the
             * sampled value at that exact active-end instant, not blindly
             * force the last keyframe. Example: dur=2s, repeatDur=5s ends
             * halfway through the third cycle, so the frozen phase is 0.5.
             */
            if (dur <= 0f) return 0f;
            float cycle = activeDuration % dur;
            if (Math.abs(cycle) < 0.00001f && activeDuration > 0f) {
                return 1f;
            }
            return Math.max(0f, Math.min(1f, cycle / dur));
        }

        if (dur <= 0f) return 0f;

        float cycle = local % dur;
        return cycle / dur;
    }

    private String evaluateGenericAnimateTrack(
            SMILTrack track, float seconds, Element target, String attr) {

        Element anim = track.getAnimationElement();
        List<String> values = new ArrayList<String>();

        String rawValues = anim.getAttribute("values");
        String from = track.getFromRaw();
        String to = track.getToRaw();
        String by = track.getByRaw();

        if (rawValues != null && !rawValues.trim().isEmpty()) {
            for (String p : rawValues.split(";")) values.add(p.trim());
        } else {
            String underlying = underlyingValueForAttribute(target, attr);

            String start = !from.isEmpty() ? from : underlying;
            String end = "";

            if (!to.isEmpty()) {
                end = to;
            } else if (!by.isEmpty()) {
                end = addGenericValue(start, by, attr);
            }

            if (start != null && !start.trim().isEmpty()) {
                values.add(start.trim());
            }
            if (end != null && !end.trim().isEmpty()) {
                values.add(end.trim());
            }
        }

        if (values.isEmpty()) return null;
        if (values.size() == 1) {
            float nt = normalizedTrackTime(track, seconds);
            return Float.isNaN(nt) ? null : values.get(0);
        }

        List<Float> times;
        if ("paced".equals(track.getCalcMode())) {
            times = pacedGenericTimes(values, attr);
        } else {
            times = track.getKeyTimes();
            if (times.size() != values.size()) {
                times = evenlySpacedTimes(values.size());
            }
        }

        float normalized = normalizedTrackTime(track, seconds);
        if (Float.isNaN(normalized)) return null;

        if (normalized <= times.get(0)) return values.get(0);

        /*
         * Do not return immediately at the last key when accumulate="sum".
         * The final frozen sample of repeated animation still needs the
         * contribution from completed repeat iterations.
         */
        if (normalized >= times.get(times.size()-1)
                && !"sum".equals(track.getAccumulate())) {
            return values.get(values.size()-1);
        }

        int left = normalized >= times.get(times.size()-1)
                ? times.size() - 2
                : findInterval(times, normalized);
        float a = times.get(left);
        float b = times.get(left + 1);
        float t = normalized >= times.get(times.size()-1)
                ? 1f
                : ((b-a) == 0f ? 0f : (normalized-a)/(b-a));

        String calcMode = track.getCalcMode();
        if ("discrete".equals(calcMode)) {
            t = 0f;
        } else if ("spline".equals(calcMode)) {
            t = applySpline(track.getKeySplines(), left, t);
        }

        String v0 = values.get(left);
        String v1 = values.get(left + 1);

        if (isColorAttribute(attr)) {
            String c = interpolateColor(v0, v1, t);
            return c != null ? c : (t < 0.5f ? v0 : v1);
        }

        Double n0 = parseSvgNumber(v0);
        Double n1 = parseSvgNumber(v1);
        if (n0 != null && n1 != null) {
            double result = n0.doubleValue()
                    + (n1.doubleValue() - n0.doubleValue()) * t;

            if ("sum".equals(track.getAccumulate())
                    && values.size() >= 2) {
                int iteration = completedRepeatIterations(track, seconds);
                Double first = parseSvgNumber(values.get(0));
                Double last = parseSvgNumber(values.get(values.size()-1));
                if (iteration > 0 && first != null && last != null) {
                    result += (last.doubleValue() - first.doubleValue())
                            * iteration;
                }
            }
            return trimDouble(result);
        }

        // Non-interpolable/string presentation attributes are discrete.
        return t < 1f ? v0 : v1;
    }

    private boolean isAdditiveNumericAttribute(String attr) {
        return !isColorAttribute(attr)
                && !"visibility".equals(attr)
                && !"stroke-linecap".equals(attr)
                && !"stroke-linejoin".equals(attr);
    }

    private String sumNumericStrings(String a, String b) {
        Double da = parseSvgNumber(a);
        Double db = parseSvgNumber(b);
        if (da == null || db == null) return b;
        return trimDouble(da.doubleValue() + db.doubleValue());
    }

    private List<Float> pacedGenericTimes(
            List<String> values, String attr) {
        if (values == null || values.size() <= 1) {
            List<Float> single = new ArrayList<Float>();
            single.add(0f);
            return single;
        }

        double[] seg = new double[values.size()-1];
        double total = 0d;
        for (int i=0; i<seg.length; i++) {
            seg[i] = genericValueDistance(
                    values.get(i), values.get(i+1), attr);
            total += seg[i];
        }
        if (total <= 0.0000001d) return evenlySpacedTimes(values.size());

        List<Float> out = new ArrayList<Float>();
        out.add(0f);
        double walked = 0d;
        for (int i=0; i<seg.length; i++) {
            walked += seg[i];
            out.add((float)(walked/total));
        }
        return out;
    }

    private double genericValueDistance(
            String a, String b, String attr) {
        Double da = parseSvgNumber(a);
        Double db = parseSvgNumber(b);
        if (da != null && db != null) {
            return Math.abs(db.doubleValue()-da.doubleValue());
        }

        if (isColorAttribute(attr)) {
            Paint pa = parseColorPaint(a);
            Paint pb = parseColorPaint(b);
            if (pa instanceof Color && pb instanceof Color) {
                Color ca=(Color)pa, cb=(Color)pb;
                double dr=cb.getRed()-ca.getRed();
                double dg=cb.getGreen()-ca.getGreen();
                double dbl=cb.getBlue()-ca.getBlue();
                return Math.sqrt(dr*dr+dg*dg+dbl*dbl);
            }
        }
        return 1d;
    }

    private int completedRepeatIterations(
            SMILTrack track, float seconds) {
        float begin = resolveBeginSeconds(track);
        float dur = track.getDurationSeconds();
        if (Float.isNaN(begin) || dur <= 0f || seconds <= begin) return 0;

        float local = seconds - begin;
        float active = computeActiveDuration(track, dur, begin);
        if (!Float.isInfinite(active)) {
            local = Math.min(local, Math.max(0f, active - 0.00001f));
        }
        int iteration = (int)Math.floor(local / dur);
        return Math.max(0, iteration);
    }

    private String addGenericValue(String base, String by, String attr) {
        Double a = parseSvgNumber(base);
        Double b = parseSvgNumber(by);
        if (a != null && b != null) {
            return trimDouble(a.doubleValue() + b.doubleValue());
        }
        return "";
    }

    private String underlyingValueForAttribute(Element target, String attr) {
        String value = authoredProperty(target, attr);
        if (value != null && !value.trim().isEmpty()) return value.trim();

        if ("opacity".equals(attr)
                || "fill-opacity".equals(attr)
                || "stroke-opacity".equals(attr)) return "1";

        if ("visibility".equals(attr)) return "visible";
        if ("fill".equals(attr) || "color".equals(attr)) return "#000000";
        if ("stroke".equals(attr)) return "none";
        if ("stroke-width".equals(attr)) return "1";
        if ("stroke-miterlimit".equals(attr)) return "4";
        if ("stroke-linecap".equals(attr)) return "butt";
        if ("stroke-linejoin".equals(attr)) return "miter";

        return "0";
    }

    private String authoredProperty(Element target, String attr) {
        if (target == null || attr == null) return "";

        String direct = target.getAttribute(attr);
        if (direct != null && !direct.trim().isEmpty()) return direct.trim();

        String style = target.getAttribute("style");
        if (style != null && !style.trim().isEmpty()) {
            for (String part : style.split(";")) {
                int colon = part.indexOf(':');
                if (colon <= 0) continue;
                String name = part.substring(0, colon).trim();
                if (attr.equals(name)) {
                    return part.substring(colon + 1).trim();
                }
            }
        }
        return "";
    }

    private Double parseSvgNumber(String raw) {
        if (raw == null) return null;
        Matcher m = Pattern.compile(
                "^\\s*([-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?)")
                .matcher(raw);
        if (!m.find()) return null;
        try {
            return Double.valueOf(Double.parseDouble(m.group(1)));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean isColorAttribute(String attr) {
        return "fill".equals(attr)
                || "stroke".equals(attr)
                || "color".equals(attr)
                || "stop-color".equals(attr)
                || "flood-color".equals(attr)
                || "lighting-color".equals(attr);
    }

    private GraphicsNode reacquireStrokeCapableNode(
            Element target, GraphicsNode currentNode) {
        if (canvas == null || target == null) return currentNode;
        if (!(currentNode instanceof ShapeNode)) return currentNode;

        List<FillShapePainter> fills = new ArrayList<FillShapePainter>();
        List<StrokeShapePainter> strokes = new ArrayList<StrokeShapePainter>();
        collectPaintAdapters(
                ((ShapeNode)currentNode).getShapePainter(), fills, strokes);
        if (!strokes.isEmpty()) return currentNode;

        /*
         * M5 1.6.3:
         * If a stroke is added from Sketsa Properties while a generic
         * stroke-width animation already exists, the currently cached GVT
         * ShapeNode may still have the old painter graph (fill only).
         * Rebuild once from the authored DOM and reacquire the node so the
         * new StrokeShapePainter becomes available immediately.
         */
        String authoredStroke = authoredProperty(target, "stroke");
        if (authoredStroke == null
                || authoredStroke.trim().isEmpty()
                || "none".equalsIgnoreCase(authoredStroke.trim())) {
            return currentNode;
        }

        try {
            canvas.refresh();
            if (target instanceof SVGElement) {
                GraphicsNode refreshed =
                        canvas.getModel().getGraphicsNode((SVGElement)target);
                if (refreshed != null) return refreshed;
            }
        } catch (RuntimeException ex) {
            // Keep using the current node; the next normal runtime pass can retry.
        }
        return currentNode;
    }

    private int applyGenericAnimateValues(
            Element target, GraphicsNode node, Map<String, String> values) {

        int handled = 0;

        // ----- Geometry adapters -----
        if (node instanceof ShapeNode) {
            Shape animated = buildAnimatedPrimitiveShape(target, values);
            if (animated != null) {
                try {
                    ((ShapeNode)node).setShape(animated);

                    /*
                     * Sketsa/Batik can rebuild the GVT during canvas.refresh().
                     * A direct ShapeNode.setShape() immediately after that
                     * refresh may update the local geometry without scheduling
                     * a visible repaint of its new bounds. In M4 this made the
                     * renderer continue showing Batik's native additive result
                     * (for test A: x=160) even though the local runtime had
                     * already computed x=200.
                     *
                     * Re-setting the existing transform forces a GraphicsNode
                     * change notification without changing geometry semantics.
                     */
                    AffineTransform invalidateTx = node.getTransform();
                    node.setTransform(invalidateTx == null
                            ? new AffineTransform()
                            : new AffineTransform(invalidateTx));

                    handled += countGeometryAnimatedAttributes(target, values);
                } catch (RuntimeException ex) { }
            }

            ShapePainter painter = ((ShapeNode)node).getShapePainter();
            List<FillShapePainter> fills = new ArrayList<FillShapePainter>();
            List<StrokeShapePainter> strokes = new ArrayList<StrokeShapePainter>();
            collectPaintAdapters(painter, fills, strokes);

            boolean hasStrokeAnimation =
                    values.containsKey("stroke")
                    || values.containsKey("stroke-opacity")
                    || values.containsKey("stroke-width")
                    || values.containsKey("stroke-linecap")
                    || values.containsKey("stroke-linejoin")
                    || values.containsKey("stroke-miterlimit")
                    || values.containsKey("stroke-dashoffset");

            if (hasStrokeAnimation && strokes.isEmpty()) {
                GraphicsNode refreshed =
                        reacquireStrokeCapableNode(target, node);
                if (refreshed instanceof ShapeNode && refreshed != node) {
                    node = refreshed;
                    painter = ((ShapeNode)node).getShapePainter();
                    fills.clear();
                    strokes.clear();
                    collectPaintAdapters(painter, fills, strokes);
                }
            }

            // ----- Fill color / fill opacity -----
            String fill = values.get("fill");
            String fillOpacity = values.get("fill-opacity");
            if (fill != null || fillOpacity != null) {
                float alpha = clamp01(fillOpacity != null
                        ? parseFloatOr(fillOpacity, 1f)
                        : parseFloatOr(underlyingValueForAttribute(
                                target, "fill-opacity"), 1f));

                Paint requested = fill != null
                        ? parseColorPaint(fill)
                        : parseColorPaint(underlyingValueForAttribute(
                                target, "fill"));

                for (FillShapePainter fp : fills) {
                    Paint p = requested != null ? requested : fp.getPaint();
                    fp.setPaint(withPaintAlpha(p, alpha));
                }
                if (fill != null) handled++;
                if (fillOpacity != null) handled++;
            }

            // ----- Stroke color / opacity / width / line style -----
            String stroke = values.get("stroke");
            String strokeOpacity = values.get("stroke-opacity");
            String strokeWidth = values.get("stroke-width");
            String linecap = values.get("stroke-linecap");
            String linejoin = values.get("stroke-linejoin");
            String miter = values.get("stroke-miterlimit");
            String dashoffset = values.get("stroke-dashoffset");

            if (stroke != null || strokeOpacity != null
                    || strokeWidth != null || linecap != null
                    || linejoin != null || miter != null
                    || dashoffset != null) {

                float alpha = clamp01(strokeOpacity != null
                        ? parseFloatOr(strokeOpacity, 1f)
                        : parseFloatOr(underlyingValueForAttribute(
                                target, "stroke-opacity"), 1f));

                Paint requested = stroke != null
                        ? parseColorPaint(stroke)
                        : parseColorPaint(underlyingValueForAttribute(
                                target, "stroke"));

                for (StrokeShapePainter sp : strokes) {
                    if (stroke != null || strokeOpacity != null) {
                        Paint p = requested != null ? requested : sp.getPaint();
                        sp.setPaint(withPaintAlpha(p, alpha));
                    }

                    Stroke oldStroke = sp.getStroke();
                    if (oldStroke instanceof BasicStroke) {
                        BasicStroke bs = (BasicStroke)oldStroke;

                        float width = strokeWidth != null
                                ? Math.max(0f, parseFloatOr(
                                        strokeWidth, bs.getLineWidth()))
                                : bs.getLineWidth();

                        int cap = linecap != null
                                ? lineCap(linecap, bs.getEndCap())
                                : bs.getEndCap();
                        int join = linejoin != null
                                ? lineJoin(linejoin, bs.getLineJoin())
                                : bs.getLineJoin();
                        float ml = miter != null
                                ? Math.max(1f, parseFloatOr(
                                        miter, bs.getMiterLimit()))
                                : bs.getMiterLimit();
                        float phase = dashoffset != null
                                ? parseFloatOr(dashoffset, bs.getDashPhase())
                                : bs.getDashPhase();

                        try {
                            sp.setStroke(new BasicStroke(
                                    width, cap, join, ml,
                                    bs.getDashArray(), phase));
                        } catch (IllegalArgumentException ex) {
                            // Keep the original stroke on invalid SVG input.
                        }
                    }
                }

                if (stroke != null) handled++;
                if (strokeOpacity != null) handled++;
                if (strokeWidth != null) handled++;
                if (linecap != null) handled++;
                if (linejoin != null) handled++;
                if (miter != null) handled++;
                if (dashoffset != null) handled++;
            }
        }

        return handled;
    }

    private Shape buildAnimatedPrimitiveShape(
            Element target, Map<String, String> values) {

        String tag = localName(target);

        if ("circle".equals(tag)) {
            if (!hasAny(values, "cx", "cy", "r")) return null;
            double cx = animatedNumber(target, values, "cx", 0d);
            double cy = animatedNumber(target, values, "cy", 0d);
            double r = Math.max(0d, animatedNumber(target, values, "r", 0d));
            return new Ellipse2D.Double(cx-r, cy-r, r*2d, r*2d);
        }

        if ("ellipse".equals(tag)) {
            if (!hasAny(values, "cx", "cy", "rx", "ry")) return null;
            double cx = animatedNumber(target, values, "cx", 0d);
            double cy = animatedNumber(target, values, "cy", 0d);
            double rx = Math.max(0d, animatedNumber(target, values, "rx", 0d));
            double ry = Math.max(0d, animatedNumber(target, values, "ry", 0d));
            return new Ellipse2D.Double(cx-rx, cy-ry, rx*2d, ry*2d);
        }

        if ("rect".equals(tag)) {
            if (!hasAny(values, "x", "y", "width", "height", "rx", "ry")) {
                return null;
            }

            double x = animatedNumber(target, values, "x", 0d);
            double y = animatedNumber(target, values, "y", 0d);
            double w = Math.max(0d,
                    animatedNumber(target, values, "width", 0d));
            double h = Math.max(0d,
                    animatedNumber(target, values, "height", 0d));
            double rx = Math.max(0d,
                    animatedNumber(target, values, "rx", 0d));
            double ry = Math.max(0d,
                    animatedNumber(target, values, "ry", 0d));

            if (rx > 0d || ry > 0d) {
                if (rx == 0d) rx = ry;
                if (ry == 0d) ry = rx;
                rx = Math.min(rx, w / 2d);
                ry = Math.min(ry, h / 2d);
                return new RoundRectangle2D.Double(
                        x, y, w, h, rx*2d, ry*2d);
            }
            return new Rectangle2D.Double(x, y, w, h);
        }

        if ("line".equals(tag)) {
            if (!hasAny(values, "x1", "y1", "x2", "y2")) return null;
            double x1 = animatedNumber(target, values, "x1", 0d);
            double y1 = animatedNumber(target, values, "y1", 0d);
            double x2 = animatedNumber(target, values, "x2", 0d);
            double y2 = animatedNumber(target, values, "y2", 0d);
            return new Line2D.Double(x1, y1, x2, y2);
        }

        return null;
    }

    private int countGeometryAnimatedAttributes(
            Element target, Map<String, String> values) {
        String tag = localName(target);
        String[] names;
        if ("circle".equals(tag)) names = new String[]{"cx","cy","r"};
        else if ("ellipse".equals(tag)) names = new String[]{"cx","cy","rx","ry"};
        else if ("rect".equals(tag)) {
            names = new String[]{"x","y","width","height","rx","ry"};
        } else if ("line".equals(tag)) {
            names = new String[]{"x1","y1","x2","y2"};
        } else return 0;

        int n = 0;
        for (String name : names) if (values.containsKey(name)) n++;
        return n;
    }

    private double animatedNumber(
            Element target, Map<String, String> values,
            String attr, double defaultValue) {
        String raw = values.get(attr);
        if (raw == null) raw = authoredProperty(target, attr);
        Double d = parseSvgNumber(raw);
        return d == null ? defaultValue : d.doubleValue();
    }

    private boolean hasAny(Map<String, String> values, String... names) {
        for (String n : names) if (values.containsKey(n)) return true;
        return false;
    }

    private void collectPaintAdapters(
            ShapePainter painter,
            List<FillShapePainter> fills,
            List<StrokeShapePainter> strokes) {

        if (painter == null) return;

        if (painter instanceof FillShapePainter) {
            fills.add((FillShapePainter)painter);
            return;
        }
        if (painter instanceof StrokeShapePainter) {
            strokes.add((StrokeShapePainter)painter);
            return;
        }
        if (painter instanceof CompositeShapePainter) {
            CompositeShapePainter c = (CompositeShapePainter)painter;
            for (int i=0; i<c.getShapePainterCount(); i++) {
                collectPaintAdapters(c.getShapePainter(i), fills, strokes);
            }
        }
    }

    private Paint withPaintAlpha(Paint paint, float alpha) {
        if (!(paint instanceof Color)) return paint;
        Color c = (Color)paint;
        return new Color(c.getRed(), c.getGreen(), c.getBlue(),
                Math.round(clamp01(alpha) * 255f));
    }

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private float parseFloatOr(String raw, float fallback) {
        Double d = parseSvgNumber(raw);
        return d == null ? fallback : d.floatValue();
    }

    private int lineCap(String value, int fallback) {
        if ("round".equals(value)) return BasicStroke.CAP_ROUND;
        if ("square".equals(value)) return BasicStroke.CAP_SQUARE;
        if ("butt".equals(value)) return BasicStroke.CAP_BUTT;
        return fallback;
    }

    private int lineJoin(String value, int fallback) {
        if ("round".equals(value)) return BasicStroke.JOIN_ROUND;
        if ("bevel".equals(value)) return BasicStroke.JOIN_BEVEL;
        if ("miter".equals(value)) return BasicStroke.JOIN_MITER;
        return fallback;
    }

    private String evaluateStringTrack(SMILTrack track, float seconds) {
        List<String> values = new ArrayList<String>(track.getValues());
        if (values.isEmpty()) return null;

        List<Float> times = track.getKeyTimes();
        if (times.size() != values.size()) {
            times = evenlySpacedTimes(values.size());
        }

        float normalized = normalizedTrackTime(track, seconds);
        if (Float.isNaN(normalized)) return null;

        if (normalized <= times.get(0)) return values.get(0);
        if (normalized >= times.get(times.size()-1)) {
            return values.get(values.size()-1);
        }

        int left = findInterval(times, normalized);

        /*
         * String-valued attributes such as visibility are inherently
         * discrete. The value changes when the next keyTime is reached.
         */
        return values.get(left);
    }

    private static final class PathToken {
        final Character command;
        final Double number;

        PathToken(char command) {
            this.command = Character.valueOf(command);
            this.number = null;
        }

        PathToken(double number) {
            this.command = null;
            this.number = Double.valueOf(number);
        }

        boolean isCommand() { return command != null; }
    }

    private Shape cloneShape(Shape shape) {
        return shape == null ? null : new Path2D.Double(shape);
    }

    private String evaluatePathTrack(SMILTrack track, float seconds) {
        List<String> values = new ArrayList<String>(track.getValues());
        if (values.isEmpty()) return null;

        List<Float> times = track.getKeyTimes();
        if (times.size() != values.size()) {
            times = evenlySpacedTimes(values.size());
        }

        float normalized = normalizedTrackTime(track, seconds);
        if (Float.isNaN(normalized)) return null;

        if (normalized <= times.get(0)) return values.get(0);
        if (normalized >= times.get(times.size()-1)) {
            return values.get(values.size()-1);
        }

        int left = findInterval(times, normalized);
        float a = times.get(left);
        float b = times.get(left + 1);
        float t = (b-a) == 0f ? 0f : (normalized-a)/(b-a);

        String calcMode = track.getCalcMode();
        if ("discrete".equals(calcMode)) {
            t = 0f;
        } else if ("spline".equals(calcMode)) {
            t = applySpline(track.getKeySplines(), left, t);
        }

        return interpolatePathData(values.get(left), values.get(left + 1), t);
    }

    private String interpolatePathData(String a, String b, float t) {
        List<PathToken> ta = tokenizePathData(a);
        List<PathToken> tb = tokenizePathData(b);

        if (ta.size() != tb.size()) {
            return t < 0.5f ? a : b;
        }

        StringBuilder out = new StringBuilder();

        for (int i = 0; i < ta.size(); i++) {
            PathToken pa = ta.get(i);
            PathToken pb = tb.get(i);

            if (pa.isCommand() != pb.isCommand()) {
                return t < 0.5f ? a : b;
            }

            if (pa.isCommand()) {
                if (Character.toUpperCase(pa.command.charValue())
                        != Character.toUpperCase(pb.command.charValue())) {
                    return t < 0.5f ? a : b;
                }
                if (out.length() > 0) out.append(' ');
                out.append(pa.command.charValue());
            } else {
                double v = pa.number.doubleValue()
                        + (pb.number.doubleValue() - pa.number.doubleValue()) * t;
                if (out.length() > 0) out.append(' ');
                out.append(trimPathNumber(v));
            }
        }
        return out.toString();
    }

    private List<PathToken> tokenizePathData(String d) {
        List<PathToken> out = new ArrayList<PathToken>();
        if (d == null) return out;

        Matcher m = Pattern.compile(
                "([AaCcHhLlMmQqSsTtVvZz])"
                + "|([-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?)")
                .matcher(d);

        while (m.find()) {
            if (m.group(1) != null) {
                out.add(new PathToken(m.group(1).charAt(0)));
            } else {
                out.add(new PathToken(Double.parseDouble(m.group(2))));
            }
        }
        return out;
    }

    private String trimPathNumber(double v) {
        if (Math.abs(v) < 0.0000001d) v = 0d;
        String s = String.format(java.util.Locale.US, "%.6f", v);
        while (s.indexOf('.') >= 0 && s.endsWith("0")) {
            s = s.substring(0, s.length()-1);
        }
        if (s.endsWith(".")) s = s.substring(0, s.length()-1);
        return s;
    }

    private Shape parsePathData(String d) {
        List<PathToken> tokens = tokenizePathData(d);
        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);

        int i = 0;
        char cmd = 0;
        char previousCmd = 0;
        double x = 0d, y = 0d;
        double sx = 0d, sy = 0d;
        double lastCubicX = 0d, lastCubicY = 0d;
        double lastQuadX = 0d, lastQuadY = 0d;

        while (i < tokens.size()) {
            PathToken tok = tokens.get(i);
            if (tok.isCommand()) {
                cmd = tok.command.charValue();
                i++;
                if (cmd == 'Z' || cmd == 'z') {
                    path.closePath();
                    x = sx;
                    y = sy;
                    previousCmd = cmd;
                    continue;
                }
            }
            if (cmd == 0) break;

            boolean rel = Character.isLowerCase(cmd);
            char op = Character.toUpperCase(cmd);

            if (op == 'M') {
                if (i + 1 >= tokens.size()) break;
                double nx = pathNumber(tokens, i++);
                double ny = pathNumber(tokens, i++);
                if (rel) { nx += x; ny += y; }
                path.moveTo(nx, ny);
                x = nx; y = ny; sx = nx; sy = ny;

                // Subsequent coordinate pairs after moveto are lineto.
                cmd = rel ? 'l' : 'L';
                previousCmd = rel ? 'm' : 'M';
                continue;
            }

            if (op == 'L') {
                if (i + 1 >= tokens.size()) break;
                double nx = pathNumber(tokens, i++);
                double ny = pathNumber(tokens, i++);
                if (rel) { nx += x; ny += y; }
                path.lineTo(nx, ny);
                x = nx; y = ny;
            } else if (op == 'H') {
                if (i >= tokens.size()) break;
                double nx = pathNumber(tokens, i++);
                if (rel) nx += x;
                path.lineTo(nx, y);
                x = nx;
            } else if (op == 'V') {
                if (i >= tokens.size()) break;
                double ny = pathNumber(tokens, i++);
                if (rel) ny += y;
                path.lineTo(x, ny);
                y = ny;
            } else if (op == 'C') {
                if (i + 5 >= tokens.size()) break;
                double x1 = pathNumber(tokens, i++);
                double y1 = pathNumber(tokens, i++);
                double x2 = pathNumber(tokens, i++);
                double y2 = pathNumber(tokens, i++);
                double nx = pathNumber(tokens, i++);
                double ny = pathNumber(tokens, i++);
                if (rel) {
                    x1 += x; y1 += y; x2 += x; y2 += y; nx += x; ny += y;
                }
                path.curveTo(x1, y1, x2, y2, nx, ny);
                lastCubicX = x2; lastCubicY = y2;
                x = nx; y = ny;
            } else if (op == 'S') {
                if (i + 3 >= tokens.size()) break;
                double x2 = pathNumber(tokens, i++);
                double y2 = pathNumber(tokens, i++);
                double nx = pathNumber(tokens, i++);
                double ny = pathNumber(tokens, i++);
                if (rel) { x2 += x; y2 += y; nx += x; ny += y; }

                double x1 = x, y1 = y;
                char p = Character.toUpperCase(previousCmd);
                if (p == 'C' || p == 'S') {
                    x1 = 2d*x - lastCubicX;
                    y1 = 2d*y - lastCubicY;
                }
                path.curveTo(x1, y1, x2, y2, nx, ny);
                lastCubicX = x2; lastCubicY = y2;
                x = nx; y = ny;
            } else if (op == 'Q') {
                if (i + 3 >= tokens.size()) break;
                double x1 = pathNumber(tokens, i++);
                double y1 = pathNumber(tokens, i++);
                double nx = pathNumber(tokens, i++);
                double ny = pathNumber(tokens, i++);
                if (rel) { x1 += x; y1 += y; nx += x; ny += y; }
                path.quadTo(x1, y1, nx, ny);
                lastQuadX = x1; lastQuadY = y1;
                x = nx; y = ny;
            } else if (op == 'T') {
                if (i + 1 >= tokens.size()) break;
                double nx = pathNumber(tokens, i++);
                double ny = pathNumber(tokens, i++);
                if (rel) { nx += x; ny += y; }

                double x1 = x, y1 = y;
                char p = Character.toUpperCase(previousCmd);
                if (p == 'Q' || p == 'T') {
                    x1 = 2d*x - lastQuadX;
                    y1 = 2d*y - lastQuadY;
                }
                path.quadTo(x1, y1, nx, ny);
                lastQuadX = x1; lastQuadY = y1;
                x = nx; y = ny;
            } else if (op == 'A') {
                if (i + 6 >= tokens.size()) break;
                double rx = pathNumber(tokens, i++);
                double ry = pathNumber(tokens, i++);
                double axisRotation = pathNumber(tokens, i++);
                double largeArcRaw = pathNumber(tokens, i++);
                double sweepRaw = pathNumber(tokens, i++);
                double nx = pathNumber(tokens, i++);
                double ny = pathNumber(tokens, i++);
                if (rel) { nx += x; ny += y; }

                /*
                 * SVG elliptical arc. Flags are discrete even while the other
                 * numeric arc parameters interpolate: threshold interpolated
                 * values at 0.5 so generated in-between path data always maps
                 * back to legal boolean arc semantics.
                 */
                boolean largeArc = largeArcRaw >= 0.5d;
                boolean sweep = sweepRaw >= 0.5d;
                appendSvgArc(path, x, y, rx, ry, axisRotation,
                        largeArc, sweep, nx, ny);
                x = nx; y = ny;
            } else {
                break;
            }

            previousCmd = cmd;
        }

        return path;
    }

    private void appendSvgArc(
            Path2D.Double path,
            double x0, double y0,
            double rx, double ry,
            double xAxisRotation,
            boolean largeArc, boolean sweep,
            double x1, double y1) {

        rx = Math.abs(rx);
        ry = Math.abs(ry);

        // SVG 1.1 implementation notes: degenerate arcs are straight lines.
        if (rx == 0d || ry == 0d) {
            path.lineTo(x1, y1);
            return;
        }

        // Coincident endpoints contribute no new segment.
        if (Math.abs(x0 - x1) < 0.000000001d
                && Math.abs(y0 - y1) < 0.000000001d) {
            return;
        }

        double phi = Math.toRadians(xAxisRotation % 360d);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);

        double dx2 = (x0 - x1) / 2d;
        double dy2 = (y0 - y1) / 2d;

        // Transform endpoints into the ellipse's local coordinate system.
        double xPrime = cosPhi * dx2 + sinPhi * dy2;
        double yPrime = -sinPhi * dx2 + cosPhi * dy2;

        double rx2 = rx * rx;
        double ry2 = ry * ry;
        double xp2 = xPrime * xPrime;
        double yp2 = yPrime * yPrime;

        // Scale radii up when the requested ellipse is too small to connect
        // the endpoints, exactly as required by SVG arc normalization.
        double lambda = xp2 / rx2 + yp2 / ry2;
        if (lambda > 1d) {
            double scale = Math.sqrt(lambda);
            rx *= scale;
            ry *= scale;
            rx2 = rx * rx;
            ry2 = ry * ry;
        }

        double numerator = rx2 * ry2 - rx2 * yp2 - ry2 * xp2;
        double denominator = rx2 * yp2 + ry2 * xp2;

        double factor = 0d;
        if (denominator > 0d) {
            double ratio = Math.max(0d, numerator / denominator);
            factor = Math.sqrt(ratio);
            if (largeArc == sweep) factor = -factor;
        }

        double cxPrime = factor * (rx * yPrime / ry);
        double cyPrime = factor * (-ry * xPrime / rx);

        // Transform ellipse center back to user space.
        double cx = cosPhi * cxPrime - sinPhi * cyPrime + (x0 + x1) / 2d;
        double cy = sinPhi * cxPrime + cosPhi * cyPrime + (y0 + y1) / 2d;

        double ux = (xPrime - cxPrime) / rx;
        double uy = (yPrime - cyPrime) / ry;
        double vx = (-xPrime - cxPrime) / rx;
        double vy = (-yPrime - cyPrime) / ry;

        double theta1 = vectorAngle(1d, 0d, ux, uy);
        double delta = vectorAngle(ux, uy, vx, vy);

        if (!sweep && delta > 0d) delta -= Math.PI * 2d;
        else if (sweep && delta < 0d) delta += Math.PI * 2d;

        // Approximate the arc with cubic Béziers, each spanning <= 90°.
        int segments = Math.max(1,
                (int)Math.ceil(Math.abs(delta) / (Math.PI / 2d)));
        double step = delta / segments;

        for (int s = 0; s < segments; s++) {
            double a0 = theta1 + s * step;
            double a1 = a0 + step;
            appendArcCubic(path, cx, cy, rx, ry, phi, a0, a1);
        }
    }

    private double vectorAngle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt((ux * ux + uy * uy)
                * (vx * vx + vy * vy));
        if (len == 0d) return 0d;

        double c = Math.max(-1d, Math.min(1d, dot / len));
        double angle = Math.acos(c);
        if (ux * vy - uy * vx < 0d) angle = -angle;
        return angle;
    }

    private void appendArcCubic(
            Path2D.Double path,
            double cx, double cy,
            double rx, double ry,
            double phi,
            double a0, double a1) {

        double delta = a1 - a0;
        double alpha = (4d / 3d) * Math.tan(delta / 4d);

        double cos0 = Math.cos(a0);
        double sin0 = Math.sin(a0);
        double cos1 = Math.cos(a1);
        double sin1 = Math.sin(a1);

        // Unit-circle cubic control points.
        double u1x = cos0 - alpha * sin0;
        double u1y = sin0 + alpha * cos0;
        double u2x = cos1 + alpha * sin1;
        double u2y = sin1 - alpha * cos1;

        double[] c1 = ellipsePoint(cx, cy, rx, ry, phi, u1x, u1y);
        double[] c2 = ellipsePoint(cx, cy, rx, ry, phi, u2x, u2y);
        double[] end = ellipsePoint(cx, cy, rx, ry, phi, cos1, sin1);

        path.curveTo(c1[0], c1[1], c2[0], c2[1], end[0], end[1]);
    }

    private double[] ellipsePoint(
            double cx, double cy,
            double rx, double ry,
            double phi,
            double ux, double uy) {

        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);
        double ex = rx * ux;
        double ey = ry * uy;

        return new double[]{
            cx + cosPhi * ex - sinPhi * ey,
            cy + sinPhi * ex + cosPhi * ey
        };
    }

    private double pathNumber(List<PathToken> tokens, int index) {
        PathToken t = tokens.get(index);
        if (t.isCommand()) throw new IllegalArgumentException("Expected path number");
        return t.number.doubleValue();
    }

    private void applyEditorPreview(float seconds) {
        Element target = selectedSVGElement();
        if (target == null || canvas == null) {
            setPreviewStatus("no target/canvas");
            return;
        }

        capturePreviewBase(target);
        if (previewGraphicsNode == null) return;

        /*
         * Re-resolve the live render node every frame. Sketsa/Batik may swap
         * GVT objects after document updates. Geometry/transform preview can
         * still appear correct while cached FillShapePainter references point
         * to an older node.
         */
        try {
            GraphicsNode liveNode = canvas.getModel().getGraphicsNode((SVGElement)target);
            if (liveNode != null) {
                previewGraphicsNode = liveNode;
            }
        } catch (RuntimeException ex) {
            setPreviewStatus("liveNode=ERROR " + ex.getClass().getSimpleName());
        }

        Double currentX = null;
        Double currentY = null;
        boolean continuousGeometryX = false;
        boolean continuousGeometryY = false;
        Double rotate = null;
        SMILTrack rotateTrack = null;
        Float opacity = null;
        Paint fillPaint = null;
        MotionSample motionSample = null;
        SMILTrack motionTrack = null;
        Double setX = null;
        Double setY = null;
        Float setOpacity = null;
        Paint setFillPaint = null;

        for (SMILTrack track : timeline.getTimelineModel().getTracks()) {
            if (track.isMotionTrack()) {
                motionTrack = track;
                motionSample = evaluateMotionTrack(track, seconds);
                continue;
            }
            if (track.isSetTrack()) {
                String setValue = evaluateSetTrack(track, seconds);
                if (setValue != null) {
                    try {
                        String attr = track.getSetAttribute();
                        if ("x".equals(attr)) setX = Double.parseDouble(setValue.trim());
                        else if ("y".equals(attr)) setY = Double.parseDouble(setValue.trim());
                        else if ("opacity".equals(attr)) setOpacity = Float.parseFloat(setValue.trim());
                        else if ("fill".equals(attr)) setFillPaint = parseColorPaint(setValue);
                    } catch (RuntimeException ex) {
                        setPreviewStatus("set ERROR " + track.getSetAttribute() + "=" + setValue);
                    }
                }
                continue;
            }
            String value = evaluateTrack(track, seconds);
            if (value == null) continue;
            try {
                if ("x".equals(track.getName())) {
                    continuousGeometryX = true;
                    double v = Double.parseDouble(value.trim());
                    if ("sum".equals(track.getAdditive())) {
                        double base = currentX != null
                                ? currentX.doubleValue() : previewBaseX;
                        currentX = Double.valueOf(base + v);
                    } else {
                        currentX = Double.valueOf(v);
                    }
                }
                else if ("y".equals(track.getName())) {
                    continuousGeometryY = true;
                    double v = Double.parseDouble(value.trim());
                    if ("sum".equals(track.getAdditive())) {
                        double base = currentY != null
                                ? currentY.doubleValue() : previewBaseY;
                        currentY = Double.valueOf(base + v);
                    } else {
                        currentY = Double.valueOf(v);
                    }
                }
                else if ("rotate".equals(track.getName())) {
                    /*
                     * animateTransform rotate must use the M4 transform
                     * evaluator, not the old scalar selected-track evaluator.
                     *
                     * The scalar path reports only the current repeat cycle,
                     * so an accumulated 0->45deg / repeatCount=4 track looked
                     * like ~0deg again at 1s, 2s and 3s even though the
                     * document-wide runtime was composing the visual transform.
                     */
                    if ("animateTransform".equals(track.getKind())) {
                        List<TransformOp> authoredOps =
                                parseTransformOps(target.getAttribute("transform"));
                        int rotateIndex = findTransformOp(authoredOps, "rotate");
                        TransformOp baseRotate = rotateIndex >= 0
                                ? authoredOps.get(rotateIndex) : null;
                        double[] rv = evaluateTransformTrack(
                                track, seconds, target, baseRotate);
                        if (rv != null && rv.length > 0) {
                            rotate = Double.valueOf(rv[0]);
                        } else {
                            rotate = Double.parseDouble(value.trim());
                        }
                    } else {
                        rotate = Double.parseDouble(value.trim());
                    }
                    rotateTrack = track;
                }
                else if ("opacity".equals(track.getName())) opacity = Float.parseFloat(value.trim());
                else if ("fill".equals(track.getName())) fillPaint = parseColorPaint(value);
            } catch (RuntimeException ex) {
                setPreviewStatus("value ERROR " + track.getName() + "=" + value);
            }
        }

        // <set> is a discrete state track and is applied after continuous
        // animation tracks in the editor preview.
        if (setX != null) currentX = setX;
        if (setY != null) currentY = setY;
        if (setOpacity != null) opacity = setOpacity;
        if (setFillPaint != null) fillPaint = setFillPaint;

        double dx = currentX == null ? 0d : currentX.doubleValue() - previewBaseX;
        double dy = currentY == null ? 0d : currentY.doubleValue() - previewBaseY;

        /*
         * 1.6.25 - do not apply rect x/y animation twice.
         *
         * The document-wide runtime handles <animate attributeName="x|y"> on
         * primitive rectangles by rebuilding the ShapeNode geometry at the
         * evaluated absolute coordinate. The selected-target preview historically
         * represented the same x/y value as a GraphicsNode translation. Running
         * both paths together therefore produced:
         *
         *   geometry x=800 + transform translate(800-120) => visual x=1480
         *
         * which became especially visible after the 1.6.24 fill=freeze final
         * pinning pass. For continuous rect geometry tracks, leave placement to
         * the authoritative document-wide geometry adapter and keep the selected
         * node's static transform only. <set> x/y is intentionally unchanged
         * because it is still handled by the selected-target preview path.
         */
        if ("rect".equals(localName(target))) {
            if (continuousGeometryX) dx = 0d;
            if (continuousGeometryY) dy = 0d;
        }

        AffineTransform tx = previewBaseTransform == null
                ? new AffineTransform()
                : new AffineTransform(previewBaseTransform);

        if (motionSample != null && previewBaseCenterValid) {
            /*
             * Imported/native SVG animateMotion uses the path as a motion
             * transform. A path beginning at M0 0 therefore means zero
             * translation at t=0; it must NOT be interpreted as an absolute
             * placement point and compensated against the object's center.
             *
             * Editor-authored Center/Origin/Custom modes, on the other hand,
             * deliberately treat the sampled point as an absolute anchor
             * placement and retain the existing anchor compensation.
             */
            boolean nativeMotion = motionTrack != null
                    && "native".equals(motionAnchorMode(motionTrack));
            if (nativeMotion) {
                tx.translate(motionSample.x, motionSample.y);
                if (motionSample.rotate) {
                    tx.rotate(Math.toRadians(motionSample.angleDegrees));
                }
            } else {
                tx.translate(
                        motionSample.x - previewBaseCenterX,
                        motionSample.y - previewBaseCenterY);
                if (motionSample.rotate) {
                    tx.rotate(
                            Math.toRadians(motionSample.angleDegrees),
                            previewBaseCenterX,
                            previewBaseCenterY);
                }
            }
        }

        if (dx != 0d || dy != 0d) tx.translate(dx, dy);

        if (rotate != null) {
            if (rotateUsesLocalOriginPivot(rotateTrack, target)) {
                /*
                 * For imported native animateTransform rotate(angle), append
                 * the rotation around local origin. If the authored base
                 * transform contains translate(150,150), that local origin is
                 * exactly the clock center in viewport/user space.
                 */
                tx.rotate(Math.toRadians(rotate.doubleValue()));
            } else {
                /*
                 * Preserve the existing editor-created rotate behavior, which
                 * uses the rendered object center as pivot.
                 */
                Rectangle2D bounds = previewGraphicsNode.getBounds();
                if (bounds != null) {
                    tx.rotate(Math.toRadians(rotate.doubleValue()),
                            bounds.getCenterX(), bounds.getCenterY());
                } else {
                    tx.rotate(Math.toRadians(rotate.doubleValue()));
                }
            }
        }

        boolean transformOk = true;
        try {
            previewGraphicsNode.setTransform(tx);
        } catch (RuntimeException ex) {
            transformOk = false;
            setPreviewStatus("node=" + previewGraphicsNode.getClass().getSimpleName()
                    + " | transformApplied=ERROR " + ex.getClass().getSimpleName());
        }

        if (opacity != null) {
            try {
                float a = Math.max(0f, Math.min(1f, opacity.floatValue()));
                previewGraphicsNode.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            } catch (RuntimeException ex) {
                // diagnosis remains visible through status below
            }
        } else {
            try { previewGraphicsNode.setComposite(previewBaseComposite); }
            catch (RuntimeException ex) { }
        }

        List<FillShapePainter> liveFillPainters = currentFillPainters(previewGraphicsNode);
        if (fillPaint != null) {
            for (FillShapePainter painter : liveFillPainters) {
                painter.setPaint(fillPaint);
            }
        } else {
            /*
             * M3 needs the underlying fill before begin and after fill=remove.
             * Painters are reacquired from the live GVT node first, so restoring
             * the captured base paint here no longer reintroduces the stale
             * painter problem fixed in 0.6.6.
             */
            int n = Math.min(liveFillPainters.size(), previewFillPaints.size());
            for (int i=0; i<n; i++) {
                liveFillPainters.get(i).setPaint(previewFillPaints.get(i).paint);
            }
        }

        if (transformOk) {
            StringBuilder s = new StringBuilder();
            s.append("node=").append(previewGraphicsNode.getClass().getSimpleName());
            s.append(" | t=").append(SMILTrack.trim(seconds)).append("s");

            /*
             * Keep the X/Y diagnostic continuously visible.
             *
             * M4 introduced more generic/document-wide paths where the
             * selected target can be animated without currentX/currentY being
             * populated by the old selected-track preview. In 1.5.0 that made
             * the familiar x/y values disappear from the Preview status even
             * though the animation itself was working.
             *
             * Fall back to the captured authored/base x/y values when no
             * selected x/y contribution is active. This is diagnostic only;
             * it does not alter rendering or SMIL evaluation.
             */
            Double genericStatusX = evaluatePreviewCoordinate(
                    target, seconds, true);
            Double genericStatusY = evaluatePreviewCoordinate(
                    target, seconds, false);

            double previewStatusX = currentX != null
                    ? currentX.doubleValue()
                    : (genericStatusX != null
                        ? genericStatusX.doubleValue()
                        : previewBaseX);
            double previewStatusY = currentY != null
                    ? currentY.doubleValue()
                    : (genericStatusY != null
                        ? genericStatusY.doubleValue()
                        : previewBaseY);

            /*
             * Motion is an absolute placement path. When active, it is the most
             * useful X/Y diagnostic and takes precedence over static geometry.
             */
            if (motionSample != null) {
                previewStatusX = motionSample.x;
                previewStatusY = motionSample.y;
            } else {
                /*
                 * For transform-driven groups/paths there is no cx/cy/x/y
                 * attribute. Report the effective transform translation from
                 * the transform actually applied by the selected-target
                 * preview. This makes animateTransform translate visible in
                 * the Preview coordinates too.
                 */
                String local = localName(target);
                boolean geometryHasOwnPosition =
                        "circle".equals(local) || "ellipse".equals(local)
                        || "line".equals(local) || "rect".equals(local)
                        || "image".equals(local) || "text".equals(local)
                        || "use".equals(local) || target.hasAttribute("x")
                        || target.hasAttribute("y");

                if (!geometryHasOwnPosition) {
                    double[] animatedTranslation =
                            evaluatePreviewTransformTranslation(target, seconds);
                    if (animatedTranslation != null) {
                        previewStatusX = animatedTranslation[0];
                        previewStatusY = animatedTranslation[1];
                    } else if (tx != null) {
                        previewStatusX = tx.getTranslateX();
                        previewStatusY = tx.getTranslateY();
                    }
                }
            }

            s.append(" | x=").append(trimDouble(previewStatusX));
            s.append(" | y=").append(trimDouble(previewStatusY));

            if (currentX != null) {
                s.append(" | dx=").append(trimDouble(dx));
            }
            if (currentY != null) {
                s.append(" | dy=").append(trimDouble(dy));
            }
            if (rotate != null) {
                s.append(" | rot=").append(trimDouble(rotate.doubleValue()));
                SMILTrack rt = null;
                for (SMILTrack t : timeline.getTimelineModel().getTracks()) {
                    if ("rotate".equals(t.getName())) { rt = t; break; }
                }
                if (rt != null) {
                    s.append(" | dur=").append(SMILTrack.trim(rt.getDurationSeconds()));
                    if (!rt.getByRaw().isEmpty()) s.append(" | by=").append(rt.getByRaw());
                    s.append(" | pivot=").append(
                            rotateUsesLocalOriginPivot(rt, target) ? "local-origin" : "bounds-center");
                }
            }
            if (motionSample != null) {
                s.append(" | motion=").append(trimDouble(motionSample.x)).append(",").append(trimDouble(motionSample.y));
                if (previewBaseCenterValid) {
                    s.append(" | anchor=").append(trimDouble(previewBaseCenterX)).append(",").append(trimDouble(previewBaseCenterY));
                }
                if (previewBaseTransform != null) {
                    double[] mx = new double[6];
                    previewBaseTransform.getMatrix(mx);
                    s.append(" | baseTx=").append(trimDouble(mx[4])).append(",").append(trimDouble(mx[5]));
                }
            }
            if (setX != null || setY != null || setOpacity != null || setFillPaint != null) {
                s.append(" | setApplied=yes");
            }
            s.append(" | transformApplied=yes");
            s.append(" | docRuntime=1.6.11");
            s.append(" | animate=").append(runtimeGenericAnimateHandled)
                    .append("/").append(runtimeGenericAnimateTracks);
            s.append(" | motion=").append(runtimeMotionResolved)
                    .append("/").append(runtimeMotionTracks);
            s.append(" | opacity=").append(runtimeOpacityTracks);
            s.append(" | vis=").append(runtimeVisibilityResolved)
                    .append("/").append(runtimeVisibilityTracks);
            s.append(" | fillPainters=").append(liveFillPainters.size());
            if (fillPaint != null) s.append(" | fillApplied=yes");
            setPreviewStatus(s.toString());
        }
    }

    private double[] evaluatePreviewTransformTranslation(
            Element target, float seconds) {
        if (target == null) return null;

        List<TransformOp> effective =
                parseTransformOps(target.getAttribute("transform"));

        /*
         * Reuse the SAME animateTransform evaluator/order as the document-wide
         * M4 runtime. The old Preview diagnostic path only understood rotate,
         * therefore translate/scale/skew tracks could animate correctly on the
         * canvas while the displayed x/y stayed frozen at the authored value.
         */
        for (SMILTrack track : timeline.getTimelineModel().getTracks()) {
            Element anim = track.getAnimationElement();
            if (anim == null || !"animateTransform".equals(localName(anim))) {
                continue;
            }

            String type = track.getName();
            int baseIndex = findTransformOp(effective, type);
            TransformOp reference =
                    baseIndex >= 0 ? effective.get(baseIndex) : null;

            double[] value = evaluateTransformTrack(
                    track, seconds, target, reference);
            if (value == null) continue;

            TransformOp animated = new TransformOp(type, value);
            if ("sum".equals(track.getAdditive())) {
                effective.add(animated);
            } else {
                if (baseIndex >= 0) effective.set(baseIndex, animated);
                else effective.add(animated);
            }
        }

        AffineTransform tx = toAffineTransform(effective);
        return new double[]{tx.getTranslateX(), tx.getTranslateY()};
    }

    private double previewCoordinateBase(Element target, boolean xAxis) {
        if (target == null) return 0d;

        String local = localName(target);
        if ("circle".equals(local) || "ellipse".equals(local)) {
            return parseNumericAttribute(target, xAxis ? "cx" : "cy", 0d);
        }
        if ("line".equals(local)) {
            return parseNumericAttribute(target, xAxis ? "x1" : "y1", 0d);
        }
        if ("rect".equals(local) || "image".equals(local)
                || "text".equals(local) || "use".equals(local)) {
            return parseNumericAttribute(target, xAxis ? "x" : "y", 0d);
        }

        String attr = xAxis ? "x" : "y";
        if (target.hasAttribute(attr)) {
            return parseNumericAttribute(target, attr, 0d);
        }

        /*
         * Groups and paths usually do not own x/y geometry attributes. Their
         * useful position is carried by transform="", especially translate().
         * Read the authored transform first so Preview can report e.g.
         * <g transform="translate(390,270)"> as x=390,y=270 instead of 0,0.
         */
        try {
            List<TransformOp> authored = parseTransformOps(
                    target.getAttribute("transform"));
            if (!authored.isEmpty()) {
                AffineTransform tx = toAffineTransform(authored);
                return xAxis ? tx.getTranslateX() : tx.getTranslateY();
            }
        } catch (RuntimeException ex) { }

        /*
         * If the element has no authored transform, use the live GVT transform.
         * This also gives a meaningful position for imported groups whose
         * transform is supplied by runtime state.
         */
        try {
            if (previewGraphicsNode != null) {
                AffineTransform tx = previewGraphicsNode.getTransform();
                if (tx != null) {
                    double v = xAxis ? tx.getTranslateX() : tx.getTranslateY();
                    if (Math.abs(v) > 0.0000001d) return v;
                }

                Rectangle2D b = previewGraphicsNode.getBounds();
                if (b != null) return xAxis ? b.getCenterX() : b.getCenterY();
            }
        } catch (RuntimeException ex) { }
        return 0d;
    }

    private Double evaluatePreviewCoordinate(
            Element target, float seconds, boolean xAxis) {
        if (target == null) return null;

        String local = localName(target);
        String[] attrs;
        if ("circle".equals(local) || "ellipse".equals(local)) {
            attrs = xAxis ? new String[]{"cx","x"} : new String[]{"cy","y"};
        } else if ("line".equals(local)) {
            attrs = xAxis ? new String[]{"x1","x"} : new String[]{"y1","y"};
        } else {
            attrs = xAxis ? new String[]{"x","cx","x1"} : new String[]{"y","cy","y1"};
        }

        List<SMILTrack> tracks = timeline.getTimelineModel().getTracks();
        for (String attr : attrs) {
            for (SMILTrack t : tracks) {
                if (t.isMotionTrack() || t.isSetTrack()) continue;
                if (!attr.equals(t.getName())) continue;

                String value = evaluateGenericAnimateTrack(
                        t, seconds, target, attr);
                if (value == null) value = evaluateTrack(t, seconds);
                if (value == null) continue;

                Double parsed = parseSvgNumber(value);
                if (parsed != null) return parsed;
            }
        }
        return null;
    }

    private double parseNumericAttribute(Element e, String attr, double fallback) {
        String raw = e.getAttribute(attr);
        if (raw == null || raw.trim().isEmpty()) return fallback;
        raw = raw.trim();
        int cut = 0;
        while (cut < raw.length()) {
            char c = raw.charAt(cut);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') cut++;
            else break;
        }
        if (cut == 0) return fallback;
        try { return Double.parseDouble(raw.substring(0, cut)); }
        catch (RuntimeException ex) { return fallback; }
    }

    private Paint parseColorPaint(String value) {
        if (value == null) return null;
        String s = value.trim().toLowerCase(java.util.Locale.US);
        if (s.isEmpty() || "none".equals(s)) return null;
        if ("transparent".equals(s)) return new Color(0,0,0,0);

        int[] rgb = parseHexColor(s);
        if (rgb != null) return new Color(rgb[0], rgb[1], rgb[2]);

        Matcher m = Pattern.compile(
                "rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)")
                .matcher(s);
        if (m.matches()) {
            try {
                return new Color(
                        Math.max(0, Math.min(255, Integer.parseInt(m.group(1)))),
                        Math.max(0, Math.min(255, Integer.parseInt(m.group(2)))),
                        Math.max(0, Math.min(255, Integer.parseInt(m.group(3)))));
            } catch (RuntimeException ex) { }
        }

        if ("black".equals(s)) return Color.BLACK;
        if ("white".equals(s)) return Color.WHITE;
        if ("red".equals(s)) return Color.RED;
        if ("green".equals(s)) return Color.GREEN;
        if ("blue".equals(s)) return Color.BLUE;
        if ("yellow".equals(s)) return Color.YELLOW;
        if ("cyan".equals(s) || "aqua".equals(s)) return Color.CYAN;
        if ("magenta".equals(s) || "fuchsia".equals(s)) return Color.MAGENTA;
        if ("gray".equals(s) || "grey".equals(s)) return Color.GRAY;
        if ("orange".equals(s)) return Color.ORANGE;
        if ("pink".equals(s)) return Color.PINK;

        return null;
    }

    private Element findElementById(Element root, String id) {
        if (root == null || id == null) return null;
        if (id.equals(root.getAttribute("id"))) return root;
        org.w3c.dom.NodeList children = root.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (n instanceof Element) {
                Element found = findElementById((Element)n, id);
                if (found != null) return found;
            }
        }
        return null;
    }

    private MotionSample evaluateMotionTrack(SMILTrack track, float seconds) {
        return evaluateMotionTrack(track, seconds, selectedSVGElement());
    }

    private MotionSample evaluateMotionTrack(
            SMILTrack track, float seconds, Element target) {
        if (target == null) return null;

        float dur = track.getDurationSeconds();
        float begin = resolveBeginSeconds(track);
        if (Float.isNaN(begin)) return null;
        float local = seconds - begin;
        if (local < 0f) return null;

        float active = computeActiveDuration(track, dur, begin);
        if (local >= active
                && !"freeze".equals(track.getFillMode())) return null;

        float t = normalizedTrackTime(track, seconds);
        if (Float.isNaN(t)) return null;
        t = Math.max(0f, Math.min(1f, t));

        Element documentRoot = target.getOwnerDocument().getDocumentElement();
        String d = track.getMotionPathData(documentRoot);
        if (d == null || d.trim().isEmpty()) return null;

        List<Point2D.Float> pts = flattenSvgPath(d, 18);
        if (pts.size() < 2) return null;

        /*
         * Coordinate-space rule:
         *
         * Inline animateMotion path="" is authored directly in the target
         * parent's user space, so those points are already usable.
         *
         * <mpath>, however, references another SVG element. Its d coordinates
         * belong to the referenced path's own user space. If that path and the
         * moving target live under different transformed ancestors, sampling
         * raw d values produces a visible offset.
         *
         * Convert:
         *
         *   referenced path local -> document root -> target parent local
         *
         * before distance/tangent sampling. This keeps the marker exactly on
         * the rendered reference path even through nested scale/rotate/
         * translate transforms.
         */
        String motionPathId = track.getMotionPathId();
        if (motionPathId != null && !motionPathId.trim().isEmpty()) {
            Element pathElement = findElementById(documentRoot, motionPathId.trim());
            if (pathElement != null
                    && !"native".equals(motionAnchorMode(track))) {
                /*
                 * Editor-authored Motion binds an already-positioned object to
                 * a visible SVG path, so the referenced path geometry must be
                 * converted into the target-parent user space.
                 *
                 * Imported/native SMIL is different: <mpath> supplies path
                 * data for the animateMotion supplemental transform in the
                 * target's current user coordinate system. Converting it via
                 * the referenced path/root/parent CTMs incorrectly cancels
                 * ancestor transforms (notably nested translate + scale on
                 * <use> targets, as in TEST-Toy_train_SMIL.svg).
                 *
                 * Therefore native/unmarked animateMotion keeps the raw path
                 * data coordinates. Parent transforms remain in the GVT tree
                 * and are applied naturally after the motion transform.
                 */
                pts = convertReferencedMotionPoints(
                        pts, pathElement, target, documentRoot);
                if (pts.size() < 2) return null;
            }
        }

        float total = 0f;
        for (int i=1; i<pts.size(); i++) total += distance(pts.get(i-1), pts.get(i));
        if (total <= 0f) return null;

        float wanted = total * t;
        float walked = 0f;
        for (int i=1; i<pts.size(); i++) {
            Point2D.Float a = pts.get(i-1), b = pts.get(i);
            float seg = distance(a,b);
            if (walked + seg >= wanted || i == pts.size()-1) {
                float u = seg <= 0f ? 0f : (wanted - walked) / seg;
                u = Math.max(0f, Math.min(1f,u));
                float x = a.x + (b.x-a.x)*u;
                float y = a.y + (b.y-a.y)*u;
                float angle = (float)Math.toDegrees(Math.atan2(b.y-a.y, b.x-a.x));
                String rotate = track.getMotionRotate();
                boolean doRotate = true;
                if ("auto-reverse".equals(rotate)) angle += 180f;
                else if (!"auto".equals(rotate)) {
                    try { angle = Float.parseFloat(rotate); }
                    catch (RuntimeException ex) { doRotate = false; }
                }
                return new MotionSample(x,y,angle,doRotate);
            }
            walked += seg;
        }
        return null;
    }

    private Point2D.Double editorMotionAnchor(
            SMILTrack track, Element target, GraphicsNode node) {

        if (track == null || target == null || node == null) return null;

        String mode = motionAnchorMode(track);

        /*
         * Imported/unmarked SVG fallback: preserve native animateMotion
         * semantics. No Sketsa-specific anchor compensation is applied.
         */
        if ("native".equals(mode)) return null;

        Point2D.Double cached = documentMotionAnchors.get(target);
        if (cached != null) {
            return new Point2D.Double(cached.x, cached.y);
        }

        try {
            Point2D.Double localAnchor;

            if ("origin".equals(mode)) {
                localAnchor = new Point2D.Double(0d, 0d);
            } else if ("custom".equals(mode)) {
                Element anim = track.getAnimationElement();
                double x = parseFloatOr(
                        anim.getAttribute("data-sketsa-motion-anchor-x"), 0f);
                double y = parseFloatOr(
                        anim.getAttribute("data-sketsa-motion-anchor-y"), 0f);
                localAnchor = new Point2D.Double(x, y);
            } else {
                Rectangle2D b = node.getBounds();
                if (b == null) return null;
                localAnchor = new Point2D.Double(
                        b.getCenterX(), b.getCenterY());
            }

            /*
             * Anchor and sampled motion point must be in the same coordinate
             * space. Convert the local anchor through the authored static
             * transform into target-parent user space.
             */
            List<TransformOp> authoredOps =
                    parseTransformOps(target.getAttribute("transform"));
            AffineTransform authored = toAffineTransform(authoredOps);

            Point2D.Double parentAnchor = new Point2D.Double();
            authored.transform(localAnchor, parentAnchor);

            documentMotionAnchors.put(target,
                    new Point2D.Double(parentAnchor.x, parentAnchor.y));
            return parentAnchor;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private List<Point2D.Float> convertReferencedMotionPoints(
            List<Point2D.Float> source,
            Element pathElement,
            Element target,
            Element documentRoot) {

        List<Point2D.Float> out = new ArrayList<Point2D.Float>();
        if (source == null || source.isEmpty()) return out;

        try {
            AffineTransform pathToRoot =
                    elementLocalToRootTransform(pathElement, documentRoot, true);

            Element parent = target.getParentNode() instanceof Element
                    ? (Element)target.getParentNode() : null;

            AffineTransform parentToRoot = parent == null
                    ? new AffineTransform()
                    : elementLocalToRootTransform(parent, documentRoot, true);

            AffineTransform rootToParent = parentToRoot.createInverse();

            AffineTransform pathToParent = new AffineTransform(rootToParent);
            pathToParent.concatenate(pathToRoot);

            for (Point2D.Float p : source) {
                Point2D.Double q = new Point2D.Double(p.x, p.y);
                pathToParent.transform(q, q);
                out.add(new Point2D.Float((float)q.x, (float)q.y));
            }
            return out;
        } catch (java.awt.geom.NoninvertibleTransformException ex) {
            return source;
        } catch (RuntimeException ex) {
            return source;
        }
    }

    private AffineTransform elementLocalToRootTransform(
            Element element, Element documentRoot, boolean includeElement) {

        List<Element> chain = new ArrayList<Element>();
        Element current = includeElement ? element
                : (element != null && element.getParentNode() instanceof Element
                    ? (Element)element.getParentNode() : null);

        while (current != null && current != documentRoot) {
            chain.add(current);
            org.w3c.dom.Node parent = current.getParentNode();
            current = parent instanceof Element ? (Element)parent : null;
        }

        java.util.Collections.reverse(chain);

        AffineTransform tx = new AffineTransform();
        for (Element e : chain) {
            String raw = e.getAttribute("transform");
            if (raw == null || raw.trim().isEmpty()) continue;
            tx.concatenate(toAffineTransform(parseTransformOps(raw)));
        }
        return tx;
    }

    private float distance(Point2D.Float a, Point2D.Float b) {
        float dx=b.x-a.x, dy=b.y-a.y;
        return (float)Math.sqrt(dx*dx+dy*dy);
    }

    private List<Point2D.Float> flattenSvgPath(String d, int curveSteps) {
        List<Point2D.Float> out = new ArrayList<Point2D.Float>();
        if (d == null || d.trim().isEmpty()) return out;

        try {
            /*
             * Use the same complete SVG path parser as d-morph preview. This
             * keeps Motion and path morph geometry in one implementation and
             * gives animateMotion M/L/H/V/C/S/Q/T/A/Z, absolute/relative,
             * including SVG elliptical arcs.
             */
            Shape shape = parsePathData(d);
            if (shape == null) return out;

            // curveSteps controls tolerance: more requested steps => smaller
            // flatness while keeping a sane lower bound for real documents.
            double flatness = Math.max(0.20d, 8d / Math.max(4, curveSteps));
            FlatteningPathIterator it = new FlatteningPathIterator(
                    shape.getPathIterator(null), flatness, 12);
            double[] c = new double[6];
            double startX = 0d, startY = 0d;
            boolean haveSubpath = false;

            while (!it.isDone()) {
                int type = it.currentSegment(c);
                if (type == PathIterator.SEG_MOVETO) {
                    startX = c[0];
                    startY = c[1];
                    out.add(new Point2D.Float((float)c[0], (float)c[1]));
                    haveSubpath = true;
                } else if (type == PathIterator.SEG_LINETO) {
                    out.add(new Point2D.Float((float)c[0], (float)c[1]));
                } else if (type == PathIterator.SEG_CLOSE && haveSubpath) {
                    if (out.isEmpty()
                            || Math.abs(out.get(out.size()-1).x - startX) > 0.0001f
                            || Math.abs(out.get(out.size()-1).y - startY) > 0.0001f) {
                        out.add(new Point2D.Float((float)startX, (float)startY));
                    }
                }
                it.next();
            }
        } catch (RuntimeException ex) {
            out.clear();
        }
        return out;
    }

    private static final class MotionSample {
        final float x,y,angleDegrees;
        final boolean rotate;
        MotionSample(float x,float y,float angleDegrees,boolean rotate) {
            this.x=x; this.y=y; this.angleDegrees=angleDegrees; this.rotate=rotate;
        }
    }

    private boolean trackIdAvailable(SMILTrack current, String id) {
        for (SMILTrack t : timeline.getTimelineModel().getTracks()) {
            if (t == current) continue;
            if (id.equals(t.getTrackId())) return false;
        }
        return true;
    }

    private boolean isEventTiming(String raw) {
        if (raw == null) return false;
        for (String part : splitTimingList(raw)) {
            if (eventExpression(part).matches()) return true;
        }
        return false;
    }

    private void stopNativeAnimationInstance(SMILTrack track) {
        if (track == null) return;
        Element anim = track.getAnimationElement();
        if (anim instanceof SVGAnimationElement) {
            try {
                ((SVGAnimationElement)anim).endElement();
            } catch (RuntimeException ex) {
                // Local GVT preview remains authoritative even if renderer refuses.
            }
        }
    }

    private boolean validClockOrIndefinite(String raw) {
        String s = raw == null ? "" : raw.trim();
        return "indefinite".equals(s) || parseClockSeconds(s) != null;
    }

    private boolean validTimingExpression(String raw, boolean allowIndefinite) {
        List<String> parts = splitTimingList(raw);
        if (parts.isEmpty()) return false;
        for (String part : parts) {
            if (!validSingleTimingExpression(part, allowIndefinite)) return false;
        }
        return true;
    }

    private boolean validSingleTimingExpression(
            String raw, boolean allowIndefinite) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return false;
        if (allowIndefinite && "indefinite".equals(s)) return true;

        Float clock = parseClockSeconds(s);
        if (clock != null) return allowIndefinite || clock.floatValue() >= 0f;

        Matcher event = eventExpression(s);
        if (event.matches()) {
            String off = event.group(3);
            return off == null || parseClockSeconds(off) != null;
        }

        Matcher sync = syncbaseExpression(s);
        if (sync.matches()) {
            String off = sync.group(3);
            return off == null || parseClockSeconds(off) != null;
        }
        return false;
    }

    private List<String> splitTimingList(String raw) {
        List<String> out = new ArrayList<String>();
        if (raw == null) return out;
        for (String part : raw.split(";")) {
            String s = part.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private Matcher eventExpression(String raw) {
        return Pattern.compile(
                "^(?:([A-Za-z_][A-Za-z0-9_.-]*)\\.)?"
                + "(click|mouseover|mouseout|mousedown|mouseup)"
                + "([+-].+)?$")
                .matcher(raw == null ? "" : raw.trim());
    }

    private Matcher syncbaseExpression(String raw) {
        return Pattern.compile(
                "^([A-Za-z_][A-Za-z0-9_.-]*)\\.(begin|end)([+-].+)?$")
                .matcher(raw == null ? "" : raw.trim());
    }

    private Float parseClockSeconds(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        try {
            if (s.endsWith("ms")) {
                return Float.parseFloat(s.substring(0, s.length()-2)) / 1000f;
            }
            if (s.endsWith("min")) {
                return Float.parseFloat(s.substring(0, s.length()-3)) * 60f;
            }
            if (s.endsWith("h")) {
                return Float.parseFloat(s.substring(0, s.length()-1)) * 3600f;
            }
            if (s.endsWith("s")) {
                return Float.parseFloat(s.substring(0, s.length()-1));
            }
            return Float.parseFloat(s);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /*
     * Document-wide animation registry.
     *
     * The timeline edits one target at a time, but SMIL syncbase IDs are
     * document-scoped. Never resolve id.begin / id.end through the currently
     * selected timeline only.
     */
    private SMILTrack findTrackById(String id) {
        if (id == null || id.trim().isEmpty() || canvas == null) return null;
        try {
            org.w3c.dom.svg.SVGDocument doc = canvas.getSVGDocument();
            if (doc == null || doc.getDocumentElement() == null) return null;
            Element e = findAnimationElementById(
                    doc.getDocumentElement(), id.trim());
            return trackFromAnimationElement(e);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Element findAnimationElementById(Element root, String id) {
        if (root == null) return null;
        String local = localName(root);
        if (isAnimationElementName(local)
                && id.equals(root.getAttribute("id"))) return root;

        org.w3c.dom.NodeList children = root.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (!(n instanceof Element)) continue;
            Element found = findAnimationElementById((Element)n, id);
            if (found != null) return found;
        }
        return null;
    }

    private boolean isAnimationElementName(String local) {
        return "animate".equals(local)
                || "animateTransform".equals(local)
                || "animateMotion".equals(local)
                || "set".equals(local);
    }

    private SMILTrack trackFromAnimationElement(Element e) {
        if (e == null) return null;
        String local = localName(e);
        if ("animate".equals(local)) {
            String attr = e.getAttribute("attributeName");
            return new SMILTrack(e,
                    attr == null || attr.isEmpty() ? "animate" : attr,
                    "animate");
        }
        if ("animateTransform".equals(local)) {
            String type = e.getAttribute("type");
            return new SMILTrack(e,
                    type == null || type.isEmpty() ? "transform" : type,
                    "animateTransform");
        }
        if ("animateMotion".equals(local)) {
            return new SMILTrack(e, "Motion Path", "animateMotion");
        }
        if ("set".equals(local)) {
            String attr = e.getAttribute("attributeName");
            return new SMILTrack(e, "Set " + attr, "set");
        }
        return null;
    }

    private float resolveBeginSeconds(SMILTrack track) {
        return resolveBeginSeconds(
                track, new java.util.HashSet<Element>());
    }

    private float resolveBeginSeconds(
            SMILTrack track, java.util.Set<Element> visiting) {
        if (track == null) return Float.NaN;

        Element anim = track.getAnimationElement();
        if (anim != null && !visiting.add(anim)) return Float.NaN;

        List<Float> candidates = new ArrayList<Float>();
        String beginRaw = track.getBeginRaw();

        for (String expr : splitTimingList(beginRaw)) {
            candidates.addAll(resolveTimingCandidates(
                    expr, track, visiting, false));
        }

        if (anim != null) visiting.remove(anim);
        if (candidates.isEmpty()) return Float.NaN;

        java.util.Collections.sort(candidates);
        return chooseRestartInstance(track, candidates);
    }

    private List<Float> resolveTimingCandidates(
            String expr,
            SMILTrack owner,
            java.util.Set<Element> visiting,
            boolean endExpression) {

        List<Float> out = new ArrayList<Float>();
        if (expr == null) return out;
        String raw = expr.trim();
        if (raw.isEmpty() || "indefinite".equals(raw)) return out;

        Float clock = parseClockSeconds(raw);
        if (clock != null) {
            out.add(Math.max(0f, clock.floatValue()));
            return out;
        }

        Matcher event = eventExpression(raw);
        if (event.matches()) {
            String eventName = event.group(2);
            String offRaw = event.group(3);
            float off = 0f;
            if (offRaw != null) {
                Float parsed = parseClockSeconds(offRaw);
                if (parsed == null) return out;
                off = parsed.floatValue();
            }

            List<Float> history = eventTriggerHistory.get(eventName);
            if (history != null) {
                for (Float fired : history) {
                    float v = fired.floatValue() + off;
                    if (v >= 0f) out.add(v);
                }
            }
            return out;
        }

        Matcher sync = syncbaseExpression(raw);
        if (sync.matches()) {
            SMILTrack ref = findTrackById(sync.group(1));
            if (ref == null || ref == owner) return out;

            float base = resolveBeginSeconds(ref, visiting);
            if (Float.isNaN(base)) return out;

            if ("end".equals(sync.group(2))) {
                float active = computeActiveDuration(
                        ref, ref.getDurationSeconds(), base);
                if (Float.isInfinite(active)) return out;
                base += active;
            }

            String offRaw = sync.group(3);
            if (offRaw != null) {
                Float offset = parseClockSeconds(offRaw);
                if (offset == null) return out;
                base += offset.floatValue();
            }
            if (base >= 0f) out.add(base);
        }

        return out;
    }

    private float chooseRestartInstance(
            SMILTrack track, List<Float> candidates) {
        if (candidates == null || candidates.isEmpty()) return Float.NaN;

        List<Float> occurred = new ArrayList<Float>();
        Float firstFuture = null;
        for (Float c : candidates) {
            if (c.floatValue() <= timingEvaluationSeconds + 0.00001f) {
                occurred.add(c);
            } else if (firstFuture == null) {
                firstFuture = c;
            }
        }

        if (occurred.isEmpty()) {
            return firstFuture == null ? Float.NaN : firstFuture.floatValue();
        }

        String restart = track.getRestart();
        if ("never".equals(restart)) {
            return occurred.get(0).floatValue();
        }

        if ("whenNotActive".equals(restart)) {
            float accepted = occurred.get(0).floatValue();
            float active = computeIntrinsicActiveDuration(
                    track, track.getDurationSeconds());

            for (int i=1; i<occurred.size(); i++) {
                float c = occurred.get(i).floatValue();
                if (Float.isInfinite(active)) break;
                if (c + 0.00001f >= accepted + active) accepted = c;
            }
            return accepted;
        }

        // SMIL restart="always" default.
        return occurred.get(occurred.size()-1).floatValue();
    }

    private float computeIntrinsicActiveDuration(
            SMILTrack track, float dur) {
        float active;
        String repeatRaw = track.getRepeatCount();
        if ("indefinite".equals(repeatRaw)) {
            active = Float.POSITIVE_INFINITY;
        } else {
            float repeats = 1f;
            try {
                repeats = Math.max(0.0001f, Float.parseFloat(repeatRaw));
            } catch (RuntimeException ex) {
                repeats = 1f;
            }
            active = dur * repeats;
        }

        String repeatDur = track.getRepeatDur();
        if (!repeatDur.isEmpty() && !"indefinite".equals(repeatDur)) {
            Float rd = parseClockSeconds(repeatDur);
            if (rd != null) {
                active = Math.min(active,
                        Math.max(0f, rd.floatValue()));
            }
        }
        return active;
    }

    private float computeActiveDuration(
            SMILTrack track, float dur, float begin) {
        float active = computeIntrinsicActiveDuration(track, dur);

        String endRaw = track.getEndRaw();
        if (endRaw == null || endRaw.trim().isEmpty()
                || "indefinite".equals(endRaw.trim())) return active;

        float earliestAbsoluteEnd = Float.POSITIVE_INFINITY;
        java.util.Set<Element> visiting = new java.util.HashSet<Element>();
        if (track.getAnimationElement() != null) {
            visiting.add(track.getAnimationElement());
        }

        for (String expr : splitTimingList(endRaw)) {
            for (Float end : resolveTimingCandidates(
                    expr, track, visiting, true)) {
                float absolute = end.floatValue();
                if (absolute + 0.00001f >= begin
                        && absolute < earliestAbsoluteEnd) {
                    earliestAbsoluteEnd = absolute;
                }
            }
        }

        if (!Float.isInfinite(earliestAbsoluteEnd)) {
            active = Math.min(active,
                    Math.max(0f, earliestAbsoluteEnd - begin));
        }
        return active;
    }

    private String evaluateSetTrack(SMILTrack track, float seconds) {
        float begin = resolveBeginSeconds(track);
        if (Float.isNaN(begin)) return null;
        float local = seconds - begin;
        if (local < 0f) return null;

        float dur = track.getDurationSeconds();
        float activeDuration = computeActiveDuration(track, dur, begin);
        if (local < activeDuration) return track.getSetValue();

        if ("freeze".equals(track.getFillMode())) {
            return track.getSetValue();
        }
        return null;
    }

    private String evaluateTrack(SMILTrack track, float seconds) {
        List<String> values = new ArrayList<String>(track.getValues());

        /*
         * Native SVG commonly uses by="" without from/to/values, especially
         * animateTransform type="rotate" (for example by="360").
         * The old importer returned no values at all, so local preview had
         * nothing to evaluate and the animation stayed still.
         *
         * For rotate, the underlying local rotation represented by this track
         * starts at 0 and advances by the requested delta while the element's
         * authored base transform (e.g. translate(150,150) rotate(0)) remains
         * in previewBaseTransform.
         *
         * For other numeric tracks, use the target's underlying numeric
         * attribute as the start value where available.
         */
        if (values.isEmpty()) {
            String byRaw = track.getByRaw();
            if (byRaw != null && !byRaw.trim().isEmpty()) {
                try {
                    float by = Float.parseFloat(byRaw.trim());
                    float start = 0f;
                    if (!"rotate".equals(track.getName())) {
                        Element target = selectedSVGElement();
                        if (target != null) {
                            start = (float)parseNumericAttribute(
                                    target, track.getName(), 0d);
                        }
                    }
                    values.add(SMILTrack.trim(start));
                    values.add(SMILTrack.trim(start + by));
                } catch (RuntimeException ex) {
                    // Unsupported non-numeric by= remains non-evaluable locally.
                }
            }
        }

        List<Float> times = track.getKeyTimes();
        if (times.size() != values.size() && values.size() == 2
                && (times.size() == 0 || times.size() == 1)) {
            times = new ArrayList<Float>();
            times.add(0f);
            times.add(1f);
        }

        int count = Math.min(times.size(), values.size());
        if (count == 0) return null;
        if (count == 1) return values.get(0);

        float dur = track.getDurationSeconds();
        float begin = resolveBeginSeconds(track);
        if (Float.isNaN(begin)) return null;
        float local = seconds - begin;

        // Before begin, the animation contributes nothing.
        if (local < 0f) return null;

        float activeDuration = computeActiveDuration(track, dur, begin);
        if (local >= activeDuration
                && !"freeze".equals(track.getFillMode())) {
            return null;
        }

        float normalized = normalizedTrackTime(track, seconds);
        if (Float.isNaN(normalized)) return null;
        if (normalized <= times.get(0)) return values.get(0);
        if (normalized >= times.get(count - 1)) return values.get(count - 1);

        int left = 0;
        for (int i=0; i<count-1; i++) {
            if (normalized >= times.get(i) && normalized <= times.get(i+1)) {
                left = i;
                break;
            }
        }

        float a = times.get(left);
        float b = times.get(left+1);
        float t = (b-a) == 0f ? 0f : (normalized-a)/(b-a);

        String calcMode = track.getCalcMode();
        if ("discrete".equals(calcMode)) {
            t = 0f;
        } else if ("spline".equals(calcMode)) {
            t = applySpline(track.getKeySplines(), left, t);
        }

        String v0 = values.get(left);
        String v1 = values.get(left+1);

        if ("fill".equals(track.getName())) {
            String c = interpolateColor(v0, v1, t);
            return c != null ? c : (t < 0.5f ? v0 : v1);
        }

        try {
            float n0 = Float.parseFloat(v0.trim());
            float n1 = Float.parseFloat(v1.trim());
            return SMILTrack.trim(n0 + (n1-n0)*t);
        } catch (RuntimeException ex) {
            return t < 0.5f ? v0 : v1;
        }
    }

    private float applySpline(String keySplines, int interval, float x) {
        if (keySplines == null || keySplines.trim().isEmpty()) return x;
        String[] groups = keySplines.split(";");
        if (interval < 0 || interval >= groups.length) return x;
        String[] n = groups[interval].trim().split("\\s+");
        if (n.length != 4) return x;

        try {
            float x1 = Float.parseFloat(n[0]);
            float y1 = Float.parseFloat(n[1]);
            float x2 = Float.parseFloat(n[2]);
            float y2 = Float.parseFloat(n[3]);

            // Robust binary inversion of cubic-bezier X(t), then evaluate Y(t).
            float lo = 0f, hi = 1f, u = x;
            for (int i=0; i<18; i++) {
                u = (lo + hi) * 0.5f;
                float bx = cubicBezier(u, 0f, x1, x2, 1f);
                if (bx < x) lo = u;
                else hi = u;
            }
            return cubicBezier(u, 0f, y1, y2, 1f);
        } catch (RuntimeException ex) {
            return x;
        }
    }

    private float cubicBezier(float t, float p0, float p1, float p2, float p3) {
        float u = 1f - t;
        return u*u*u*p0 + 3f*u*u*t*p1 + 3f*u*t*t*p2 + t*t*t*p3;
    }

    private String interpolateColor(String a, String b, float t) {
        Paint pa = parseColorPaint(a);
        Paint pb = parseColorPaint(b);
        if (!(pa instanceof Color) || !(pb instanceof Color)) return null;
        Color ca = (Color)pa;
        Color cb = (Color)pb;
        int r = Math.round(ca.getRed() + (cb.getRed()-ca.getRed())*t);
        int g = Math.round(ca.getGreen() + (cb.getGreen()-ca.getGreen())*t);
        int bl = Math.round(ca.getBlue() + (cb.getBlue()-ca.getBlue())*t);
        int al = Math.round(ca.getAlpha() + (cb.getAlpha()-ca.getAlpha())*t);
        if (al < 255) return String.format("#%02x%02x%02x%02x", r, g, bl, al);
        return String.format("#%02x%02x%02x", r, g, bl);
    }

    private int[] parseHexColor(String value) {
        if (value == null) return null;
        String s = value.trim();
        if (!s.startsWith("#")) return null;
        s = s.substring(1);
        try {
            if (s.length() == 3) {
                return new int[]{
                    Integer.parseInt(s.substring(0,1)+s.substring(0,1),16),
                    Integer.parseInt(s.substring(1,2)+s.substring(1,2),16),
                    Integer.parseInt(s.substring(2,3)+s.substring(2,3),16)
                };
            }
            if (s.length() == 6) {
                return new int[]{
                    Integer.parseInt(s.substring(0,2),16),
                    Integer.parseInt(s.substring(2,4),16),
                    Integer.parseInt(s.substring(4,6),16)
                };
            }
        } catch (RuntimeException ex) { return null; }
        return null;
    }

    private String trimDouble(double v) {
        return SMILTrack.trim((float)v);
    }

    private void setPreviewStatus(final String text) {
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            previewStatusLabel.setText("Preview: " + text);
        } else {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    previewStatusLabel.setText("Preview: " + text);
                }
            });
        }
    }

    private static final class FillPaintState {
        final FillShapePainter painter;
        final Paint paint;
        FillPaintState(FillShapePainter painter, Paint paint) {
            this.painter = painter;
            this.paint = paint;
        }
    }

    private void startPlayback() {
        if (canvas == null) return;

        /*
         * The visible timeline was historically based on simple dur only.
         * Finite repeatCount/repeatDur animations therefore got cut off by
         * Play at the end of the first cycle. Extend the playback range to the
         * document-wide finite active end before starting.
         */
        ensureFinitePlaybackRange();

        paused = false;
        playbackStartTime = sliderSeconds();
        playbackStartNanos = System.nanoTime();
        lastEventCanvasRefreshNanos = 0L;
        playButton.setText("❚❚");
        playbackTimer.start();
    }

    private void pausePlayback() {
        paused = true;
        playbackTimer.stop();
        playButton.setText("▶");
    }

    private float documentFinitePlaybackEndSeconds() {
        if (canvas == null) return timeSlider.getMaximum() / (float)TIME_SCALE;
        try {
            org.w3c.dom.svg.SVGDocument doc = canvas.getSVGDocument();
            if (doc == null || doc.getDocumentElement() == null) {
                return timeSlider.getMaximum() / (float)TIME_SCALE;
            }
            return documentFinitePlaybackEndSecondsRecursive(
                    doc.getDocumentElement(), 0f);
        } catch (RuntimeException ex) {
            return timeSlider.getMaximum() / (float)TIME_SCALE;
        }
    }

    private float documentFinitePlaybackEndSecondsRecursive(
            Element element, float currentMax) {
        if (element == null) return currentMax;

        String local = localName(element);
        if (isAnimationElementName(local)) {
            SMILTrack t = new SMILTrack(
                    element,
                    "animateTransform".equals(local)
                            ? element.getAttribute("type")
                            : element.getAttribute("attributeName"),
                    local);

            float begin = resolveBeginSeconds(t);
            if (!Float.isNaN(begin)) {
                float dur = t.getDurationSeconds();
                float active = computeActiveDuration(t, dur, begin);
                if (!Float.isInfinite(active)) {
                    currentMax = Math.max(currentMax, begin + active);
                }
            }
        }

        org.w3c.dom.NodeList children = element.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (n instanceof Element) {
                currentMax = documentFinitePlaybackEndSecondsRecursive(
                        (Element)n, currentMax);
            }
        }
        return currentMax;
    }

    private void ensureFinitePlaybackRange() {
        float needed = documentFinitePlaybackEndSeconds();
        if (!(needed > 0f) || Float.isInfinite(needed)) return;

        int desired = Math.max(
                timeline.getMaximum(),
                (int)Math.ceil(needed));
        desired = Math.min(86400, desired);

        if (desired > timeline.getMaximum()) {
            timeline.setMaximum(desired);
            timeSlider.setMaximum(desired * TIME_SCALE);
        }
    }

    private boolean hasIndefiniteRepeatTracks() {
        /*
         * Playback is document-wide now, so the stop/extend decision must also
         * be document-wide. The previous implementation inspected only tracks
         * of the currently selected target.
         */
        if (canvas != null) {
            try {
                org.w3c.dom.svg.SVGDocument doc = canvas.getSVGDocument();
                if (doc != null && doc.getDocumentElement() != null
                        && hasIndefiniteRepeatInSubtree(doc.getDocumentElement())) {
                    return true;
                }
            } catch (RuntimeException ex) {
                // Fall back to the selected timeline below.
            }
        }

        for (SMILTrack t : timeline.getTimelineModel().getTracks()) {
            if ("indefinite".equals(t.getRepeatCount())) return true;
        }
        return false;
    }

    private boolean hasIndefiniteRepeatInSubtree(Element element) {
        if (element == null) return false;

        String local = localName(element);
        if (("animate".equals(local)
                || "animateTransform".equals(local)
                || "animateMotion".equals(local)
                || "set".equals(local))
                && "indefinite".equals(element.getAttribute("repeatCount"))) {
            return true;
        }

        org.w3c.dom.NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (n instanceof Element
                    && hasIndefiniteRepeatInSubtree((Element)n)) {
                return true;
            }
        }
        return false;
    }

    private void extendTimelineForPlayback(float currentSeconds) {
        int max = timeline.getMaximum();
        if (currentSeconds < max) return;

        /*
         * The timeline maximum is a viewing range, not the active duration of
         * an indefinite SMIL animation. Grow the visible range while playback
         * advances instead of stopping at the right edge.
         */
        int next = max;
        while (currentSeconds >= next && next < 86400) {
            next = Math.min(86400, Math.max(next + 1, next * 2));
        }
        if (next != max) {
            timeline.setMaximum(next);
            timeSlider.setMaximum(next * TIME_SCALE);
        }
    }

    private void updatePlaybackFrame() {
        if (canvas == null) { pausePlayback(); return; }
        float elapsed = (System.nanoTime() - playbackStartNanos) / 1_000_000_000f;
        float current = playbackStartTime + elapsed;
        float max = timeSlider.getMaximum() / (float)TIME_SCALE;

        if (current >= max) {
            if (hasIndefiniteRepeatTracks()) {
                /*
                 * repeatCount="indefinite" must not be stopped by the UI
                 * timeline boundary. Keep absolute SMIL time increasing so
                 * tracks with different periods (60s / 60min / 12h) stay in
                 * phase, and expand the visible timeline as needed.
                 */
                extendTimelineForPlayback(current);
            } else {
                current = max;
                pausePlayback();
            }
        }

        setCurrentTime(current);
        setSliderSeconds(current);
    }

    private Element findFirstAnimatedTarget() {
        if (canvas == null) return null;

        try {
            org.w3c.dom.svg.SVGDocument doc = canvas.getSVGDocument();
            if (doc == null) return null;

            org.w3c.dom.Element root = doc.getDocumentElement();
            if (root == null) return null;

            return findFirstAnimatedTargetRecursive(root);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean hasDirectAnimationChild(Element element) {
        if (element == null) return false;
        org.w3c.dom.NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (!(n instanceof Element)) continue;
            Element child = (Element)n;
            String local = child.getLocalName();
            if (local == null || local.isEmpty()) local = child.getTagName();
            if ("animate".equals(local)
                    || "animateTransform".equals(local)
                    || "animateMotion".equals(local)
                    || "set".equals(local)) {
                return true;
            }
        }
        return false;
    }

    private void collectAnimatedTargetsRecursive(
            Element element, java.util.List<Element> out, int limit) {
        if (element == null || out == null || out.size() >= limit) return;
        if (hasDirectAnimationChild(element)) {
            out.add(element);
            if (out.size() >= limit) return;
        }
        org.w3c.dom.NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength() && out.size() < limit; i++) {
            org.w3c.dom.Node n = children.item(i);
            if (n instanceof Element) {
                collectAnimatedTargetsRecursive((Element)n, out, limit);
            }
        }
    }

    private Element resolveUniqueAnimatedDescendant(Element selected) {
        if (selected == null || hasDirectAnimationChild(selected)) return selected;
        java.util.List<Element> found = new java.util.ArrayList<Element>();
        collectAnimatedTargetsRecursive(selected, found, 2);
        return found.size() == 1 ? found.get(0) : selected;
    }

    private Element findFirstAnimatedTargetRecursive(Element element) {
        if (element == null) return null;

        org.w3c.dom.NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (!(n instanceof Element)) continue;

            Element child = (Element)n;
            String local = child.getLocalName();
            if (local == null || local.isEmpty()) local = child.getTagName();

            if ("animate".equals(local)
                    || "animateTransform".equals(local)
                    || "animateMotion".equals(local)
                    || "set".equals(local)) {
                return element;
            }
        }

        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (!(n instanceof Element)) continue;
            Element found = findFirstAnimatedTargetRecursive((Element)n);
            if (found != null) return found;
        }

        return null;
    }

    private void bindInitialAnimatedTargetIfNeeded() {
        if (canvas == null) return;
        if (timeline.getSVGElement() != null) return;

        Element target = findFirstAnimatedTarget();
        if (!(target instanceof SVGElement)) return;

        /*
         * Sketsa can emit an empty selection immediately after opening a file,
         * which previously left Animation Editor with no bound target until
         * the user clicked the element again in DOM Editor.
         *
         * Bind the first unambiguous animated SVG element so its tracks are
         * available immediately. A later real canvas/DOM selection still
         * replaces this normally through updateTimeline().
         */
        timeline.setSVGElement((SVGElement)target);
        refreshInspector();
        updateButtons();
    }

    void refreshAfterUndoRedo() {
        if (canvas == null) return;

        final float stableSeconds = sliderSeconds();

        /*
         * Undo/Redo can rebuild animation DOM nodes and temporarily desync the
         * timeline selection/inspector from the actual document state while
         * focus remains inside the Animation window. Re-bind from the current
         * canvas selection, fall back to the first animated target if needed,
         * then re-apply the preview time on the next Swing turn.
         */
        restorePreviewBase();
        updateTimeline(canvas.getCanvasSelection().getSelectionList());
        bindInitialAnimatedTargetIfNeeded();

        javax.swing.SwingUtilities.invokeLater(() -> {
            if (canvas == null) return;
            updateTimeline(canvas.getCanvasSelection().getSelectionList());
            bindInitialAnimatedTargetIfNeeded();
            setCurrentTime(stableSeconds);
            timeline.repaint();
            refreshInspector();
            updateButtons();
        });
    }

    public void setVectorCanvas(VectorCanvas newCanvas) {
        pausePlayback();
        scrubTimer.stop();
        restorePreviewBase();

        VectorCanvas old = canvas;
        if (old != null) {
            old.getCanvasSelection().removeSelectionListener(canvasSelectionHandler);
            old.removePropertyChangeListener(VectorCanvas.DOCUMENT_PROPERTY, canvasPropertyChangeListener);
            try {
                Toolkit.getDefaultToolkit().removeAWTEventListener(canvasEventListener);
            } catch (RuntimeException ex) { }
        }

        canvas = newCanvas;
        timeline.setSVGElement(null);
        documentBaseTransformOps.clear();
        documentBaseVisibility.clear();
        documentBaseShapes.clear();
        documentVisibilityBootstrapped.clear();
        documentBaseComposites.clear();
        documentMotionAnchors.clear();

        if (canvas == null) {
            updateButtons();
            return;
        }

        canvas.addPropertyChangeListener(VectorCanvas.DOCUMENT_PROPERTY, canvasPropertyChangeListener);
        SVGAnimationEngine eng = canvas.getAnimationEngine();
        if (!eng.hasStarted()) eng.start(0);
        eng.setCurrentTime(0);

        setSliderSeconds(0f);

        CanvasSelection sel = canvas.getCanvasSelection();
        sel.addSelectionListener(canvasSelectionHandler);
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(canvasEventListener);
            Toolkit.getDefaultToolkit().addAWTEventListener(
                    canvasEventListener,
                    AWTEvent.MOUSE_EVENT_MASK);
        } catch (RuntimeException ex) { }
        eventTriggerHistory.clear();
        updateTimeline(sel.getSelectionList());
        bindInitialAnimatedTargetIfNeeded();
        updateButtons();
    }

    private void triggerPreviewEvent(String eventName) {
        if (canvas == null || eventName == null) return;
        if (!hasEventTimedTracks()) return;

        List<Float> history = eventTriggerHistory.get(eventName);
        if (history == null) {
            history = new ArrayList<Float>();
            eventTriggerHistory.put(eventName, history);
        }

        float now = sliderSeconds();
        history.add(Float.valueOf(now));
        timingEvaluationSeconds = now;

        /*
         * Event timing is document-wide. Re-evaluate both the selected target
         * and every other animated target; do not refresh Batik here because
         * native SMIL event state can otherwise override the editor runtime.
         */
        applyEditorPreview(now);
        applyDocumentSMILPreview(now);
        canvas.refresh();
        stopNativeEventAnimationsDocumentWide();
        applyDocumentSMILPreview(now);
        lastEventCanvasRefreshNanos = System.nanoTime();
        canvas.repaint();
    }

    private void restoreTrackSelection(Element target, Element animationElement) {
        if (target == null || !(target instanceof SVGElement)) return;
        timeline.setSVGElement((SVGElement)target);
        List<SMILTrack> tracks = timeline.getTimelineModel().getTracks();
        for (int i=0; i<tracks.size(); i++) {
            if (tracks.get(i).getAnimationElement() == animationElement) {
                timeline.setRowSelectionInterval(i + 1, i + 1);
                timeline.setSelectedKeyIndex(-1);
                break;
            }
        }
        refreshInspector();
        updateButtons();
    }

    protected void updateTimeline(List<SVGElement> selectionList) {
        Element next = null;
        if (selectionList != null && selectionList.size() == 1
                && selectionList.get(0) instanceof Element) {
            next = resolveUniqueAnimatedDescendant(
                    (Element)selectionList.get(0));
        }

        Element previous = selectedSVGElement();
        boolean realSelectionChange =
                next != null && previous != next;

        if (previewTarget != null && previewTarget != next) {
            restorePreviewBase();
        }

        if (selectionList == null || selectionList.isEmpty()) {
            if (!preserveTimelineSelection) {
                /*
                 * Do not discard a valid SMIL target merely because Sketsa
                 * emitted a transient empty CanvasSelection during DOM refresh.
                 */
                if (timeline.getTimelineModel().getTracks().isEmpty()) {
                    timeline.setSVGElement(null);
                    bindInitialAnimatedTargetIfNeeded();
                }
            }
        } else if (selectionList.size() == 1) {
            SVGElement selected = selectionList.get(0);
            if (selected instanceof Element) {
                Element resolved = resolveUniqueAnimatedDescendant(
                        (Element)selected);
                if (resolved instanceof SVGElement) {
                    timeline.setSVGElement((SVGElement)resolved);
                } else {
                    timeline.setSVGElement(selected);
                }
            } else {
                timeline.setSVGElement(selected);
            }
        }

        refreshInspector();
        updateButtons();

        /*
         * Keep Preview diagnostics synchronized with the newly selected DOM/
         * canvas object.
         *
         * Before 1.5.7 the timeline could correctly switch from (for example)
         * the purple circle to the orange <g>, while the Preview status line
         * still showed the previous object's cached t/x/y values until another
         * scrub/play callback happened. The screenshot that exposed this had:
         *
         *   timeline time = 4.71s
         *   selected object = <g>
         *   Preview = node=ShapeNode | t=0s | x=70 | y=260
         *
         * i.e. a stale status from the prior circle, not the orange group's
         * coordinates.
         *
         * Re-evaluate only on a genuine non-empty object change. This avoids
         * reacting to the transient empty selections Sketsa emits during
         * canvas.refresh().
         */
        if (realSelectionChange && canvas != null) {
            applyEditorPreview(sliderSeconds());
            canvas.repaint();
        }
    }

    private void updateButtons() {
        boolean hasElement = canvas != null && timeline.getSVGElement() != null;
        boolean hasAuthoringSelection =
                canvas != null && !authoringTargets().isEmpty();
        SMILTrack selected = selectedTrack();
        boolean keyframedTrack = selected != null && !selected.isMotionTrack() && !selected.isSetTrack();
        playButton.setEnabled(canvas != null);
        addTrackButton.setEnabled(hasElement || hasAuthoringSelection);
        addKeyButton.setEnabled(keyframedTrack);
        deleteTrackButton.setEnabled(selected != null);
        deleteKeyButton.setEnabled(keyframedTrack && timeline.getSelectedKeyIndex() >= 0);
    }

    private final class CanvasSelectionHandler extends CanvasSelectionAdapter {
        @Override public void valueChanged(CanvasSelectionEvent evt) {
            updateTimeline(evt.getSelectionList());
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
