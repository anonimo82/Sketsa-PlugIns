package kiyut.sketsa.modules.switcher.integration;

import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.TopComponent;

/** Opens the independent Switch plugin panel. */
public final class OpenSwitchPanelAction extends CallableSystemAction {
    @Override
    public void performAction() {
        TopComponent tc = SwitchTopComponent.findInstance();
        tc.open();
        tc.requestActive();
    }

    @Override
    public String getName() {
        return "Switch";
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
