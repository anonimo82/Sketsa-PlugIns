package kiyut.sketsa.modules.links.integration;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JPanel;
import kiyut.sketsa.windows.properties.PropertiesTopComponent;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

public final class LinksIntegrator {

    private static final String PANEL_NAME = "SketsaLinks010Panel";

    private LinksIntegrator() {}

    public static boolean install() {
        TopComponent tc = WindowManager.getDefault().findTopComponent("PropertiesTopComponent");
        if (!(tc instanceof PropertiesTopComponent)) {
            return false;
        }

        if (findByName(tc, PANEL_NAME) != null) {
            return true;
        }

        Component[] original = tc.getComponents();

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        JPanel nativeHolder = new JPanel(new BorderLayout());
        nativeHolder.setOpaque(false);

        for (Component c : original) {
            tc.remove(c);
            nativeHolder.add(c, BorderLayout.CENTER);
        }

        LinksPanel links = new LinksPanel();
        links.setName(PANEL_NAME);

        holder.add(nativeHolder, BorderLayout.CENTER);
        holder.add(links, BorderLayout.SOUTH);

        tc.setLayout(new BorderLayout());
        tc.add(holder, BorderLayout.CENTER);
        tc.revalidate();
        tc.repaint();
        return true;
    }

    private static Component findByName(Container root, String name) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof Container) {
                Component found = findByName((Container)c, name);
                if (found != null) return found;
            }
        }
        return null;
    }
}
