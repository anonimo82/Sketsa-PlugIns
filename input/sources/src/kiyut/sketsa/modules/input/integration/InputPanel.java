package kiyut.sketsa.modules.input.integration;

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
import javax.swing.JTextField;
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

final class InputPanel extends JPanel {
    private static final String ATTR_ACTION="data-sketsa-input-action";
    private static final String ATTR_KEYS="data-sketsa-input-keys";
    private static final String ATTR_PREVENT="data-sketsa-input-prevent-default";
    private static final String ATTR_GP_BUTTON="data-sketsa-input-gamepad-button";
    private static final String ATTR_GP_AXIS="data-sketsa-input-gamepad-axis";
    private static final String ATTR_GP_DIRECTION="data-sketsa-input-gamepad-axis-direction";
    private static final String ATTR_GP_DEADZONE="data-sketsa-input-gamepad-deadzone";
    private static final String ATTR_GP_INDEX="data-sketsa-input-gamepad-index";
    private static final String ATTR_CONTROL="data-sketsa-input-control";
    private static final String ATTR_CONTROL_LEFT="data-sketsa-input-control-left";
    private static final String ATTR_CONTROL_RIGHT="data-sketsa-input-control-right";
    private static final String ATTR_CONTROL_UP="data-sketsa-input-control-up";
    private static final String ATTR_CONTROL_DOWN="data-sketsa-input-control-down";
    private static final String ATTR_CONTROL_DEADZONE="data-sketsa-input-control-deadzone";
    private static final String ATTR_TARGET_SOURCE="data-sketsa-input-target-source";
    private static final String ATTR_TARGET_ACTION="data-sketsa-input-target-action";
    private static final String ATTR_TARGET_ID="data-sketsa-input-target-id";
    private static final String ATTR_TARGET_EVENT="data-sketsa-input-target-event";
    private static final String ATTR_TARGET_X="data-sketsa-input-target-x";
    private static final String ATTR_TARGET_Y="data-sketsa-input-target-y";

    private final JTextField action=new JTextField();
    private final JTextField keys=new JTextField();
    private final JCheckBox preventDefault=new JCheckBox("Prevent browser default");
    private final JTextField gamepadButton=new JTextField();
    private final JTextField gamepadAxis=new JTextField();
    private final JTextField gamepadDirection=new JTextField();
    private final JTextField gamepadDeadzone=new JTextField("0.15");
    private final JTextField gamepadIndex=new JTextField("-1");
    private final JTextField controlType=new JTextField();
    private final JTextField controlLeft=new JTextField();
    private final JTextField controlRight=new JTextField();
    private final JTextField controlUp=new JTextField();
    private final JTextField controlDown=new JTextField();
    private final JTextField controlDeadzone=new JTextField("0.15");
    private final JTextField targetSource=new JTextField();
    private final JTextField targetAction=new JTextField();
    private final JTextField targetId=new JTextField();
    private final JTextField targetEvent=new JTextField("down");
    private final JTextField targetX=new JTextField("0");
    private final JTextField targetY=new JTextField("0");
    private final JCheckBox includeCompanions=new JCheckBox("Include companion runtimes",true);
    private final JButton applyButton=new JButton("Apply Input");
    private final JButton removeButton=new JButton("Remove Input");
    private final JButton exportButton=new JButton("Export Runtime HTML...");
    private final JLabel status=new JLabel("Select exactly one SVG object.");
    private final JPanel form=new JPanel(new GridBagLayout());

    private final Lookup.Result<SVGEditorCookie> lookupResult;
    private final LookupListener lookupListener;
    private final SelectionHandler selectionHandler=new SelectionHandler();
    private VectorCanvas canvas;
    private SVGElement selected;
    private Document currentDocument;
    private boolean loading;

