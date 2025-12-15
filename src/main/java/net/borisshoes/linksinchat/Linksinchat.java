package net.borisshoes.linksinchat;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.MessageArgument.getMessage;
import static net.minecraft.commands.arguments.MessageArgument.message;

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
               .then(argument("player", EntityArgument.player())
                     .then(argument("message", message())
                           .executes(ctx -> whisper(ctx.getSource(), getMessage(ctx, "message").getString(), EntityArgument.getPlayer(ctx,"player")))))
         );
      });
   }
   
   public static int broadcast(CommandSourceStack source, String message) throws CommandSyntaxException{
      try{
         final Component error = Component.literal("Invalid Link").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
         ServerPlayer player = source.getPlayer();
         
         URI uri;
         try{
            uri = ensureHttpsAndValidate(message);
            if(uri == null) throw new RuntimeException();
         }catch(Exception e){
            player.displayClientMessage(error, false);
            return -1;
         }
         
         Team abstractTeam = player.getTeam();
         ChatFormatting playerColor = abstractTeam != null ? abstractTeam.getColor() : ChatFormatting.WHITE;
         
         final Component announceText = Component.literal("")
               .append(Component.literal(source.getTextName()).withStyle(playerColor).withStyle())
               .append(Component.literal(" has a link to share!").withStyle());
         URI finalUri = uri;
         final Component text = Component.literal(message).withStyle(s ->
               s.withClickEvent(new ClickEvent.OpenUrl(finalUri))
                     .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to Open Link!")))
                     .withColor(ChatFormatting.BLUE).withUnderlined(true));
         
         source.getServer().getPlayerList().broadcastSystemMessage(announceText, false);
         source.getServer().getPlayerList().broadcastSystemMessage(text, false);
         return Command.SINGLE_SUCCESS; // Success
      }catch(Exception e){
         e.printStackTrace();
         return 0;
      }
   }
   
   public static int whisper(CommandSourceStack source, String message, ServerPlayer target) throws CommandSyntaxException{
      try{
         ServerPlayer player = source.getPlayer();
         
         URI uri;
         try{
            uri = ensureHttpsAndValidate(message);
            if(uri == null) throw new RuntimeException();
         }catch(Exception e){
            final Component error = Component.literal("Invalid Link").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
            player.displayClientMessage(error, false);
            return -1;
         }
         
         Team abstractTeam = player.getTeam();
         ChatFormatting playerColor = abstractTeam != null ? abstractTeam.getColor() : ChatFormatting.WHITE;
         
         if (!player.equals(target)){
            final Component senderText = Component.literal("")
                  .append(Component.literal("You whisper a link to ").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                  .append(Component.literal(source.getTextName()).withStyle(playerColor).withStyle(ChatFormatting.ITALIC));
            
            final Component senderLink = Component.literal(message).withStyle(s ->
                  s.withClickEvent(new ClickEvent.OpenUrl(uri))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to Open Link!")))
                        .withColor(ChatFormatting.BLUE).withItalic(true));
            player.sendSystemMessage(senderText);
            player.sendSystemMessage(senderLink);
         }
         
         final Component announceText = Component.literal("")
               .append(Component.literal(source.getTextName()).withStyle(playerColor).withStyle(ChatFormatting.ITALIC))
               .append(Component.literal(" whispers a link to you!").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
         final Component text = Component.literal(message).withStyle(s ->
               s.withClickEvent(new ClickEvent.OpenUrl(uri))
                     .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to Open Link!")))
                     .withColor(ChatFormatting.BLUE).withItalic(true).withUnderlined(true));
         
         target.sendSystemMessage(announceText);
         target.sendSystemMessage(text);
         return Command.SINGLE_SUCCESS; // Success
      }catch(Exception e){
         e.printStackTrace();
         return 0;
      }
   }
   
   public static URI ensureHttpsAndValidate(String input) {
      if (input == null) return null;
      String urlStr = input.trim();
      if (urlStr.isEmpty()) return null;
      if (!urlStr.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
         urlStr = "https://" + urlStr;
      }
      try {
         URI uri = new URI(urlStr);
         String scheme = uri.getScheme();
         if (scheme == null) return null;
         if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) return null;
         String host = uri.getHost();
         if (host == null || host.isEmpty()) return null;
         return uri;
      } catch (URISyntaxException e) {
         return null;
      }
   }
}
