package kiyut.sketsa.modules.textspacing;

import java.awt.EventQueue;
import javax.swing.Timer;
import kiyut.sketsa.modules.textspacing.integration.TextStyleIntegrator;
import org.openide.modules.ModuleInstall;

public final class Installer extends ModuleInstall {
    @Override
    public void restored() {
        EventQueue.invokeLater(() -> {
            if (TextStyleIntegrator.install()) {
                return;
            }
            final int[] attempts = {0};
            Timer timer = new Timer(500, null);
            timer.addActionListener(e -> {
                attempts[0]++;
                if (TextStyleIntegrator.install() || attempts[0] >= 20) {
                    timer.stop();
                }
            });
            timer.setInitialDelay(500);
            timer.start();
        });
    }
}
