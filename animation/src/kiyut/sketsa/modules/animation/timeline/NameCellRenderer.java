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
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import kiyut.sketsa.util.SVGConstants;
import org.w3c.dom.svg.SVGAnimateElement;
import org.w3c.dom.svg.SVGAnimationElement;
import org.w3c.dom.svg.SVGSetElement;

/**
 *
 * @author Tonny Kohar
 */
public class NameCellRenderer extends JLabel implements TableCellRenderer {

    public NameCellRenderer() {
        super();
        setOpaque(true); //MUST do this for background to show up.
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        SVGAnimationElement elt = (SVGAnimationElement)value;
        String text = elt.getNodeName();
        
        if (elt instanceof SVGAnimateElement) {
            text = text + "(" + elt.getAttributeNS(null, SVGConstants.SVG_ATTRIBUTE_NAME_ATTRIBUTE) + ")";
        } else if (elt instanceof SVGSetElement) {
            text = text + "(" + elt.getAttributeNS(null, SVGConstants.SVG_ATTRIBUTE_NAME_ATTRIBUTE) + ")";
        }
        
        setText(text);
        
        return this;
    }

}
