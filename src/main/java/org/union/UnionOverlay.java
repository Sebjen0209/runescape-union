package org.union;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class UnionOverlay extends OverlayPanel {

    private final ExamplePlugin plugin;

    @Inject
    public UnionOverlay(ExamplePlugin plugin) {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Goblin Slayer")
                .build());

        int total = plugin.getKillCountsByName().values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Goblins:")
                .right(String.valueOf(total))
                .build());

        return super.render(graphics);
    }
}