package kiyut.sketsa.modules.physics.integration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/** Input 0.8.0 companion export support without a Java dependency on the Input module. */
final class InputCompanionExporter {
    static final String RUNTIME_VERSION="0.8.0";
    private InputCompanionExporter() {}

    static boolean hasInput(Document d) {
        if(d==null||d.getDocumentElement()==null)return false;
        NodeList all=d.getElementsByTagName("*");
        for(int i=0;i<all.getLength();i++) if(all.item(i) instanceof Element) {
            NamedNodeMap attrs=((Element)all.item(i)).getAttributes();
            for(int a=0;a<attrs.getLength();a++) if(attrs.item(a).getNodeName().startsWith("data-sketsa-input-")) return true;
        }
        return false;
    }

    static void prepareCompanionAssets(File assets)throws Exception {
        if(!assets.exists()&&!assets.mkdirs())throw new IOException("Could not create runtime assets directory: "+assets);
        Files.write(new File(assets,"sketsa-input-runtime.js").toPath(),runtimeScript().getBytes(StandardCharsets.UTF_8));
    }
    static String companionHeadHtml(){return "<meta name=\"sketsa-input-runtime\" content=\""+RUNTIME_VERSION+"; contract 1.0; interop\">\n<style>svg{touch-action:none}</style>\n";}
    static String companionControlsHtml(){return "<div id=\"sketsa-input-status\" style=\"padding:8px;border-bottom:1px solid #ccc;background:#f5f5f5;font-size:12px\">Loading input…</div>\n";}
    static String companionScriptsHtml(String assetPath){return "<script src=\""+assetPath+"/sketsa-input-runtime.js\"></script>\n";}
    private static String runtimeScript()throws IOException{try(InputStream in=InputCompanionExporter.class.getResourceAsStream("sketsa-input-runtime.js")){if(in==null)throw new IOException("Missing companion resource: sketsa-input-runtime.js");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[]b=new byte[8192];for(int n;(n=in.read(b))>=0;)out.write(b,0,n);return new String(out.toByteArray(),StandardCharsets.UTF_8);}}
}
