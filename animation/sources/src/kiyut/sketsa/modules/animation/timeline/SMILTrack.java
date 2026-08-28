package kiyut.sketsa.modules.animation.timeline;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;

public final class SMILTrack {
    private final Element animationElement;
    private final String name;
    private final String kind;

    public SMILTrack(Element animationElement, String name, String kind) {
        this.animationElement = animationElement;
        this.name = name;
        this.kind = kind;
    }

    public Element getAnimationElement() { return animationElement; }
    public String getName() { return name; }
    public String getKind() { return kind; }

    public float getDurationSeconds() {
        String dur = animationElement.getAttribute("dur");
        if (dur == null || dur.trim().isEmpty()) return 5f;
        dur = dur.trim();
        try {
            if (dur.endsWith("ms")) {
                return Math.max(0.001f,
                        Float.parseFloat(dur.substring(0, dur.length()-2)) / 1000f);
            }
            if (dur.endsWith("min")) {
                return Math.max(0.001f,
                        Float.parseFloat(dur.substring(0, dur.length()-3)) * 60f);
            }
            if (dur.endsWith("h")) {
                return Math.max(0.001f,
                        Float.parseFloat(dur.substring(0, dur.length()-1)) * 3600f);
            }
            if (dur.endsWith("s")) {
                return Math.max(0.001f,
                        Float.parseFloat(dur.substring(0, dur.length()-1)));
            }
            return Math.max(0.001f, Float.parseFloat(dur));
        } catch (RuntimeException ex) {
            return 5f;
        }
    }

    public List<Float> getKeyTimes() {
        String raw = animationElement.getAttribute("keyTimes");
        List<Float> out = new ArrayList<Float>();
        if (raw != null && !raw.trim().isEmpty()) {
            String[] parts = raw.split(";");
            for (String p : parts) {
                try { out.add(Float.parseFloat(p.trim())); }
                catch (RuntimeException ex) { }
            }
        }
        if (out.isEmpty()) {
            List<String> values = getValues();
            if (values.size() <= 1) {
                out.add(0f);
            } else {
                for (int i=0; i<values.size(); i++) {
                    out.add((float)i / (float)(values.size()-1));
                }
            }
        }
        return out;
    }

    public List<String> getValues() {
        String raw = animationElement.getAttribute("values");
        List<String> out = new ArrayList<String>();
        if (raw != null && !raw.isEmpty()) {
            for (String p : raw.split(";")) out.add(p.trim());
        }
        if (out.isEmpty()) {
            String from = animationElement.getAttribute("from");
            String to = animationElement.getAttribute("to");
            if (!from.isEmpty()) out.add(from);
            if (!to.isEmpty()) out.add(to);
        }
        return out;
    }

    public String getFromRaw() {
        String v = animationElement.getAttribute("from");
        return v == null ? "" : v.trim();
    }

    public String getToRaw() {
        String v = animationElement.getAttribute("to");
        return v == null ? "" : v.trim();
    }

    public String getByRaw() {
        String v = animationElement.getAttribute("by");
        return v == null ? "" : v.trim();
    }

    public void setDurationSeconds(float seconds) {
        animationElement.setAttribute("dur", trim(seconds) + "s");
    }

    public float getBeginSeconds() {
        String begin = animationElement.getAttribute("begin");
        if (begin == null || begin.trim().isEmpty()) return 0f;
        begin = begin.trim();
        try {
            if (begin.endsWith("ms")) {
                return Float.parseFloat(begin.substring(0, begin.length()-2)) / 1000f;
            }
            if (begin.endsWith("s")) begin = begin.substring(0, begin.length()-1);
            return Math.max(0f, Float.parseFloat(begin));
        } catch (RuntimeException ex) {
            // Event/syncbase timing is deferred to M5 preview.
            return 0f;
        }
    }

    public String getBeginRaw() {
        String v = animationElement.getAttribute("begin");
        return v == null || v.trim().isEmpty() ? "0s" : v.trim();
    }

    public String getTrackId() {
        String v = animationElement.getAttribute("id");
        return v == null ? "" : v.trim();
    }

    public void setTrackId(String id) {
        String v = id == null ? "" : id.trim();
        if (v.isEmpty()) animationElement.removeAttribute("id");
        else animationElement.setAttribute("id", v);
    }

