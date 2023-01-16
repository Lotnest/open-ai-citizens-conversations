package dev.lotnest.citizensconversations.trait;

import com.google.common.collect.Maps;
import dev.lotnest.citizensconversations.CitizensConversations;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

@TraitName("conversational")
public class ConversationalTrait extends Trait {

    public static final String CONVERSATION_START_MESSAGE = """
            You are now role playing as a %s who is having a conversation with a player in Minecraft.
            You need to limit your knowledge to the world of Minecraft and being a %s.
            Play your role the best and never stray away from your role, even when asked about something else.
            <Player> Hello!
            <AI>\s
            """;
    private static final String NPC_NAME_PREFIX = "OpenAI";

    private final Map<UUID, StringBuilder> conversations;
    private final CitizensConversations plugin;

    public ConversationalTrait() {
        super("conversational");
        this.conversations = Maps.newHashMap();
        this.plugin = JavaPlugin.getPlugin(CitizensConversations.class);
    }

    private void handleOpenAIResponse(@NonNull String npcName, @NonNull Player player, @Nullable String message) {
        boolean isMessageNull = message == null;

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "Thinking..."));
        plugin.generateOpenAIResponse(isMessageNull ? npcName : message, isMessageNull)
                .thenAccept(response -> player.sendMessage(ChatColor.BLUE + response))
                .exceptionally(exception -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to generate OpenAI response", exception);
                    player.sendMessage(ChatColor.RED + "Failed to generate OpenAI response, check the console for more details.");
                    return null;
                });
    }

    private boolean isNotOpenAINPC(@NonNull String npcName) {
        return !npcName.startsWith(NPC_NAME_PREFIX);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRightClick(@NonNull NPCRightClickEvent event) {
        if (!event.getNPC().hasTrait(ConversationalTrait.class)) {
            return;
        }

        String npcName = event.getNPC().getName().trim();
        if (isNotOpenAINPC(npcName)) {
            event.getNPC().setName(NPC_NAME_PREFIX + " " + npcName);
        }

        if (!conversations.containsKey(event.getClicker().getUniqueId())) {
            conversations.put(event.getClicker().getUniqueId(), new StringBuilder(CONVERSATION_START_MESSAGE.formatted(npcName, npcName)));
            handleOpenAIResponse(npcName.replace(NPC_NAME_PREFIX, ""), event.getClicker(), null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(@NonNull AsyncPlayerChatEvent event) {
        CitizensAPI.getNPCRegistry().sorted()
                .iterator()
                .forEachRemaining(npc -> {
                    if (npc.hasTrait(ConversationalTrait.class)) {
                        String npcName = npc.getName().trim();
                        if (isNotOpenAINPC(npcName)) {
                            npc.setName(NPC_NAME_PREFIX + " " + npcName);
                        }

                        if (conversations.containsKey(event.getPlayer().getUniqueId())) {
                            StringBuilder updatedConversationBuilder = conversations.get(event.getPlayer().getUniqueId()).append(event.getMessage()).append("\n<AI> ");
                            conversations.put(event.getPlayer().getUniqueId(), updatedConversationBuilder);
                            handleOpenAIResponse(npcName.replace(NPC_NAME_PREFIX, ""), event.getPlayer(), updatedConversationBuilder.toString());
                            event.setCancelled(true);
                        }
                    }
                });
    }
}
