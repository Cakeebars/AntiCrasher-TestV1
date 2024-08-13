package net.craftsupport.anticrasher.packet;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;
import me.clip.placeholderapi.PlaceholderAPI;
import net.craftsupport.anticrasher.AntiCrasher;
import net.craftsupport.anticrasher.utils.utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import static org.bukkit.Bukkit.getLogger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TabCompleteListener implements PacketListener {
    private final AntiCrasher plugin;
    private final utils utilsInstance;

    public TabCompleteListener(AntiCrasher plugin, utils util) {
        this.plugin = plugin;
        this.utilsInstance = util;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
            WrapperPlayClientTabComplete wrapper = new WrapperPlayClientTabComplete(event);
            String text = wrapper.getText();
            final int length = text.length();
            Player player = (Player) event.getPlayer();

            // general length limit
            if (length > 256 && !player.hasPermission("anticrasher.bypass")) {
                handleInvalidPacket(event);
            }
            // paper's patch
            final int index;
            if (text.length() > 64 && ((index = text.indexOf(' ')) == -1 && !player.hasPermission("anticrasher.bypass") || index >= 64 && !player.hasPermission("anticrasher.bypass"))) {
                handleInvalidPacket(event);
            }
        }
    }
    public void handleInvalidPacket(PacketReceiveEvent event) {
        event.setCancelled(true);
        event.getUser().closeConnection();
        if (utilsInstance.logtofile) {
            try {
                log(event.getUser().getName() + " most likely tried to use a Tab Complete Crash Exploit");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (utilsInstance.logattempts) {
            getLogger().warning(event.getUser().getName() + " most likely tried to use a Tab Complete Crash Exploit");
        }
        if (utilsInstance.punishonattempt) {
            String replacedString = utilsInstance.punishcommand.replace("%player%", event.getUser().getName());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (plugin.isPAPIEnabled()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(event.getUser().getUUID()), replacedString));
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replacedString);
                }
            });
        }
    }



    public void log(String message) throws IOException {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(utilsInstance.dataFolder + "/LOGS", true));

            String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            writer.write(timestamp + " - " + message);
            writer.newLine();
            writer.close();

        } catch (IOException e) {
            getLogger().info(("An error occurred: " + e.getMessage()));
        }
    }
}
