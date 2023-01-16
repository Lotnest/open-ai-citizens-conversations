package dev.lotnest.citizensconversations;

import com.google.common.base.Strings;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import dev.lotnest.citizensconversations.command.CreateNPCCommand;
import dev.lotnest.citizensconversations.trait.ConversationalTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class CitizensConversations extends JavaPlugin {

    private OpenAiService openAiService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        checkAndSetOpenAIKey();
        registerCommands();
        registerListeners();
        registerTraits();
    }

    private void checkAndSetOpenAIKey() {
        String defaultValue = "API_KEY";
        String configValue = getConfig().getString("open-ai-api-key", defaultValue);

        if (Objects.equals(configValue, defaultValue) || configValue.isBlank()) {
            getLogger().severe("Open AI API key is not set in your config.yml, disabling the plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        openAiService = new OpenAiService(configValue, Duration.of(60, ChronoUnit.SECONDS));
    }

    private void registerCommands() {
        Field bukkitCommandMap;
        try {
            bukkitCommandMap = getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);

            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(getServer());
            commandMap.register("citizensconversations", new CreateNPCCommand());
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            exception.printStackTrace();
        }
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new ConversationalTrait(), this);
    }

    private void registerTraits() {
        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(ConversationalTrait.class));
    }

    public CompletableFuture<String> generateOpenAIResponse(String prompt, boolean isStarterPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            if (Strings.isNullOrEmpty(prompt)) {
                return "";
            }

            String conversationStartMessageFormatted = ConversationalTrait.CONVERSATION_START_MESSAGE.formatted(prompt, prompt);

            CompletionRequest completionRequest = CompletionRequest.builder()
                    .prompt(isStarterPrompt ? conversationStartMessageFormatted : conversationStartMessageFormatted + prompt)
                    .model("text-davinci-003")
                    .frequencyPenalty(0.0)
                    .temperature(0.7)
                    .topP(1.0)
                    .presencePenalty(0.7)
                    .stop(List.of("<Player>", "<AI>"))
                    .build();
            List<CompletionChoice> completionChoices = openAiService.createCompletion(completionRequest).getChoices();

            if (completionChoices.isEmpty()) {
                return "";
            }

            return completionChoices.get(0).getText().stripLeading();
        });
    }
}
