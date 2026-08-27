package kiyut.sketsa.modules.input.integration;

import java.io.StringWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Base64;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

final class PhysicsCompanionExporter {
    private PhysicsCompanionExporter() {}

    private static final String MATTER_VERSION = "0.20.0";
    private static final String MATTER_URL = "https://cdn.jsdelivr.net/npm/matter-js@0.20.0/build/matter.min.js";
    private static final String MATTER_SHA512 = "6+7rTBmR6pRFe9fa0vCFjFaHZj/XYa7774bEBzRtxgdpIJOS++R3cKd6Prg/eJmxtsJotd8KAg4g57uuVQsZKA==";

    static void exportStandalone(Document document, File htmlFile) throws Exception {
        String svg = serialize(document);
        boolean physics = hasPhysics(document);
        File parent = htmlFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create export directory: " + parent);
        }

        String html;
        if (!physics) {
            html = plainHtml(svg);
        } else {
            String base = stripExtension(htmlFile.getName());
            File assets = new File(parent, base + "-assets");
            if (!assets.exists() && !assets.mkdirs()) {
                throw new IOException("Could not create runtime assets directory: " + assets);
            }
            File matter = new File(assets, "matter.min.js");
            ensureMatterJs(matter);
            File runtime = new File(assets, "sketsa-physics-runtime.js");
            Files.write(runtime.toPath(), runtimeScript().getBytes(StandardCharsets.UTF_8));
            String assetPath = assets.getName();
            html = physicsHtml(svg, assetPath);
        }
        Files.write(htmlFile.toPath(), html.getBytes(StandardCharsets.UTF_8));
    }

    static boolean hasPhysics(Document document) {
        if (document == null || document.getDocumentElement() == null) return false;
        if (document.getElementsByTagName("*").getLength() == 0) return false;
        org.w3c.dom.NodeList all = document.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            org.w3c.dom.Node node = all.item(i);
            if (node instanceof org.w3c.dom.Element) {
                org.w3c.dom.Element e = (org.w3c.dom.Element) node;
                if (e.hasAttribute("data-sketsa-physics-body")) return true;
            }
        }
        return false;
    }

    private static String plainHtml(String svg) {
        return "<!doctype html>\n<html><head><meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
                + "<title>Sketsa Export</title>\n"
                + "<style>html,body{margin:0;padding:0;background:#fff}svg{display:block;max-width:100%;height:auto}</style>\n"
                + "</head><body>\n" + svg + "\n</body></html>\n";
    }

    private static String physicsHtml(String svg, String assetPath) {
        return "<!doctype html>\n"
                + "<html><head><meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
                + "<meta name=\"sketsa-physics-runtime\" content=\"0.9.5; contract 1.0; matter-js " + MATTER_VERSION + "\">\n"
                + "<title>Sketsa Physics Export</title>\n"
                + "<style>html,body{margin:0;padding:0;background:#fff;font-family:sans-serif}#sketsa-physics-controls{display:flex;gap:8px;flex-wrap:wrap;padding:8px;border-bottom:1px solid #ccc;background:#f5f5f5}#sketsa-physics-controls button{padding:6px 10px}#sketsa-physics-status{align-self:center;font-size:12px;color:#444}#sketsa-physics-events{padding:6px 10px;border-bottom:1px solid #ddd;background:#fafafa;font-size:12px}#sketsa-physics-event-summary{margin-left:8px}#sketsa-physics-event-log{margin:4px 0 0 18px;padding:0;max-height:90px;overflow:auto}svg{display:block;max-width:100%;height:auto}</style>\n"
                + "</head><body>\n"
                + "<div id=\"sketsa-physics-controls\"><button id=\"sketsa-physics-pause\" type=\"button\">Pause</button><button id=\"sketsa-physics-reset\" type=\"button\">Reset</button><button id=\"sketsa-physics-sleep\" type=\"button\">Sleep dynamics</button><button id=\"sketsa-physics-wake\" type=\"button\">Wake dynamics</button><span id=\"sketsa-physics-status\">Running</span></div>\n"
                + "<div id=\"sketsa-physics-events\"><strong>Physics events</strong><span id=\"sketsa-physics-event-summary\">start 0 · active 0 · end 0</span><ol id=\"sketsa-physics-event-log\"></ol></div>\n"
                + svg + "\n"
                + "<script src=\"" + assetPath + "/matter.min.js\"></script>\n"
                + "<script src=\"" + assetPath + "/sketsa-physics-runtime.js\"></script>\n"
                + "</body></html>\n";
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static void ensureMatterJs(File destination) throws Exception {
        if (destination.isFile() && verifyMatter(destination)) return;
        File cache = matterCacheFile();
        if (!cache.isFile() || !verifyMatter(cache)) {
            downloadMatter(cache);
        }
        Files.copy(cache.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        if (!verifyMatter(destination)) throw new IOException("Matter.js integrity check failed after copy");
    }

    private static File matterCacheFile() {
        File dir = new File(new File(System.getProperty("user.home"), ".sketsa-physics"), "matter-js-" + MATTER_VERSION);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "matter.min.js");
    }

    private static void downloadMatter(File cache) throws Exception {
        File parent = cache.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Could not create Matter.js cache directory");
        File temp = new File(parent, "matter.min.js.download");
        URLConnection connection = new URL(MATTER_URL).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "Sketsa-Physics/0.9.5");
        try (InputStream in = connection.getInputStream(); OutputStream out = Files.newOutputStream(temp.toPath())) {
            byte[] buffer = new byte[16384];
            int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
        } catch (Exception ex) {
            temp.delete();
            throw new IOException("Matter.js 0.20.0 is not cached and could not be downloaded for local export. Connect to the Internet once and export again.", ex);
        }
        if (!verifyMatter(temp)) {
            temp.delete();
            throw new IOException("Downloaded Matter.js failed SHA-512 verification");
        }
        Files.move(temp.toPath(), cache.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean verifyMatter(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            try (InputStream in = Files.newInputStream(file.toPath())) {
                byte[] buffer = new byte[16384];
                int n;
                while ((n = in.read(buffer)) >= 0) md.update(buffer, 0, n);
            }
            return MATTER_SHA512.equals(Base64.getEncoder().encodeToString(md.digest()));
        } catch (Exception ex) {
            return false;
        }
    }

    private static String serialize(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter out = new StringWriter();
        transformer.transform(new DOMSource(document.getDocumentElement()), new StreamResult(out));
        return out.toString();
    }

    private static String runtimeScript() {
        StringBuilder s = new StringBuilder();
        s.append("(function(){\n");
        s.append("'use strict';\n");
        s.append("if(!window.Matter){console.error('Matter.js failed to load');return;}\n");
        s.append("const M=Matter, svg=document.querySelector('svg'), status=document.getElementById('sketsa-physics-status');\n");
        s.append("const eventSummary=document.getElementById('sketsa-physics-event-summary'), eventLog=document.getElementById('sketsa-physics-event-log');\n");
        s.append("const eventCounts={collisionStart:0,collisionActive:0,collisionEnd:0};\n");
        s.append("const engine=M.Engine.create(), world=engine.world, entries=[], byId=new Map(), byBodyId=new Map(), constraints=[], constraintVisuals=[], runtimeIdCounts=new Map(); let paused=false, last=performance.now();\n");
        s.append("function num(el,name,fallback){const v=parseFloat(el.getAttribute(name));return Number.isFinite(v)?v:fallback;}\n");
        s.append("function clamp(v,min,max){return Math.max(min,Math.min(max,v));}\n");
        s.append("function uint(el,name,fallback){const raw=el.getAttribute(name);if(raw==null||raw==='')return fallback>>>0;const v=Number(raw);return Number.isFinite(v)?(Math.trunc(v)>>>0):(fallback>>>0);}\n");
        s.append("function sint(el,name,fallback){const raw=el.getAttribute(name);if(raw==null||raw==='')return fallback|0;const v=Number(raw);return Number.isFinite(v)?(Math.trunc(v)|0):(fallback|0);}\n");
        s.append("function boolAttr(el,name){return (el.getAttribute(name)||'false').toLowerCase()==='true';}\n");
        s.append("engine.gravity.x=num(svg,'data-sketsa-physics-gravity-x',0);\n");
        s.append("engine.gravity.y=num(svg,'data-sketsa-physics-gravity-y',1);\n");
        s.append("engine.gravity.scale=Math.max(0,num(svg,'data-sketsa-physics-gravity-scale',0.001));\n");
        s.append("engine.timing.timeScale=Math.max(0,num(svg,'data-sketsa-physics-time-scale',1));\n");
        s.append("engine.enableSleeping=(svg.getAttribute('data-sketsa-physics-enable-sleeping')||'false')==='true';\n");
        s.append("function mat(m){return {a:m.a,b:m.b,c:m.c,d:m.d,e:m.e,f:m.f};}\n");
        s.append("function mm(a,b){return {a:a.a*b.a+a.c*b.b,b:a.b*b.a+a.d*b.b,c:a.a*b.c+a.c*b.d,d:a.b*b.c+a.d*b.d,e:a.a*b.e+a.c*b.f+a.e,f:a.b*b.e+a.d*b.f+a.f};}\n");
        s.append("function mi(m){const det=m.a*m.d-m.b*m.c;if(Math.abs(det)<1e-12)return {a:1,b:0,c:0,d:1,e:0,f:0};return {a:m.d/det,b:-m.b/det,c:-m.c/det,d:m.a/det,e:(m.c*m.f-m.d*m.e)/det,f:(m.b*m.e-m.a*m.f)/det};}\n");
        s.append("function mp(m,p){return {x:m.a*p.x+m.c*p.y+m.e,y:m.b*p.x+m.d*p.y+m.f};}\n");
        s.append("function mt(x,y){return {a:1,b:0,c:0,d:1,e:x,f:y};}function mr(r){const c=Math.cos(r),q=Math.sin(r);return {a:c,b:q,c:-q,d:c,e:0,f:0};}\n");
        s.append("function matrixText(m){return 'matrix('+[m.a,m.b,m.c,m.d,m.e,m.f].map(function(v){return Number(v.toFixed(8));}).join(' ')+')';}\n");
        s.append("function localPolygon(el){const raw=el.getAttribute('points')||'';const n=raw.trim().split(/[\\s,]+/).map(Number).filter(Number.isFinite);const out=[];for(let i=0;i+1<n.length;i+=2)out.push({x:n[i],y:n[i+1]});return out.length>=3?out:null;}\n");
        s.append("function transformedVertices(el,box,shape,tag,rootMatrix){let local=[];const polygonLike=shape==='polygon'||(shape==='auto'&&(tag==='polygon'||tag==='polyline')),circleLike=shape==='circle'||(shape==='auto'&&(tag==='circle'||tag==='ellipse'));if(polygonLike)local=localPolygon(el)||[];if(circleLike){const cx=box.x+box.width/2,cy=box.y+box.height/2,rx=box.width/2,ry=box.height/2;for(let i=0;i<24;i++){const a=i*Math.PI*2/24;local.push({x:cx+Math.cos(a)*rx,y:cy+Math.sin(a)*ry});}}if(local.length<3)local=[{x:box.x,y:box.y},{x:box.x+box.width,y:box.y},{x:box.x+box.width,y:box.y+box.height},{x:box.x,y:box.y+box.height}];return local.map(function(p){return mp(rootMatrix,p);});}\n");
        s.append("function runtimeId(el,index){const base=(el.getAttribute('id')||'physics-'+(index+1)).trim()||('physics-'+(index+1));const n=(runtimeIdCounts.get(base)||0)+1;runtimeIdCounts.set(base,n);const id=n===1?base:base+'~'+n;el.setAttribute('data-sketsa-physics-runtime-id',id);return id;}\n");
        s.append("function applyImpulse(body,x,y){if(body.isStatic||(x===0&&y===0))return;const m=Number.isFinite(body.mass)&&body.mass>0?body.mass:1;M.Body.setVelocity(body,{x:body.velocity.x+x/m,y:body.velocity.y+y/m});}\n");
        s.append("function launch(entry){const b=entry.body, el=entry.el;if(b.isStatic)return;const fx=num(el,'data-sketsa-physics-force-x',0),fy=num(el,'data-sketsa-physics-force-y',0);if(fx!==0||fy!==0)M.Body.applyForce(b,b.position,{x:fx,y:fy});const ix=num(el,'data-sketsa-physics-impulse-x',0),iy=num(el,'data-sketsa-physics-impulse-y',0);applyImpulse(b,ix,iy);const tq=num(el,'data-sketsa-physics-torque',0);if(tq!==0)b.torque+=tq;}\n");
        s.append("document.querySelectorAll('[data-sketsa-physics-body]').forEach(function(el,index){\n");
        s.append("  let box,ctm;try{box=el.getBBox();ctm=el.getCTM();}catch(e){return;}if(!box||box.width<=0||box.height<=0||!ctm)return;const rootMatrix=mat(ctm);\n");
        s.append("  const type=el.getAttribute('data-sketsa-physics-body')||'dynamic', shape=el.getAttribute('data-sketsa-physics-shape')||'auto', opts={isStatic:type==='static'};\n");
        s.append("  const density=num(el,'data-sketsa-physics-density',NaN), friction=num(el,'data-sketsa-physics-friction',NaN), fs=num(el,'data-sketsa-physics-friction-static',NaN), fa=num(el,'data-sketsa-physics-friction-air',NaN), restitution=num(el,'data-sketsa-physics-restitution',NaN);\n");
        s.append("  if(Number.isFinite(density)&&density>0)opts.density=density;if(Number.isFinite(friction))opts.friction=clamp(friction,0,1);if(Number.isFinite(fs))opts.frictionStatic=Math.max(0,fs);if(Number.isFinite(fa))opts.frictionAir=clamp(fa,0,1);if(Number.isFinite(restitution))opts.restitution=clamp(restitution,0,1);opts.isSensor=boolAttr(el,'data-sketsa-physics-sensor');\n");
        s.append("  opts.collisionFilter={category:uint(el,'data-sketsa-physics-collision-category',1),mask:uint(el,'data-sketsa-physics-collision-mask',0xFFFFFFFF),group:sint(el,'data-sketsa-physics-collision-group',0)};\n");
        s.append("  const tag=(el.localName||'').toLowerCase(),verts=transformedVertices(el,box,shape,tag,rootMatrix),centre=M.Vertices.centre(verts);let body=M.Bodies.fromVertices(centre.x,centre.y,[verts],opts,true);if(!body)return;\n");
        s.append("  const mass=num(el,'data-sketsa-physics-mass',0);if(!opts.isStatic&&mass>0)M.Body.setMass(body,mass);const deg=num(el,'data-sketsa-physics-angle',0);if(deg!==0)M.Body.setAngle(body,deg*Math.PI/180);\n");
        s.append("  const vx=num(el,'data-sketsa-physics-velocity-x',0),vy=num(el,'data-sketsa-physics-velocity-y',0);if(!opts.isStatic&&(vx!==0||vy!==0))M.Body.setVelocity(body,{x:vx,y:vy});const av=num(el,'data-sketsa-physics-angular-velocity',0);if(!opts.isStatic&&av!==0)M.Body.setAngularVelocity(body,av);\n");
        s.append("  const wantSleep=boolAttr(el,'data-sketsa-physics-sleeping');if(!opts.isStatic&&wantSleep&&M.Sleeping)M.Sleeping.set(body,true);\n");
        s.append("  const rid=runtimeId(el,index),svgId=el.getAttribute('id')||'';M.Composite.add(world,body);const entry={el:el,body:body,runtimeId:rid,svgId:svgId,rootInitial:rootMatrix,bodyOrigin:{x:centre.x,y:centre.y},initial:{position:{x:body.position.x,y:body.position.y},angle:body.angle,velocity:{x:body.velocity.x,y:body.velocity.y},angularVelocity:body.angularVelocity,sleeping:wantSleep}};entries.push(entry);if(svgId&&!byId.has(svgId))byId.set(svgId,entry);byBodyId.set(body.id,entry);if(body.parts)body.parts.forEach(function(part){byBodyId.set(part.id,entry);});launch(entry);\n");
        s.append("});\n");
        s.append("function createConstraints(){entries.forEach(function(entry){const el=entry.el,type=(el.getAttribute('data-sketsa-physics-constraint')||'none').toLowerCase();if(type==='none'||type==='')return;const pointA={x:num(el,'data-sketsa-physics-constraint-point-a-x',0),y:num(el,'data-sketsa-physics-constraint-point-a-y',0)},pointB={x:num(el,'data-sketsa-physics-constraint-point-b-x',entry.body.position.x),y:num(el,'data-sketsa-physics-constraint-point-b-y',entry.body.position.y)};const opts={bodyA:entry.body,pointA:pointA,stiffness:clamp(num(el,'data-sketsa-physics-constraint-stiffness',1),0,1),damping:clamp(num(el,'data-sketsa-physics-constraint-damping',0),0,1)};const len=num(el,'data-sketsa-physics-constraint-length',-1);if(len>=0)opts.length=len;if(type==='distance'){const targetId=(el.getAttribute('data-sketsa-physics-constraint-target')||'').trim();const target=targetId?byId.get(targetId):null;if(target){opts.bodyB=target.body;opts.pointB=pointB;}else{opts.pointB=pointB;}}else if(type==='pin'){opts.pointB=pointB;if(len<0)opts.length=0;}else{return;}const c=M.Constraint.create(opts);constraints.push(c);M.Composite.add(world,c);const line=document.createElementNS('http://www.w3.org/2000/svg','line');line.setAttribute('stroke','#777');line.setAttribute('stroke-width','2');line.setAttribute('stroke-dasharray',type==='distance'&&opts.stiffness<0.5?'5 4':'');line.setAttribute('pointer-events','none');line.setAttribute('data-sketsa-physics-constraint-visual','true');svg.insertBefore(line,svg.firstChild);constraintVisuals.push({constraint:c,line:line});});}\n");
        s.append("createConstraints();\n");
        s.append("const CONTRACT_VERSION='1.0', RUNTIME_VERSION='0.9.5';\n");
        s.append("function copyPoint(p){return{x:Number(p&&p.x)||0,y:Number(p&&p.y)||0};}\n");
        s.append("function resolveEntry(target){if(target==null||target==='')return null;const key=String(target);let found=null;entries.some(function(x){if(x.runtimeId===key||x.svgId===key){found=x;return true;}return false;});return found;}\n");
        s.append("function bodyState(entry){if(!entry)return null;const b=entry.body;return{runtimeId:entry.runtimeId||'',svgId:entry.svgId||'',static:!!b.isStatic,sensor:!!b.isSensor,sleeping:!!b.isSleeping,position:copyPoint(b.position),angle:Number(b.angle)||0,velocity:copyPoint(b.velocity),angularVelocity:Number(b.angularVelocity)||0,mass:Number.isFinite(b.mass)?b.mass:null};}\n");
        s.append("function emitGeneric(type,data){const detail={contractVersion:CONTRACT_VERSION,source:'physics',type:type,data:data||{}};document.dispatchEvent(new CustomEvent('sketsa:physics:event',{detail:detail}));document.dispatchEvent(new CustomEvent('sketsa:runtime:event',{detail:detail}));return detail;}\n");
        s.append("function enabled(entry,phase){if(!entry)return false;const attr=phase==='collisionStart'?'data-sketsa-physics-event-start':phase==='collisionActive'?'data-sketsa-physics-event-active':'data-sketsa-physics-event-end';return boolAttr(entry.el,attr);}\n");
        s.append("function eventDetail(entry,other,phase,pair){\n");
        s.append("  const selfId=entry.runtimeId||entry.svgId||'',otherId=other?(other.runtimeId||other.svgId||''):'';\n");
        s.append("  const eventId=(entry.el.getAttribute('data-sketsa-physics-event-id')||selfId||'physics').trim();\n");
        s.append("  const relX=entry.body.velocity.x-(other?other.body.velocity.x:0),relY=entry.body.velocity.y-(other?other.body.velocity.y:0);\n");
        s.append("  const p=pair&&pair.collision&&pair.collision.supports&&pair.collision.supports.length?pair.collision.supports[0]:{x:(entry.body.position.x+(other?other.body.position.x:entry.body.position.x))/2,y:(entry.body.position.y+(other?other.body.position.y:entry.body.position.y))/2};\n");
        s.append("  return {phase:phase,eventId:eventId,selfId:selfId,otherId:otherId,svgId:entry.svgId||'',otherSvgId:other?(other.svgId||''):'',pairId:pair?pair.id:'',sensor:!!(entry.body.isSensor||(other&&other.body.isSensor)),relativeSpeed:Math.sqrt(relX*relX+relY*relY),position:{x:p.x,y:p.y},filter:{category:entry.body.collisionFilter.category>>>0,mask:entry.body.collisionFilter.mask>>>0,group:entry.body.collisionFilter.group|0}};\n");
        s.append("}\n");
        s.append("function publish(entry,other,phase,pair){if(!enabled(entry,phase))return;const detail=eventDetail(entry,other,phase,pair);document.dispatchEvent(new CustomEvent('sketsa:physics:'+phase,{detail:detail}));document.dispatchEvent(new CustomEvent('sketsa:physics:collision',{detail:detail}));emitGeneric(phase,detail);}\n");
        s.append("function handleMatterCollision(phase,event){event.pairs.forEach(function(pair){const a=byBodyId.get(pair.bodyA.id)||byBodyId.get(pair.bodyA.parent&&pair.bodyA.parent.id),b=byBodyId.get(pair.bodyB.id)||byBodyId.get(pair.bodyB.parent&&pair.bodyB.parent.id);if(a)publish(a,b,phase,pair);if(b)publish(b,a,phase,pair);});}\n");
        s.append("M.Events.on(engine,'collisionStart',function(e){handleMatterCollision('collisionStart',e);});\n");
        s.append("M.Events.on(engine,'collisionActive',function(e){handleMatterCollision('collisionActive',e);});\n");
        s.append("M.Events.on(engine,'collisionEnd',function(e){handleMatterCollision('collisionEnd',e);});\n");
        s.append("function refreshEventSummary(){if(eventSummary)eventSummary.textContent='start '+eventCounts.collisionStart+' \u00b7 active '+eventCounts.collisionActive+' \u00b7 end '+eventCounts.collisionEnd;}\n");
        s.append("function monitorEvent(e){const d=e.detail||{},phase=d.phase||'collisionStart';eventCounts[phase]=(eventCounts[phase]||0)+1;refreshEventSummary();if(!eventLog)return;if(phase==='collisionActive'&&eventCounts[phase]%15!==1)return;const li=document.createElement('li');li.textContent=phase+' ['+(d.eventId||'physics')+'] '+(d.selfId||'?')+' \u2194 '+(d.otherId||'?')+(d.sensor?' sensor':'');eventLog.insertBefore(li,eventLog.firstChild);while(eventLog.children.length>8)eventLog.removeChild(eventLog.lastChild);}\n");
        s.append("document.addEventListener('sketsa:physics:collisionStart',monitorEvent);document.addEventListener('sketsa:physics:collisionActive',monitorEvent);document.addEventListener('sketsa:physics:collisionEnd',monitorEvent);refreshEventSummary();\n");
        s.append("function clearEventMonitor(){eventCounts.collisionStart=0;eventCounts.collisionActive=0;eventCounts.collisionEnd=0;if(eventLog)eventLog.textContent='';refreshEventSummary();}\n");
        s.append("function reset(){entries.forEach(function(x){const b=x.body,i=x.initial;M.Body.setPosition(b,{x:i.position.x,y:i.position.y});M.Body.setAngle(b,i.angle);M.Body.setVelocity(b,{x:i.velocity.x,y:i.velocity.y});M.Body.setAngularVelocity(b,i.angularVelocity);b.force.x=0;b.force.y=0;b.torque=0;if(M.Sleeping&&!b.isStatic)M.Sleeping.set(b,!!i.sleeping);launch(x);});clearEventMonitor();status.textContent=paused?'Paused / reset':'Running / reset';}\n");
        s.append("function setSleeping(value){entries.forEach(function(x){if(!x.body.isStatic&&M.Sleeping)M.Sleeping.set(x.body,value);});status.textContent=value?'Dynamics sleeping':(paused?'Paused':'Running');}\n");
        s.append("function actionResult(req,ok,error,entry){return{contractVersion:CONTRACT_VERSION,source:'physics',action:String(req&&req.action||''),target:String(req&&req.target||''),ok:!!ok,error:error||'',state:entry?bodyState(entry):null};}\n");
        s.append("function performAction(req){req=req||{};const action=String(req.action||''),payload=req.payload||req.value||{},target=req.target==null?'':String(req.target),entry=target?resolveEntry(target):null;try{switch(action){case'pause':paused=true;status.textContent='Paused';break;case'resume':paused=false;last=performance.now();status.textContent='Running';break;case'reset':reset();break;case'sleep':setSleeping(true);break;case'wake':setSleeping(false);break;case'setPosition':if(!entry)throw new Error('Body not found: '+target);M.Body.setPosition(entry.body,{x:Number(payload.x)||0,y:Number(payload.y)||0});break;case'setAngle':if(!entry)throw new Error('Body not found: '+target);M.Body.setAngle(entry.body,Number(payload.angle!=null?payload.angle:payload)||0);break;case'setVelocity':if(!entry)throw new Error('Body not found: '+target);if(entry.body.isStatic)throw new Error('Cannot set velocity on static body');M.Body.setVelocity(entry.body,{x:Number(payload.x)||0,y:Number(payload.y)||0});break;case'setAngularVelocity':if(!entry)throw new Error('Body not found: '+target);if(entry.body.isStatic)throw new Error('Cannot set angular velocity on static body');M.Body.setAngularVelocity(entry.body,Number(payload.angularVelocity!=null?payload.angularVelocity:payload)||0);break;case'setSleeping':if(!entry)throw new Error('Body not found: '+target);if(M.Sleeping&&!entry.body.isStatic)M.Sleeping.set(entry.body,!!(payload.sleeping!=null?payload.sleeping:payload));break;case'applyForce':if(!entry)throw new Error('Body not found: '+target);if(entry.body.isStatic)throw new Error('Cannot apply force to static body');M.Body.applyForce(entry.body,entry.body.position,{x:Number(payload.x)||0,y:Number(payload.y)||0});break;case'applyImpulse':if(!entry)throw new Error('Body not found: '+target);if(entry.body.isStatic)throw new Error('Cannot apply impulse to static body');applyImpulse(entry.body,Number(payload.x)||0,Number(payload.y)||0);break;default:throw new Error('Unknown Physics action: '+action);}const result=actionResult(req,true,'',entry);emitGeneric('action',result);return result;}catch(ex){const result=actionResult(req,false,String(ex&&ex.message||ex),entry);emitGeneric('actionError',result);return result;}}\n");
        s.append("function dispatchAction(action,target,payload){return performAction({action:action,target:target,payload:payload||{}});}\n");
        s.append("const publicApi={contractVersion:CONTRACT_VERSION,runtimeVersion:RUNTIME_VERSION,dispatch:dispatchAction,getBody:function(target){return bodyState(resolveEntry(target));},listBodies:function(){return entries.map(bodyState);},isPaused:function(){return paused;},on:function(type,handler){const name=String(type||'');const eventName=name.indexOf('sketsa:')===0?name:'sketsa:physics:'+name;document.addEventListener(eventName,handler);return function(){document.removeEventListener(eventName,handler);};}};Object.freeze(publicApi);window.SketsaPhysics=publicApi;\n");
        s.append("document.addEventListener('sketsa:physics:action',function(e){const req=e.detail||{},result=performAction(req);document.dispatchEvent(new CustomEvent('sketsa:physics:actionResult',{detail:result}));});\n");
        s.append("document.getElementById('sketsa-physics-pause').addEventListener('click',function(){paused=!paused;this.textContent=paused?'Resume':'Pause';status.textContent=paused?'Paused':'Running';last=performance.now();});\n");
        s.append("document.getElementById('sketsa-physics-reset').addEventListener('click',reset);document.getElementById('sketsa-physics-sleep').addEventListener('click',function(){setSleeping(true);});document.getElementById('sketsa-physics-wake').addEventListener('click',function(){setSleeping(false);});\n");
        s.append("function worldPoint(body,p){if(!body)return{x:p.x,y:p.y};const c=Math.cos(body.angle),q=Math.sin(body.angle);return{x:body.position.x+p.x*c-p.y*q,y:body.position.y+p.x*q+p.y*c};}\n");
        s.append("function renderConstraintVisuals(){constraintVisuals.forEach(function(v){const c=v.constraint,a=worldPoint(c.bodyA,c.pointA),b=worldPoint(c.bodyB,c.pointB);v.line.setAttribute('x1',a.x);v.line.setAttribute('y1',a.y);v.line.setAttribute('x2',b.x);v.line.setAttribute('y2',b.y);});}\n");
        s.append("function renderEntry(x){const delta=mm(mt(x.body.position.x,x.body.position.y),mm(mr(x.body.angle),mt(-x.bodyOrigin.x,-x.bodyOrigin.y))),desiredRoot=mm(delta,x.rootInitial);let parentMatrix={a:1,b:0,c:0,d:1,e:0,f:0};try{if(x.el.parentNode&&x.el.parentNode.getCTM){const pm=x.el.parentNode.getCTM();if(pm)parentMatrix=mat(pm);}}catch(e){}x.el.setAttribute('transform',matrixText(mm(mi(parentMatrix),desiredRoot)));}\n");
        s.append("function render(){entries.forEach(renderEntry);renderConstraintVisuals();}\n");
        s.append("function tick(now){const delta=Math.min(33.333,Math.max(0,now-last));last=now;if(!paused)M.Engine.update(engine,delta);render();requestAnimationFrame(tick);}\n");
        s.append("const readyDetail={contractVersion:CONTRACT_VERSION,runtimeVersion:RUNTIME_VERSION,source:'physics',bodies:entries.map(bodyState)};document.dispatchEvent(new CustomEvent('sketsa:physics:ready',{detail:readyDetail}));emitGeneric('ready',readyDetail);\n");
        s.append("requestAnimationFrame(tick);\n");
        s.append("})();\n");
        return s.toString();
    }
    static void prepareCompanionAssets(File assets) throws Exception {
        if (!assets.exists() && !assets.mkdirs()) {
            throw new IOException("Could not create runtime assets directory: " + assets);
        }
        File matter = new File(assets, "matter.min.js");
        ensureMatterJs(matter);
        File runtime = new File(assets, "sketsa-physics-runtime.js");
        Files.write(runtime.toPath(), runtimeScript().getBytes(StandardCharsets.UTF_8));
    }

    static String companionHeadHtml() {
        return "<meta name=\"sketsa-physics-runtime\" content=\"0.9.5; contract 1.0; matter-js " + MATTER_VERSION + "\">\n"
                + "<style>#sketsa-physics-controls{display:flex;gap:8px;flex-wrap:wrap;padding:8px;border-bottom:1px solid #ccc;background:#f5f5f5}#sketsa-physics-controls button{padding:6px 10px}#sketsa-physics-status{align-self:center;font-size:12px;color:#444}#sketsa-physics-events{padding:6px 10px;border-bottom:1px solid #ddd;background:#fafafa;font-size:12px}#sketsa-physics-event-summary{margin-left:8px}#sketsa-physics-event-log{margin:4px 0 0 18px;padding:0;max-height:90px;overflow:auto}</style>\n";
    }

    static String companionControlsHtml() {
        return "<div id=\"sketsa-physics-controls\"><button id=\"sketsa-physics-pause\" type=\"button\">Pause</button><button id=\"sketsa-physics-reset\" type=\"button\">Reset</button><button id=\"sketsa-physics-sleep\" type=\"button\">Sleep dynamics</button><button id=\"sketsa-physics-wake\" type=\"button\">Wake dynamics</button><span id=\"sketsa-physics-status\">Running</span></div>\n"
                + "<div id=\"sketsa-physics-events\"><strong>Physics events</strong><span id=\"sketsa-physics-event-summary\">start 0 · active 0 · end 0</span><ol id=\"sketsa-physics-event-log\"></ol></div>\n";
    }

    static String companionScriptsHtml(String assetPath) {
        return "<script src=\"" + assetPath + "/matter.min.js\"></script>\n"
                + "<script src=\"" + assetPath + "/sketsa-physics-runtime.js\"></script>\n";
    }

}
