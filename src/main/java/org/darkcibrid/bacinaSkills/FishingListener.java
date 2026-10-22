package org.darkcibrid.bacinaSkills;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTables;
import org.bukkit.util.Vector;
import org.darkcibrid.bacinaSkills.PlayerData.SkillData;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class FishingListener implements Listener {

    private static final Set<Material> FISH = EnumSet.of(
            Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH);

    private static final Set<Material> TREASURE = EnumSet.of(
            Material.ENCHANTED_BOOK, Material.NAME_TAG, Material.SADDLE, Material.BOW,
            Material.FISHING_ROD, Material.NAUTILUS_SHELL);

    private final BacinaSkills plugin;

    public FishingListener(BacinaSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        Player p = e.getPlayer();
        SkillData sd = plugin.data().get(p.getUniqueId()).skill(Skill.FISHING);
        switch (e.getState()) {
            case FISHING -> speedUp(e.getHook(), sd);
            case CAUGHT_FISH -> caught(e, p, sd);
            default -> { }
        }
    }

    // =====================================================================
    // Faster fishing: shorten the hook's wait window. Lure is applied by the game on top of this,
    // so the two stack.
    // =====================================================================
    private void speedUp(FishHook hook, SkillData sd) {
        double s = Perks.fishSpeed(sd);
        if (s <= 0) return;
        int min = hook.getMinWaitTime();
        int max = hook.getMaxWaitTime();
        int newMin = Math.max(1, (int) Math.round(min * (1.0 - s)));
        int newMax = Math.max(newMin, (int) Math.round(max * (1.0 - s)));
        hook.setMinWaitTime(newMin);
        hook.setMaxWaitTime(newMax);
    }

    // =====================================================================
    // Catch: XP, bonus Luck of the Sea, extra loot rolls
    // =====================================================================
    private void caught(PlayerFishEvent e, Player p, SkillData sd) {
        Entity ent = e.getCaught();
        if (!(ent instanceof Item item)) return;
        FishHook hook = e.getHook();
        ThreadLocalRandom r = ThreadLocalRandom.current();

        int bonusLuck = Perks.fishLuck(sd);
        float luck = (float) (rodLuck(p, e) + bonusLuck + playerLuck(p));

        // triple check first, then double, else single
        int rolls = 1;
        if (r.nextDouble() < Perks.fishTriple(sd)) rolls = 3;
        else if (r.nextDouble() < Perks.fishDouble(sd)) rolls = 2;

        // On a triple, one random pull of the three gets triple treasure chance.
        int boosted = rolls == 3 ? r.nextInt(3) : -1;

        // Pull 0 is the normal catch. The game already rolled it with the rod's own Luck of the Sea,
        // so we only re-roll it when the skill gives bonus luck, or when it is the boosted pull
        // (and the vanilla roll wasn't already treasure).
        boolean vanillaIsTreasure = TREASURE.contains(item.getItemStack().getType());
        boolean rerollBase = bonusLuck > 0 || (boosted == 0 && !vanillaIsTreasure);
        if (rerollBase) {
            List<ItemStack> rolled = boosted == 0 ? rollBoosted(hook, p, luck) : roll(hook, p, luck);
            if (!rolled.isEmpty()) {
                item.setItemStack(rolled.get(0));
                for (int i = 1; i < rolled.size(); i++) giveExtra(p, hook, rolled.get(i));
            }
        }

        // XP for the (final) base catch
        Material t = item.getItemStack().getType();
        double xp;
        if (FISH.contains(t)) xp = plugin.getConfig().getDouble("xp.fishing.fish", 10);
        else if (TREASURE.contains(t)) xp = plugin.getConfig().getDouble("xp.fishing.treasure", 40);
        else xp = plugin.getConfig().getDouble("xp.fishing.junk", 5);
        plugin.xp().addXp(p, Skill.FISHING, xp);

        // extra pulls
        for (int i = 1; i < rolls; i++) {
            List<ItemStack> extras = (i == boosted) ? rollBoosted(hook, p, luck) : roll(hook, p, luck);
            for (ItemStack extra : extras) giveExtra(p, hook, extra);
        }
    }

    /**
     * Roll with roughly triple treasure chance: up to 3 independent rolls, the first one that
     * contains treasure wins (chance = 1-(1-q)^3, about 3q for small q), otherwise the first roll is kept.
     */
    private List<ItemStack> rollBoosted(FishHook hook, Player p, float luck) {
        List<ItemStack> first = roll(hook, p, luck);
        if (containsTreasure(first)) return first;
        for (int i = 0; i < 2; i++) {
            List<ItemStack> again = roll(hook, p, luck);
            if (containsTreasure(again)) return again;
        }
        return first;
    }

    private static boolean containsTreasure(List<ItemStack> items) {
        for (ItemStack s : items) if (TREASURE.contains(s.getType())) return true;
        return false;
    }

    private static double rodLuck(Player p, PlayerFishEvent e) {
        EquipmentSlot hand = e.getHand() != null ? e.getHand() : EquipmentSlot.HAND;
        ItemStack rod = p.getInventory().getItem(hand);
        if (rod == null) return 0;
        return rod.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA);
    }

    private static double playerLuck(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.LUCK);
        return a == null ? 0 : a.getValue();
    }

    /** Rolls the vanilla fishing loot table once with the given luck. */
    private List<ItemStack> roll(FishHook hook, Player p, float luck) {
        try {
            LootContext ctx = new LootContext.Builder(hook.getLocation())
                    .luck(luck)
                    .lootedEntity(hook)
                    .killer(p)
                    .build();
            return new ArrayList<>(LootTables.FISHING.getLootTable().populateLoot(ThreadLocalRandom.current(), ctx));
        } catch (Exception ex) {
            plugin.getLogger().warning("Fishing loot roll failed: " + ex.getMessage());
            return List.of();
        }
    }

    /** Spawns the item at the hook and flings it toward the player like a normal catch. */
    private void giveExtra(Player p, FishHook hook, ItemStack stack) {
        Location from = hook.getLocation();
        if (from.getWorld() == null) return;
        Item drop = from.getWorld().dropItem(from, stack);
        double dx = p.getX() - from.getX();
        double dy = p.getY() - from.getY();
        double dz = p.getZ() - from.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        drop.setVelocity(new Vector(dx * 0.1, dy * 0.1 + Math.sqrt(dist) * 0.08, dz * 0.1));
    }
}