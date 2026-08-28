package kiyut.sketsa.modules.physics.integration;

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
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
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

final class PhysicsPanel extends JPanel {
    private static final String ATTR_BODY = "data-sketsa-physics-body";
    private static final String ATTR_SHAPE = "data-sketsa-physics-shape";
    private static final String ATTR_DENSITY = "data-sketsa-physics-density";
    private static final String ATTR_MASS = "data-sketsa-physics-mass";
    private static final String ATTR_FRICTION = "data-sketsa-physics-friction";
    private static final String ATTR_FRICTION_STATIC = "data-sketsa-physics-friction-static";
    private static final String ATTR_FRICTION_AIR = "data-sketsa-physics-friction-air";
    private static final String ATTR_RESTITUTION = "data-sketsa-physics-restitution";
    private static final String ATTR_ANGLE = "data-sketsa-physics-angle";
    private static final String ATTR_VELOCITY_X = "data-sketsa-physics-velocity-x";
    private static final String ATTR_VELOCITY_Y = "data-sketsa-physics-velocity-y";
    private static final String ATTR_ANGULAR_VELOCITY = "data-sketsa-physics-angular-velocity";
    private static final String ATTR_SENSOR = "data-sketsa-physics-sensor";
    private static final String ATTR_FORCE_X = "data-sketsa-physics-force-x";
    private static final String ATTR_FORCE_Y = "data-sketsa-physics-force-y";
    private static final String ATTR_IMPULSE_X = "data-sketsa-physics-impulse-x";
    private static final String ATTR_IMPULSE_Y = "data-sketsa-physics-impulse-y";
    private static final String ATTR_TORQUE = "data-sketsa-physics-torque";
    private static final String ATTR_SLEEPING = "data-sketsa-physics-sleeping";
    private static final String ATTR_CONSTRAINT_TYPE = "data-sketsa-physics-constraint";
    private static final String ATTR_CONSTRAINT_TARGET = "data-sketsa-physics-constraint-target";
    private static final String ATTR_CONSTRAINT_POINT_A_X = "data-sketsa-physics-constraint-point-a-x";
    private static final String ATTR_CONSTRAINT_POINT_A_Y = "data-sketsa-physics-constraint-point-a-y";
    private static final String ATTR_CONSTRAINT_POINT_B_X = "data-sketsa-physics-constraint-point-b-x";
    private static final String ATTR_CONSTRAINT_POINT_B_Y = "data-sketsa-physics-constraint-point-b-y";
    private static final String ATTR_CONSTRAINT_LENGTH = "data-sketsa-physics-constraint-length";
    private static final String ATTR_CONSTRAINT_STIFFNESS = "data-sketsa-physics-constraint-stiffness";
    private static final String ATTR_CONSTRAINT_DAMPING = "data-sketsa-physics-constraint-damping";
    private static final String ATTR_COLLISION_CATEGORY = "data-sketsa-physics-collision-category";
    private static final String ATTR_COLLISION_MASK = "data-sketsa-physics-collision-mask";
    private static final String ATTR_COLLISION_GROUP = "data-sketsa-physics-collision-group";
    private static final String ATTR_EVENT_ID = "data-sketsa-physics-event-id";
    private static final String ATTR_EVENT_START = "data-sketsa-physics-event-start";
    private static final String ATTR_EVENT_ACTIVE = "data-sketsa-physics-event-active";
    private static final String ATTR_EVENT_END = "data-sketsa-physics-event-end";

    private static final String ATTR_GRAVITY_X = "data-sketsa-physics-gravity-x";
    private static final String ATTR_GRAVITY_Y = "data-sketsa-physics-gravity-y";
    private static final String ATTR_GRAVITY_SCALE = "data-sketsa-physics-gravity-scale";
    private static final String ATTR_TIME_SCALE = "data-sketsa-physics-time-scale";
    private static final String ATTR_ENABLE_SLEEPING = "data-sketsa-physics-enable-sleeping";

