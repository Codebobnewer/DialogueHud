package com.goga221.dialoguehud.session;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Suppresses a player's jump for the duration of a dialogue via a transient
 * {@link AttributeModifier} on {@link Attribute#JUMP_STRENGTH} - "transient" meaning it is
 * never written to the player's saved data, unlike mutating the attribute's base value
 * directly. If plugin/server state is ever lost mid-session (a crash, a reload) before
 * {@link #restore(Player)} runs, a transient modifier simply doesn't survive the player's
 * next login, so jumping can never be left permanently broken the way a base-value change
 * could be.
 */
public final class JumpSuppressor {

    private final AttributeModifier modifier;

    public JumpSuppressor(Plugin plugin) {
        this.modifier = new AttributeModifier(
                new NamespacedKey(plugin, "dialogue-no-jump"),
                -1.0,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
        );
    }

    public void suppress(Player player) {
        AttributeInstance jumpStrength = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jumpStrength != null && jumpStrength.getModifier(modifier.getKey()) == null) {
            jumpStrength.addTransientModifier(modifier);
        }
    }

    public void restore(Player player) {
        AttributeInstance jumpStrength = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (jumpStrength != null) {
            jumpStrength.removeModifier(modifier.getKey());
        }
    }
}
