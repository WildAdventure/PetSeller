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
package com.gmail.filoghost.petseller.menu;

import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;

import com.dsh105.echopet.compat.api.entity.PetType;
import com.gmail.filoghost.petseller.EconomyBridge;
import com.gmail.filoghost.petseller.PetSeller;
import com.gmail.filoghost.petseller.file.Database;
import com.gmail.filoghost.petseller.file.PetConfig;

import wild.api.WildCommons;
import wild.api.item.ItemBuilder;
import wild.api.menu.IconBuilder;
import wild.api.menu.IconMenu;
import wild.api.menu.StaticIcon;
import wild.core.WildCommonsPlugin;

public class PetsMenu extends IconMenu {

	public PetsMenu(Player relativePlayer) {
		super("Pets", (int) Math.ceil(PetSeller.petConfigs.size() / 9.0) + 1);
		
		int index = 0;
		for (Entry<PetType, PetConfig> entry : PetSeller.petConfigs.entrySet()) {
			PetType type = entry.getKey();
			PetConfig petConfig = entry.getValue();
			
			if (petConfig.getPrice() == null) {
				continue; // Nessun prezzo = disabilitato per sicurezza
			}
			
			boolean useDefaultIcon = petConfig.getMenuIcon() == null || petConfig.getMenuIcon() == Material.AIR;
			
			ItemStack item;
			
			if (Database.hasUsePermission(relativePlayer, type)) {
				if (Database.hasPet(relativePlayer, type)) {
					item = ItemBuilder
								.of(useDefaultIcon ? Material.MONSTER_EGG : petConfig.getMenuIcon())
								.durability(useDefaultIcon ? 0 : (petConfig.getMenuIconData() != null ? petConfig.getMenuIconData() : 0))
								.name(ChatColor.GREEN + WildCommons.color(petConfig.getFriendlyName()))
								.lore(ChatColor.GRAY + "Clicca per evocare questo pet")
								.build();
				} else {
					item = ItemBuilder
								.of(Material.STAINED_GLASS_PANE)
								.durability(4)
								.name(ChatColor.YELLOW + WildCommons.color(petConfig.getFriendlyName()))
								.lore(ChatColor.GRAY + "Costo: " + ChatColor.YELLOW + EconomyBridge.formatMoney(petConfig.getPrice()))
								.build();
				}
			} else {
				item = ItemBuilder
					.of(Material.STAINED_GLASS_PANE)
					.durability(14)
					.name(ChatColor.RED + WildCommons.color(petConfig.getFriendlyName()))
					.lore(ChatColor.GRAY + (petConfig.getCantUseString() != null ?  WildCommons.color(petConfig.getCantUseString()) : "Non hai il permesso di utilizzare questo pet"))
					.build();
			}

			
			if (useDefaultIcon && item.getType() == Material.MONSTER_EGG) {
				SpawnEggMeta meta = (SpawnEggMeta) item.getItemMeta();
				meta.setSpawnedType(EntityType.fromName(type.getMinecraftName()));
				item.setItemMeta(meta);
			}
			
			StaticIcon icon = new StaticIcon(item, true);
			icon.setClickHandler(clicker -> {
				if (Database.hasUsePermission(clicker, type)) {
					if (Database.hasPet(clicker, type)) {
						PetSeller.plugin.trySummon(type, clicker);
						Bukkit.getScheduler().runTaskLater(WildCommonsPlugin.instance, () -> clicker.closeInventory(), 1);
					} else {
						new ConfirmDialogMenu(
							"Sei sicuro?",
							ChatColor.GRAY + "Compra " + petConfig.getFriendlyName() + " per " + ChatColor.YELLOW + EconomyBridge.formatMoney(petConfig.getPrice()),
							confirmer -> {
								PetSeller.plugin.tryBuy(type, confirmer);
							}
						).open(clicker);
					}
				} else {
					PetSeller.sendChatError(clicker, "Non hai il permesso per utilizzarlo.");
				}
			});
			
			setIconRaw(index++, icon);
		}
		
		setIcon(9, getRows(), new IconBuilder(Material.BARRIER).name(ChatColor.GRAY + "Manda via il pet attuale").clickHandler(clicker -> {
			PetSeller.plugin.tryGoAway(clicker);
		}).closeOnClick(true).build());
		
		refresh();
	}

}
