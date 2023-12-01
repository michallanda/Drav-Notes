package wtf.drav.sootnotes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import kong.unirest.Unirest;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Main extends Plugin {
	
	private Plugin plugin;
	
	// Default messages
	ComponentBuilder pluginHelp = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
			.append("Soot").color(ChatColor.GOLD).bold(true)
			.append("Notes").color(ChatColor.RED)
			.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false)
			.append("Welcome to SootNotes! SootNotes has 3 main components: "
					+ "\n- Note, which adds notes to a player "
					+ "\n- Xray, which marks a player as possible xray "
					+ "\n- Siblings, adds players to a siblings list"
					+ "\nTo get help for any of these, just run the command:"
					+ "\n/notes (note/xray/siblings)").color(ChatColor.WHITE)
			.append("\n-------------------------").color(ChatColor.DARK_GRAY);
	
	ComponentBuilder noteHelp = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
			.append("Soot").color(ChatColor.GOLD).bold(true)
			.append("Notes").color(ChatColor.RED)
			.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false)
			.append("Help for the Note component: "
					+ "\n/notes note add (player) (note)").color(ChatColor.WHITE)
			.append("\nAdds a note to a player.").color(ChatColor.GRAY)
			.append("\n/notes note remove (player) (number)").color(ChatColor.WHITE)
			.append("\nRemoves the sepecified note from a player.").color(ChatColor.GRAY)
			.append("\n/notes note get (player)").color(ChatColor.WHITE)
			.append("\nGets the players' notes.").color(ChatColor.GRAY)
			.append("\n-------------------------").color(ChatColor.DARK_GRAY);
	
	ComponentBuilder xrayHelp = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
			.append("Soot").color(ChatColor.GOLD).bold(true)
			.append("Notes").color(ChatColor.RED)
			.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false)
			.append("Help for the Xray component: ").color(ChatColor.WHITE)
			.append("\n/notes xray add (player)").color(ChatColor.WHITE)
			.append("\nAdds the player to the xray list.").color(ChatColor.GRAY)
			.append("\n/notes xray remove (player)").color(ChatColor.WHITE)
			.append("\nRemoves player from the xray list.").color(ChatColor.GRAY)
			.append("\n/notes xray list").color(ChatColor.WHITE)
			.append("\nGets the xray list.").color(ChatColor.GRAY)
			.append("\n-------------------------").color(ChatColor.DARK_GRAY);
	
	ComponentBuilder siblingHelp = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
			.append("Soot").color(ChatColor.GOLD).bold(true)
			.append("Notes").color(ChatColor.RED)
			.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false)
			.append("Help for the Siblings component: ").color(ChatColor.WHITE)
			.append("\n/notes siblings add {players list}").color(ChatColor.WHITE)
			.append("\nAccepts a list of players, which are added").color(ChatColor.GRAY)
			.append("\nto a siblings list. If any of the listed").color(ChatColor.GRAY)
			.append("\nplayers already have siblings, they will ").color(ChatColor.GRAY)
			.append("\nalso be accounted for, no need to readd.").color(ChatColor.GRAY)
			.append("\n/notes sibling remove (player)").color(ChatColor.WHITE)
			.append("\nRemoves the sepecific player from siblings list.").color(ChatColor.GRAY)
			.append("\n/notes siblings get (player)").color(ChatColor.WHITE)
			.append("\nGets the specified players' siblings.").color(ChatColor.GRAY)
			.append("\n-------------------------").color(ChatColor.DARK_GRAY);
	
	@Override
	public void onEnable() {
		plugin = this; 
		
		ProxyServer.getInstance().getPluginManager().registerCommand(this, new NotesCommand());
	}
	
	public class NotesCommand extends Command implements TabExecutor {
		public NotesCommand() {
			super("notes");
		}

		@Override
		public void execute(final CommandSender sender, final String[] args) {
			// check if player sending command
			if ((sender instanceof ProxiedPlayer && sender.hasPermission("sootnotes.use"))) {
				// run async task to not block up main thread
				getProxy().getScheduler().runAsync(plugin, new Runnable() {
					public void run() {
			            ProxiedPlayer player = (ProxiedPlayer)sender;
			            String uuid = null;
			            Configuration playerConfig = null;
			            
			            // making sure arguments are correct
			            if(args.length == 0) {
							player.sendMessage(pluginHelp.create());
							return;
			            }
			            else {
			            	if(args[0].equalsIgnoreCase("note")) {
			            		if((args.length == 1 || args.length == 2) ||
			            				(args.length == 3 && !(args[1].equalsIgnoreCase("get"))) || 
			            				(!args[1].equalsIgnoreCase("add") && !args[1].equalsIgnoreCase("remove") && !args[1].equalsIgnoreCase("get"))) {
				            		player.sendMessage(noteHelp.create());
				            		return;
			            		}
			            		else if(args[1].equalsIgnoreCase("remove")) {
			            			try {
			            				Integer.parseInt(args[3]);
			            			}
			            			catch(NumberFormatException e){
			            				player.sendMessage(noteHelp.create());
					            		return;
			            			}
			            		}
		            			uuid = getPlayerUUID(player, args[2]);
		            			if(uuid == null)	return;
		            			playerConfig = getConfig(uuid);
		            			playerConfig.set("name", args[2]);
		            			
			            	}
			            	else if(args[0].equalsIgnoreCase("xray")) {
			            		if((args.length == 1) || (args.length == 2 && !args[1].equalsIgnoreCase("list")) ||
			            				(!args[1].equalsIgnoreCase("add") && !args[1].equalsIgnoreCase("remove") && !args[1].equalsIgnoreCase("get") && !args[1].equalsIgnoreCase("list"))) {
			            			player.sendMessage(xrayHelp.create());
			            			return;
			            		}
		            			if(!args[1].equalsIgnoreCase("list")) {
		            				uuid = getPlayerUUID(player, args[2]);
			            			if(uuid == null)	return;
			            			playerConfig = getConfig(uuid);
			            			playerConfig.set("name", args[2]);
			            		}
			            	}
			            	else if(args[0].equalsIgnoreCase("siblings")) {
			            		if((args.length == 1 || args.length == 2) ||
			            				(args.length == 3 && !(args[1].equalsIgnoreCase("get") || args[1].equalsIgnoreCase("remove"))) || 
			            				(!args[1].equalsIgnoreCase("add") && !args[1].equalsIgnoreCase("remove") && !args[1].equalsIgnoreCase("get"))) {
				            		player.sendMessage(siblingHelp.create());
				            		return;
			            		}
		            			uuid = getPlayerUUID(player, args[2]);
		            			if(uuid == null)	return;
		            			playerConfig = getConfig(uuid);
		            			playerConfig.set("name", args[2]);
			            	}
			            	else{
			            		player.sendMessage(pluginHelp.create());
			            		return;
			            	}
			            }
			            
			            // main code
			            switch(args[0].toLowerCase()) {
				            case "note": {
				            	switch(args[1].toLowerCase()) {
					            	case "add": {
				            			String note = "";
				            			for(int i = 3; i < args.length; i++)	note += args[i] + " ";
				            			List<String> allNotes = playerConfig.getStringList("notes");
				            			allNotes.add(player.getName() + ": " + note);
				            			playerConfig.set("notes", allNotes);
				            			  
				            			ComponentBuilder msg = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
				            					.append("Soot").color(ChatColor.GOLD).bold(true)
				            					.append("Notes").color(ChatColor.RED)
				            					.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false)
				            					.append("Player: " + args[2] + "\nAdded Note: " + note).color(ChatColor.WHITE)
				            					.append("\n-------------------------").color(ChatColor.DARK_GRAY);
				            			player.sendMessage(msg.create());
				            			
				            			saveConfig(uuid, playerConfig);
				            			addToLogs(player.getName(), args[2], " added note: " + note);
					            		return;
					            	}
					            	case "remove": {
					            		List<String> allNotes = playerConfig.getStringList("notes");
				            			if(allNotes.size() == 0) {
				            				ComponentBuilder msg = new ComponentBuilder("User has no Notes!").color(ChatColor.RED);
				            				player.sendMessage(msg.create());
				            				return;
				            			}
				            			ComponentBuilder msg = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
				            					.append("Soot").color(ChatColor.GOLD).bold(true)
				            					.append("Notes").color(ChatColor.RED)
				            					.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false);
				            			
				            			if(allNotes.size() == 0)	msg.append("User has no Notes!").color(ChatColor.WHITE);
				            			else {
				            				msg.append("Player: " + args[2] + "\nRemoved Note: " + allNotes.get(Integer.parseInt(args[3])-1)).color(ChatColor.WHITE);
					            			addToLogs(player.getName(), args[2], " removed note: " + allNotes.get(Integer.parseInt(args[3])-1));
					            			allNotes.remove(Integer.parseInt(args[3])-1);
					            			playerConfig.set("name", args[2]);
				            			}
				            			
				            			msg.append("\n-------------------------").color(ChatColor.DARK_GRAY);
			            				player.sendMessage(msg.create());
			            				
				            			if(allNotes.size() == 0)	playerConfig.set("notes", null);
				            			else	playerConfig.set("notes", allNotes);
				            			saveConfig(uuid, playerConfig);
					            		return;
					            	}
					            	case "get": {
				            			List<String> notes = playerConfig.getStringList("notes");
				            			ComponentBuilder msg = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
				            					.append("Soot").color(ChatColor.GOLD).bold(true)
				            					.append("Notes").color(ChatColor.RED)
				            					.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false)
				            					.append("Notes for: " + args[2] + "\n").color(ChatColor.WHITE);
				            			if(notes.size() == 0)	msg.append("No Notes Found").color(ChatColor.WHITE);
				            			for(int i = 0; i < notes.size(); i++) {
				            				msg.append("\n" + (i + 1)).color(ChatColor.GRAY).append(": " + notes.get(i)).color(ChatColor.WHITE);
				            			}
				            			msg.append("\n-------------------------").color(ChatColor.DARK_GRAY);
				            			player.sendMessage(msg.create());
					            		return;
					            	}
				            	}
				            }
				            case "xray": {
				            	switch(args[1].toLowerCase()) {
					            	case "add": {
				            			ComponentBuilder msg = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
				            					.append("Soot").color(ChatColor.GOLD).bold(true)
				            					.append("Notes").color(ChatColor.RED)
				            					.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false);
				            			
				            			if(playerConfig.getBoolean("xray"))	msg.append("Player: " + args[2] + "\nWas Already Marked for Xray").color(ChatColor.WHITE);
				            			else {
				            				msg.append("Player: " + args[2] + "\nMarked for Xray").color(ChatColor.WHITE);
				            				playerConfig.set("xray", true);
				            				addToLogs(player.getName(), args[2], " added xray ");
				            				saveConfig(uuid, playerConfig);
				            				
				            			}
				            			msg.append("\n-------------------------").color(ChatColor.DARK_GRAY);
				            			player.sendMessage(msg.create());
				            			
					            		return;
					            	}
					            	case "remove": {
				            			ComponentBuilder msg = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
				            					.append("Soot").color(ChatColor.GOLD).bold(true)
				            					.append("Notes").color(ChatColor.RED)
				            					.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false);
				            			
				            			if(!playerConfig.getBoolean("xray"))	msg.append("Player: " + args[2] + "\nWasn't Previously Marked").color(ChatColor.WHITE);
				            			else {
				            				msg.append("Player: " + args[2] + "\nUnmarked for Xray").color(ChatColor.WHITE);
					            			playerConfig.set("xray", null);
				            				addToLogs(player.getName(), args[2], " removed xray ");
					            			saveConfig(uuid, playerConfig);
				            			}
				            			
				            			msg.append("\n-------------------------").color(ChatColor.DARK_GRAY);
				            			player.sendMessage(msg.create());
				            			
					            		return;
					            	}
					            	case "get": {
					            		ComponentBuilder msg = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
				            					.append("Soot").color(ChatColor.GOLD).bold(true)
				            					.append("Notes").color(ChatColor.RED)
				            					.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false);
				            			
				            			if(playerConfig.getBoolean("xray"))	msg.append("Player: " + args[2] + "\nIs Marked for Xray").color(ChatColor.WHITE);
				            			else	msg.append("Player: " + args[2] + "\nIs not Marked for Xray").color(ChatColor.WHITE);
				            			msg.append("\n-------------------------").color(ChatColor.DARK_GRAY);
				            			player.sendMessage(msg.create());
				            			
					            		return;
					            	}
					            	case "list": {
					            		ComponentBuilder msg = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
				            					.append("Soot").color(ChatColor.GOLD).bold(true)
				            					.append("Notes").color(ChatColor.RED)
				            					.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false)
				            					.append("Xray List:").color(ChatColor.WHITE);
				            			
					            		for(String xrayUser : getFiles()) {
					            			if(getConfig(xrayUser).getBoolean("xray")) {
					            				msg.append("\n- " + getConfig(xrayUser).getString("name")).color(ChatColor.WHITE);
					            			}
					            		}
					            		msg.append("\n-------------------------").color(ChatColor.DARK_GRAY);
					            		player.sendMessage(msg.create());
					            		return;
					            	}
				            	}
				            }
				            case "siblings": {
				            	switch(args[1].toLowerCase()) {
					            	case "add": {
					            		Hashtable<String, Configuration> uuids = new Hashtable<>();
				            			for(int i = 2; i < args.length; i++) {
				            				String playerUUID = getPlayerUUID(player, args[i]);
				            				if(playerUUID == null)	return;
				            				if(!uuids.contains(playerUUID)) {
			            						uuids.put(playerUUID, getConfig(playerUUID));
			            						uuids.get(playerUUID).set("name", args[i]);
			            					}
				            				for(Object sibling : uuids.get(playerUUID).getList("siblings")) {
				            					if(!uuids.contains(sibling.toString())) {
				            						uuids.put(sibling.toString(),getConfig(sibling.toString()));
				            					}
				            				}
				            			}
				            			String log = " changed siblings list of: ";
				            			ComponentBuilder msg = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
				            					.append("Soot").color(ChatColor.GOLD).bold(true)
				            					.append("Notes").color(ChatColor.RED)
				            					.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false)
				            					.append("New Siblings List: ").color(ChatColor.WHITE);
				            			
				            			for(String playerUUID : uuids.keySet()) {
				            				log += uuids.get(playerUUID).getString("name") + ", ";
				            				msg.append("\n- " + uuids.get(playerUUID).getString("name")).color(ChatColor.WHITE);
				            				List<String> uuidsClone = new ArrayList<String>(uuids.keySet());
				            				uuidsClone.remove(playerUUID);
				            				uuids.get(playerUUID).set("siblings", uuidsClone);

					            			saveConfig(playerUUID, uuids.get(playerUUID));
				            			}		            			
				            			
				            			msg.append("\n-------------------------").color(ChatColor.DARK_GRAY);
				            			player.sendMessage(msg.create());
				            			addToLogs(player.getName(), null, log.substring(0, log.length()-2));
				            			
					            		return;
					            	}
					            	case "remove": {
					            		ArrayList<String> uuids = new ArrayList<String>();
				            			for(Object sibling : playerConfig.getList(uuid + ".siblings")) {
				            				uuids.add(sibling.toString());
			            				}
				            			
				            			ComponentBuilder msg = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
				            					.append("Soot").color(ChatColor.GOLD).bold(true)
				            					.append("Notes").color(ChatColor.RED)
				            					.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false);
				            			if(uuids.size() == 0) {
				            				msg.append("Player: " + args[2] + "\n").color(ChatColor.WHITE)
			            					.append("Had no Siblings Added");
				            			}
				            			else {
			            					msg.append("Removed Player: " + args[2] + "\n").color(ChatColor.WHITE)
			            					.append("New Siblings List: ");
				            			}
				            			
				            			for(String sibling : uuids) {
				            				Configuration config = getConfig(sibling);
				            				msg.append("\n- " + config.getString("name"));
				            				ArrayList<String> uuidsClone = new ArrayList<String>(uuids);
				            				uuidsClone.remove(sibling);
				            				config.set("siblings", uuidsClone);
				            				if(config.getList("siblings") == null) {
				            					config.set("siblings", null);
				            				}
				            				saveConfig(sibling, config);
				            			}
				            			
				            			msg.append("\n-------------------------").color(ChatColor.DARK_GRAY);
				            			player.sendMessage(msg.create());
				            			
				            			playerConfig.set("siblings", null);
				            			
				            			saveConfig(uuid, playerConfig);
				            			addToLogs(player.getName(), args[2], " removed siblings ");
					            		return;
					            	}
					            	case "get": {
					            		List<String> uuids = new ArrayList<String>();
				            			
				            			ComponentBuilder msg = new ComponentBuilder("------- ").color(ChatColor.DARK_GRAY)
				            					.append("Soot").color(ChatColor.GOLD).bold(true)
				            					.append("Notes").color(ChatColor.RED)
				            					.append(" -------\n").color(ChatColor.DARK_GRAY).bold(false)
				            					.append("Siblings for: " + args[2]).color(ChatColor.WHITE);
				            			for(Object sibling : playerConfig.getList("siblings")) {
				            				uuids.add(sibling.toString());
			            				}
				            			for(String sibling : uuids) {
				            				msg.append("\n- " + getConfig(sibling).getString("name"));
			            				}
				            			
				            			msg.append("\n-------------------------").color(ChatColor.DARK_GRAY);
				            			player.sendMessage(msg.create());
					            		return;
					            	}
				            	}
				            }
			            }
					}
				});
			}
			else {
				sender.sendMessage(new ComponentBuilder("Lacking the permissions to execute this command!").color(ChatColor.RED).create());
			}
		}
		
		@Override
		public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
			if(sender.hasPermission("sootnotes.use")) {
				Set<String> match = new HashSet<String>();
		        if (args.length == 1) {
		        	match.addAll(Arrays.asList("note","xray","siblings"));
		        }
		        else if(args.length == 2) {
		        	if(args[0].equalsIgnoreCase("note") || args[0].equalsIgnoreCase("siblings")) {
		        		match.addAll(Arrays.asList("add","remove","get"));
		        	}
		        	else if(args[0].equalsIgnoreCase("xray")) {
		        		match.addAll(Arrays.asList("add","remove","list","get"));
		        	}
		        }
		        else if(args.length == 3) {
		        	String search = args[2].toLowerCase();
		            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
		                if (player.getName().toLowerCase().startsWith(search)) {
		                    match.add(player.getName());
		                }
		            }
		        }
		        return match;
			}
			return null;
		}
	}
	
	public List<String> getFiles(){
		List<String> files = new ArrayList<String>();
		File folder = new File(getDataFolder().getAbsolutePath() + "/notes");
		if(!folder.exists())	return null;
		for(File file : folder.listFiles()) {
			files.add(file.getName().substring(0,file.getName().length()-4));
		}
		return files;
	}
	
	public void addToLogs(String mod, String player, String action) {
		TimeZone timezone = TimeZone.getTimeZone("America/Chicago");
		Calendar cal = Calendar.getInstance(timezone);
		File file = new File(getDataFolder(), "logs.txt");
		try {
			file.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
			if(player == null) out.write("[" + cal.get(Calendar.YEAR) + "/" + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.DATE) + " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + "] " + mod + action + "\n");
			else	out.write("[" + cal.get(Calendar.YEAR) + "/" + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.DATE) + " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + "] " + mod + action + "for user " + player + "\n");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Configuration getConfig(String name) {
		if (!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}
		File dir = new File(getDataFolder() + "/notes");
		if(!dir.exists()) {
			dir.mkdir();
		}
        File file = new File(getDataFolder() + "/notes/", name + ".yml");

        if (!file.exists()) {
        	try {
				file.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
        }
		
		try {
			return ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder() + "/notes",  name + ".yml"));
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		return null;
	}
	
	public void saveConfig(String name, Configuration config) {
		try {
			ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, new File(getDataFolder() + "/notes", name + ".yml"));
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}
	
	public String getPlayerUUID(ProxiedPlayer player, String name) {
		if(ProxyServer.getInstance().getPlayer(name) != null)	return ProxyServer.getInstance().getPlayer(name).getUniqueId().toString();
		
		for(String uuid : getFiles()) {
			  if(getConfig(uuid).getString("name").equalsIgnoreCase(name)) {
				  return uuid;
			  }
		  }
		
    	String httpResponse = Unirest.get("https://api.mojang.com/users/profiles/minecraft/" + name).asString().getBody();
		
    	if(!httpResponse.equals("")) {
    		String rawUUID = httpResponse.split("\"")[7];
        	return rawUUID.substring(0, 8) + "-" + rawUUID.substring(8, 12) + "-" + rawUUID.substring(12, 16) + "-" + rawUUID.substring(16, 20) + "-" + rawUUID.substring(20);
    	}
    	else {
    		httpResponse = Unirest.get("https://xbl-api.prouser123.me/xuid/" + name.substring(1) + "/raw").asString().getBody();
        	String xuid = null;
        	try{
        		xuid = Long.toHexString(Long.parseLong(httpResponse));
        	}
        	catch(NumberFormatException e){
        		player.sendMessage(new ComponentBuilder("User: " + name + " Not Found!").color(ChatColor.RED).create());
        		return null;
        	}
        	
        	String finaluuid = "";
        	for(int i = 0; i < 32; i++) {
        		if(i == 8 || i == 12 || i == 16 || i == 20 || i == 32) {
        			finaluuid += "-";
        		}
        		if(i < 32 - xuid.length())	finaluuid += 0;
        		else	finaluuid += xuid.charAt(i - 32 + xuid.length());
        	}
        	
        	return finaluuid;
    	}
	}
}
