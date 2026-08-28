package kiyut.sketsa.modules.audio.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Compiles the DOM-backed Audio Tree into a neutral runtime IR. */
final class AudioTreeCompiler {
    static final String NS = AudioTreePanel.AUDIO_TREE_NS;
    static final String META_ID = AudioTreePanel.META_ID;

    static final class NodeInfo {
        final String id;
        final String type;
        final String label;
        final Map<String,Object> params = new LinkedHashMap<>();
        NodeInfo(String id, String type, String label) {
            this.id=id; this.type=type; this.label=label;
        }
    }

    static final class Connection {
        final String from,to,role,referenceId;
        final double amount;
        Connection(String from,String to,String role,double amount,String referenceId){
            this.from=from;this.to=to;this.role=role;this.amount=amount;this.referenceId=referenceId;
        }
    }

    static final class Result {
        final Map<String,NodeInfo> nodes=new LinkedHashMap<>();
        final List<Connection> connections=new ArrayList<>();
        final List<Map<String,Object>> modulations=new ArrayList<>();
        final List<Map<String,Object>> automations=new ArrayList<>();
        final List<Map<String,Object>> events=new ArrayList<>();
        final List<Map<String,Object>> sidechains=new ArrayList<>();
        final List<String> errors=new ArrayList<>();
        final List<String> warnings=new ArrayList<>();
        final List<String> infos=new ArrayList<>();
        boolean isValid(){return errors.isEmpty();}
    }

    private AudioTreeCompiler(){}

    static boolean hasTree(Document d){return findMetadata(d)!=null;}

    static Result compile(Document d){
        Result out=new Result();
        Element metadata=findMetadata(d);
        if(metadata==null){out.errors.add("Audio Tree metadata not found.");return out;}
        Element tree=firstChild(metadata,"tree");
        if(tree==null){out.errors.add("Audio Tree root not found.");return out;}
        validateStableIds(tree,out);
        collectNodes(tree,out);
        if(!out.errors.isEmpty())return out;
        collectOwnershipConnections(tree,out);
        collectReferenceConnections(tree,out);
        collectAutomationTargets(tree,out);
        detectCycles(out);
        if(out.isValid()) out.infos.add("Audio Tree validation completed successfully: "+out.nodes.size()+" owned nodes, "+out.connections.size()+" routed connections.");
        return out;
    }

    static String toJavascript(Result r){
        StringBuilder s=new StringBuilder();
        s.append("(function(){window.__SketsaAudioTreeIR={version:\"1.5\",valid:").append(r.isValid()).append(",nodes:[");
        boolean first=true;
        for(NodeInfo n:r.nodes.values()){
            if(!first)s.append(','); first=false;
            s.append("{id:").append(js(n.id)).append(",type:").append(js(n.type)).append(",label:").append(js(n.label)).append(",params:").append(jsObject(n.params)).append('}');
        }
        s.append("],connections:[");first=true;
        for(Connection c:r.connections){
            if(!first)s.append(','); first=false;
            s.append("{from:").append(js(c.from)).append(",to:").append(js(c.to)).append(",role:").append(js(c.role))
             .append(",amount:").append(c.amount).append(",referenceId:").append(js(c.referenceId)).append('}');
        }
        s.append("],modulations:").append(jsMapArray(r.modulations)).append(",automations:").append(jsMapArray(r.automations))
         .append(",events:").append(jsMapArray(r.events)).append(",sidechains:").append(jsMapArray(r.sidechains)).append(",errors:").append(jsArray(r.errors)).append(",warnings:").append(jsArray(r.warnings)).append(",infos:").append(jsArray(r.infos)).append("};})();\n");
        return s.toString();
    }

    private static void validateStableIds(Node root, Result out){
        Set<String> ids=new HashSet<>();
        validateStableIdsRecursive(root,out,ids);
    }

    private static void validateStableIdsRecursive(Node n,Result out,Set<String> ids){
        if(n instanceof Element){
            Element e=(Element)n;
            if(isModel(e,"node")||isModel(e,"reference")){
                String id=e.getAttribute("id").trim();
                String kind=isModel(e,"reference")?"Reference":"Owned node";
                if(id.isEmpty()) out.errors.add(kind+" without stable ID: "+label(e));
                else if(!ids.add(id)) out.errors.add("Duplicate Audio Tree stable ID: "+id);
            }
        }
        NodeList kids=n.getChildNodes();
        for(int i=0;i<kids.getLength();i++) validateStableIdsRecursive(kids.item(i),out,ids);
    }

