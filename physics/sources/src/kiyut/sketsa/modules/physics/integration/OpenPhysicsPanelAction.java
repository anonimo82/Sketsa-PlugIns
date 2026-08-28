package kiyut.sketsa.modules.physics.integration;

import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.TopComponent;

/** Opens the independent Physics plugin panel. */
public final class OpenPhysicsPanelAction extends CallableSystemAction {
    @Override
    public void performAction() {
        TopComponent tc = PhysicsTopComponent.findInstance();
        tc.open();
        tc.requestActive();
    }

    @Override
    public String getName() {
        return "Physics";
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
