package com.onetoys.salt;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

import com.onetoys.Onetoys;

@EventBusSubscriber(modid = Onetoys.MODID)
public class VillagerEvents {
    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (!com.onetoys.module.content.SaltModule.isSaltEnabled())
            return;
        if (SaltConfiguration.isLoaded() && !SaltConfiguration.BUTCHER_SALT_TRADES_ENABLED.get())
            return;

        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
        VillagerProfession profession = event.getType();
        if (profession == VillagerProfession.BUTCHER) {
            trades.get(1).add(emeraldForItemsTrade(SaltItems.RAW_ROCK_SALT.get(), 14, 16, 2));
            trades.get(1).add(emeraldForItemsTrade(SaltItems.SALT.get(), 8, 16, 2));
            trades.get(3).add(itemForEmeraldTrade(SaltItems.SALT.get(), 14, 5, 10, 6));
            trades.get(3).add(itemForEmeraldTrade(SaltItems.SALT.get(), 8, 3, 12, 6));
        }
    }

    private static BasicItemListing emeraldForItemsTrade(ItemLike item, int count, int maxTrades, int xp) {
        return new BasicItemListing(new ItemStack(item, count), new ItemStack(Items.EMERALD), maxTrades, xp, 0.05F);
    }

    private static BasicItemListing itemForEmeraldTrade(ItemLike item, int itemsCount, int emeralds, int maxTrades, int xp) {
        return new BasicItemListing(emeralds, new ItemStack(item, itemsCount), maxTrades, xp, 0.05F);
    }
}