    private static void collectNodes(Node parent,Result out){
        NodeList children=parent.getChildNodes();
        for(int i=0;i<children.getLength();i++){
            Node n=children.item(i); if(!(n instanceof Element))continue;
            Element e=(Element)n;
            if(isModel(e,"node")){
                String id=e.getAttribute("id").trim();
                String type=defaultString(e.getAttribute("type"),"group");
                if(id.isEmpty())out.errors.add("Owned node without stable ID: "+label(e));
                else if(!out.nodes.containsKey(id)) {
                    NodeInfo info=new NodeInfo(id,type,label(e));
                    readNodeParams(e,info,out);
                    out.nodes.put(id,info);
                }
            }
            collectNodes(e,out);
        }
    }

    private static void readNodeParams(Element e,NodeInfo n,Result out){
        switch(n.type){
            case "source":
                n.params.put("waveform",enumValue(e,"waveform","sine",new String[]{"sine","square","sawtooth","triangle"},out,n.id));
                n.params.put("frequency",number(e,"frequency",440,20,20000,out,n.id));
                n.params.put("level",number(e,"level",0.05,0,1,out,n.id));
                break;
            case "gain":
                n.params.put("gain",number(e,"gain",1,0,4,out,n.id));
                break;
            case "pan":
                n.params.put("pan",number(e,"pan",0,-1,1,out,n.id));
                break;
            case "filter":
                n.params.put("filterType",enumValue(e,"filterType","lowpass",new String[]{"lowpass","highpass","bandpass","lowshelf","highshelf","peaking","notch","allpass"},out,n.id));
                n.params.put("frequency",number(e,"filterFrequency",1200,10,24000,out,n.id));
                n.params.put("q",number(e,"filterQ",0.707,0.0001,1000,out,n.id));
                n.params.put("gain",number(e,"filterGain",0,-40,40,out,n.id));
                break;
            case "compressor":
                n.params.put("threshold",number(e,"threshold",-24,-100,0,out,n.id));
                n.params.put("knee",number(e,"knee",30,0,40,out,n.id));
                n.params.put("ratio",number(e,"ratio",12,1,20,out,n.id));
                n.params.put("attack",number(e,"attack",0.003,0,1,out,n.id));
                n.params.put("release",number(e,"release",0.25,0,1,out,n.id));
                break;
            case "analyser":
                n.params.put("fftSize",fftSize(e.getAttribute("fftSize"),out,n.id));
                n.params.put("smoothing",number(e,"smoothing",0.8,0,1,out,n.id));
                break;
            case "return":
                n.params.put("gain",number(e,"returnGain",1,0,4,out,n.id));
                break;
            case "delay":
                n.params.put("delayTime",number(e,"delayTime",0.28,0,10,out,n.id));
                break;
            case "reverb":
                n.params.put("duration",number(e,"reverbDuration",1.5,0.05,12,out,n.id));
                n.params.put("decay",number(e,"reverbDecay",2.0,0.1,10,out,n.id));
                break;
            case "lfo":
                n.params.put("waveform",enumValue(e,"waveform","sine",new String[]{"sine","square","sawtooth","triangle"},out,n.id));
                n.params.put("frequency",number(e,"frequency",2.0,0.01,100,out,n.id));
                n.params.put("depth",number(e,"depth",0.25,0,10000,out,n.id));
                break;
            case "automation":
                String curve=defaultString(e.getAttribute("curve"),"0:0,1:1");
                validateAutomationCurve(curve,out,n.id);
                n.params.put("curve",curve);
                break;
            case "master": case "bus": case "effect": case "group":
                break;
            default:
                out.warnings.add("Unknown node type '"+n.type+"' compiled as pass-through GainNode: "+n.id);
        }
    }

    private static void validateAutomationCurve(String curve,Result out,String id){
        String[] points=curve.split(",");
        double previous=-Double.MAX_VALUE;
        for(String point:points){
            String[] pair=point.trim().split(":",-1);
            if(pair.length!=2){out.errors.add("Invalid automation curve point on "+id+": "+point.trim());return;}
            try{
                double t=Double.parseDouble(pair[0].trim()), v=Double.parseDouble(pair[1].trim());
                if(Double.isNaN(t)||Double.isInfinite(t)||Double.isNaN(v)||Double.isInfinite(v)) throw new NumberFormatException();
                if(t<0){out.errors.add("Automation time below zero on "+id+": "+t);return;}
                if(t<previous){out.errors.add("Automation times are not ordered on "+id);return;}
                previous=t;
            }catch(NumberFormatException ex){out.errors.add("Invalid automation curve point on "+id+": "+point.trim());return;}
        }
    }

