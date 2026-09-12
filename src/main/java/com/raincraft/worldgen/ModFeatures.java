package com.raincraft.worldgen;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

public class ModFeatures {

    public static final Feature<DefaultFeatureConfig> SHELTER_ROOM =
            new ShelterRoomFeature(DefaultFeatureConfig.CODEC);

    public static void register() {
        Registry.register(Registries.FEATURE, new Identifier("raincraft", "shelter_room"), SHELTER_ROOM);
    }
}
