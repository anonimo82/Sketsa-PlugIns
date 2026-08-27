package kiyut.sketsa.modules.links.integration;

import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.TopComponent;

/** Opens the independent Links plugin panel. */
public final class OpenLinksPanelAction extends CallableSystemAction {
    @Override
    public void performAction() {
        TopComponent tc = LinksTopComponent.findInstance();
        tc.open();
        tc.requestActive();
    }

    @Override
    public String getName() {
        return "Links";
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
