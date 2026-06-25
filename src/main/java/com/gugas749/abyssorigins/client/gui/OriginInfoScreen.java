package com.gugas749.abyssorigins.client.gui;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class OriginInfoScreen extends Screen {

    public OriginInfoScreen() {
        super(Component.translatable("gui.abyssorigins.origin_info"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        // Dark background
        gfx.fill(0, 0, width, height, 0xCC000000);
        super.render(gfx, mouseX, mouseY, delta);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Origin origin = AbyssOriginsAPI.getPlayerOrigin(mc.player);
        PlayerOriginData data = mc.player.getData(OriginAttachments.PLAYER_ORIGIN.get());

        int cx = width / 2;
        int y = height / 4;
        int lineH = 12;

        // Title
        gfx.drawCenteredString(font,
            Component.literal("§6✦ Origin Info ✦"), cx, y, 0xFFFFFF);
        y += lineH * 2;

        if (origin == null) {
            gfx.drawCenteredString(font,
                Component.literal("§7No origin assigned."), cx, y, 0xAAAAAA);
        } else {
            gfx.drawCenteredString(font,
                Component.literal("§b" + origin.getDisplayName()), cx, y, 0xFFFFFF);
            y += lineH;

            // Description (wrap manually at ~50 chars)
            String desc = origin.getDescription();
            for (String line : wrapText(desc, 50)) {
                gfx.drawCenteredString(font, Component.literal("§7" + line), cx, y, 0xAAAAAA);
                y += lineH;
            }
            y += lineH;

            // Stats
            gfx.drawCenteredString(font,
                Component.literal("§eLevel: §f" + data.getOriginLevel()
                    + "   §eXP: §f" + data.getOriginXp()), cx, y, 0xFFFFFF);
            y += lineH;

            if (origin.getResourceType() != null) {
                int blood = (int) data.getResource();
                gfx.drawCenteredString(font,
                    Component.literal("§c" + origin.getResourceType().getDisplayName().getString()
                        + ": §f" + blood + " / " + origin.getResourceType().getMaxAmount()),
                    cx, y, 0xFFFFFF);
                y += lineH;
            }

            y += lineH;

            // Abilities
            gfx.drawCenteredString(font, Component.literal("§aAbilities:"), cx, y, 0xFFFFFF);
            y += lineH;
            for (int i = 0; i < origin.getAbilities().size(); i++) {
                var ability = origin.getAbilities().get(i);
                String key = i == 0 ? "[R]" : "[V]";
                int cd = i == 0 ? data.getCooldown1() : data.getCooldown2();
                String cdStr = cd > 0 ? " §c(" + (cd / 20) + "s)" : " §a(Ready)";
                gfx.drawCenteredString(font,
                    Component.literal("§e" + key + " §f" + ability.getDisplayName() + cdStr),
                    cx, y, 0xFFFFFF);
                y += lineH;
            }

            y += lineH;

            // Passives
            gfx.drawCenteredString(font, Component.literal("§dPassives:"), cx, y, 0xFFFFFF);
            y += lineH;
            for (var passive : origin.getPassives()) {
                if (passive.getDescription().equals("Internal bat form countdown.")) continue;
                gfx.drawCenteredString(font,
                    Component.literal("§7• " + passive.getDisplayName()), cx, y, 0xCCCCCC);
                y += lineH;
            }
        }

        // Close hint
        gfx.drawCenteredString(font,
            Component.literal("§8[O or ESC to close]"), cx, height - 20, 0x888888);
    }

    private java.util.List<String> wrapText(String text, int maxChars) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String word : words) {
            if (cur.length() + word.length() + 1 > maxChars) {
                lines.add(cur.toString().trim());
                cur = new StringBuilder();
            }
            cur.append(word).append(" ");
        }
        if (!cur.isEmpty()) lines.add(cur.toString().trim());
        return lines;
    }
}
