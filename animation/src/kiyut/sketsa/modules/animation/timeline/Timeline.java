package kiyut.sketsa.modules.animation.timeline;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGElement;

public final class Timeline extends JTable {
    public static final String MAXIMUM_PROPERTY = "maximum";
    private final TimelineModel timelineModel;
    private int maximum = 60;
    private int selectedKeyIndex = -1;
    private float currentTime = 0f;

    public Timeline() {
        timelineModel = new TimelineModel();
        setModel(timelineModel);
        setRowHeight(26);
        setShowGrid(false);
        setFillsViewportHeight(true);
        getColumnModel().getColumn(0).setPreferredWidth(180);
        getColumnModel().getColumn(0).setMaxWidth(320);
        getColumnModel().getColumn(0).setCellRenderer(new NameRenderer());
        getColumnModel().getColumn(1).setCellRenderer(new KeyRenderer());
        getTableHeader().setDefaultRenderer(new HeaderRenderer());
    }

    public TimelineModel getTimelineModel() { return timelineModel; }

    public void setSVGElement(SVGElement element) {
        selectedKeyIndex = -1;
        timelineModel.setSVGElement(element);
    }

    public SVGElement getSVGElement() { return timelineModel.getSVGElement(); }

    public void setMaximum(int max) {
        int old = maximum;
        maximum = Math.max(1, max);
        repaint();
        getTableHeader().repaint();
        firePropertyChange(MAXIMUM_PROPERTY, old, maximum);
    }

    public int getMaximum() { return maximum; }

    public void setCurrentTime(float seconds) {
        currentTime = Math.max(0f, Math.min(maximum, seconds));
        repaint();
        getTableHeader().repaint();
    }

    public float getCurrentTime() { return currentTime; }

    public float secondsAtX(int xInCell) {
        int width = Math.max(1, getColumnModel().getColumn(1).getWidth());
        float ratio = Math.max(0f, Math.min(1f, xInCell / (float)width));
        return ratio * maximum;
    }

    public void setSelectedKeyIndex(int index) {
        selectedKeyIndex = index;
        repaint();
    }

    public int getSelectedKeyIndex() { return selectedKeyIndex; }

    public int keyIndexAt(int row, int xInCell) {
        SMILTrack track = timelineModel.getTrackAtModelRow(row);
        if (track == null || track.isSetTrack() || track.isMotionTrack()) return -1;
        int width = getColumnModel().getColumn(1).getWidth();
        java.util.List<Float> times = track.getKeyTimes();
        int best = -1;
        int bestDist = 9999;
        for (int i=0; i<times.size(); i++) {
            int px = Math.round(times.get(i) * track.getDurationSeconds() / maximum * width);
            int d = Math.abs(px - xInCell);
            if (d < bestDist) { best = i; bestDist = d; }
        }
        return bestDist <= 10 ? best : -1;
    }

    private final class NameRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            if (row == 0 && value instanceof Element) {
                Element e = (Element)value;
                String local = e.getLocalName();
                if (local == null) local = e.getTagName();
                String id = e.getAttribute("id");
                setText((timelineModel.isExpanded() ? "▼ " : "▶ ") + "<" + local +
                        (id.isEmpty() ? "" : "#" + id) + ">");
                setFont(getFont().deriveFont(java.awt.Font.BOLD));
            } else if (value instanceof SMILTrack) {
                SMILTrack track = (SMILTrack)value;
                String id = track.getTrackId();
                setText("    ◆ " + track.getName()
                        + (id == null || id.isEmpty() ? "" : " [" + id + "]"));
                setFont(getFont().deriveFont(java.awt.Font.PLAIN));
            }
            return this;
        }
    }

    private final class KeyRenderer extends JLabel implements TableCellRenderer {
        private SMILTrack track;
        private boolean selectedRow;

        KeyRenderer() { setOpaque(true); }

        @Override public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            track = value instanceof SMILTrack ? (SMILTrack)value : null;
            selectedRow = selected;
            setBackground(selected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (track == null) return;
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = getHeight()/2;
            g2.setColor(selectedRow ? getForeground() : Color.GRAY);
            g2.drawLine(0, y, getWidth(), y);

            int playheadX = Math.round(currentTime / maximum * getWidth());
            g2.setColor(new Color(180, 60, 60));
            g2.drawLine(playheadX, 0, playheadX, getHeight());

            g2.setColor(selectedRow ? getForeground() : Color.GRAY);
            if (track.isSetTrack() || track.isMotionTrack()) {
                float seconds = track.getBeginSeconds();
                int x = Math.round(seconds / maximum * getWidth());
                Polygon diamond = new Polygon(
                        new int[]{x, x+5, x, x-5},
                        new int[]{y-5, y, y+5, y}, 4);
                g2.fillPolygon(diamond);
            } else {
                java.util.List<Float> times = track.getKeyTimes();
                for (int i=0; i<times.size(); i++) {
                    float seconds = times.get(i) * track.getDurationSeconds();
                    int x = Math.round(seconds / maximum * getWidth());
                    Polygon diamond = new Polygon(
                            new int[]{x, x+5, x, x-5},
                            new int[]{y-5, y, y+5, y}, 4);
                    if (i == selectedKeyIndex && getSelectedRow() > 0) {
                        g2.fillPolygon(diamond);
                    } else {
                        g2.drawPolygon(diamond);
                    }
                }
            }
            g2.dispose();
        }
    }

    private final class HeaderRenderer extends JLabel implements TableCellRenderer {
        private int column;

        HeaderRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
        }

        @Override public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focus, int row, int col) {
            column = col;
            setText(col == 0 ? "Object / Track" : "");
            setBackground(table.getTableHeader().getBackground());
            setForeground(table.getTableHeader().getForeground());
            setBorder(javax.swing.UIManager.getBorder("TableHeader.cellBorder"));
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (column != 1) return;

            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int h = getHeight();
            int w = Math.max(1, getWidth());

            int major;
            if (maximum <= 10) major = 1;
            else if (maximum <= 30) major = 2;
            else if (maximum <= 60) major = 5;
            else if (maximum <= 180) major = 10;
            else major = 30;

            g2.setColor(getForeground());
            for (int s=0; s<=maximum; s+=major) {
                int x = Math.round(s / (float)maximum * w);
                g2.drawLine(x, h-8, x, h-1);
                String label = s + "s";
                if (x + 4 < w) g2.drawString(label, x + 3, Math.max(11, h-10));
            }

            int playheadX = Math.round(currentTime / maximum * w);
            g2.setColor(new Color(180, 60, 60));
            g2.drawLine(playheadX, 0, playheadX, h);

            g2.dispose();
        }
    }
}
