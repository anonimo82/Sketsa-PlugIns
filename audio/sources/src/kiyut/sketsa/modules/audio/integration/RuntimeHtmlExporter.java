package kiyut.sketsa.modules.audio.integration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class RuntimeHtmlExporter {
    private static final String RUNTIME_VERSION="0.8.2";
    private static final String RUNTIME_RESOURCE="sketsa-audio-runtime.js";
    private static final String TREE_RUNTIME_VERSION="0.16.3";
    private static final String TREE_RUNTIME_RESOURCE="sketsa-audio-tree-runtime.js";
    private static final String TREE_IR_FILE="sketsa-audio-tree-ir-"+TREE_RUNTIME_VERSION+".js";
    private static final String TREE_RUNTIME_FILE="sketsa-audio-tree-runtime-"+TREE_RUNTIME_VERSION+".js";
    private RuntimeHtmlExporter(){}

    static void export(Document document, File htmlFile) throws Exception {
        export(document, htmlFile, false);
    }

    static void export(Document document, File htmlFile, boolean includeCompanionRuntimes) throws Exception {
        File parent=htmlFile.getAbsoluteFile().getParentFile();
        if (parent!=null && !parent.exists() && !parent.mkdirs())
            throw new IOException("Could not create export directory: "+parent);

        Document copy=(Document)document.cloneNode(true);
        boolean audio=hasAudio(copy);
        boolean audioTree=AudioTreeCompiler.hasTree(copy);
        // T6: when a valid Audio Tree exists it is the authoritative audio backend.
        // Legacy object-level audio remains available only as a fallback for documents
        // that have no Audio Tree, avoiding two independent Web Audio graphs/contexts.
        boolean legacyRuntime=audio && !audioTree;
        boolean physics=includeCompanionRuntimes && PhysicsCompanionExporter.hasPhysics(copy);
        String inlineScripts=extractInlineSvgScripts(copy);
        if (!audio && !audioTree && !physics) {
            writeAtomic(htmlFile,plainHtml(normalizeSvgForHtml(serialize(copy)),inlineScripts).getBytes(StandardCharsets.UTF_8));
            return;
        }

        String base=stripExtension(htmlFile.getName());
        File assets=new File(parent,base+"-assets");
        if (!assets.exists() && !assets.mkdirs())
            throw new IOException("Could not create runtime assets directory: "+assets);

        if (legacyRuntime) {
            cleanupTreeRuntimeAssets(assets);
            File audioDir=new File(assets,"audio");
            resetAudioDirectory(audioDir);
            Map<String,String> assetData=prepareAssets(copy,audioDir,assets.getName());
            writeAtomic(new File(assets,"sketsa-audio-asset-data.js"),assetDataScript(assetData).getBytes(StandardCharsets.UTF_8));
            writeAtomic(new File(assets,"sketsa-audio-runtime.js"),runtimeScript().getBytes(StandardCharsets.UTF_8));
        }
        if (audioTree) {
            AudioTreeCompiler.Result compiled=AudioTreeCompiler.compile(copy);
            cleanupTreeRuntimeAssets(assets);
            cleanupLegacyRuntimeAssets(assets);
            writeAtomic(new File(assets,TREE_IR_FILE), AudioTreeCompiler.toJavascript(compiled).getBytes(StandardCharsets.UTF_8));
            writeAtomic(new File(assets,TREE_RUNTIME_FILE), treeRuntimeScript().getBytes(StandardCharsets.UTF_8));
        }
        if (physics) PhysicsCompanionExporter.prepareCompanionAssets(assets);

        writeAtomic(htmlFile,combinedHtml(normalizeSvgForHtml(serialize(copy)),assets.getName(),legacyRuntime,audioTree,physics,inlineScripts).getBytes(StandardCharsets.UTF_8));
    }

    private static boolean hasAudio(Document d) {
        NodeList all=d.getElementsByTagName("*");
        for(int i=0;i<all.getLength();i++) if(all.item(i) instanceof Element && ((Element)all.item(i)).hasAttribute("data-sketsa-audio-src")) return true;
        return false;
    }

    private static Map<String,String> prepareAssets(Document d, File audioDir, String assetRoot) throws Exception {
        NodeList all=d.getElementsByTagName("*");
        Map<String,String> byDigest=new HashMap<>();
        Map<String,String> assetData=new LinkedHashMap<>();
        Map<String,Integer> idCounts=new HashMap<>();
        int audioIndex=0;
        for(int i=0;i<all.getLength();i++) if(all.item(i) instanceof Element) {
            Element e=(Element)all.item(i);
            if(!e.hasAttribute("data-sketsa-audio-src")) continue;
            String src=e.getAttribute("data-sketsa-audio-src").trim();
            if(src.isEmpty()) continue;
            audioIndex++;

            // Runtime ids are deterministic and unique even after SVG duplication or when id is missing.
            String svgId=e.getAttribute("id").trim();
            String baseId=svgId.isEmpty()?"audio-"+audioIndex:svgId;
            int occurrence=idCounts.containsKey(baseId)?idCounts.get(baseId)+1:1;
            idCounts.put(baseId,occurrence);
            String runtimeId=occurrence==1?baseId:baseId+"~"+occurrence;
            e.setAttribute("data-sketsa-audio-runtime-id",runtimeId);
            e.setAttribute("data-sketsa-audio-svg-id",svgId);

            try {
                AssetData asset=readAsset(src);
                String digest=sha256(asset.bytes);
                String name=byDigest.get(digest);
                if(name==null) {
                    String ext=asset.extension;
                    String base=safeStem(asset.displayName);
                    name=digest.substring(0,16)+"-"+base+ext;
                    File out=new File(audioDir,name);
                    Files.write(out.toPath(),asset.bytes);
                    byDigest.put(digest,name);
                }
                String runtimeSrc=assetRoot+"/audio/"+name;
                e.setAttribute("data-sketsa-audio-runtime-src",runtimeSrc);
                e.setAttribute("data-sketsa-audio-asset-key",digest);
                e.removeAttribute("data-sketsa-audio-missing");
                if(!assetData.containsKey(runtimeSrc))
                    assetData.put(runtimeSrc,"data:application/octet-stream;base64,"+java.util.Base64.getEncoder().encodeToString(asset.bytes));
            } catch(Exception ex) {
                // A missing/moved asset must not make the whole document unexportable.
                e.removeAttribute("data-sketsa-audio-runtime-src");
                e.removeAttribute("data-sketsa-audio-asset-key");
                e.setAttribute("data-sketsa-audio-missing","true");
            }
        }
        return assetData;
    }

    private static String extractInlineSvgScripts(Document d) {
        NodeList scripts=d.getElementsByTagName("script");
        List<Node> extracted=new ArrayList<>();
        StringBuilder html=new StringBuilder();
        for(int i=0;i<scripts.getLength();i++) {
            if(!(scripts.item(i) instanceof Element)) continue;
            Element e=(Element)scripts.item(i);
            boolean external=e.hasAttribute("src") || e.hasAttribute("href")
                    || e.hasAttributeNS("http://www.w3.org/1999/xlink","href");
            String code=e.getTextContent();
            if(external || code==null || code.trim().isEmpty()) continue;
            html.append("<script>\n").append(escapeHtmlScriptEnd(code)).append("\n</script>\n");
            extracted.add(e);
        }
        for(Node n:extracted) if(n.getParentNode()!=null) n.getParentNode().removeChild(n);
        return html.toString();
    }

    private static String escapeHtmlScriptEnd(String code) {
        // In HTML, a literal closing script tag terminates the element even when
        // it appears inside a JavaScript string/comment. Escaping the slash keeps
        // the JavaScript value identical while making the generated HTML robust.
        return code.replaceAll("(?i)</script", "<\\/script");
    }

    private static String normalizeSvgForHtml(String svg) {
        // Keep compatibility for any non-extracted SVG scripts (for example an
        // external script element) while inline executable scripts are relocated
        // to normal HTML script elements after the runtime assets have loaded.
        return svg.replaceAll("(?s)(<script\\b[^>]*>)<!\\[CDATA\\[", "$1")
                  .replace("]]></script>", "</script>");
    }

    private static void cleanupLegacyRuntimeAssets(File assets) throws IOException {
        String[] names={"sketsa-audio-runtime.js","sketsa-audio-asset-data.js"};
        for(String name:names){
            File f=new File(assets,name);
            if(f.exists() && !f.delete()) throw new IOException("Could not remove stale legacy audio runtime asset: "+f);
        }
        File audioDir=new File(assets,"audio");
        if(audioDir.exists()) deleteRecursively(audioDir);
    }

    private static void cleanupTreeRuntimeAssets(File assets) throws IOException {
        File[] files=assets.listFiles();
        if(files==null)return;
        for(File f:files){
            String n=f.getName();
            if((n.startsWith("sketsa-audio-tree-ir-") || n.startsWith("sketsa-audio-tree-runtime-")) && n.endsWith(".js")) {
                if(!f.delete() && f.exists()) throw new IOException("Could not remove stale Audio Tree runtime asset: "+f);
            }
            // Remove the unversioned T2-T5 asset names from older exports too.
            if(("sketsa-audio-tree-ir.js".equals(n) || "sketsa-audio-tree-runtime.js".equals(n)) && !f.delete() && f.exists())
                throw new IOException("Could not remove stale Audio Tree runtime asset: "+f);
        }
    }

    private static void writeAtomic(File target, byte[] bytes) throws IOException {
        File parent=target.getAbsoluteFile().getParentFile();
        if(parent!=null && !parent.exists() && !parent.mkdirs()) throw new IOException("Could not create directory: "+parent);
        File tmp=File.createTempFile(target.getName()+".", ".tmp", parent);
        try {
            Files.write(tmp.toPath(), bytes);
            try {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ex) {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if(tmp.exists()) tmp.delete();
        }
    }

    private static void resetAudioDirectory(File audioDir) throws IOException {
        if(audioDir.exists()) deleteRecursively(audioDir);
        if(!audioDir.mkdirs() && !audioDir.isDirectory())
            throw new IOException("Could not create audio assets directory: "+audioDir);
    }

    private static void deleteRecursively(File file) throws IOException {
        File[] children=file.listFiles();
        if(children!=null) for(File child:children) deleteRecursively(child);
        if(!file.delete() && file.exists()) throw new IOException("Could not clean generated audio asset: "+file);
    }

    private static final class AssetData {
        final byte[] bytes; final String displayName; final String extension;
        AssetData(byte[] bytes,String displayName,String extension){this.bytes=bytes;this.displayName=displayName;this.extension=extension;}
    }

    private static AssetData readAsset(String src) throws Exception {
        if(src.startsWith("data:")) {
            int comma=src.indexOf(',');
            if(comma<0) throw new IOException("Invalid audio data URI");
            String head=src.substring(5,comma);
            String body=src.substring(comma+1);
            boolean base64=head.toLowerCase().contains(";base64");
            String mime=head.split(";",2)[0].trim().toLowerCase();
            byte[] bytes=base64 ? java.util.Base64.getDecoder().decode(body) : java.net.URLDecoder.decode(body,"UTF-8").getBytes(StandardCharsets.ISO_8859_1);
            String ext=extensionForMime(mime);
            return new AssetData(bytes,"embedded-audio",ext);
        }
        File f=src.startsWith("file:")?new File(new URI(src)):new File(src);
        if(!f.isFile()) throw new IOException("Audio source not found: "+src);
        String ext=extensionOf(f.getName());
        return new AssetData(Files.readAllBytes(f.toPath()),f.getName(),ext);
    }

    private static String extensionForMime(String mime) {
        if("audio/wav".equals(mime)||"audio/x-wav".equals(mime)||"audio/wave".equals(mime)) return ".wav";
        if("audio/mpeg".equals(mime)) return ".mp3";
        if("audio/ogg".equals(mime)) return ".ogg";
        if("audio/mp4".equals(mime)||"audio/aac".equals(mime)) return ".m4a";
        if("audio/flac".equals(mime)) return ".flac";
        return ".bin";
    }

    private static String extensionOf(String name) {
        int dot=name.lastIndexOf('.');
        if(dot<0||dot==name.length()-1) return ".bin";
        String e=name.substring(dot).toLowerCase();
        return e.matches("\\.[a-z0-9]{1,8}")?e:".bin";
    }

    private static String safeStem(String n) {
        String base=n; int dot=base.lastIndexOf('.'); if(dot>0) base=base.substring(0,dot);
        String s=base.replaceAll("[^A-Za-z0-9._-]+","_").replaceAll("^[_\\.-]+|[_\\.-]+$","");
        if(s.isEmpty()) s="audio";
        return s.length()>48?s.substring(0,48):s;
    }

    private static String sha256(byte[] data) throws Exception {
        java.security.MessageDigest md=java.security.MessageDigest.getInstance("SHA-256");
        byte[] d=md.digest(data); StringBuilder sb=new StringBuilder(d.length*2);
        for(byte b:d) sb.append(String.format("%02x",b&255));
        return sb.toString();
    }

    private static String stripExtension(String n){int dot=n.lastIndexOf('.');return dot>0?n.substring(0,dot):n;}

    private static String plainHtml(String svg,String inlineScripts) {
        return "<!doctype html>\n<html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Sketsa Export</title></head><body>"+svg+"\n"+inlineScripts+"</body></html>\n";
    }

    private static String audioHtml(String svg,String assetPath) {
        return "<!doctype html>\n<html><head><meta charset=\"utf-8\">\n"
            +"<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
            +"<meta name=\"sketsa-audio-runtime\" content=\""+RUNTIME_VERSION+"; web-audio\">\n"
            +"<title>Sketsa Audio Export</title>\n"
            +"<style>html,body{margin:0;background:#fff;font-family:sans-serif}#sketsa-audio-controls{display:flex;gap:8px;align-items:center;padding:8px;border-bottom:1px solid #ccc;background:#f5f5f5}svg{display:block;max-width:100%;height:auto}</style>\n"
            +"</head><body><div id=\"sketsa-audio-controls\"><button id=\"sketsa-audio-enable\" type=\"button\">Enable Audio</button><span id=\"sketsa-audio-runtime-status\">Loading audio…</span></div>\n"
            +svg+"\n<script src=\""+assetPath+"/sketsa-audio-asset-data.js\"></script>"
            +"\n<script src=\""+assetPath+"/sketsa-audio-runtime.js\"></script></body></html>\n";
    }


    private static String combinedHtml(String svg,String assetPath,boolean legacyRuntime,boolean audioTree,boolean physics,String inlineScripts) {
        StringBuilder h=new StringBuilder();
        h.append("<!doctype html>\n<html><head><meta charset=\"utf-8\">\n");
        h.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n");
        if(legacyRuntime) h.append("<meta name=\"sketsa-audio-runtime\" content=\"").append(RUNTIME_VERSION).append("; contract 1.0; backend legacy\">\n");
        if(audioTree) {
            h.append("<meta name=\"sketsa-audio-runtime\" content=\"").append(TREE_RUNTIME_VERSION).append("; contract 1.2; backend audio-tree; primary\">\n");
            h.append("<meta name=\"sketsa-audio-tree-runtime\" content=\"").append(TREE_RUNTIME_VERSION).append("; contract 1.2; hierarchical-routing; primary\">\n");
        }
        if(physics) h.append(PhysicsCompanionExporter.companionHeadHtml());
        h.append("<title>Sketsa Runtime Export</title>\n");
        h.append("<style>html,body{margin:0;background:#fff;font-family:sans-serif}#sketsa-audio-controls{display:flex;gap:8px;align-items:center;padding:8px;border-bottom:1px solid #ccc;background:#f5f5f5}svg{display:block;max-width:100%;height:auto}</style>\n");
        h.append("</head><body>\n");
        if(legacyRuntime || audioTree) h.append("<div id=\"sketsa-audio-controls\"><button id=\"sketsa-audio-enable\" type=\"button\">Enable Audio</button><span id=\"sketsa-audio-runtime-status\">Loading audio…</span></div>\n");
        if(physics) h.append(PhysicsCompanionExporter.companionControlsHtml());
        h.append(svg).append("\n");
        // Audio loads before Physics so the neutral-event consumer is already listening when simulation begins.
        if(legacyRuntime) {
            h.append("<script src=\"").append(assetPath).append("/sketsa-audio-asset-data.js\"></script>\n");
            h.append("<script src=\"").append(assetPath).append("/sketsa-audio-runtime.js\"></script>\n");
        }
        if(audioTree) {
            h.append("<script src=\"").append(assetPath).append("/").append(TREE_IR_FILE).append("\"></script>\n");
            h.append("<script src=\"").append(assetPath).append("/").append(TREE_RUNTIME_FILE).append("\"></script>\n");
        }
        if(physics) h.append(PhysicsCompanionExporter.companionScriptsHtml(assetPath));
        h.append(inlineScripts);
        h.append("</body></html>\n");
        return h.toString();
    }


    private static String assetDataScript(Map<String,String> data) {
        StringBuilder out=new StringBuilder("(function(){\n'use strict';\nwindow.__SketsaAudioAssetData=window.__SketsaAudioAssetData||{};\n");
        for(Map.Entry<String,String> e:data.entrySet()) {
            out.append("window.__SketsaAudioAssetData[").append(jsString(e.getKey())).append("]=")
               .append(jsString(e.getValue())).append(";\n");
        }
        return out.append("})();\n").toString();
    }

    private static String jsString(String s) {
        StringBuilder out=new StringBuilder("\"");
        for(int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if(c=='\\'||c=='\"') out.append('\\').append(c);
            else if(c=='\n') out.append("\\n");
            else if(c=='\r') out.append("\\r");
            else if(c=='\t') out.append("\\t");
            else if(c<32) out.append(String.format("\\u%04x",(int)c));
            else out.append(c);
        }
        return out.append('\"').toString();
    }

    private static String serialize(Document d) throws Exception {
        Transformer t=TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
        t.setOutputProperty(OutputKeys.INDENT,"no");
        StringWriter out=new StringWriter();
        t.transform(new DOMSource(d.getDocumentElement()),new StreamResult(out));
        return out.toString();
    }

    private static String treeRuntimeScript() throws IOException {
        try(InputStream in=RuntimeHtmlExporter.class.getResourceAsStream(TREE_RUNTIME_RESOURCE)) {
            if(in==null) throw new IOException("Missing runtime resource: "+TREE_RUNTIME_RESOURCE);
            byte[] bytes=readAll(in);
            return new String(bytes,StandardCharsets.UTF_8);
        }
    }

    private static String runtimeScript() throws IOException {
        try(InputStream in=RuntimeHtmlExporter.class.getResourceAsStream(RUNTIME_RESOURCE)) {
            if(in==null) throw new IOException("Missing runtime resource: "+RUNTIME_RESOURCE);
            byte[] bytes=readAll(in);
            return new String(bytes,StandardCharsets.UTF_8);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
        byte[] buf=new byte[8192];
        for(int n;(n=in.read(buf))>=0;) out.write(buf,0,n);
        return out.toByteArray();
    }
}
