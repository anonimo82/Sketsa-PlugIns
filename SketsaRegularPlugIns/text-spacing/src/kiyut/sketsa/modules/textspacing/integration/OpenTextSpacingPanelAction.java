package kiyut.sketsa.modules.textspacing.integration;

import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.TopComponent;

/** Opens the independent Text Spacing plugin panel. */
public final class OpenTextSpacingPanelAction extends CallableSystemAction {
    @Override
    public void performAction() {
        TopComponent tc = TextSpacingTopComponent.findInstance();
        tc.open();
        tc.requestActive();
    }

    @Override
    public String getName() {
        return "Text Spacing";
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
