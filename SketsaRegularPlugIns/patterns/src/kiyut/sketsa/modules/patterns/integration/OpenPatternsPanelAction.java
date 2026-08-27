package kiyut.sketsa.modules.patterns.integration;

import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.TopComponent;

/** Opens the independent Patterns plugin panel. */
public final class OpenPatternsPanelAction extends CallableSystemAction {
    @Override
    public void performAction() {
        TopComponent tc = PatternsTopComponent.findInstance();
        tc.open();
        tc.requestActive();
    }

    @Override
    public String getName() {
        return "Patterns";
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
