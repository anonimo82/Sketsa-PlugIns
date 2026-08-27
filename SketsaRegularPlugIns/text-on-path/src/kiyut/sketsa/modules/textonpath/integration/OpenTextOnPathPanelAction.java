package kiyut.sketsa.modules.textonpath.integration;

import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.TopComponent;

/** Opens the independent Text on Path plugin panel. */
public final class OpenTextOnPathPanelAction extends CallableSystemAction {
    @Override
    public void performAction() {
        TopComponent tc = TextOnPathTopComponent.findInstance();
        tc.open();
        tc.requestActive();
    }

    @Override
    public String getName() {
        return "Text on Path";
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
}
