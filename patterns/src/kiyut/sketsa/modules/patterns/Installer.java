package kiyut.sketsa.modules.patterns;

import java.awt.EventQueue;
import javax.swing.Timer;
import kiyut.sketsa.modules.patterns.integration.PatternsIntegrator;
import org.openide.modules.ModuleInstall;

public final class Installer extends ModuleInstall {
    @Override
    public void restored() {
        EventQueue.invokeLater(() -> {
            if (PatternsIntegrator.install()) {
                return;
            }
            final int[] attempts = {0};
            Timer timer = new Timer(500, null);
            timer.addActionListener(e -> {
                attempts[0]++;
                if (PatternsIntegrator.install() || attempts[0] >= 20) {
                    timer.stop();
                }
            });
            timer.setInitialDelay(500);
            timer.start();
        });
    }
}
