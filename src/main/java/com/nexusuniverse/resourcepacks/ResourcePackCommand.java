package com.nexusuniverse.resourcepacks;

import com.nexusuniverse.resourcepacks.config.NexusResourcePacksConfig;
import com.nexusuniverse.resourcepacks.config.PackDefinition;
import com.nexusuniverse.resourcepacks.delivery.PackDeliveryManager;
import com.nexusuniverse.resourcepacks.gui.ResourcePackMenu;
import com.nexusuniverse.resourcepacks.hosting.PackHostServer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ResourcePackCommand implements CommandExecutor {

    private final PackDeliveryManager delivery;
    private final ResourcePackMenu menu;
    private final NexusResourcePacksConfig config;
    private final PackHostServer host;

    public ResourcePackCommand(PackDeliveryManager delivery, ResourcePackMenu menu, NexusResourcePacksConfig config, PackHostServer host) {
        this.delivery = delivery;
        this.menu = menu;
        this.config = config;
        this.host = host;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("nexusresourcepacks.admin")) {
                sender.sendMessage("§cYou don't have permission to do that.");
                return true;
            }
            config.reload();
            host.refreshAll();
            sender.sendMessage("§aConfig reloaded -- any pack you added or removed under packs: will be picked up in the background.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
            List<PackDefinition> packs = config.packs();
            if (packs.isEmpty()) {
                sender.sendMessage("§7No packs are configured yet -- add one under packs: in config.yml.");
            } else {
                String names = packs.stream().map(p -> p.displayName() + " §7(" + p.id() + "§7)").collect(Collectors.joining("§7, "));
                sender.sendMessage("§bAvailable packs: §f" + names);
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("select")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /resourcepack select <id|none> -- see /resourcepack list for ids.");
                return true;
            }
            String id = args[1];
            if (!"none".equalsIgnoreCase(id) && config.findPack(id) == null) {
                player.sendMessage("§cNo pack named \"" + id + "\" -- see /resourcepack list for ids.");
                return true;
            }
            delivery.setPreference(player, "none".equalsIgnoreCase(id) ? NexusResourcePacksConfig.NONE_ID : id);
            player.sendMessage("§aDone.");
            return true;
        }

        player.openInventory(menu.build(player));
        return true;
    }
}
