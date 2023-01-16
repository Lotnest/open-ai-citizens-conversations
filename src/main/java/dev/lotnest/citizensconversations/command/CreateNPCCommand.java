package dev.lotnest.citizensconversations.command;

import dev.lotnest.citizensconversations.trait.ConversationalTrait;
import dev.lotnest.citizensconversations.util.TextureConstants;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CreateNPCCommand extends Command {

    public CreateNPCCommand() {
        super("createnpc");
    }

    @Override
    public boolean execute(@NonNull CommandSender sender, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
        } else {
            if (!player.hasPermission("citizensconversations.createnpc")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to execute this command.");
            } else {
                switch (args.length) {
                    case 0 ->
                            player.sendMessage(ChatColor.RED + "You must specify a role for the NPC, for example: " + ChatColor.YELLOW + "/createnpc miner");
                    case 1 -> {
                        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, ChatColor.GREEN + "OpenAI " + args[0].substring(0, 1).toUpperCase()
                                + args[0].substring(1));
                        SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);

                        npc.addTrait(ConversationalTrait.class);
                        skinTrait.setSkinPersistent("ai", TextureConstants.ROBOT_SIGNATURE, TextureConstants.ROBOT_VALUE);
                        skinTrait.setShouldUpdateSkins(false);
                        npc.spawn(player.getLocation());

                        player.sendMessage(ChatColor.GREEN + "NPC with the role " + ChatColor.GOLD + args[0] + ChatColor.GREEN + " has been created.");
                    }
                    default ->
                            player.sendMessage(ChatColor.RED + "Invalid arguments. Usage: " + ChatColor.YELLOW + "/createnpc <role>");
                }
            }
        }

        return true;
    }
}
