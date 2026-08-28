package kiyut.sketsa.modules.audio.integration;

import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.TopComponent;

/** Opens the independent Audio Tree editor. */
public final class OpenAudioTreeAction extends CallableSystemAction {
    @Override public void performAction() {
        TopComponent tc = AudioTreeTopComponent.findInstance();
        tc.open();
        tc.requestActive();
    }
    @Override public String getName() { return "Audio Tree"; }
    @Override public HelpCtx getHelpCtx() { return HelpCtx.DEFAULT_HELP; }
    @Override protected boolean asynchronous() { return false; }
}
