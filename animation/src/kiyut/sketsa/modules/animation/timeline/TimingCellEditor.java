/*
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 
*/

package kiyut.sketsa.modules.animation.timeline;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.table.TableCellEditor;
import org.w3c.dom.svg.SVGAnimationElement;

/**
 *
 * @author Tonny Kohar
 */
public class TimingCellEditor extends TimingCellRenderer implements TableCellEditor {
    
    /** Unknown Handle Type */
    public static int UNKNOWN_HANDLE = -1;
    
    /** Bounds Handle Type*/
    public static int BOUNDS_HANDLE = 0;
    
    /** Start Handle Type*/
    public static int START_HANDLE = 1;
    
    /** Duration Handle Type*/
    public static int DUR_HANDLE = 2;
    
    protected int handleType = UNKNOWN_HANDLE;
    
    protected transient ChangeEvent changeEvent = null;
    
    //protected boolean handleInitialized = false;
    
    protected int dragX;
    protected int dragY;
    
    protected TimingValue timingValue;
    
    public TimingCellEditor(Timeline owner) {
        super(owner);
        
        MouseInputListener mouseInputListener = new MouseInputAdapter() {
            @Override
            public void mousePressed(MouseEvent evt) { TimingCellEditor.this.mousePressed(evt); } 
            @Override
            public void mouseDragged(MouseEvent evt) { TimingCellEditor.this.mouseDragged(evt); }  
            @Override
            public void mouseReleased(MouseEvent evt) { TimingCellEditor.this.mouseReleased(evt); }  
        };
        
        addMouseListener(mouseInputListener);
        addMouseMotionListener(mouseInputListener);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (element == null) { return; }
        if (handleType == UNKNOWN_HANDLE) { return; }
        if (timingValue == null) { return; }
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setPaint(UIManager.getDefaults().getColor("Panel.foreground"));
        g2d.draw(timingValue.getTiming());
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        element = (SVGAnimationElement)value;
        return this;
    }

    public Object getCellEditorValue() {
        return null;
    }

    public boolean isCellEditable(EventObject evt) {
        return true;
    }

    public boolean shouldSelectCell(EventObject evt) {
        return false;
    }

    public boolean stopCellEditing() {
        fireEditingStopped(); 
	return true;
    }

    public void cancelCellEditing() {
        fireEditingCanceled();
    }
    
    public void addCellEditorListener(CellEditorListener l) {
        listenerList.add(CellEditorListener.class, l);
    }

    public void removeCellEditorListener(CellEditorListener l) {
        listenerList.remove(CellEditorListener.class, l);
    }
    
    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is created lazily.
     *
     * @see EventListenerList
     */
    protected void fireEditingStopped() {
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length-2; i>=0; i-=2) {
	    if (listeners[i]==CellEditorListener.class) {
		// Lazily create the event:
		if (changeEvent == null)
		    changeEvent = new ChangeEvent(this);
		((CellEditorListener)listeners[i+1]).editingStopped(changeEvent);
	    }	       
	}
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is created lazily.
     *
     * @see EventListenerList
     */
    protected void fireEditingCanceled() {
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length-2; i>=0; i-=2) {
	    if (listeners[i]==CellEditorListener.class) {
		// Lazily create the event:
		if (changeEvent == null)
		    changeEvent = new ChangeEvent(this);
		((CellEditorListener)listeners[i+1]).editingCanceled(changeEvent);
	    }	       
	}
    }
    
    protected void updateDOM() {
        // TODO update the underlying DOM Attributes
    }
    
    protected void updateValue() {
        if (timingValue == null) { return; }
        Rectangle rTiming = timingValue.getTiming().getBounds();
        
        //double width = bounds.getWidth();
        int start = rTiming.x;
        int dur = start + rTiming.width;
        int height = rTiming.height;
        
        Rectangle rect = new Rectangle();
        
        // draw the edited start and duration time
        if (handleType == START_HANDLE) {
            rect.setFrame(dragX, 1, dur-start, height);
            timingValue.setTiming(rect);
        } else if (handleType == DUR_HANDLE) {
            rect.setFrame(start, 1, dragX-start, height);
            timingValue.setTiming(rect);
        }
    }
    
    protected void mousePressed(MouseEvent evt) {
        if (!SwingUtilities.isLeftMouseButton(evt)) { return; }
        
        //System.out.println("pressed");
        
        timingValue = null;
        
        dragX = evt.getX();
        dragY = evt.getY();
        
        for (int i=0; i<timingValues.length; i++) {
            TimingValue curTimingValue = timingValues[i];
            if (curTimingValue == null) {
                continue;
            }
            
            if (curTimingValue.getDurHandle().contains(dragX,dragY)) {
                handleType = DUR_HANDLE;
                //Rectangle r = curTimingValue.getDurHandle().getBounds();
                //dragX = r.x + r.width;
                timingValue = curTimingValue;
                break;
            
            } else if (curTimingValue.getStartHandle().contains(dragX,dragY)) {
                handleType = START_HANDLE;
                //Rectangle r = curTimingValue.getStartHandle().getBounds();
                //dragX = r.x;
                timingValue = curTimingValue;
                break;
            }
        }
        
        repaint();
    }
    
    protected void mouseDragged(MouseEvent evt) {
        if (!SwingUtilities.isLeftMouseButton(evt)) { return; }
        if (handleType == UNKNOWN_HANDLE) { return; }
        
        dragX  = evt.getX();
        dragY = evt.getY();
        
        if (dragX < 0) { dragX = 0; }
        updateValue();
        
        repaint();
        
        //System.out.println("Dragged: " + dragX +"," + dragY);
    }
    
    protected void mouseReleased(MouseEvent evt) {
        if (!SwingUtilities.isLeftMouseButton(evt)) { return; }
        if (handleType == UNKNOWN_HANDLE) { 
            //System.out.println("Released: cancel editing");
            cancelCellEditing();
            return; 
        }

        dragX  = evt.getX();
        dragY = evt.getY();
        
        if (dragX < 0) { dragX = 0; }
        
        stopCellEditing();
        
        // update DOM
        updateDOM();
        
        handleType = UNKNOWN_HANDLE;
        timingValue = null;
        repaint();
        
        //System.out.println("Released: stop editing");
    }

}
