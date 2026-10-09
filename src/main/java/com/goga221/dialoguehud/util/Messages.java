package com.goga221.dialoguehud.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Messages() {
    }

    public static void send(Audience audience, String miniMessageText) {
        audience.sendMessage(MINI_MESSAGE.deserialize(miniMessageText));
    }
}