    private static double number(Element e,String attr,double fallback,double min,double max,Result out,String id){
        String raw=e.getAttribute(attr).trim(); if(raw.isEmpty())return fallback;
        try{
            double v=Double.parseDouble(raw); if(Double.isNaN(v)||Double.isInfinite(v))throw new NumberFormatException();
            if(v<min){out.warnings.add(attr+" below range on "+id+"; clamped to "+min);return min;}
            if(v>max){out.warnings.add(attr+" above range on "+id+"; clamped to "+max);return max;}
            return v;
        }catch(NumberFormatException ex){out.errors.add("Invalid "+attr+" on "+id+": "+raw);return fallback;}
    }

    private static String enumValue(Element e,String attr,String fallback,String[] allowed,Result out,String id){
        String v=defaultString(e.getAttribute(attr),fallback);
        for(String a:allowed)if(a.equals(v))return v;
        out.warnings.add("Unsupported "+attr+" '"+v+"' on "+id+"; using "+fallback);
        return fallback;
    }

    private static int fftSize(String raw,Result out,String id){
        if(raw==null||raw.trim().isEmpty())return 2048;
        try{
            int v=Integer.parseInt(raw.trim());
            if(v>=32&&v<=32768&&(v&(v-1))==0)return v;
        }catch(NumberFormatException ex){}
        out.errors.add("Invalid fftSize on "+id+": "+raw+" (must be power of two from 32 to 32768)");
        return 2048;
    }

    private static void collectOwnershipConnections(Node parent,Result out){
        NodeList children=parent.getChildNodes();
        for(int i=0;i<children.getLength();i++){
            Node n=children.item(i); if(!(n instanceof Element))continue;
            Element e=(Element)n;
            if(isModel(e,"node")){
                String nodeType=defaultString(e.getAttribute("type"),"group");
                Element owner=nearestOwnedParent(e.getParentNode());
                if(owner!=null && !"lfo".equals(nodeType) && !"automation".equals(nodeType))
                    out.connections.add(new Connection(e.getAttribute("id"),owner.getAttribute("id"),"parent",1.0,""));
            }
            collectOwnershipConnections(e,out);
        }
    }

    private static void collectReferenceConnections(Node parent,Result out){
        NodeList children=parent.getChildNodes();
        for(int i=0;i<children.getLength();i++){
            Node n=children.item(i);if(!(n instanceof Element))continue;
            Element e=(Element)n;
            if(isModel(e,"reference")){
                Element source=nearestOwnedParent(e.getParentNode());
                String target=e.getAttribute("targetId").trim();
                String role=defaultString(e.getAttribute("role"),"route");
                if(source==null)out.errors.add("Reference "+e.getAttribute("id")+" has no owned source node.");
                else if(!out.nodes.containsKey(target))out.errors.add("Broken reference "+e.getAttribute("id")+": target "+target+" not found.");
                else if("route".equals(role)||"send".equals(role)){
                    double amount="send".equals(role)?parseAmount(e.getAttribute("amount"),out,e.getAttribute("id")):1.0;
                    if("send".equals(role)){
                        NodeInfo targetInfo=out.nodes.get(target);
                        String tt=targetInfo==null?"":targetInfo.type;
                        if(!("return".equals(tt)||"bus".equals(tt)||"effect".equals(tt)||"delay".equals(tt)||"reverb".equals(tt)))
                            out.warnings.add("Send target "+target+" is type '"+tt+"'; T4 recommends a return/bus/effect target.");
                    }
                    out.connections.add(new Connection(source.getAttribute("id"),target,role,amount,e.getAttribute("id")));
                } else if("modulation".equals(role)){
                    NodeInfo sourceInfo=out.nodes.get(source.getAttribute("id"));
                    if(sourceInfo!=null && !"automation".equals(sourceInfo.type)){
                        String param=defaultString(e.getAttribute("targetParam"),"gain");
                        Map<String,Object> m=new LinkedHashMap<>();
                        m.put("from",source.getAttribute("id")); m.put("to",target); m.put("param",param);
                        m.put("amount",parseAmountWide(e.getAttribute("amount"),out,e.getAttribute("id")));
                        m.put("referenceId",e.getAttribute("id"));
                        out.modulations.add(m);
                    }
                } else if("event".equals(role)||"event-target".equals(role)){
                    String eventName=e.getAttribute("event").trim();
                    String action=defaultString(e.getAttribute("action"),"trigger");
                    if(eventName.isEmpty()) out.errors.add("Event reference "+e.getAttribute("id")+" has no event name.");
                    if(!isEventAction(action)) out.errors.add("Unsupported event action '"+action+"' on reference "+e.getAttribute("id"));
                    Map<String,Object> ev=new LinkedHashMap<>();
                    ev.put("id",e.getAttribute("id")); ev.put("event",eventName); ev.put("action",action); ev.put("target",target);
                    if("set-param".equals(action)){
                        String param=e.getAttribute("targetParam").trim();
                        if(param.isEmpty()) out.errors.add("Event set-param reference "+e.getAttribute("id")+" has no targetParam.");
                        else if(!supportsParam(out.nodes.get(target),param)) out.errors.add("Event target parameter not exposed: "+target+"."+param);
                        ev.put("param",param);
                        ev.put("scale",parseOptionalDouble(e,"scale",1.0,out)); ev.put("offset",parseOptionalDouble(e,"offset",0.0,out));
                        Double min=parseNullableDouble(e,"min",out), max=parseNullableDouble(e,"max",out);
                        if(min!=null)ev.put("min",min); if(max!=null)ev.put("max",max);
                        if(min!=null&&max!=null&&min>max)out.errors.add("Event mapping min > max on reference "+e.getAttribute("id"));
                    }
                    out.events.add(ev);
                } else if("sidechain".equals(role)){
                    Map<String,Object> sc=new LinkedHashMap<>();
                    sc.put("from",source.getAttribute("id")); sc.put("to",target); sc.put("referenceId",e.getAttribute("id"));
                    out.sidechains.add(sc);
                } else out.warnings.add("Reference role '"+role+"' is stored but not compiled in T8: "+e.getAttribute("id"));
            }
            collectReferenceConnections(e,out);
        }
    }

