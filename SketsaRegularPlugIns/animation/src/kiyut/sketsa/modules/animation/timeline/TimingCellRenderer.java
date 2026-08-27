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
import java.lang.reflect.Field;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import org.apache.batik.anim.timing.IndefiniteTimingSpecifier;
import org.apache.batik.anim.timing.OffsetTimingSpecifier;
import org.apache.batik.anim.timing.TimedElement;
import org.apache.batik.anim.timing.TimingSpecifier;
import org.apache.batik.bridge.SVGAnimationElementBridge;
import org.apache.batik.anim.dom.SVGOMAnimationElement;
import org.w3c.dom.DOMException;
import org.w3c.dom.svg.SVGAnimationElement;

/**
 * Timing cell renderer and editor
 * 
 * @author Tonny Kohar
 */
public class TimingCellRenderer extends JLabel implements TableCellRenderer {

    /** handle size, default 6px */
    protected int handleSize = 6;
    
    protected Timeline timeline;
    protected SVGAnimationElement element;
    
    protected TimingValue[] timingValues;
    
    public TimingCellRenderer(Timeline owner) {
        super();

        timingValues = new TimingValue[0];

        this.timeline = owner;
        
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (element == null) { return; }
        updateValues();
        if (timingValues.length == 0) { return; }
        
        Graphics2D g2d = (Graphics2D) g;
        
        for (int i=0; i<timingValues.length; i++) {
            TimingValue timingValue = timingValues[i];
            if (timingValue == null) {
                continue;
            }
            
            g2d.setPaint(UIManager.getDefaults().getColor("ProgressBar.foreground"));
            g2d.fill(timingValue.getTiming());
            
            g2d.setPaint(UIManager.getDefaults().getColor("Panel.foreground"));
            g2d.draw(timingValue.getTiming());
            g2d.fill(timingValue.getStartHandle());
            g2d.fill(timingValue.getDurHandle());
        }

        // XXX for debug only
        /*g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setPaint(UIManager.getDefaults().getColor("TextPane.foreground"));
        g2d.drawString(getText(), 0, (int)(height-4));
        */
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        element = (SVGAnimationElement)value;
        
        // XXX for debug only
        /*try {
            String str = "start:" + element.getStartTime() + " dur:" + element.getSimpleDuration() + " [debug]";
            setText(str);
        } catch (Exception ex) {
            // do nothing
        }*/
                
        return this;
    }
    
    protected void updateValues() {
        int maximum = timeline.getMaximum();
        //int tickDiv = maximum/60;

        Rectangle bounds = getBounds();
        double width = bounds.getWidth();
        double height = bounds.getHeight();
        double tickIncrement = width / maximum;

        boolean durIndefinite = false;
        double dur = maximum;
        try {
            dur = element.getSimpleDuration();
        } catch (DOMException ex) { /* do nothing */}
        
        if (Double.isNaN(dur)) {
            dur = maximum;
            durIndefinite = true;
        }
        
        SVGOMAnimationElement animElt = (SVGOMAnimationElement)element;
        TimedElement te = ((SVGAnimationElementBridge)animElt.getSVGContext()).getTimedElement();
        TimingSpecifier[] beginTimings = te.getBeginTimingSpecifiers();
        
        timingValues = new TimingValue[beginTimings.length];
        
        for (int i=0; i<beginTimings.length; i++) {
            TimingValue timingValue = new TimingValue();
            Rectangle timing = new Rectangle();
            Rectangle startHandle = new Rectangle();
            Rectangle durHandle = new Rectangle();
            
            double offset = 0;
            TimingSpecifier ts = beginTimings[i];
            if (ts instanceof IndefiniteTimingSpecifier) {
                startHandle.setFrame(offset, 1, handleSize, height - 2);
            } else if (ts instanceof OffsetTimingSpecifier) {
                // TODO request Batik public API for OffsetTimingSpecifier.getOffset()
                // XXX workaround use reflection to access offset value
                try {
                    final Field field = OffsetTimingSpecifier.class.getDeclaredField("offset");
                    field.setAccessible(true);
                    offset = ((Float)field.get(ts)).floatValue();
                    //System.out.println("reflection val: " + offset);
                } catch (Exception ex) { /* do nothing */ }
                
                offset = offset * tickIncrement;
                startHandle.setFrame(offset, (height-1-handleSize), handleSize, handleSize);
            } else {
                timingValues[i] = null;
                continue;
            }
            
            dur = offset + (dur * tickIncrement);
            
            if (durIndefinite) {
                durHandle.setFrame(dur-handleSize, 1, handleSize, height - 2);
            } else {
                durHandle.setFrame(dur-handleSize, (height-1-handleSize), handleSize, handleSize);
            } 
        
            timing.setRect(offset, 1, dur-offset, height-2);
            
            timingValue.setTiming(timing);
            timingValue.setStartHandle(startHandle);
            timingValue.setDurHandle(durHandle);
            timingValues[i] = timingValue;
        }
    }
    
}