    public void setBeginRaw(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty() || "0".equals(v) || "0s".equals(v)) animationElement.removeAttribute("begin");
        else animationElement.setAttribute("begin", v);
    }

    public String getEndRaw() {
        String v = animationElement.getAttribute("end");
        return v == null ? "" : v.trim();
    }

    public void setEndRaw(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) animationElement.removeAttribute("end");
        else animationElement.setAttribute("end", v);
    }

    public String getRepeatDur() {
        String v = animationElement.getAttribute("repeatDur");
        return v == null ? "" : v.trim();
    }

    public void setRepeatDur(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) animationElement.removeAttribute("repeatDur");
        else animationElement.setAttribute("repeatDur", v);
    }

    public String getRestart() {
        String v = animationElement.getAttribute("restart");
        if ("whenNotActive".equals(v) || "never".equals(v)) return v;
        return "always";
    }

    public void setRestart(String value) {
        String v = value == null ? "always" : value.trim();
        if ("always".equals(v) || v.isEmpty()) animationElement.removeAttribute("restart");
        else if ("whenNotActive".equals(v) || "never".equals(v)) animationElement.setAttribute("restart", v);
        else animationElement.removeAttribute("restart");
    }

    public void setBeginSeconds(float seconds) {
        if (seconds <= 0.000001f) animationElement.removeAttribute("begin");
        else animationElement.setAttribute("begin", trim(seconds) + "s");
    }

    public String getRepeatCount() {
        String v = animationElement.getAttribute("repeatCount");
        return v == null || v.trim().isEmpty() ? "1" : v.trim();
    }

    public void setRepeatCount(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty() || "1".equals(v)) animationElement.removeAttribute("repeatCount");
        else animationElement.setAttribute("repeatCount", v);
    }

    public String getFillMode() {
        String v = animationElement.getAttribute("fill");
        return "remove".equals(v) ? "remove" : "freeze";
    }

    public void setFillMode(String mode) {
        animationElement.setAttribute("fill", "remove".equals(mode) ? "remove" : "freeze");
    }

    public String getCalcMode() {
        String v = animationElement.getAttribute("calcMode");
        if ("discrete".equals(v)
                || "spline".equals(v)
                || "paced".equals(v)) return v;
        return "linear";
    }

    public void setCalcMode(String mode) {
        if (!"discrete".equals(mode)
                && !"spline".equals(mode)
                && !"paced".equals(mode)) mode = "linear";
        animationElement.setAttribute("calcMode", mode);
    }

    public String getAdditive() {
        String v = animationElement.getAttribute("additive");
        return "sum".equals(v) ? "sum" : "replace";
    }

    public String getAccumulate() {
        String v = animationElement.getAttribute("accumulate");
        return "sum".equals(v) ? "sum" : "none";
    }

    public void setAdditive(String value) {
        String v = value == null ? "replace" : value.trim();
        if ("sum".equals(v)) animationElement.setAttribute("additive", "sum");
        else animationElement.removeAttribute("additive");
    }

    public void setAccumulate(String value) {
        String v = value == null ? "none" : value.trim();
        if ("sum".equals(v)) animationElement.setAttribute("accumulate", "sum");
        else animationElement.removeAttribute("accumulate");
    }

    public String getKeySplines() {
        String v = animationElement.getAttribute("keySplines");
        return v == null ? "" : v.trim();
    }

    public void setKeySplines(String splines) {
        String v = splines == null ? "" : splines.trim();
        if (v.isEmpty()) animationElement.removeAttribute("keySplines");
        else animationElement.setAttribute("keySplines", v);
    }

    public boolean isMotionTrack() {
        return "animateMotion".equals(kind);
    }

    public boolean isSetTrack() {
        return "set".equals(kind);
    }

    public String getSetAttribute() {
        return animationElement.getAttribute("attributeName");
    }

    public String getSetValue() {
        return animationElement.getAttribute("to");
    }

    public void setSetValue(String value) {
        animationElement.setAttribute("to", value == null ? "" : value.trim());
    }

    public String getMotionPathId() {
        String own = animationElement.getAttribute("data-sketsa-motion-path-id");
        if (own != null && !own.trim().isEmpty()) return own.trim();

        org.w3c.dom.NodeList children = animationElement.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (!(n instanceof Element)) continue;
            Element e = (Element)n;

            String local = e.getLocalName();
            String tag = e.getTagName();
            boolean isMPath = "mpath".equals(local)
                    || "mpath".equals(tag)
                    || (tag != null && tag.endsWith(":mpath"));
            if (!isMPath) continue;

            /*
             * Be deliberately tolerant here. SVG 1.1 files in Sketsa/Batik
             * can expose xlink:href differently depending on how the document
             * was parsed/reloaded. Check all common DOM representations.
             */
            String href = e.getAttribute("href");

            if (href == null || href.trim().isEmpty()) {
                href = e.getAttribute("xlink:href");
            }

            if (href == null || href.trim().isEmpty()) {
                href = e.getAttributeNS("http://www.w3.org/1999/xlink", "href");
            }

            if (href == null || href.trim().isEmpty()) {
                org.w3c.dom.NamedNodeMap attrs = e.getAttributes();
                for (int a=0; a<attrs.getLength(); a++) {
                    org.w3c.dom.Node attr = attrs.item(a);
                    if (attr == null) continue;
                    String attrLocal = attr.getLocalName();
                    String attrName = attr.getNodeName();
                    if ("href".equals(attrLocal)
                            || "href".equals(attrName)
                            || (attrName != null && attrName.endsWith(":href"))) {
                        href = attr.getNodeValue();
                        if (href != null && !href.trim().isEmpty()) break;
                    }
                }
            }

            if (href != null) {
                href = href.trim();
                if (href.startsWith("#") && href.length() > 1) {
                    return href.substring(1);
                }
            }
        }
        return "";
    }

    public String getMotionPathData(Element documentRoot) {
        /*
         * SVG animateMotion supports two path sources:
         * 1. <mpath href="#id"> / xlink:href
         * 2. inline path="..."
         *
         * When both happen to exist, prefer <mpath>. This matches the editor's
         * authoring model and avoids competing path sources in imported files.
         */
        String id = getMotionPathId();
        if (id != null && !id.trim().isEmpty() && documentRoot != null) {
            Element path = findElementById(documentRoot, id.trim());
            if (path != null) {
                String d = path.getAttribute("d");
                if (d != null && !d.trim().isEmpty()) return d.trim();
            }
        }

        String inline = animationElement.getAttribute("path");
        return inline == null ? "" : inline.trim();
    }

    private Element findElementById(Element root, String id) {
        if (root == null || id == null || id.isEmpty()) return null;
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

    public void setMotionPathId(String id) {
        String v = id == null ? "" : id.trim();
        animationElement.setAttribute("data-sketsa-motion-path-id", v);

        org.w3c.dom.NodeList children = animationElement.getChildNodes();
        Element mpath = null;
        for (int i=0; i<children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (!(n instanceof Element)) continue;
            Element e = (Element)n;
            String local = e.getLocalName();
            if (local == null) local = e.getTagName();
            if (local != null && local.indexOf(':') >= 0) {
                local = local.substring(local.indexOf(':') + 1);
            }
            if ("mpath".equals(local)) { mpath = e; break; }
        }

        if (v.isEmpty()) {
            /*
             * Empty ID means no referenced path. Remove an editor-generated
             * mpath instead of serializing href="#". Inline path="..." is
             * deliberately left untouched so imported inline Motion remains
             * valid and editable in the DOM.
             */
            animationElement.removeAttribute("data-sketsa-motion-path-id");
            if (mpath != null) animationElement.removeChild(mpath);
            return;
        }

        // Explicit mpath binding owns the path source.
        animationElement.removeAttribute("path");

        if (mpath == null) {
            mpath = animationElement.getOwnerDocument().createElementNS(
                    "http://www.w3.org/2000/svg", "mpath");
            animationElement.appendChild(mpath);
        }

        /*
         * SVG 1.1/Batik compatibility: if xlink:href is written, ensure the
         * document root declares the xlink namespace so serialized SVG can be
         * reopened without an unbound-prefix SAXParseException.
         */
        Element root = animationElement.getOwnerDocument().getDocumentElement();
        if (root != null) {
            root.setAttributeNS(
                    "http://www.w3.org/2000/xmlns/",
                    "xmlns:xlink",
                    "http://www.w3.org/1999/xlink");
        }

        String ref = "#" + v;
        mpath.setAttributeNS(null, "href", ref);
        mpath.setAttributeNS(
                "http://www.w3.org/1999/xlink", "xlink:href", ref);
    }

    public String getInlineMotionPath() {
        String v = animationElement.getAttribute("path");
        return v == null ? "" : v.trim();
    }

    public void setInlineMotionPath(String pathData) {
        String v = pathData == null ? "" : pathData.trim();
        setMotionPathId("");
        if (v.isEmpty()) animationElement.removeAttribute("path");
        else animationElement.setAttribute("path", v);
    }

    public String getMotionRotate() {
        String v = animationElement.getAttribute("rotate");
        return v == null || v.trim().isEmpty() ? "auto" : v.trim();
    }

    public void setMotionRotate(String rotate) {
        String v = rotate == null ? "auto" : rotate.trim();
        if (v.isEmpty()) v = "auto";
        animationElement.setAttribute("rotate", v);
    }

    public void setKeys(List<Float> times, List<String> values) {
        StringBuilder kt = new StringBuilder();
        StringBuilder vv = new StringBuilder();
        for (int i=0; i<times.size(); i++) {
            if (i > 0) { kt.append(';'); vv.append(';'); }
            kt.append(trim(times.get(i)));
            vv.append(values.get(i));
        }
        animationElement.setAttribute("keyTimes", kt.toString());
        animationElement.setAttribute("values", vv.toString());
        animationElement.removeAttribute("from");
        animationElement.removeAttribute("to");
        animationElement.removeAttribute("by");
    }

    public static String trim(float v) {
        if (Math.abs(v - Math.round(v)) < 0.000001f) return Integer.toString(Math.round(v));
        String s = String.format(java.util.Locale.US, "%.4f", v);
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
            s = s.substring(0, s.length()-1);
        }
        return s;
    }
}
