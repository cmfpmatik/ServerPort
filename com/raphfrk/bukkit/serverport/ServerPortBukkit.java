/*******************************************************************************
 * Copyright (C) 2012 Raphfrk
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.raphfrk.bukkit.serverport;
import java.util.List;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerPortBukkit extends JavaPlugin {
	
	Server server;
	List<World> worlds;
	
	ServerPortListenerCommon serverPortListenerCommon = new ServerPortListenerCommon();
	
	ServerPort serverPort = new ServerPort();
	
	ServerPortCommon serverPortCommon = new ServerPortCommon();
	
	//MyPermissions permissions = new MyPermissions(this);
	
	protected final ServerPortPlayerListener playerListener = new ServerPortPlayerListener(this);
	
	private final ServerPortBlockListener blockListener = new ServerPortBlockListener(this);
	
	private final ServerPortEntityListener entityListener = new ServerPortEntityListener(this);

	private final ServerPortVehicleListener vehicleListener = new ServerPortVehicleListener(this);

	private final ServerPortWorldListener worldListener = new ServerPortWorldListener(this);
	
    public void onEnable() {
    	
    	server = getServer();
    	worlds = server.getWorlds();
    	
    	MyServer.setBukkitServer(getServer());
        MyServer.setJavaPlugin(this);
        
        MyServer.baseFolder = this.getDataFolder();
		
		serverPortCommon.init(serverPortListenerCommon);    
    	
        // TODO: Place any custom enable code here including the registration of any events
    	MyServer.setBukkitServer(server);
        // Register our events
        MyServer.setJavaPlugin(this);
    	registerHooks();
    	
    	//permissions.init();

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
        MiscUtils.safeLogging( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }
    
    public void onDisable() {
    	MyPlayer.hashMaps = null;
		serverPortCommon.disable();
    }
	
	synchronized void registerHooks() {
		
		PluginManager pm = getServer().getPluginManager();
		
		pm.registerEvents(blockListener, this);
		pm.registerEvents(playerListener, this);
		pm.registerEvents(vehicleListener, this);
		pm.registerEvents(entityListener, this);
		pm.registerEvents(worldListener, this);
        	
	}
	

	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String commandLabel, String[] args) {
		
		return serverPortListenerCommon.onCommand(commandSender, commandLabel, args);
		
	}
	
	
}
