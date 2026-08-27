package kiyut.sketsa.modules.physics.integration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Audio 0.8.2 companion export support without a Java dependency on the Audio module. */
final class AudioCompanionExporter {
    static final String RUNTIME_VERSION = "0.8.2";
    private AudioCompanionExporter() {}

    static boolean hasAudio(Document d) {
        if (d == null || d.getDocumentElement() == null) return false;
        NodeList all = d.getElementsByTagName("*");
        for (int i=0;i<all.getLength();i++) {
            if (all.item(i) instanceof Element && ((Element)all.item(i)).hasAttribute("data-sketsa-audio-src")) return true;
        }
        return false;
    }

    static void prepareCompanionAssets(Document d, File assets, String assetRoot) throws Exception {
        if (!assets.exists() && !assets.mkdirs()) throw new IOException("Could not create runtime assets directory: "+assets);
        File audioDir = new File(assets,"audio");
        resetDirectory(audioDir);
        Map<String,String> assetData = prepareAssets(d,audioDir,assetRoot);
        Files.write(new File(assets,"sketsa-audio-asset-data.js").toPath(),assetDataScript(assetData).getBytes(StandardCharsets.UTF_8));
        Files.write(new File(assets,"sketsa-audio-runtime.js").toPath(),runtimeScript().getBytes(StandardCharsets.UTF_8));
    }

    static String companionHeadHtml() {
        return "<meta name=\"sketsa-audio-runtime\" content=\""+RUNTIME_VERSION+"; contract 1.0; web-audio\">\n"
             + "<style>#sketsa-audio-controls{display:flex;gap:8px;align-items:center;padding:8px;border-bottom:1px solid #ccc;background:#f5f5f5}</style>\n";
    }

    static String companionControlsHtml() {
        return "<div id=\"sketsa-audio-controls\"><button id=\"sketsa-audio-enable\" type=\"button\">Enable Audio</button><span id=\"sketsa-audio-runtime-status\">Loading audio…</span></div>\n";
    }

    static String companionScriptsHtml(String assetPath) {
        return "<script src=\""+assetPath+"/sketsa-audio-asset-data.js\"></script>\n"
             + "<script src=\""+assetPath+"/sketsa-audio-runtime.js\"></script>\n";
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
                    name=digest.substring(0,16)+"-"+safeStem(asset.displayName)+asset.extension;
                    Files.write(new File(audioDir,name).toPath(),asset.bytes);
                    byDigest.put(digest,name);
                }
                String runtimeSrc=assetRoot+"/audio/"+name;
                e.setAttribute("data-sketsa-audio-runtime-src",runtimeSrc);
                e.setAttribute("data-sketsa-audio-asset-key",digest);
                e.removeAttribute("data-sketsa-audio-missing");
                if(!assetData.containsKey(runtimeSrc))
                    assetData.put(runtimeSrc,"data:application/octet-stream;base64,"+java.util.Base64.getEncoder().encodeToString(asset.bytes));
            } catch(Exception ex) {
                e.removeAttribute("data-sketsa-audio-runtime-src");
                e.removeAttribute("data-sketsa-audio-asset-key");
                e.setAttribute("data-sketsa-audio-missing","true");
            }
        }
        return assetData;
    }

    private static final class AssetData {
        final byte[] bytes; final String displayName; final String extension;
        AssetData(byte[] bytes,String displayName,String extension){this.bytes=bytes;this.displayName=displayName;this.extension=extension;}
    }

    private static AssetData readAsset(String src) throws Exception {
        if(src.startsWith("data:")) {
            int comma=src.indexOf(',');
            if(comma<0) throw new IOException("Invalid audio data URI");
            String head=src.substring(5,comma), body=src.substring(comma+1);
            boolean base64=head.toLowerCase().contains(";base64");
            String mime=head.split(";",2)[0].trim().toLowerCase();
            byte[] bytes=base64?java.util.Base64.getDecoder().decode(body):java.net.URLDecoder.decode(body,"UTF-8").getBytes(StandardCharsets.ISO_8859_1);
            return new AssetData(bytes,"embedded-audio",extensionForMime(mime));
        }
        File f=src.startsWith("file:")?new File(new URI(src)):new File(src);
        if(!f.isFile()) throw new IOException("Audio source not found: "+src);
        return new AssetData(Files.readAllBytes(f.toPath()),f.getName(),extensionOf(f.getName()));
    }

    private static String extensionForMime(String mime) {
        if("audio/wav".equals(mime)||"audio/x-wav".equals(mime)||"audio/wave".equals(mime)) return ".wav";
        if("audio/mpeg".equals(mime)) return ".mp3";
        if("audio/ogg".equals(mime)) return ".ogg";
        if("audio/mp4".equals(mime)||"audio/aac".equals(mime)) return ".m4a";
        if("audio/flac".equals(mime)) return ".flac";
        return ".bin";
    }
    private static String extensionOf(String name){int dot=name.lastIndexOf('.');if(dot<0||dot==name.length()-1)return ".bin";String e=name.substring(dot).toLowerCase();return e.matches("\\.[a-z0-9]{1,8}")?e:".bin";}
    private static String safeStem(String n){String base=n;int dot=base.lastIndexOf('.');if(dot>0)base=base.substring(0,dot);String s=base.replaceAll("[^A-Za-z0-9._-]+","_").replaceAll("^[_\\.-]+|[_\\.-]+$","");if(s.isEmpty())s="audio";return s.length()>48?s.substring(0,48):s;}
    private static String sha256(byte[] data)throws Exception{java.security.MessageDigest md=java.security.MessageDigest.getInstance("SHA-256");byte[] d=md.digest(data);StringBuilder sb=new StringBuilder(d.length*2);for(byte b:d)sb.append(String.format("%02x",b&255));return sb.toString();}

    private static void resetDirectory(File dir)throws IOException{if(dir.exists())deleteRecursively(dir);if(!dir.mkdirs()&&!dir.isDirectory())throw new IOException("Could not create audio assets directory: "+dir);}
    private static void deleteRecursively(File f)throws IOException{File[] c=f.listFiles();if(c!=null)for(File x:c)deleteRecursively(x);if(!f.delete()&&f.exists())throw new IOException("Could not clean generated audio asset: "+f);}

    private static String assetDataScript(Map<String,String> data){StringBuilder out=new StringBuilder("(function(){\n'use strict';\nwindow.__SketsaAudioAssetData=window.__SketsaAudioAssetData||{};\n");for(Map.Entry<String,String>e:data.entrySet())out.append("window.__SketsaAudioAssetData[").append(jsString(e.getKey())).append("]=").append(jsString(e.getValue())).append(";\n");return out.append("})();\n").toString();}
    private static String jsString(String s){StringBuilder out=new StringBuilder("\"");for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='\\'||c=='\"')out.append('\\').append(c);else if(c=='\n')out.append("\\n");else if(c=='\r')out.append("\\r");else if(c=='\t')out.append("\\t");else if(c<32)out.append(String.format("\\u%04x",(int)c));else out.append(c);}return out.append('\"').toString();}

    private static String runtimeScript() throws IOException {
        try(InputStream in=AudioCompanionExporter.class.getResourceAsStream("sketsa-audio-runtime.js")) {
            if(in==null) throw new IOException("Missing companion resource: sketsa-audio-runtime.js");
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];for(int n;(n=in.read(b))>=0;)out.write(b,0,n);
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
}
