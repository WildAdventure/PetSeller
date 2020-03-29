/*
 * Copyright (c) 2020, Wild Adventure
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 4. Redistribution of this software in source or binary forms shall be free
 *    of all charges or fees to the recipient of this software.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gmail.filoghost.petseller;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.dsh105.echopet.compat.api.entity.PetType;
import com.gmail.filoghost.petseller.file.Database;
import com.gmail.filoghost.petseller.menu.PetsMenu;

import net.cubespace.yamler.YamlerConfigurationException;
import wild.api.command.SubCommandFramework;
import wild.api.uuid.UUIDRegistry;

public class PetCommand extends SubCommandFramework {
	
	public PetCommand(JavaPlugin plugin, String label, String... aliases) {
		super(plugin, label, aliases);
	}

	@Override
	public void noArgs(CommandSender sender) {
		Player player = CommandValidate.getPlayerSender(sender);
		new PetsMenu(player).open(player);
	}
	
	@SubCommand("help")
	public void help(CommandSender sender, String label, String[] args) {
		sender.sendMessage(ChatColor.DARK_GREEN + "Comandi disponibili:");
		sender.sendMessage(ChatColor.GREEN + "/" + super.label);
		for (SubCommandDetails subCommand : super.getAccessibleSubCommands(sender)) {
			sender.sendMessage(ChatColor.GREEN + "/" + super.label + " " + subCommand.getName() + (subCommand.getUsage() != null ? " " + subCommand.getUsage() : ""));
		}
	}
	
	@SubCommand("reload")
	@SubCommandPermission("petseller.reload")
	public void reload(CommandSender sender, String label, String[] args) {
		try {
			PetSeller.plugin.load();
			sender.sendMessage(ChatColor.GRAY + "Database ricaricato.");
		} catch (YamlerConfigurationException e) {
			e.printStackTrace();
			sender.sendMessage(ChatColor.RED + "Errore di configurazione, guarda la console.");
		}
	}
	
	@SubCommand("give")
	@SubCommandPermission("petseller.give")
	@SubCommandMinArgs(2)
	@SubCommandUsage("<giocatore> <tipo>")
	public void give(CommandSender sender, String label, String[] args) {
		PetType type = Utils.matchPetType(args[1]);
		CommandValidate.notNull(type, "Tipo di pet non riconosciuto.");
		
		String playerName = args[0];
		UUID uuid = UUIDRegistry.getUUID(playerName);
		CommandValidate.notNull(uuid, "UUID del giocatore " + playerName + " non trovato.");

		Database.setHasPet(uuid, type);
		Database.trySaveAsync();
		sender.sendMessage(ChatColor.GREEN + "Hai dato a " + uuid + "/" + playerName + " il pet " + Utils.formatPetType(type) + "!");
	}

}