    private static void collectAutomationTargets(Node parent,Result out){
        NodeList children=parent.getChildNodes();
        for(int i=0;i<children.getLength();i++){
            Node n=children.item(i); if(!(n instanceof Element)) continue;
            Element e=(Element)n;
            if(isModel(e,"node") && "automation".equals(defaultString(e.getAttribute("type"),""))){
                NodeInfo info=out.nodes.get(e.getAttribute("id"));
                NodeList kids=e.getChildNodes();
                for(int k=0;k<kids.getLength();k++){
                    if(!(kids.item(k) instanceof Element)) continue;
                    Element ref=(Element)kids.item(k);
                    if(isModel(ref,"reference") && "modulation".equals(defaultString(ref.getAttribute("role"),""))){
                        String target=ref.getAttribute("targetId").trim();
                        if(!out.nodes.containsKey(target)){out.errors.add("Broken automation reference "+ref.getAttribute("id")+": target "+target+" not found.");continue;}
                        Map<String,Object> a=new LinkedHashMap<>();
                        a.put("id",e.getAttribute("id")); a.put("target",target);
                        a.put("param",defaultString(ref.getAttribute("targetParam"),"gain"));
                        a.put("curve",info==null?"0:0,1:1":String.valueOf(info.params.get("curve")));
                        a.put("referenceId",ref.getAttribute("id"));
                        out.automations.add(a);
                    }
                }
            }
            collectAutomationTargets(e,out);
        }
    }

    private static boolean isEventAction(String action){
        return "trigger".equals(action)||"start".equals(action)||"stop".equals(action)||"toggle".equals(action)||"set-param".equals(action);
    }

    private static boolean supportsParam(NodeInfo n,String p){
        if(n==null)return false;
        switch(n.type){
            case "gain": case "return": return "gain".equals(p);
            case "pan": return "pan".equals(p);
            case "filter": return "frequency".equals(p)||"q".equalsIgnoreCase(p)||"gain".equals(p);
            case "delay": return "delayTime".equals(p);
            case "compressor": return "threshold".equals(p)||"knee".equals(p)||"ratio".equals(p)||"attack".equals(p)||"release".equals(p);
            default: return false;
        }
    }

    private static double parseOptionalDouble(Element e,String attr,double fallback,Result out){
        String raw=e.getAttribute(attr).trim(); if(raw.isEmpty())return fallback;
        try{double v=Double.parseDouble(raw);if(Double.isNaN(v)||Double.isInfinite(v))throw new NumberFormatException();return v;}
        catch(NumberFormatException ex){out.errors.add("Invalid "+attr+" on reference "+e.getAttribute("id")+": "+raw);return fallback;}
    }
    private static Double parseNullableDouble(Element e,String attr,Result out){
        String raw=e.getAttribute(attr).trim(); if(raw.isEmpty())return null;
        try{double v=Double.parseDouble(raw);if(Double.isNaN(v)||Double.isInfinite(v))throw new NumberFormatException();return v;}
        catch(NumberFormatException ex){out.errors.add("Invalid "+attr+" on reference "+e.getAttribute("id")+": "+raw);return null;}
    }

