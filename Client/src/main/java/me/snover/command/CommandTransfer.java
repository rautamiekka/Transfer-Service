package me.snover.command;

import me.snover.ColorPalette;
import me.snover.TransferClient;
import me.snover.config.CompositeTransferConfiguration;
import me.snover.config.ResourceOptions;
import me.snover.messaging.PluginMessageSender;
import me.snover.pointer.CoordinateContainer;
import me.snover.pointer.CoordinateServerRegistry;
import me.snover.pointer.CoordinateSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * This class handles everything for the transfer command
 */
public class CommandTransfer extends Command {

    final Component USAGE = Component.text("Usage:\n/transfer register\n/transfer listservers\n/transfer remserver\n/transfer showcoord\n/transfer remcoord\n/transfer test\n/transfer setspawn\n/transfer toggleforcedspawn", TextColor.color(ColorPalette.DARK_RED.getR(), ColorPalette.DARK_RED.getG(), ColorPalette.DARK_RED.getB()));
    final Component REGISTER_USAGE = Component.text("Usage: /transfer register <server> <x> <y> <z> (Coordinates optional when used in-game. Server name is case sensitive!)", TextColor.color(ColorPalette.DARK_RED.getR(), ColorPalette.DARK_RED.getG(), ColorPalette.DARK_RED.getB()));
    final Component REMSERVER_USAGE = Component.text("Usage: /transfer remserver <server>", TextColor.color(ColorPalette.DARK_RED.getR(), ColorPalette.DARK_RED.getG(), ColorPalette.DARK_RED.getB()));
    final Component SHOWCOORD_USAGE = Component.text("Usage: /transfer showcoord <server>", TextColor.color(ColorPalette.DARK_RED.getR(), ColorPalette.DARK_RED.getG(), ColorPalette.DARK_RED.getB()));
    final Component REMCOORD_USAGE = Component.text("Usage: /transfer remcoord <server> <x> <y> <z>", TextColor.color(ColorPalette.DARK_RED.getR(), ColorPalette.DARK_RED.getG(), ColorPalette.DARK_RED.getB()));
    final Component TEST_USAGE = Component.text("Usage: /transfer test <server> <player>", TextColor.color(ColorPalette.DARK_RED.getR(), ColorPalette.DARK_RED.getG(), ColorPalette.DARK_RED.getB()));
    final Component SET_SPAWN_USAGE = Component.text("Usage: /transfer setspawn <x> <y> <z> (coordinates optional when used in-game)", TextColor.color(ColorPalette.DARK_RED.getR(), ColorPalette.DARK_RED.getG(), ColorPalette.DARK_RED.getB()));
    final Component SERVER_NOT_FOUND = Component.text("Server not not found in registry!", TextColor.color(ColorPalette.DARK_RED.getR(), ColorPalette.DARK_RED.getG(), ColorPalette.DARK_RED.getB()));
    final Component DISALLOW = Component.text("Not Allowed!", TextColor.color(ColorPalette.DARK_RED.getR(), ColorPalette.DARK_RED.getG(), ColorPalette.DARK_RED.getB()));

    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    private CompositeTransferConfiguration config;
    public CommandTransfer(CompositeTransferConfiguration config) {
        super("transfer");
        this.setDescription("Facilitates the use of the transfer plugin");
        this.config = config;
    }

    /**
     * The entry point where the command will be executed
     * @param sender Source object which is executing this command
     * @param commandLabel The alias of the command used
     * @param args All arguments passed to the command, split via ' '
     * @return Returns true if command executes successfully.
     */
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if(args.length < 1) {
            sender.sendMessage(USAGE);
            return false;
        }
        if(args[0].equalsIgnoreCase("register")) return executeRegister(sender, args);
        if(args[0].equalsIgnoreCase("listservers")) return executeListServers(sender);
        if(args[0].equalsIgnoreCase("remserver")) return executeRemServer(sender, args);
        if(args[0].equalsIgnoreCase("showcoord")) return executeShowCoord(sender, args);
        if(args[0].equalsIgnoreCase("remcoord"))  return executeRemCoord(sender, args);
        if(args[0].equalsIgnoreCase("test")) return executeTest(sender, args);
        if(args[0].equalsIgnoreCase("setspawn")) return executeSetSpawn(sender, args);
        if(args[0].equalsIgnoreCase("toggleforcedspawn")) return executeToggleForcedSpawn(sender);