    InputPanel(){
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Input / Actions + Interop"));
        setPreferredSize(new Dimension(10,240));
        setMinimumSize(new Dimension(10,140));
        form.setBorder(BorderFactory.createEmptyBorder(2,6,5,6));
        JScrollPane scroll=new JScrollPane(form,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18); scroll.setBorder(BorderFactory.createEmptyBorder()); add(scroll,BorderLayout.CENTER);

        GridBagConstraints c=new GridBagConstraints(); c.insets=new Insets(2,3,2,3); c.anchor=GridBagConstraints.WEST; int row=0;
        addRow(c,row++,"Action",action);
        addRow(c,row++,"Keys (codes)",keys);
        addWide(c,row++,new JLabel("Example: ArrowLeft, KeyA"));
        addWide(c,row++,new JLabel("Pointer Events support mouse, touch, pen and multi-pointer."));
        addRow(c,row++,"Gamepad button",gamepadButton);
        addRow(c,row++,"Gamepad axis",gamepadAxis);
        addRow(c,row++,"Axis direction",gamepadDirection);
        addRow(c,row++,"Deadzone",gamepadDeadzone);
        addRow(c,row++,"Gamepad index",gamepadIndex);
        addWide(c,row++,new JLabel("Gamepad: blank = unused; direction -1 / 0 / +1; index -1 = any."));
        addRow(c,row++,"On-screen control",controlType);
        addWide(c,row++,new JLabel("blank / button / dpad / stick"));
        addRow(c,row++,"Control left",controlLeft);
        addRow(c,row++,"Control right",controlRight);
        addRow(c,row++,"Control up",controlUp);
        addRow(c,row++,"Control down",controlDown);
        addRow(c,row++,"Control deadzone",controlDeadzone);
        addWide(c,row++,new JLabel("button uses Action; dpad/stick use the directional actions above."));
        addWide(c,row++,new JLabel("Interop target (optional)"));
        addRow(c,row++,"Target source",targetSource);
        addRow(c,row++,"Target action",targetAction);
        addRow(c,row++,"Target object ID",targetId);
        addRow(c,row++,"Trigger event",targetEvent);
        addRow(c,row++,"Payload X",targetX);
        addRow(c,row++,"Payload Y",targetY);
        addWide(c,row++,new JLabel("Example: physics / applyForce / player / down / 0.08 / 0"));
        addWide(c,row++,preventDefault);
        addWide(c,row++,includeCompanions);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0)); buttons.add(applyButton); buttons.add(removeButton); addWide(c,row++,buttons);
        addWide(c,row++,exportButton); addWide(c,row++,status);

        applyButton.addActionListener(e->applyInput()); removeButton.addActionListener(e->removeInput()); exportButton.addActionListener(e->exportRuntime());
                SuggestionPopup.install(action, this::availableActions);
        SuggestionPopup.install(controlType, () -> java.util.Arrays.asList("button", "dpad", "stick"));
        SuggestionPopup.install(targetSource, () -> java.util.Arrays.asList("physics", "audio", "input", "dom"));
        SuggestionPopup.install(targetAction, this::availableActions);
        SuggestionPopup.install(targetId, this::availableSvgIds);
        SuggestionPopup.install(targetEvent, () -> java.util.Arrays.asList("down", "up", "click", "collisionStart", "collisionEnd"));

