package net.borisshoes.linksinchat;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.minecraft.command.argument.MessageArgumentType.getMessage;
import static net.minecraft.command.argument.MessageArgumentType.message;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Linksinchat implements ModInitializer {
   public static final Logger LOGGER = LogManager.getLogger("Links In Chat");
   
   @Override
   public void onInitialize(){
      LOGGER.info("Loading Links In Chat!");
      
      CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, registrationEnvironment) -> {
         dispatcher.register(literal("link")
               .then(argument("message", message())
                     .executes(ctx -> broadcast(ctx.getSource(), getMessage(ctx, "message").getString())))
         );
         dispatcher.register(literal("linkwhisper")
               .then(argument("player", EntityArgumentType.player())
                     .then(argument("message", message())
                           .executes(ctx -> whisper(ctx.getSource(), getMessage(ctx, "message").getString(),EntityArgumentType.getPlayer(ctx,"player")))))
         );
      });
      
      
   }
   
   public static int broadcast(ServerCommandSource source, String message) throws CommandSyntaxException{
      try{
         ServerPlayerEntity player = source.getPlayer();
         
         if(message.contains(" ") || !(message.contains("https://")||message.contains("http://"))){
            final Text error = Text.literal("Links cannot have spaces and must have http://").formatted(Formatting.RED,Formatting.ITALIC);
            player.sendMessage(error,false);
            return -1;
         }
         
         AbstractTeam abstractTeam = player.getScoreboardTeam();
         Formatting playerColor = abstractTeam != null && abstractTeam.getColor() != null ? abstractTeam.getColor() : Formatting.WHITE;
         
         final Text announceText = Text.literal("")
               .append(Text.literal(source.getName()).formatted(playerColor).formatted())
               .append(Text.literal(" has a link to share!").formatted());
         final Text text = Text.literal(message).styled(s ->
               s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, message))
                     .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to Open Link!")))
                     .withColor(Formatting.BLUE).withUnderline(true));
         
         source.getServer().getPlayerManager().broadcast(announceText,false);
         source.getServer().getPlayerManager().broadcast(text,false);
         return Command.SINGLE_SUCCESS; // Success
      }catch(Exception e){
         e.printStackTrace();
         return 0;
      }
   }
   
   public static int whisper(ServerCommandSource source, String message, ServerPlayerEntity target) throws CommandSyntaxException{
      try{
         ServerPlayerEntity player = source.getPlayer();
         
         if(message.contains(" ") || !(message.contains("https://")||message.contains("http://"))){
            final Text error = Text.literal("Links cannot have spaces and must have http://").formatted(Formatting.RED,Formatting.ITALIC);
            player.sendMessage(error,false);
            return -1;
         }
         
         AbstractTeam abstractTeam = player.getScoreboardTeam();
         Formatting playerColor = abstractTeam != null && abstractTeam.getColor() != null ? abstractTeam.getColor() : Formatting.WHITE;
         
         if (!player.equals(target)){
            final Text senderText = Text.literal("")
                  .append(Text.literal("You whisper a link to ").formatted(Formatting.GRAY,Formatting.ITALIC))
                  .append(Text.literal(source.getName()).formatted(playerColor).formatted(Formatting.ITALIC));
            
            final Text senderLink = Text.literal(message).styled(s ->
                  s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, message))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to Open Link!")))
                        .withColor(Formatting.BLUE).withItalic(true));
            player.sendMessage(senderText);
            player.sendMessage(senderLink);
         }
         
         final Text announceText = Text.literal("")
               .append(Text.literal(source.getName()).formatted(playerColor).formatted(Formatting.ITALIC))
               .append(Text.literal(" whispers a link to you!").formatted(Formatting.GRAY,Formatting.ITALIC));
         final Text text = Text.literal(message).styled(s ->
               s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, message))
                     .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to Open Link!")))
                     .withColor(Formatting.BLUE).withItalic(true).withUnderline(true));
         
         target.sendMessage(announceText);
         target.sendMessage(text);
         return Command.SINGLE_SUCCESS; // Success
      }catch(Exception e){
         e.printStackTrace();
         return 0;
      }
   }
   
   
}