    private static double parseAmountWide(String value,Result out,String refId){
        if(value==null||value.trim().isEmpty())return 1.0;
        try{double v=Double.parseDouble(value.trim());if(Double.isNaN(v)||Double.isInfinite(v))throw new NumberFormatException();return v;}
        catch(NumberFormatException ex){out.errors.add("Invalid modulation amount on reference "+refId+": "+value);return 1.0;}
    }

    private static double parseAmount(String value,Result out,String refId){
        if(value==null||value.trim().isEmpty())return 1.0;
        try{double v=Double.parseDouble(value.trim());if(Double.isNaN(v)||Double.isInfinite(v))throw new NumberFormatException();if(v<0){out.warnings.add("Send amount below 0 clamped for "+refId);return 0;}if(v>1){out.warnings.add("Send amount above 1 clamped for "+refId);return 1;}return v;}
        catch(NumberFormatException ex){out.errors.add("Invalid send amount on reference "+refId+": "+value);return 1.0;}
    }

    private static void detectCycles(Result out){
        Map<String,List<String>> edges=new HashMap<>();for(String id:out.nodes.keySet())edges.put(id,new ArrayList<>());
        for(Connection c:out.connections)if(edges.containsKey(c.from))edges.get(c.from).add(c.to);
        Set<String> visiting=new HashSet<>(),visited=new HashSet<>();
        for(String id:edges.keySet())if(dfsCycle(id,edges,visiting,visited)){out.errors.add("Routing cycle detected involving node: "+id);return;}
    }
    private static boolean dfsCycle(String id,Map<String,List<String>> edges,Set<String> visiting,Set<String> visited){if(visited.contains(id))return false;if(!visiting.add(id))return true;for(String to:edges.get(id))if(dfsCycle(to,edges,visiting,visited))return true;visiting.remove(id);visited.add(id);return false;}

    private static Element nearestOwnedParent(Node n){while(n instanceof Element){Element e=(Element)n;if(isModel(e,"node"))return e;n=n.getParentNode();}return null;}
    private static Element findMetadata(Document d){if(d==null)return null;NodeList list=d.getElementsByTagNameNS("http://www.w3.org/2000/svg","metadata");for(int i=0;i<list.getLength();i++){Element e=(Element)list.item(i);if(META_ID.equals(e.getAttribute("id")))return e;}return null;}
    private static Element firstChild(Element p,String local){NodeList children=p.getChildNodes();for(int i=0;i<children.getLength();i++)if(children.item(i) instanceof Element&&isModel((Element)children.item(i),local))return(Element)children.item(i);return null;}
    private static boolean isModel(Element e,String local){String l=e.getLocalName();if(l==null||l.isEmpty()){String n=e.getTagName();int c=n.indexOf(':');l=c>=0?n.substring(c+1):n;}return local.equals(l)&&(NS.equals(e.getNamespaceURI())||isUnderMetadata(e));}
    private static boolean isUnderMetadata(Element e){Node p=e.getParentNode();while(p instanceof Element){Element x=(Element)p;if(META_ID.equals(x.getAttribute("id")))return true;p=p.getParentNode();}return false;}
    private static String label(Element e){return defaultString(e.getAttribute("label"),defaultString(e.getAttribute("type"),"node"));}
    private static String defaultString(String s,String f){return s==null||s.trim().isEmpty()?f:s.trim();}
    private static String jsMapArray(List<Map<String,Object>> a){StringBuilder s=new StringBuilder("[");for(int i=0;i<a.size();i++){if(i>0)s.append(',');s.append(jsObject(a.get(i)));}return s.append(']').toString();}
    private static String jsArray(List<String>a){StringBuilder s=new StringBuilder("[");for(int i=0;i<a.size();i++){if(i>0)s.append(',');s.append(js(a.get(i)));}return s.append(']').toString();}
    private static String jsObject(Map<String,Object> m){StringBuilder s=new StringBuilder("{");boolean first=true;for(Map.Entry<String,Object>e:m.entrySet()){if(!first)s.append(',');first=false;s.append(js(e.getKey())).append(':');Object v=e.getValue();if(v instanceof Number||v instanceof Boolean)s.append(String.valueOf(v));else s.append(js(String.valueOf(v)));}return s.append('}').toString();}
    private static String js(String s){StringBuilder o=new StringBuilder("\"");for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='\\'||c=='\"')o.append('\\').append(c);else if(c=='\n')o.append("\\n");else if(c=='\r')o.append("\\r");else if(c=='\t')o.append("\\t");else if(c<32)o.append(String.format("\\u%04x",(int)c));else o.append(c);}return o.append('\"').toString();}
}