    private final JComboBox<String> bodyType = new JComboBox<>(new String[]{"dynamic", "static"});
    private final JComboBox<String> shape = new JComboBox<>(new String[]{"auto", "rectangle", "circle", "polygon"});
    private final JSpinner density = new JSpinner(new SpinnerNumberModel(0.001, 0.000001, 1000.0, 0.001));
    private final JSpinner mass = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1000000.0, 0.1));
    private final JSpinner friction = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 1.0, 0.05));
    private final JSpinner frictionStatic = new JSpinner(new SpinnerNumberModel(0.5, 0.0, 10.0, 0.05));
    private final JSpinner frictionAir = new JSpinner(new SpinnerNumberModel(0.01, 0.0, 1.0, 0.01));
    private final JSpinner restitution = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0, 0.05));
    private final JSpinner angle = new JSpinner(new SpinnerNumberModel(0.0, -360000.0, 360000.0, 1.0));
    private final JSpinner velocityX = new JSpinner(new SpinnerNumberModel(0.0, -100000.0, 100000.0, 0.5));
    private final JSpinner velocityY = new JSpinner(new SpinnerNumberModel(0.0, -100000.0, 100000.0, 0.5));
    private final JSpinner angularVelocity = new JSpinner(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.01));
    private final JCheckBox sensor = new JCheckBox("Sensor");
    private final JSpinner forceX = new JSpinner(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.001));
    private final JSpinner forceY = new JSpinner(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.001));
    private final JSpinner impulseX = new JSpinner(new SpinnerNumberModel(0.0, -100000.0, 100000.0, 0.1));
    private final JSpinner impulseY = new JSpinner(new SpinnerNumberModel(0.0, -100000.0, 100000.0, 0.1));
    private final JSpinner torque = new JSpinner(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.001));
    private final JCheckBox sleeping = new JCheckBox("Start sleeping");
    private final JComboBox<String> constraintType = new JComboBox<>(new String[]{"none", "distance", "pin"});
    private final JTextField constraintTarget = new JTextField();
    private final JSpinner constraintPointAX = new JSpinner(new SpinnerNumberModel(0.0, -100000.0, 100000.0, 1.0));
    private final JSpinner constraintPointAY = new JSpinner(new SpinnerNumberModel(0.0, -100000.0, 100000.0, 1.0));
    private final JSpinner constraintPointBX = new JSpinner(new SpinnerNumberModel(0.0, -100000.0, 100000.0, 1.0));
    private final JSpinner constraintPointBY = new JSpinner(new SpinnerNumberModel(0.0, -100000.0, 100000.0, 1.0));
    private final JSpinner constraintLength = new JSpinner(new SpinnerNumberModel(-1.0, -1.0, 1000000.0, 1.0));
    private final JSpinner constraintStiffness = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 1.0, 0.05));
    private final JSpinner constraintDamping = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0, 0.05));
    private final JSpinner collisionCategory = new JSpinner(new SpinnerNumberModel(1L, 1L, 4294967295L, 1L));
    private final JSpinner collisionMask = new JSpinner(new SpinnerNumberModel(4294967295L, 0L, 4294967295L, 1L));
    private final JSpinner collisionGroup = new JSpinner(new SpinnerNumberModel(0L, -2147483648L, 2147483647L, 1L));
    private final JTextField eventId = new JTextField();
    private final JCheckBox eventStart = new JCheckBox("Publish collisionStart");
    private final JCheckBox eventActive = new JCheckBox("Publish collisionActive");
    private final JCheckBox eventEnd = new JCheckBox("Publish collisionEnd");

    private final JSpinner gravityX = new JSpinner(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.1));
    private final JSpinner gravityY = new JSpinner(new SpinnerNumberModel(1.0, -1000.0, 1000.0, 0.1));
    private final JSpinner gravityScale = new JSpinner(new SpinnerNumberModel(0.001, 0.0, 10.0, 0.001));
    private final JSpinner timeScale = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 10.0, 0.1));
    private final JCheckBox enableSleeping = new JCheckBox("Enable sleeping");

    private final JButton applyButton = new JButton("Apply Physics");
    private final JButton removeButton = new JButton("Remove Physics");
    private final JButton applyWorldButton = new JButton("Apply World");
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

    PhysicsPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Physics / Matter.js"));
        setPreferredSize(new Dimension(10, 330));
        setMinimumSize(new Dimension(10, 180));

        form.setBorder(BorderFactory.createEmptyBorder(2, 6, 5, 6));

        JScrollPane scroll = new JScrollPane(
                form,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 3, 2, 3);
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        addSection(c, row++, "Body / Collider");
        addRow(c, row++, "Body:", bodyType);
        addRow(c, row++, "Collider:", shape);
        addRow(c, row++, "Density:", density);
        addRow(c, row++, "Mass (0=auto):", mass);
        addRow(c, row++, "Friction:", friction);
        addRow(c, row++, "Static friction:", frictionStatic);
        addRow(c, row++, "Air friction:", frictionAir);
        addRow(c, row++, "Restitution:", restitution);
        addRow(c, row++, "Angle (deg):", angle);
        addRow(c, row++, "Velocity X:", velocityX);
        addRow(c, row++, "Velocity Y:", velocityY);
        addRow(c, row++, "Angular vel.:", angularVelocity);
        addWide(c, row++, sensor);

        addSection(c, row++, "Initial Forces / State");
        addRow(c, row++, "Force X:", forceX);
        addRow(c, row++, "Force Y:", forceY);
        addRow(c, row++, "Impulse X:", impulseX);
        addRow(c, row++, "Impulse Y:", impulseY);
        addRow(c, row++, "Torque:", torque);
        addWide(c, row++, sleeping);

        addSection(c, row++, "Constraint");
        addRow(c, row++, "Type:", constraintType);
        addRow(c, row++, "Target SVG id:", constraintTarget);
        addRow(c, row++, "Point A X:", constraintPointAX);
        addRow(c, row++, "Point A Y:", constraintPointAY);
        addRow(c, row++, "Point B X:", constraintPointBX);
        addRow(c, row++, "Point B Y:", constraintPointBY);
        addRow(c, row++, "Length (-1=auto):", constraintLength);
        addRow(c, row++, "Stiffness:", constraintStiffness);
        addRow(c, row++, "Damping:", constraintDamping);

        addSection(c, row++, "Collision / Events");
        addRow(c, row++, "Category:", collisionCategory);
        addRow(c, row++, "Mask:", collisionMask);
        addRow(c, row++, "Group:", collisionGroup);
        addRow(c, row++, "Event id:", eventId);
        addWide(c, row++, eventStart);
        addWide(c, row++, eventActive);
        addWide(c, row++, eventEnd);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        actions.add(applyButton);
        actions.add(removeButton);
        addWide(c, row++, actions);

        addSection(c, row++, "Physics World");
        addRow(c, row++, "Gravity X:", gravityX);
        addRow(c, row++, "Gravity Y:", gravityY);
        addRow(c, row++, "Gravity scale:", gravityScale);
        addRow(c, row++, "Time scale:", timeScale);
        addWide(c, row++, enableSleeping);
        addWide(c, row++, applyWorldButton);
        addWide(c, row++, includeCompanions);
        addWide(c, row++, exportButton);
        addWide(c, row, status);

        applyButton.addActionListener(e -> applyPhysics());
        removeButton.addActionListener(e -> removePhysics());
        applyWorldButton.addActionListener(e -> applyWorld());
        exportButton.addActionListener(e -> exportRuntime());

        lookupListener = (LookupEvent e) -> updateCanvasFromLookup();
                SuggestionPopup.install(constraintTarget, this::availableSvgIds);
        SuggestionPopup.install(eventId, this::availablePhysicsEventIds);

