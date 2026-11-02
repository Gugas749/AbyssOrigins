package com.gugas749.abyssorigins.client.gui.admin;

import com.gugas749.abyssorigins.admin.ProfileService;
import com.gugas749.abyssorigins.admin.ProfileService.Action;
import com.gugas749.abyssorigins.admin.ProfileSnapshot;
import com.gugas749.abyssorigins.network.packet.ProfileEditPacket;
import com.gugas749.abysscore.api.gui.dialog.AbyssDialog;
import com.gugas749.abysscore.api.gui.screen.AbyssPanelScreen;
import com.gugas749.abysscore.api.gui.screen.AbyssTab;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Staff profile — /abyssorigins profile &lt;player&gt;. Shows a player's origin, class, levels,
 * XP, skills and abilities (hidden ones included), and edits them.
 *
 * Purely a view: every button sends a ProfileEditPacket that the server re-validates against
 * AbyssCore ADMIN, then answers with a fresh snapshot that re-renders this screen.
 */
public class ProfileScreen extends AbyssPanelScreen {

    /** The profile currently open (or behind a dialog), so refreshes can find it. */
    private static ProfileScreen current;

    private ProfileSnapshot snapshot;

    private ProfileScreen(ProfileSnapshot snapshot) {
        super(Component.translatable("gui.abyssorigins.profile.title", snapshot.name()));
        this.snapshot = snapshot;
    }

    /** From OpenProfilePacket: open a new profile, or refresh the one on screen. */
    public static void receive(ProfileSnapshot snapshot, boolean open) {
        Minecraft mc = Minecraft.getInstance();
        if (open) {
            current = new ProfileScreen(snapshot);
            mc.setScreen(current);
            return;
        }
        if (current == null || !current.snapshot.uuid().equals(snapshot.uuid())) return;
        current.snapshot = snapshot;
        if (mc.screen == current) current.rebuildWidgets();   // behind a dialog: rebuilt when it returns
    }

    public ProfileSnapshot snapshot() { return snapshot; }

    @Override
    protected void addTabs(List<AbyssTab> tabs) {
        tabs.add(new OverviewTab(this));
        tabs.add(new AbilitiesTab(this));
        tabs.add(new SkillsTab(this));
        tabs.add(new AssignTab(this));
    }

    @Override protected int panelWidth()  { return 420; }
    @Override protected int panelHeight() { return 240; }
    @Override protected AbyssPermissionLevel requiredLevel() { return ProfileService.REQUIRED; }

    @Override
    public void onClose() {
        current = null;
        super.onClose();
    }

    // ---- Helpers for the tabs ------------------------------------------------------------

    void send(Action action, String arg, int value) {
        PacketDistributor.sendToServer(new ProfileEditPacket(snapshot.uuid(), action, arg, value));
    }

    void send(Action action) { send(action, "", 0); }

    /** Asks for a whole number in [1, max], then runs {@code then}. */
    void askAmount(Component title, int max, IntConsumer then) {
        if (minecraft == null) return;
        minecraft.setScreen(AbyssDialog.input(this, title,
            Component.translatable("gui.abyssorigins.profile.amount_hint", max), "",
            s -> parse(s, max) > 0,
            s -> then.accept(parse(s, max))));
    }

    void confirm(Component title, Component message, Runnable action) {
        if (minecraft != null) minecraft.setScreen(AbyssDialog.confirmDanger(this, title, message, action));
    }

    void goToTab(int index) { selectTab(index); }

    private static int parse(String s, int max) {
        try {
            int v = Integer.parseInt(s.trim());
            return v >= 1 && v <= max ? v : -1;
        } catch (NumberFormatException e) { return -1; }
    }
}
