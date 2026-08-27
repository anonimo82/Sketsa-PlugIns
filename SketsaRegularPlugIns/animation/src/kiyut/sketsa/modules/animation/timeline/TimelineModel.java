package kiyut.sketsa.modules.animation.timeline;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGElement;

public final class TimelineModel extends AbstractTableModel {
    private SVGElement element;
    private final List<SMILTrack> tracks = new ArrayList<SMILTrack>();
    private boolean expanded = true;

    public void setSVGElement(SVGElement element) {
        this.element = element;
        refresh();
    }

    public SVGElement getSVGElement() { return element; }
    public boolean isExpanded() { return expanded; }

    public void toggleExpanded() {
        expanded = !expanded;
        fireTableDataChanged();
    }

    public List<SMILTrack> getTracks() { return tracks; }

    public SMILTrack getTrackAtModelRow(int row) {
        if (!expanded || row <= 0) return null;
        int i = row - 1;
        return (i >= 0 && i < tracks.size()) ? tracks.get(i) : null;
    }

    public void refresh() {
        tracks.clear();
        if (element != null) {
            NodeList children = ((Element)element).getChildNodes();
            for (int i=0; i<children.getLength(); i++) {
                Node n = children.item(i);
                if (!(n instanceof Element)) continue;
                Element e = (Element)n;
                String local = e.getLocalName();
                if (local == null) local = e.getTagName();

                if ("animate".equals(local)) {
                    String attr = e.getAttribute("attributeName");
                    if (!attr.isEmpty()) tracks.add(new SMILTrack(e, attr, "animate"));
                } else if ("animateTransform".equals(local)) {
                    if ("true".equals(e.getAttribute("data-sketsa-pivot-helper"))) {
                        continue;
                    }
                    String type = e.getAttribute("type");
                    String name = type == null || type.isEmpty() ? "transform" : type;
                    tracks.add(new SMILTrack(e, name, "animateTransform"));
                } else if ("animateMotion".equals(local)) {
                    tracks.add(new SMILTrack(e, "Motion Path", "animateMotion"));
                } else if ("set".equals(local)) {
                    String attr = e.getAttribute("attributeName");
                    if (!attr.isEmpty()) {
                        tracks.add(new SMILTrack(e, "Set " + attr, "set"));
                    }
                }
            }
        }
        fireTableDataChanged();
    }

    @Override public int getColumnCount() { return 2; }
    @Override public String getColumnName(int col) { return col == 0 ? "Object / Track" : "Timeline"; }
    @Override public int getRowCount() {
        if (element == null) return 0;
        return 1 + (expanded ? tracks.size() : 0);
    }

    @Override public Object getValueAt(int row, int col) {
        if (row == 0) return element;
        return getTrackAtModelRow(row);
    }
}
