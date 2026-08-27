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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import kiyut.sketsa.options.CanvasOptions;

/**
 * This header renderer draw Timing Ruler
 * @author Tonny Kohar
 */
public class TimingHeaderRenderer extends JLabel implements TableCellRenderer {

    protected Timeline timeline;
    
    protected Point caret;
    protected boolean caretVisible;
    
    public TimingHeaderRenderer(Timeline owner) {
        super();
        //setOpaque(true); //MUST do this for background to show up.
        this.setFont(new Font("Monospaced", Font.PLAIN, 10));
        this.setBackground(Color.WHITE);
        
        caret = new Point();
        this.timeline = owner;
        timeline.addMouseMotionListener(new MouseInputAdapter() {
            @Override
            public void mouseMoved(MouseEvent evt) {
                updateCaret(evt);
            }
            @Override
            public void mouseDragged(MouseEvent evt) {
                updateCaret(evt);
            }
            
            private void updateCaret(MouseEvent evt) {
                TableColumn col = timeline.getColumnModel().getColumn(0);
                int colOffset =  col.getPreferredWidth();
                
                caret.x = evt.getPoint().x - colOffset;
                
                if (caret.x < 0) {
                    return;
                }
                
                // clear old one
                caretVisible = false;
                repaintCaret();
        
                caretVisible = true;
                repaintCaret();
           }
        });
        timeline.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseExited(MouseEvent evt) {
                // clear old one
                caretVisible = false;
                repaintCaret();
            }
        });
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        
        // Draw the ruler labels in a small font that's black.
        g2d.setPaint(UIManager.getDefaults().getColor("Panel.foreground"));
        
        g.setFont(getFont());
        
        FontMetrics fontMetrics = g.getFontMetrics();
        //int fontHeight = fontMetrics.getHeight();
        int fontAscent = fontMetrics.getAscent();
        
        double maxWidth = (getBounds().getWidth());
        int maxHeight = 20;
        int tickCount = 10;
        
        int maximum = timeline.getMaximum();
        int tickDiv = maximum/60;
        
        int tickLengthFull = maxHeight;
        int tickLengthMiddle = maxHeight/2;
        int tickLengthSmall = tickLengthMiddle/2;
        
        double tickIncrement = maxWidth / (maximum/tickDiv);
        double tickOffset = 0;
        int tickOffsetInt = 0;
        int tickLength;
        String text;
        
        // draw the left line, make it pretty
        g2d.drawLine(0, 0, 0,maxHeight);
        
        // draw Tick and Label
        for (int i=0; i<=(maximum/tickDiv); i++) {
            tickOffset = i * tickIncrement;
            tickOffsetInt = (int)tickOffset;
            
            tickLength = tickLengthSmall;
            if (i % tickCount == 0) {
                tickLength = tickLengthFull;
                // do not draw the text for the last tick
                if (i < (maximum/tickDiv)) {
                    text = Integer.toString(i * tickDiv); 
                    g2d.drawString(text, tickOffsetInt+2, fontAscent);
                }
            } else if (i % (tickCount/2) == 0) {
                tickLength = tickLengthMiddle;
            } 
            
            g2d.drawLine(tickOffsetInt, maxHeight, tickOffsetInt, maxHeight-tickLength);
        }
        
        // draw Caret
        if (caretVisible == true) {
            g.setColor(CanvasOptions.getInstance().getSelectionColor());
            g2d.drawLine(caret.x, 0, caret.x, maxHeight);
            //System.out.println("TimingHeaderRenderer.paintComponent() caretVisible = true");
        }
    }
    
    protected final synchronized void repaintCaret() {
        if (caret != null) {
            //System.out.println(caret.x +"," + caret.y);
            //repaint(caret.x,0,1,20);
            
            // weird JTable repaint mechanism
            timeline.getTableHeader().resizeAndRepaint();
        }
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }
}