        sender.sendMessage(USAGE);
        return false;
    }

    /**
     * Executes the set server portion of the transfer command
     * @param sender The command sender
     * @param args The command arguments
     * @return Returns {@code true} if command successful, returns {@code false} if command fails
     */
    private boolean executeRegister(CommandSender sender, String[] args) {
        //Check if there are enough arguments
        if(args.length < 2) {
            sender.sendMessage(REGISTER_USAGE);
            return false;
        }

        String tgtServer = args[1];
        //Begin registration process for 2 arguments
        if(args.length == 2) {
            if(sender instanceof Player player) {
                if(!player.isOp()) {
                    player.sendMessage(DISALLOW);
                    return false;
                }

                CoordinateContainer container;
                if(CoordinateServerRegistry.exists(tgtServer)) container = CoordinateServerRegistry.getContainer(tgtServer);
                else container = new CoordinateContainer(tgtServer);

                int x = player.getLocation().getBlockX();
                int y = player.getLocation().getBlockY();
                int z = player.getLocation().getBlockZ();
                sender.sendMessage(Component.text("Registering a new coordinate set:" + "\nx: " + x + "\ny: " + y + "\nz: " + z, TextColor.color(ColorPalette.AQUA.getR(), ColorPalette.AQUA.getG(), ColorPalette.AQUA.getB())));
                container.addCoordinateSet(x, y, z);
                CoordinateServerRegistry.add(tgtServer, container);
                if(!ResourceOptions.servers.contains(tgtServer)) ResourceOptions.servers.add(tgtServer);
                config.saveResources(true, true);
                return true;
            }
        }

        //Begin registration process with given coordinates
        if (args.length == 5) {
            int x;
            int y;
            int z;

            try {
                x = Integer.parseInt(args[2]);
                y = Integer.parseInt(args[3]);
                z = Integer.parseInt(args[4]);
            } catch(NumberFormatException e) {
                sender.sendMessage(Component.text("Coordinates must contain only whole numbers!\n" + REGISTER_USAGE, TextColor.color(ColorPalette.DARK_RED.getR(), ColorPalette.DARK_RED.getG(), ColorPalette.DARK_RED.getB())));
                return false;
            }

            CoordinateContainer container;
            if(CoordinateServerRegistry.exists(tgtServer)) {
                container = CoordinateServerRegistry.getContainer(tgtServer);
            } else container = new CoordinateContainer(tgtServer);

            container.addCoordinateSet(x, y, z);
            CoordinateServerRegistry.add(tgtServer, container);
            config.saveResources(false, true);
            return true;
        }

        sender.sendMessage(REGISTER_USAGE);
        return false;
    }

    @SuppressWarnings("SameReturnValue")
    private boolean executeListServers(CommandSender sender) {
        final List<String> REG = CoordinateServerRegistry.getRegisteredServers();
        int regSize = REG.size();
        if(regSize < 1) {
            sender.sendMessage(Component.text("No servers registered.", TextColor.color(ColorPalette.AQUA.getR(), ColorPalette.AQUA.getG(), ColorPalette.AQUA.getB())));
            return true;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Servers:");
        for(int i = 0; i < regSize; i++) {
            builder.append("\n").append(REG.get(i));
        }
        sender.sendMessage(Component.text(builder.toString(), TextColor.color(ColorPalette.AQUA.getR(), ColorPalette.AQUA.getG(), ColorPalette.AQUA.getB())));
        return true;
    }

    /**
     * Executes the server removal portion of this command
     * @param sender The command sender
     * @param args The command arguments
     * @return Returns {@code true} if command successful
     */
    private boolean executeRemServer(CommandSender sender, String[] args) {
        if(sender instanceof Player player) {
            if (!player.isOp()) {
                player.sendMessage(DISALLOW);
                return false;
            }
        }
        if(args.length < 2) {
            sender.sendMessage(REMSERVER_USAGE);
            return false;
        }
        if(!CoordinateServerRegistry.exists(args[1])) {
            sender.sendMessage(SERVER_NOT_FOUND);
            return false;
        }
        CoordinateServerRegistry.remove(args[1]);
        ResourceOptions.servers.remove(args[1]);
        config.saveResources(true, true);
        TransferClient.getPlugin().getLogger().info("Removed " + args[1]);
        return true;
    }

    /**
     * Executes the portion of the command that shows all coordinate sets associated with a server
     * @param sender The command sender
     * @param args The command arguments
     * @return Returns {@code true} if command successful
     */
    private boolean executeShowCoord(CommandSender sender, String[] args) {
        if(args.length < 2) {
            sender.sendMessage(SHOWCOORD_USAGE);
            return false;
        }
        if(!CoordinateServerRegistry.exists(args[1])) {
            sender.sendMessage(SERVER_NOT_FOUND);
            return false;
        }

        CoordinateContainer container = CoordinateServerRegistry.getContainer(args[1]);
        CoordinateSet[] coordinateSets = container.getCoordinateSets();
        StringBuilder builder = new StringBuilder();
        builder.append("All coordinates associated with ").append(args[1]).append(":");
        for(int i = 0; i < coordinateSets.length; i++) {
            int setNumber = i + 1;
            builder.append("\nCoordinate Set ").append(setNumber).append(": ").append(coordinateSets[i].getX()).append(", ").append(coordinateSets[i].getY()).append(", ").append(coordinateSets[i].getZ());
        }
        sender.sendMessage(Component.text(builder.toString(), TextColor.color(ColorPalette.AQUA.getR(), ColorPalette.AQUA.getG(), ColorPalette.AQUA.getB())));
        return true;
    }

    /**
     * Executes the portion of the transfer command that removes a set of coordinates for a server
     * @param sender The command sender
     * @param args The command arguments (Arguments MUST NOT be modified)
     * @return Returns {@code true} if command successful
     */
    private boolean executeRemCoord(CommandSender sender, String[] args) {
        if(sender instanceof Player player) {
            if (!player.isOp()) {
                player.sendMessage(DISALLOW);
                return false;
            }
        }
        if(args.length < 5) {
            sender.sendMessage(REMCOORD_USAGE);
            return false;
        }
        CoordinateServerRegistry.getContainer(args[1]).removeCoordinateSet(Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        config.saveResources(false, true);
        sender.sendMessage(Component.text("Removed coordinate set for server: " + args[1], TextColor.color(ColorPalette.AQUA.getR(), ColorPalette.AQUA.getG(), ColorPalette.AQUA.getB())));
        return true;
    }

    /**
     * Executes the test portion of the transfer command
     * @param sender The command sender
     * @param args The command arguments (Arguments MUST NOT be modified)
     * @return Returns {@code true} if command successful
     */
    private boolean executeTest(CommandSender sender, String[] args) {
        if(sender instanceof Player player) {
            if (!player.isOp()) {
                player.sendMessage(DISALLOW);
                return false;
            }
        }
        if(args.length != 3) {
            sender.sendMessage(TEST_USAGE);
            return false;
        }
        String server = args[1];
        Player tgtPlayer = TransferClient.getPlugin().getServer().getPlayerExact(args[2]);
        if(!tgtPlayer.isOnline()) {
            sender.sendMessage(Component.text("Player is not online!", TextColor.color(ColorPalette.DARK_RED.getR(), ColorPalette.DARK_RED.getG(), ColorPalette.DARK_RED.getB())));
            return false;
        }

        PluginMessageSender.sendTransferMessage(TransferClient.getPlugin(), tgtPlayer, server);
        return true;
    }

    /**
     * Sets coordinates for forced spawning
     * @param sender The command sender
     * @param args The command arguments
     * @return Returns {@code true} if command successful
     */
    public boolean executeSetSpawn(CommandSender sender, String[] args) {
        if(!(sender instanceof Player player)) {
            if(args.length < 4) {
                sender.sendMessage(SET_SPAWN_USAGE);
                return false;
            }
            int x;
            int y;
            int z;

            try {
                x = Integer.parseInt(args[1]);
                y = Integer.parseInt(args[2]);
                z = Integer.parseInt(args[3]);
            } catch(NumberFormatException e) {
                sender.sendMessage(Component.text("Coordinates must be whole numbers!", TextColor.color(ColorPalette.DARK_RED.getR(), ColorPalette.DARK_RED.getG(), ColorPalette.DARK_RED.getB())));
                return false;
            }
            ResourceOptions.spawnLocation = new Location(TransferClient.getPlugin().getServer().getWorld("world"), x, y, z);
            config.saveResources(true, false);
            sender.sendMessage(Component.text("Spawn-point set", TextColor.color(ColorPalette.AQUA.getR(), ColorPalette.AQUA.getG(), ColorPalette.AQUA.getB())));
            return true;
        }
        if(!player.isOp()) {
            player.sendMessage(DISALLOW);
            return false;
        }
        ResourceOptions.spawnLocation = player.getLocation();
        config.saveResources(true, false);
        sender.sendMessage(Component.text("Spawn-point set", TextColor.color(ColorPalette.AQUA.getR(), ColorPalette.AQUA.getG(), ColorPalette.AQUA.getB())));
        return true;
    }

    /**
     * Toggles forced spawning
     * @return Returns {@code true} if command successful
     */
    private boolean executeToggleForcedSpawn(CommandSender sender) {
        if(sender instanceof Player player) {
            if (!player.isOp()) {
                player.sendMessage(DISALLOW);
                return false;
            }
        }
        if(!ResourceOptions.forcedSpawn) {
            ResourceOptions.forcedSpawn = true;
            sender.sendMessage(Component.text("Enabled forced spawning", TextColor.color(ColorPalette.AQUA.getR(), ColorPalette.AQUA.getG(), ColorPalette.AQUA.getB())));
        } else {
            ResourceOptions.forcedSpawn = false;
            sender.sendMessage(Component.text("Disabled forced spawning", TextColor.color(ColorPalette.AQUA.getR(), ColorPalette.AQUA.getG(), ColorPalette.AQUA.getB())));
        }
        config.saveResources(true, false);
        return true;
    }
}