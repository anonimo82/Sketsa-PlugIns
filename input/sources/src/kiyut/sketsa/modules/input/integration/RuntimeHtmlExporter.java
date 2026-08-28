package kiyut.sketsa.modules.input.integration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

final class RuntimeHtmlExporter {
    static final String RUNTIME_VERSION="0.8.0";
    private RuntimeHtmlExporter(){}

    static void export(Document document,File htmlFile)throws Exception{ export(document,htmlFile,false); }

    static void export(Document document,File htmlFile,boolean includeCompanionRuntimes)throws Exception{
        File parent=htmlFile.getAbsoluteFile().getParentFile();
        if(parent!=null&&!parent.exists()&&!parent.mkdirs())throw new IOException("Could not create export directory: "+parent);
        Document copy=(Document)document.cloneNode(true);
        String base=stripExtension(htmlFile.getName());
        File assets=new File(parent,base+"-assets");
        if(!assets.exists()&&!assets.mkdirs())throw new IOException("Could not create input assets directory: "+assets);
        cleanupGeneratedAssets(assets);
        Files.write(new File(assets,"sketsa-input-runtime.js").toPath(),runtimeScript().getBytes(StandardCharsets.UTF_8));
        boolean physics=includeCompanionRuntimes&&PhysicsCompanionExporter.hasPhysics(copy);
        if(physics)PhysicsCompanionExporter.prepareCompanionAssets(assets);
        String svg=serialize(copy);
        Files.write(htmlFile.toPath(),(physics?combinedHtml(svg,assets.getName()):inputHtml(svg,assets.getName())).getBytes(StandardCharsets.UTF_8));
    }
    private static void cleanupGeneratedAssets(File assets)throws IOException{
        // I8: repeated exports to the same destination must not leave stale
        // companion files behind when the document or export options change.
        Files.deleteIfExists(new File(assets,"sketsa-input-runtime.js").toPath());
        Files.deleteIfExists(new File(assets,"matter.min.js").toPath());
        Files.deleteIfExists(new File(assets,"sketsa-physics-runtime.js").toPath());
    }
    private static String stripExtension(String n){int dot=n.lastIndexOf('.');return dot>0?n.substring(0,dot):n;}
    private static String inputHtml(String svg,String assetPath){return "<!doctype html>\n<html><head><meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n<meta name=\"sketsa-input-runtime\" content=\""+RUNTIME_VERSION+"; keyboard; pointer; multi-pointer; gamepad; on-screen; actions-api; interop\">\n<title>Sketsa Input Export</title><style>html,body{margin:0;background:#fff;font-family:sans-serif}#sketsa-input-status{padding:8px;border-bottom:1px solid #ccc;background:#f5f5f5}svg{display:block;max-width:100%;height:auto;touch-action:none}</style></head><body><div id=\"sketsa-input-status\">Loading input…</div>"+svg+"\n<script src=\""+assetPath+"/sketsa-input-runtime.js\"></script></body></html>\n";}
    private static String combinedHtml(String svg,String assetPath){
        StringBuilder h=new StringBuilder();
        h.append("<!doctype html>\n<html><head><meta charset=\"utf-8\">\n");
        h.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n");
        h.append("<meta name=\"sketsa-input-runtime\" content=\"").append(RUNTIME_VERSION).append("; contract 1.0; interop\">\n");
        h.append(PhysicsCompanionExporter.companionHeadHtml());
        h.append("<title>Sketsa Input + Physics Export</title>\n");
        h.append("<style>html,body{margin:0;background:#fff;font-family:sans-serif}#sketsa-input-status{padding:8px;border-bottom:1px solid #ccc;background:#f5f5f5}svg{display:block;max-width:100%;height:auto;touch-action:none}</style>\n");
        h.append("</head><body><div id=\"sketsa-input-status\">Loading input…</div>\n");
        h.append(PhysicsCompanionExporter.companionControlsHtml());
        h.append(svg).append("\n");
        // Input loads first so its interop bindings are listening before the Physics runtime starts.
        h.append("<script src=\"").append(assetPath).append("/sketsa-input-runtime.js\"></script>\n");
        h.append(PhysicsCompanionExporter.companionScriptsHtml(assetPath));
        h.append("</body></html>\n");
        return h.toString();
    }
    private static String serialize(Document d)throws Exception{Transformer t=TransformerFactory.newInstance().newTransformer();t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");t.setOutputProperty(OutputKeys.INDENT,"no");StringWriter out=new StringWriter();t.transform(new DOMSource(d.getDocumentElement()),new StreamResult(out));return out.toString();}
    private static String runtimeScript()throws IOException{
        try(InputStream in=RuntimeHtmlExporter.class.getResourceAsStream("sketsa-input-runtime.js")){
            if(in==null)throw new IOException("Missing sketsa-input-runtime.js resource");
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
}
