package today.exists;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;
import java.util.UUID;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Main implements EventListener {

    public static void main(String[] args) throws IOException {
        new File("backup").mkdirs();
        new File("data").mkdirs();
        System.out.println("Hello world!");
        // Note: It is important to register your ReadyListener before building
        JDA jda = JDABuilder.createDefault(Files.readString(new File("secret.txt").toPath()))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new Main())
                .build();
        jda.updateCommands().addCommands(
                Commands.slash("begin_listening", "Begins listening for people sending messages").setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("stop_listening", "Stops listening and sends what was sent as one txt file").setDefaultPermissions(DefaultMemberPermissions.DISABLED)
        ).queue();

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Working!");
    }

    @Override
    public void onEvent(@NotNull GenericEvent genericEvent) {
        if (genericEvent instanceof SlashCommandInteractionEvent event) {
            switch (event.getFullCommandName()) {
                case "begin_listening" -> {
                    File file = new File("data/" + event.getChannelId() + ".md");
                    if (!file.exists()) {
                        event.reply("Beginning to listen to messages in channel with id [" + event.getChannelId() + "] and name [" + event.getChannel().getName() + "]").queue();
                        try {
                            Files.write(file.toPath(), ("# Listening from " + Calendar.getInstance().getTime()).getBytes());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        event.reply("Already listening in channel with id [" + event.getChannelId() + "] and name [" + event.getChannel().getName() + "]").queue();
                    }
                }
                case "stop_listening" -> {
                    File file = new File("data/" + event.getChannelId() + ".md");
                    if (file.exists()) {
                        event.replyFiles(FileUpload.fromData(file, event.getChannel().getName() + "_listened_messages.md")).queue();
                        try {
                            Files.move(file.toPath(), new File("backup/" + Calendar.getInstance().getTime() + "-" + UUID.randomUUID() + "-" + event.getChannelId() + ".md").toPath());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        event.reply("Haven't been listening in channel with id [" + event.getChannelId() + "] and name [" + event.getChannel().getName() + "]").queue();
                    }
                }
            }
        } else if (genericEvent instanceof MessageReceivedEvent event) {
            if (event.getAuthor().isBot()) {
                return;
            }
            File file = new File("data/" + event.getChannel().getId() + ".md");
            if (file.exists()) {
                try {
                    Files.write(file.toPath(), ("\n### From " + event.getAuthor().getAsTag() + " at " + Calendar.getInstance().getTime() + "\n" + event.getMessage().getContentRaw()).getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                event.getMessage().addReaction(Emoji.fromUnicode("\uD83E\uDD3A")).queue();
            }
        }
    }
}