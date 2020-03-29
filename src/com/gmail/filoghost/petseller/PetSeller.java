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

import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.dsh105.echopet.api.EchoPetAPI;
import com.dsh105.echopet.compat.api.entity.IPet;
import com.dsh105.echopet.compat.api.entity.PetType;
import com.dsh105.echopet.compat.api.entity.type.pet.ISlimePet;
import com.dsh105.echopet.compat.api.plugin.EchoPet;
import com.gmail.filoghost.petseller.file.Database;
import com.gmail.filoghost.petseller.file.PetConfig;
import com.gmail.filoghost.petseller.file.Settings;
import com.gmail.filoghost.petseller.listener.BoundingBoxFixListener;
import com.gmail.filoghost.petseller.listener.DismountFixListener;
import com.google.common.collect.Maps;

import net.cubespace.yamler.YamlerConfigurationException;
import wild.api.command.CommandFramework.CommandValidate;
import wild.api.command.CommandFramework.ExecuteException;
import wild.api.sound.EasySound;
import wild.core.utils.ReflectionUtils;

public class PetSeller extends JavaPlugin {
	
	public static PetSeller plugin;
	public static Map<PetType, PetConfig> petConfigs;
	
	@Override
	public void onEnable() {
		if (!Bukkit.getPluginManager().isPluginEnabled("SonarPet") && !Bukkit.getPluginManager().isPluginEnabled("EchoPet")) {
			fatalException("Richiesto EchoPet o SonarPet!");
			return;
		}
		
		plugin = this;
		
		if (!EconomyBridge.setupEconomy()) {
			fatalException("Questo plugin necessità di un'economia valida");
			return;
		}
		
		try {
			Database.init();
		} catch (Exception e) {
			fatalException("Impossibile caricare database.yml");
			return;
		}
		
		new PetCommand(this, "pet", "pets", "petseller");
		
		Bukkit.getPluginManager().registerEvents(new BoundingBoxFixListener(), this);
		Bukkit.getPluginManager().registerEvents(new DismountFixListener(), this);

		try {
			load();
		} catch (YamlerConfigurationException e) {
			getLogger().log(Level.SEVERE, "Impossibile caricare config.yml", e);
		}
		
		try {
			ReflectionUtils.setPrivateField(EchoPet.getPlugin(), "MANAGER", new PetManagerPatched());
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Impossibile eseguire la patch di PetManager", e);
		}
	}
	
	public void load() throws YamlerConfigurationException {
		petConfigs = Maps.newLinkedHashMap();
		Settings settings = new Settings(this, "config.yml");
		settings.init();
		
		for (PetConfig petConfig : settings.petConfigs) {
			PetType type = Utils.matchPetType(petConfig.getType());
			if (type != null) {
				petConfigs.put(type, petConfig);
			} else {
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Tipo di pet non valido: " + petConfig.getType());
			}
		}
		
		getLogger().info("Pet abilitati: " + petConfigs.keySet());
	}

	public void tryBuy(PetType type, Player player) {
		try {
			PetConfig petConfig = petConfigs.get(type);
			
			CommandValidate.notNull(petConfig, "Tipo di pet non disponibile.");
			CommandValidate.isTrue(Database.hasUsePermission(player, type), "Non hai il permesso per utilizzare questo tipo di pet.");
			CommandValidate.isTrue(!Database.hasPet(player, type), "Possiedi già questo tipo di pet.");
			
			Integer price = petConfig.getPrice();
			
			CommandValidate.notNull(price, "Il prezzo non è ancora stato impostato.");
			CommandValidate.isTrue(EconomyBridge.getMoney(player) >= price, "Non hai abbastanza soldi, servono " + EconomyBridge.formatMoney(price) + ".");
			CommandValidate.isTrue(EconomyBridge.takeMoney(player, price), "Errore durante l'acquisto, contatta lo staff.");
			
			Database.setHasPet(player, type);
			Database.trySaveAsync();
			player.sendMessage(ChatColor.GREEN + "Hai comprato " + petConfig.getFriendlyName() + " per " + EconomyBridge.formatMoney(price));
			EasySound.quickPlay(player, Sound.BLOCK_NOTE_PLING);
			
		} catch (ExecuteException e) {
			sendChatError(player, e.getMessage());
		}
	}

	public void trySummon(PetType type, Player player) throws ExecuteException {
		try {
			PetConfig petConfig = petConfigs.get(type);
			
			CommandValidate.notNull(petConfig, "Tipo di pet non disponibile.");
			CommandValidate.isTrue(Database.hasUsePermission(player, type), "Non hai il permesso per utilizzare questo tipo di pet.");
			CommandValidate.isTrue(Database.hasPet(player, type), "Non possiedi questo tipo di pet.");
			
			IPet oldPet = EchoPetAPI.getAPI().getPet(player);
			if (oldPet != null) {
				oldPet.setAsHat(false);
			}
			
			IPet pet = EchoPetAPI.getAPI().givePet(player, type, true);
			pet.setHidden(false);
			pet.spawnPet(player, true);
			
			if (pet != null) {
				pet.setPetName("Pet di " + player.getName());
			}
			if (pet instanceof ISlimePet) {
				ISlimePet slimePet = (ISlimePet) pet;
				if (slimePet.getSize() == 0) {
					slimePet.setSize(2);
				}
			}

			// TODO collisions handling
			
		} catch (ExecuteException e) {
			sendChatError(player, e.getMessage());
		}
	}
	
	public void tryGoAway(Player player) throws ExecuteException {
		try {
			IPet pet = EchoPetAPI.getAPI().getPet(player);
			CommandValidate.notNull(pet, "Non hai pet attivi.");
			pet.setAsHat(false);
			EchoPetAPI.getAPI().removePet(player, false, false);
			player.sendMessage(ChatColor.GREEN + "Hai mandato via il tuo pet.");
		} catch (ExecuteException e) {
			sendChatError(player, e.getMessage());
		}
	}
	
	public static void sendChatError(Player player, String message) {
		player.sendMessage(ChatColor.RED + message);
		EasySound.quickPlay(player, Sound.BLOCK_NOTE_BASS);
	}
	
	private void fatalException(String message) {
		Bukkit.getConsoleSender().sendMessage(ChatColor.RED + message);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) { }
		setEnabled(false);
	}
	
}