lookupResult=Utilities.actionsGlobalContext().lookupResult(SVGEditorCookie.class);
        lookupListener=(LookupEvent ev)->updateCanvasFromLookup(); lookupResult.addLookupListener(lookupListener);
        updateCanvasFromLookup(); updateEnabledState();
    }

    private void addWide(GridBagConstraints c,int row,java.awt.Component component){ c.gridx=0;c.gridy=row;c.gridwidth=2;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;form.add(component,c); }
    private void addRow(GridBagConstraints c,int row,String label,java.awt.Component component){ c.gridx=0;c.gridy=row;c.gridwidth=1;c.weightx=0;c.fill=GridBagConstraints.NONE;form.add(new JLabel(label),c);c.gridx=1;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;form.add(component,c); }
    private java.util.List<String> availableActions(){java.util.LinkedHashSet<String> out=new java.util.LinkedHashSet<String>();if(currentDocument!=null){org.w3c.dom.NodeList nodes=currentDocument.getElementsByTagName("*");for(int i=0;i<nodes.getLength();i++)if(nodes.item(i) instanceof Element){Element e=(Element)nodes.item(i);String v=e.getAttribute(ATTR_ACTION);if(v!=null&&!v.trim().isEmpty())out.add(v.trim());String t=e.getAttribute(ATTR_TARGET_ACTION);if(t!=null&&!t.trim().isEmpty())out.add(t.trim());}}return new java.util.ArrayList<String>(out);}
    private java.util.List<String> availableSvgIds(){java.util.LinkedHashSet<String> out=new java.util.LinkedHashSet<String>();if(currentDocument!=null){org.w3c.dom.NodeList nodes=currentDocument.getElementsByTagName("*");for(int i=0;i<nodes.getLength();i++)if(nodes.item(i) instanceof Element){String id=((Element)nodes.item(i)).getAttribute("id");if(id!=null&&!id.trim().isEmpty())out.add("#"+id.trim());}}return new java.util.ArrayList<String>(out);}

    private void updateCanvasFromLookup(){ Collection<? extends SVGEditorCookie> cookies=lookupResult.allInstances(); if(cookies.isEmpty())return; SVGEditorCookie cookie=cookies.iterator().next(); if(cookie.isOpened())setCanvas(cookie.getVectorCanvas()); }
    private void setCanvas(VectorCanvas newCanvas){ if(canvas==newCanvas){cacheSelection();return;} if(canvas!=null)canvas.getCanvasSelection().removeSelectionListener(selectionHandler); canvas=newCanvas;selected=null;currentDocument=null; if(canvas!=null){canvas.getCanvasSelection().addSelectionListener(selectionHandler);cacheSelection();} updateEnabledState(); }
    private void cacheSelection(){ if(canvas==null)return; List<SVGElement> selection=canvas.getCanvasSelection().getSelectionList(); if(selection==null||selection.size()!=1){selected=null;updateEnabledState();return;} selected=selection.get(0); if(selected instanceof Element){currentDocument=((Element)selected).getOwnerDocument();loadFields((Element)selected);} updateEnabledState(); }
    private void loadFields(Element e){ loading=true; try{action.setText(e.getAttribute(ATTR_ACTION));keys.setText(e.getAttribute(ATTR_KEYS));preventDefault.setSelected("true".equalsIgnoreCase(e.getAttribute(ATTR_PREVENT)));gamepadButton.setText(e.getAttribute(ATTR_GP_BUTTON));gamepadAxis.setText(e.getAttribute(ATTR_GP_AXIS));gamepadDirection.setText(e.hasAttribute(ATTR_GP_DIRECTION)?e.getAttribute(ATTR_GP_DIRECTION):"0");gamepadDeadzone.setText(e.hasAttribute(ATTR_GP_DEADZONE)?e.getAttribute(ATTR_GP_DEADZONE):"0.15");gamepadIndex.setText(e.hasAttribute(ATTR_GP_INDEX)?e.getAttribute(ATTR_GP_INDEX):"-1");controlType.setText(e.getAttribute(ATTR_CONTROL));controlLeft.setText(e.getAttribute(ATTR_CONTROL_LEFT));controlRight.setText(e.getAttribute(ATTR_CONTROL_RIGHT));controlUp.setText(e.getAttribute(ATTR_CONTROL_UP));controlDown.setText(e.getAttribute(ATTR_CONTROL_DOWN));controlDeadzone.setText(e.hasAttribute(ATTR_CONTROL_DEADZONE)?e.getAttribute(ATTR_CONTROL_DEADZONE):"0.15");targetSource.setText(e.getAttribute(ATTR_TARGET_SOURCE));targetAction.setText(e.getAttribute(ATTR_TARGET_ACTION));targetId.setText(e.getAttribute(ATTR_TARGET_ID));targetEvent.setText(e.hasAttribute(ATTR_TARGET_EVENT)?e.getAttribute(ATTR_TARGET_EVENT):"down");targetX.setText(e.hasAttribute(ATTR_TARGET_X)?e.getAttribute(ATTR_TARGET_X):"0");targetY.setText(e.hasAttribute(ATTR_TARGET_Y)?e.getAttribute(ATTR_TARGET_Y):"0");boolean hasMeta=e.hasAttribute(ATTR_ACTION)||e.hasAttribute(ATTR_CONTROL);status.setText(hasMeta?"Input metadata loaded.":"Selected object has no input metadata.");}finally{loading=false;} }
    private void updateEnabledState(){ boolean has=selected instanceof Element;applyButton.setEnabled(has);removeButton.setEnabled(has&&(((Element)selected).hasAttribute(ATTR_ACTION)||((Element)selected).hasAttribute(ATTR_CONTROL)));exportButton.setEnabled(currentDocument!=null);if(!has)status.setText("Select exactly one SVG object."); }
    private void applyInput(){ if(loading||canvas==null||!(selected instanceof Element))return; String a=action.getText().trim();String k=normalizeKeys(keys.getText());String ct=controlType.getText().trim().toLowerCase();boolean hasGamepad=!gamepadButton.getText().trim().isEmpty()||!gamepadAxis.getText().trim().isEmpty();boolean hasDirectional=!controlLeft.getText().trim().isEmpty()||!controlRight.getText().trim().isEmpty()||!controlUp.getText().trim().isEmpty()||!controlDown.getText().trim().isEmpty();boolean hasControl=!ct.isEmpty();if(!ct.isEmpty()&&!ct.equals("button")&&!ct.equals("dpad")&&!ct.equals("stick")){status.setText("On-screen control must be blank, button, dpad or stick.");return;}if(a.isEmpty()&&!(hasControl&&hasDirectional)){status.setText("Set Action, or directional actions for dpad/stick.");return;}if(!hasControl&&k.isEmpty()&&!hasGamepad){status.setText("Add a keyboard, gamepad or on-screen binding.");return;}if("button".equals(ct)&&a.isEmpty()){status.setText("Virtual button requires Action.");return;}if(("dpad".equals(ct)||"stick".equals(ct))&&!hasDirectional){status.setText("D-pad/stick requires at least one directional action.");return;}Element e=(Element)selected;DOMUndoManager undo=canvas.getUndoManager();beginUndoTransaction(undo, "Apply Input");try{setOrRemove(e,ATTR_ACTION,a);setOrRemove(e,ATTR_KEYS,k);e.setAttribute(ATTR_PREVENT,Boolean.toString(preventDefault.isSelected()));setOrRemove(e,ATTR_GP_BUTTON,gamepadButton.getText().trim());setOrRemove(e,ATTR_GP_AXIS,gamepadAxis.getText().trim());setOrRemove(e,ATTR_GP_DIRECTION,gamepadDirection.getText().trim());setOrRemove(e,ATTR_GP_DEADZONE,gamepadDeadzone.getText().trim());setOrRemove(e,ATTR_GP_INDEX,gamepadIndex.getText().trim());setOrRemove(e,ATTR_CONTROL,ct);setOrRemove(e,ATTR_CONTROL_LEFT,controlLeft.getText().trim());setOrRemove(e,ATTR_CONTROL_RIGHT,controlRight.getText().trim());setOrRemove(e,ATTR_CONTROL_UP,controlUp.getText().trim());setOrRemove(e,ATTR_CONTROL_DOWN,controlDown.getText().trim());setOrRemove(e,ATTR_CONTROL_DEADZONE,controlDeadzone.getText().trim());setOrRemove(e,ATTR_TARGET_SOURCE,targetSource.getText().trim().toLowerCase());setOrRemove(e,ATTR_TARGET_ACTION,targetAction.getText().trim());setOrRemove(e,ATTR_TARGET_ID,targetId.getText().trim());setOrRemove(e,ATTR_TARGET_EVENT,targetEvent.getText().trim().toLowerCase());setOrRemove(e,ATTR_TARGET_X,targetX.getText().trim());setOrRemove(e,ATTR_TARGET_Y,targetY.getText().trim());selected=e instanceof SVGElement?(SVGElement)e:null;currentDocument=e.getOwnerDocument();undo.end();status.setText("Input mapping applied.");}catch(RuntimeException ex){undo.cancel();status.setText("Could not apply input: "+ex.getMessage());}updateEnabledState(); }
    private void removeInput(){ if(canvas==null||!(selected instanceof Element))return;Element e=(Element)selected;DOMUndoManager undo=canvas.getUndoManager();beginUndoTransaction(undo, "Remove Input");try{e.removeAttribute(ATTR_ACTION);e.removeAttribute(ATTR_KEYS);e.removeAttribute(ATTR_PREVENT);e.removeAttribute(ATTR_GP_BUTTON);e.removeAttribute(ATTR_GP_AXIS);e.removeAttribute(ATTR_GP_DIRECTION);e.removeAttribute(ATTR_GP_DEADZONE);e.removeAttribute(ATTR_GP_INDEX);e.removeAttribute(ATTR_CONTROL);e.removeAttribute(ATTR_CONTROL_LEFT);e.removeAttribute(ATTR_CONTROL_RIGHT);e.removeAttribute(ATTR_CONTROL_UP);e.removeAttribute(ATTR_CONTROL_DOWN);e.removeAttribute(ATTR_CONTROL_DEADZONE);e.removeAttribute(ATTR_TARGET_SOURCE);e.removeAttribute(ATTR_TARGET_ACTION);e.removeAttribute(ATTR_TARGET_ID);e.removeAttribute(ATTR_TARGET_EVENT);e.removeAttribute(ATTR_TARGET_X);e.removeAttribute(ATTR_TARGET_Y);selected=e instanceof SVGElement?(SVGElement)e:null;currentDocument=e.getOwnerDocument();undo.end();status.setText("Input mapping removed.");}catch(RuntimeException ex){undo.cancel();status.setText("Could not remove input: "+ex.getMessage());}updateEnabledState(); }
    private void setOrRemove(Element e,String attr,String value){if(value==null||value.trim().isEmpty())e.removeAttribute(attr);else e.setAttribute(attr,value.trim());}
    private String normalizeKeys(String raw){ StringBuilder out=new StringBuilder(); for(String p:raw.split("[,;]")){String s=p.trim();if(s.isEmpty())continue;if(out.length()>0)out.append(",");out.append(s);}return out.toString(); }
    private void exportRuntime(){ if(currentDocument==null)return;JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Export Sketsa Input Runtime");chooser.setFileFilter(new FileNameExtensionFilter("HTML file","html","htm"));chooser.setSelectedFile(new File("sketsa-input.html"));if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;File f=chooser.getSelectedFile();if(!f.getName().toLowerCase().endsWith(".html")&&!f.getName().toLowerCase().endsWith(".htm"))f=new File(f.getParentFile(),f.getName()+".html");try{RuntimeHtmlExporter.export(currentDocument,f,includeCompanions.isSelected());status.setText("Exported runtime package: "+f.getName());}catch(Exception ex){status.setText("Export failed: "+ex.getMessage());} }
    private final class SelectionHandler extends CanvasSelectionAdapter { @Override public void valueChanged(CanvasSelectionEvent event){cacheSelection();} }
    /** Ensure every plugin edit is a separate Sketsa undo transaction. */
    private static void beginUndoTransaction(DOMUndoManager undo, String name) {
        // end() is a no-op when no transaction is open. If Sketsa left a
        // previous editor transaction pending, commit it before starting ours.
        undo.end();
        undo.start(name);
    }

}
