package com.crunchiest.commands.complete;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class NodeCommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("nodebuilder")) {
            if (args.length == 2) {
                completions.add("global");
                completions.add("player");
            } else if (args.length == 3) {
                completions.add("tool");
                completions.add("better");
            }
        }
        return completions;
    }
}