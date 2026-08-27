package kiyut.sketsa.modules.links;

import java.awt.EventQueue;
import javax.swing.Timer;
import kiyut.sketsa.modules.links.integration.LinksIntegrator;
import org.openide.modules.ModuleInstall;

public final class Installer extends ModuleInstall {
    @Override
    public void restored() {
        EventQueue.invokeLater(() -> {
            if (LinksIntegrator.install()) {
                return;
            }
            final int[] attempts = {0};
            Timer timer = new Timer(500, null);
            timer.addActionListener(e -> {
                attempts[0]++;
                if (LinksIntegrator.install() || attempts[0] >= 20) {
                    timer.stop();
                }
            });
            timer.setInitialDelay(500);
            timer.start();
        });
    }
}
