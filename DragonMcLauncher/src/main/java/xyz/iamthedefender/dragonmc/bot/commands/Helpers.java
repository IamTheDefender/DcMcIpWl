package xyz.iamthedefender.dragonmc.bot.commands;

import xyz.iamthedefender.dragonmc.Main;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Helpers {
    public static boolean isAdmin(SlashCommandInteractionEvent e) {
        if (e.getMember() == null) return false;
        return e.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(Main.config.getAdminRoleId()));
    }

    public static boolean isModOrAdmin(SlashCommandInteractionEvent e) {
        if (e.getMember() == null) return false;
        String a = Main.config.getAdminRoleId(), m = Main.config.getModRoleId();
        return e.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(a) || r.getId().equals(m));
    }

    public static String opt(SlashCommandInteractionEvent e, String name, String def) {
        var o = e.getOption(name);
        return o != null ? o.getAsString() : def;
    }
}