lookupResult = Utilities.actionsGlobalContext().lookupResult(SVGEditorCookie.class);
        lookupResult.addLookupListener(lookupListener);
        updateCanvasFromLookup();
        updateEnabledState();
    }

    private void addSection(GridBagConstraints c, int row, String title) {
        JLabel label = new JLabel(title);
        label.setBorder(BorderFactory.createEmptyBorder(5, 0, 1, 0));
        addWide(c, row, label);
    }

    private void addWide(GridBagConstraints c, int row, java.awt.Component component) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(component, c);
    }

    private void addRow(GridBagConstraints c, int row, String label, java.awt.Component component) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        form.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(component, c);
    }

    private java.util.List<String> availableSvgIds() {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<String>();
        if (currentDocument != null) {
            org.w3c.dom.NodeList nodes = currentDocument.getElementsByTagName("*");
            for (int i=0;i<nodes.getLength();i++) if (nodes.item(i) instanceof Element) {
                String id=((Element)nodes.item(i)).getAttribute("id");
                if (id!=null && !id.trim().isEmpty()) out.add("#"+id.trim());
            }
        }
        return new java.util.ArrayList<String>(out);
    }

    private java.util.List<String> availablePhysicsEventIds() {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<String>();
        if (currentDocument != null) {
            org.w3c.dom.NodeList nodes = currentDocument.getElementsByTagName("*");
            for (int i=0;i<nodes.getLength();i++) if (nodes.item(i) instanceof Element) {
                String id=((Element)nodes.item(i)).getAttribute("data-sketsa-physics-event-id");
                if (id!=null && !id.trim().isEmpty()) out.add(id.trim());
            }
        }
        return new java.util.ArrayList<String>(out);
    }

    private void updateCanvasFromLookup() {
        Collection<? extends SVGEditorCookie> cookies = lookupResult.allInstances();
        if (cookies.isEmpty()) return;
        SVGEditorCookie cookie = cookies.iterator().next();
        if (cookie.isOpened()) setCanvas(cookie.getVectorCanvas());
    }

    private void setCanvas(VectorCanvas newCanvas) {
        if (canvas == newCanvas) {
            cacheSelection();
            return;
        }
        if (canvas != null) canvas.getCanvasSelection().removeSelectionListener(selectionHandler);
        canvas = newCanvas;
        selected = null;
        currentDocument = null;
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
            selected = null;
            updateEnabledState();
            return;
        }
        selected = selection.get(0);
        if (selected instanceof Element) {
            currentDocument = ((Element) selected).getOwnerDocument();
            loadWorldFields();
            loadFields((Element) selected);
        }
        updateEnabledState();
    }

    private void loadFields(Element e) {
        loading = true;
        try {
            String bt = e.getAttribute(ATTR_BODY);
            if (!bt.isEmpty()) bodyType.setSelectedItem(bt);
            String sh = e.getAttribute(ATTR_SHAPE);
            if (!sh.isEmpty()) shape.setSelectedItem(sh);
            setSpinner(density, e.getAttribute(ATTR_DENSITY));
            setSpinner(mass, e.getAttribute(ATTR_MASS));
            setSpinner(friction, e.getAttribute(ATTR_FRICTION));
            setSpinner(frictionStatic, e.getAttribute(ATTR_FRICTION_STATIC));
            setSpinner(frictionAir, e.getAttribute(ATTR_FRICTION_AIR));
            setSpinner(restitution, e.getAttribute(ATTR_RESTITUTION));
            setSpinner(angle, e.getAttribute(ATTR_ANGLE));
            setSpinner(velocityX, e.getAttribute(ATTR_VELOCITY_X));
            setSpinner(velocityY, e.getAttribute(ATTR_VELOCITY_Y));
            setSpinner(angularVelocity, e.getAttribute(ATTR_ANGULAR_VELOCITY));
            sensor.setSelected("true".equalsIgnoreCase(e.getAttribute(ATTR_SENSOR)));
            setSpinner(forceX, e.getAttribute(ATTR_FORCE_X));
            setSpinner(forceY, e.getAttribute(ATTR_FORCE_Y));
            setSpinner(impulseX, e.getAttribute(ATTR_IMPULSE_X));
            setSpinner(impulseY, e.getAttribute(ATTR_IMPULSE_Y));
            setSpinner(torque, e.getAttribute(ATTR_TORQUE));
            sleeping.setSelected("true".equalsIgnoreCase(e.getAttribute(ATTR_SLEEPING)));
            String ct = e.getAttribute(ATTR_CONSTRAINT_TYPE);
            constraintType.setSelectedItem(ct.isEmpty() ? "none" : ct);
            constraintTarget.setText(e.getAttribute(ATTR_CONSTRAINT_TARGET));
            setSpinner(constraintPointAX, e.getAttribute(ATTR_CONSTRAINT_POINT_A_X));
            setSpinner(constraintPointAY, e.getAttribute(ATTR_CONSTRAINT_POINT_A_Y));
            setSpinner(constraintPointBX, e.getAttribute(ATTR_CONSTRAINT_POINT_B_X));
            setSpinner(constraintPointBY, e.getAttribute(ATTR_CONSTRAINT_POINT_B_Y));
            setSpinner(constraintLength, e.getAttribute(ATTR_CONSTRAINT_LENGTH));
            setSpinner(constraintStiffness, e.getAttribute(ATTR_CONSTRAINT_STIFFNESS));
            setSpinner(constraintDamping, e.getAttribute(ATTR_CONSTRAINT_DAMPING));
            collisionCategory.setValue(1L);
            collisionMask.setValue(4294967295L);
            collisionGroup.setValue(0L);
            setLongSpinner(collisionCategory, e.getAttribute(ATTR_COLLISION_CATEGORY));
            setLongSpinner(collisionMask, e.getAttribute(ATTR_COLLISION_MASK));
            setLongSpinner(collisionGroup, e.getAttribute(ATTR_COLLISION_GROUP));
            eventId.setText(e.getAttribute(ATTR_EVENT_ID));
            eventStart.setSelected("true".equalsIgnoreCase(e.getAttribute(ATTR_EVENT_START)));
            eventActive.setSelected("true".equalsIgnoreCase(e.getAttribute(ATTR_EVENT_ACTIVE)));
            eventEnd.setSelected("true".equalsIgnoreCase(e.getAttribute(ATTR_EVENT_END)));
            status.setText(bt.isEmpty() ? "Selected object has no physics metadata." : "Physics metadata loaded.");
        } finally {
            loading = false;
        }
    }

    private void loadWorldFields() {
        if (currentDocument == null || currentDocument.getDocumentElement() == null) return;
        Element root = currentDocument.getDocumentElement();
        setSpinner(gravityX, root.getAttribute(ATTR_GRAVITY_X));
        setSpinner(gravityY, root.getAttribute(ATTR_GRAVITY_Y));
        setSpinner(gravityScale, root.getAttribute(ATTR_GRAVITY_SCALE));
        setSpinner(timeScale, root.getAttribute(ATTR_TIME_SCALE));
        String sleepValue = root.getAttribute(ATTR_ENABLE_SLEEPING);
        if (!sleepValue.isEmpty()) enableSleeping.setSelected("true".equalsIgnoreCase(sleepValue));
    }

    private void setSpinner(JSpinner spinner, String value) {
        if (value == null || value.isEmpty()) return;
        try { spinner.setValue(Double.parseDouble(value)); }
        catch (NumberFormatException ignored) {}
    }

    private void setLongSpinner(JSpinner spinner, String value) {
        if (value == null || value.isEmpty()) return;
        try { spinner.setValue(Long.parseLong(value)); }
        catch (NumberFormatException ignored) {}
    }

    private void updateEnabledState() {
        boolean hasElement = selected instanceof Element;
        applyButton.setEnabled(hasElement);
        removeButton.setEnabled(hasElement && ((Element) selected).hasAttribute(ATTR_BODY));
        applyWorldButton.setEnabled(currentDocument != null);
        exportButton.setEnabled(currentDocument != null);
        if (!hasElement) status.setText("Select exactly one SVG object.");
    }

    private void applyPhysics() {
        if (loading || canvas == null || !(selected instanceof Element)) return;
        Element e = (Element) selected;
        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, "Apply Physics");
        try {
            e.setAttribute(ATTR_BODY, String.valueOf(bodyType.getSelectedItem()));
            e.setAttribute(ATTR_SHAPE, String.valueOf(shape.getSelectedItem()));
            e.setAttribute(ATTR_DENSITY, number(density));
            e.setAttribute(ATTR_MASS, number(mass));
            e.setAttribute(ATTR_FRICTION, number(friction));
            e.setAttribute(ATTR_FRICTION_STATIC, number(frictionStatic));
            e.setAttribute(ATTR_FRICTION_AIR, number(frictionAir));
            e.setAttribute(ATTR_RESTITUTION, number(restitution));
            e.setAttribute(ATTR_ANGLE, number(angle));
            e.setAttribute(ATTR_VELOCITY_X, number(velocityX));
            e.setAttribute(ATTR_VELOCITY_Y, number(velocityY));
            e.setAttribute(ATTR_ANGULAR_VELOCITY, number(angularVelocity));
            e.setAttribute(ATTR_SENSOR, Boolean.toString(sensor.isSelected()));
            e.setAttribute(ATTR_FORCE_X, number(forceX));
            e.setAttribute(ATTR_FORCE_Y, number(forceY));
            e.setAttribute(ATTR_IMPULSE_X, number(impulseX));
            e.setAttribute(ATTR_IMPULSE_Y, number(impulseY));
            e.setAttribute(ATTR_TORQUE, number(torque));
            e.setAttribute(ATTR_SLEEPING, Boolean.toString(sleeping.isSelected()));
            e.setAttribute(ATTR_CONSTRAINT_TYPE, String.valueOf(constraintType.getSelectedItem()));
            e.setAttribute(ATTR_CONSTRAINT_TARGET, constraintTarget.getText().trim());
            e.setAttribute(ATTR_CONSTRAINT_POINT_A_X, number(constraintPointAX));
            e.setAttribute(ATTR_CONSTRAINT_POINT_A_Y, number(constraintPointAY));
            e.setAttribute(ATTR_CONSTRAINT_POINT_B_X, number(constraintPointBX));
            e.setAttribute(ATTR_CONSTRAINT_POINT_B_Y, number(constraintPointBY));
            e.setAttribute(ATTR_CONSTRAINT_LENGTH, number(constraintLength));
            e.setAttribute(ATTR_CONSTRAINT_STIFFNESS, number(constraintStiffness));
            e.setAttribute(ATTR_CONSTRAINT_DAMPING, number(constraintDamping));
            e.setAttribute(ATTR_COLLISION_CATEGORY, integer(collisionCategory));
            e.setAttribute(ATTR_COLLISION_MASK, integer(collisionMask));
            e.setAttribute(ATTR_COLLISION_GROUP, integer(collisionGroup));
            e.setAttribute(ATTR_EVENT_ID, eventId.getText().trim());
            e.setAttribute(ATTR_EVENT_START, Boolean.toString(eventStart.isSelected()));
            e.setAttribute(ATTR_EVENT_ACTIVE, Boolean.toString(eventActive.isSelected()));
            e.setAttribute(ATTR_EVENT_END, Boolean.toString(eventEnd.isSelected()));
            selected = e instanceof SVGElement ? (SVGElement) e : null;
            currentDocument = e.getOwnerDocument();
            undo.end();
            status.setText("Physics metadata applied.");
        } catch (RuntimeException ex) {
            undo.cancel();
            status.setText("Could not apply physics: " + ex.getMessage());
        }
        updateEnabledState();
    }

    private void removePhysics() {
        if (canvas == null || !(selected instanceof Element)) return;
        Element e = (Element) selected;
        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, "Remove Physics");
        try {
            String[] attrs = {ATTR_BODY, ATTR_SHAPE, ATTR_DENSITY, ATTR_MASS, ATTR_FRICTION,
                ATTR_FRICTION_STATIC, ATTR_FRICTION_AIR, ATTR_RESTITUTION, ATTR_ANGLE,
                ATTR_VELOCITY_X, ATTR_VELOCITY_Y, ATTR_ANGULAR_VELOCITY, ATTR_SENSOR,
                ATTR_FORCE_X, ATTR_FORCE_Y, ATTR_IMPULSE_X, ATTR_IMPULSE_Y, ATTR_TORQUE, ATTR_SLEEPING,
                ATTR_CONSTRAINT_TYPE, ATTR_CONSTRAINT_TARGET, ATTR_CONSTRAINT_POINT_A_X, ATTR_CONSTRAINT_POINT_A_Y,
                ATTR_CONSTRAINT_POINT_B_X, ATTR_CONSTRAINT_POINT_B_Y, ATTR_CONSTRAINT_LENGTH,
                ATTR_CONSTRAINT_STIFFNESS, ATTR_CONSTRAINT_DAMPING,
                ATTR_COLLISION_CATEGORY, ATTR_COLLISION_MASK, ATTR_COLLISION_GROUP,
                ATTR_EVENT_ID, ATTR_EVENT_START, ATTR_EVENT_ACTIVE, ATTR_EVENT_END};
            for (String attr : attrs) e.removeAttribute(attr);
            selected = e instanceof SVGElement ? (SVGElement) e : null;
            currentDocument = e.getOwnerDocument();
            undo.end();
            status.setText("Physics metadata removed.");
        } catch (RuntimeException ex) {
            undo.cancel();
            status.setText("Could not remove physics: " + ex.getMessage());
        }
        updateEnabledState();
    }

    private void applyWorld() {
        if (canvas == null || currentDocument == null || currentDocument.getDocumentElement() == null) return;
        Element root = currentDocument.getDocumentElement();
        DOMUndoManager undo = canvas.getUndoManager();
        beginUndoTransaction(undo, "Apply Physics World");
        try {
            root.setAttribute(ATTR_GRAVITY_X, number(gravityX));
            root.setAttribute(ATTR_GRAVITY_Y, number(gravityY));
            root.setAttribute(ATTR_GRAVITY_SCALE, number(gravityScale));
            root.setAttribute(ATTR_TIME_SCALE, number(timeScale));
            root.setAttribute(ATTR_ENABLE_SLEEPING, Boolean.toString(enableSleeping.isSelected()));
            currentDocument = root.getOwnerDocument();
            selected = null;
            undo.end();
            status.setText("Physics world metadata applied.");
        } catch (RuntimeException ex) {
            undo.cancel();
            status.setText("Could not apply world: " + ex.getMessage());
        }
        updateEnabledState();
    }

    private String number(JSpinner spinner) {
        return String.valueOf(((Number) spinner.getValue()).doubleValue());
    }

    private String integer(JSpinner spinner) {
        return String.valueOf(((Number) spinner.getValue()).longValue());
    }

    private void exportRuntime() {
        if (currentDocument == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Sketsa Physics Runtime");
        chooser.setFileFilter(new FileNameExtensionFilter("HTML file", "html", "htm"));
        chooser.setSelectedFile(new File("sketsa-physics.html"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".html") && !file.getName().toLowerCase().endsWith(".htm")) {
            file = new File(file.getParentFile(), file.getName() + ".html");
        }

        try {
            RuntimeHtmlExporter.export(currentDocument, file, includeCompanions.isSelected());
            status.setText("Exported runtime package: " + file.getName());
        } catch (Exception ex) {
            status.setText("Export failed: " + ex.getMessage());
        }
    }

    private final class SelectionHandler extends CanvasSelectionAdapter {
        @Override
        public void valueChanged(CanvasSelectionEvent event) {
            cacheSelection();
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
