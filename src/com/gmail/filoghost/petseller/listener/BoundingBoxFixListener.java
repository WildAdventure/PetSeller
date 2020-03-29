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
package com.gmail.filoghost.petseller.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.spigotmc.event.entity.EntityMountEvent;

import com.dsh105.echopet.api.EchoPetAPI;
import com.dsh105.echopet.compat.api.entity.EntitySize;
import com.dsh105.echopet.compat.api.entity.IAgeablePet;
import com.dsh105.echopet.compat.api.entity.IEntityPet;
import com.dsh105.echopet.compat.api.entity.IPet;
import com.dsh105.echopet.compat.api.entity.type.pet.IGuardianPet;
import com.dsh105.echopet.compat.api.entity.type.pet.ISlimePet;

public class BoundingBoxFixListener implements Listener {
	
	private static final float
		MIN_PET_HEIGHT = 1.8f, // Same as player
		MAX_PET_HEIGHT = 4.0f, // Wither
		GLOBAL_PET_WIDTH = 0.8f,
		DEFAULT_ADDITIONAL_HEIGHT = 1.1f;
	
	public static void fixBoundingBox(IPet pet) {
		IEntityPet entityPet = pet.getEntityPet();
		EntitySize defaultSize = entityPet.getClass().getAnnotation(EntitySize.class);

		float petHeight = calculatePetHeight(pet, defaultSize.height());
		if (petHeight < MIN_PET_HEIGHT) {
			petHeight = MIN_PET_HEIGHT;
		} else if (petHeight > MAX_PET_HEIGHT) {
			petHeight = MAX_PET_HEIGHT;
		}
		
		entityPet.setEntitySize(GLOBAL_PET_WIDTH, petHeight);
	}
	
	@EventHandler
	public void vehicleEnterEvent(EntityMountEvent event) {
		if (event.getEntity() instanceof Player) {
			Player mounter = (Player) event.getEntity();
			IPet pet = EchoPetAPI.getAPI().getPet(mounter);
			
			if (pet != null && pet.getCraftPet() == event.getMount()) {
				IEntityPet entityPet = pet.getEntityPet();
				EntitySize defaultSize = entityPet.getClass().getAnnotation(EntitySize.class);
				
				float petHeight = calculatePetHeight(pet, defaultSize.height());
				if (petHeight < MIN_PET_HEIGHT) {
					petHeight = MIN_PET_HEIGHT;
				} else if (petHeight > MAX_PET_HEIGHT) {
					petHeight = MAX_PET_HEIGHT;
				}
				
				entityPet.setEntitySize(GLOBAL_PET_WIDTH, petHeight);
			}
		}
	}
	
	private static float calculatePetHeight(IPet pet, float defaultHeight) {
		switch (pet.getPetType()) {
			case WITHER:
				return defaultHeight;
			case GUARDIAN:
				if (pet instanceof IGuardianPet && ((IGuardianPet) pet).isElder()) {
					return defaultHeight + 2.1f;
				} else {
					return defaultHeight + 1.2f;
				}
			case IRONGOLEM:
				return defaultHeight + 0.5f;
			case CREEPER:
				return defaultHeight + 0.8f;
			case ENDERMAN:
				return defaultHeight + 0.7f;
			case PIG:
			case SHEEP:
			case COW:
			case MUSHROOMCOW:
			case WOLF:
			case OCELOT:
			case HORSE:
				if (pet instanceof IAgeablePet && ((IAgeablePet) pet).isBaby()) {
					return defaultHeight + 0.7f;
				}
				break;
			case ZOMBIE:
				if (pet instanceof IAgeablePet && ((IAgeablePet) pet).isBaby()) {
					return defaultHeight + 0.4f;
				}
			case MAGMACUBE:
			case SLIME:
				if (pet instanceof ISlimePet) {
					int size = ((ISlimePet) pet).getSize();
					if (size == 1) {
						return defaultHeight + DEFAULT_ADDITIONAL_HEIGHT;
					} else if (size == 2) {
						return defaultHeight + DEFAULT_ADDITIONAL_HEIGHT + 0.5f;
					} else if (size == 4) {
						return defaultHeight + DEFAULT_ADDITIONAL_HEIGHT + 1.2f;
					}
				}
				break;
			default:
				break;
		}
		
		return defaultHeight + DEFAULT_ADDITIONAL_HEIGHT;
	}

}
