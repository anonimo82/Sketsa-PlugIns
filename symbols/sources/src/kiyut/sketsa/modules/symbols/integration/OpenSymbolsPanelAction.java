package kiyut.sketsa.modules.symbols.integration;

import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.TopComponent;

/** Opens the independent Symbols plugin panel. */
public final class OpenSymbolsPanelAction extends CallableSystemAction {
    @Override
    public void performAction() {
        TopComponent tc = SymbolsTopComponent.findInstance();
        tc.open();
        tc.requestActive();
    }

    @Override
    public String getName() {
        return "Symbols";
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
