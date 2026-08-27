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

import java.awt.Shape;

/**
 * TimingValue for (@code TimingCellRenderer} and {@code TimingCellEditor}.
 * It is only a helper class to store the current TimingValue to be drawn.
 * 
 * @author Tonny Kohar
 */
public class TimingValue {
    
    protected Shape timing;
    protected Shape startHandle;
    protected Shape durHandle;
    
    protected TimingCellRenderer timingCellRenderer;
    
    public TimingValue() {
    }

    public void setTiming(Shape timing) {
        this.timing = timing;
    }
    
    public Shape getTiming() {
        return timing;
    }
   
    public void setStartHandle(Shape startHandle) {
        this.startHandle = startHandle;
    }
    
    public Shape getStartHandle() {
        return startHandle;
    }
    
    public void setDurHandle(Shape durHandle) {
        this.durHandle = durHandle;
    }
    
    public Shape getDurHandle() {
        return durHandle;
    }
    
    
    
}
