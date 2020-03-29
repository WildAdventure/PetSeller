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
package com.gmail.filoghost.petseller.file;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import com.dsh105.echopet.compat.api.entity.PetType;
import com.gmail.filoghost.petseller.PetSeller;
import com.gmail.filoghost.petseller.Utils;

import wild.api.config.PluginConfig;

public class Database {

	private static File dbFile;
	private static PluginConfig dbConfig;
	
	public static void init() throws IOException, InvalidConfigurationException {
		dbFile = new File(PetSeller.plugin.getDataFolder(), "database.yml");
		dbConfig = new PluginConfig(PetSeller.plugin, dbFile);
	}
	
	private static boolean hasOwnPermission(Player player, PetType type) {
		return player.hasPermission("petseller.own." + Utils.formatPetType(type));
	}
	
	public static boolean hasUsePermission(Player player, PetType type) {
		return hasOwnPermission(player, type) || player.hasPermission("petseller.pet." + Utils.formatPetType(type));
	}
	
	public static boolean hasPet(Player player, PetType type) {
		return hasOwnPermission(player, type) || dbConfig.getBoolean(player.getUniqueId() + "." + Utils.formatPetType(type));
	}
	
	public static void setHasPet(UUID playerUUID, PetType type) {
		dbConfig.set(playerUUID + "." + Utils.formatPetType(type), true);
	}
	
	public static void setHasPet(Player player, PetType type) {
		setHasPet(player.getUniqueId(), type);
	}
	
	public static void trySaveAsync() {
		Bukkit.getScheduler().runTaskAsynchronously(PetSeller.plugin, () -> {
			try {
				dbConfig.save(dbFile);
			} catch (IOException e) {
				PetSeller.plugin.getLogger().log(Level.SEVERE, "Impossibile salvare il database!", e);
			}
		});
	}
	
	
}
