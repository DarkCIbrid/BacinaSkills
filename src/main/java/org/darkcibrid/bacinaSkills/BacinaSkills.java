package org.darkcibrid.bacinaSkills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class BacinaSkills extends JavaPlugin implements Listener {

    /** Crops that have an age and must be fully grown to give XP. */
    public static final Set<Material> AGE_CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.COCOA, Material.SWEET_BERRY_BUSH);

    /** Blocks that count as "crops" for double/triple drops. */
    public static final Set<Material> CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.COCOA, Material.SWEET_BERRY_BUSH,
            Material.MELON, Material.PUMPKIN, Material.SUGAR_CANE, Material.CACTUS, Material.BAMBOO);

    private static final Set<Material> STONE_LIKE = EnumSet.of(
            Material.END_STONE, Material.CALCITE, Material.DRIPSTONE_BLOCK, Material.SMOOTH_BASALT);

    private DataManager data;
    private XpManager xp;
    private PlacedBlockTracker tracker;
    private MiningListener mining;

    // "what kind of block did this player just break this tick" - used for durability perks
    private final Map<UUID, Skill> ctxSkill = new HashMap<>();
    private final Map<UUID, Integer> ctxTick = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        data = new DataManager(this);
        xp = new XpManager(this);
        tracker = new PlacedBlockTracker(this);
        mining = new MiningListener(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(this, this);          // join / quit handlers below
        pm.registerEvents(tracker, this);
        pm.registerEvents(mining, this);
        pm.registerEvents(new WoodcuttingListener(this), this);
        pm.registerEvents(new HerbalismListener(this), this);
        pm.registerEvents(new HerdmasterListener(this), this);
        pm.registerEvents(new FishingListener(this), this);

        SkillsCommand cmd = new SkillsCommand(this);
        if (getCommand("bskills") != null) {
            getCommand("bskills").setExecutor(cmd);
            getCommand("bskills").setTabCompleter(cmd);
        }

        mining.startSpeedTask();
        long period = Math.max(1, getConfig().getInt("settings.autosave-minutes", 5)) * 60L * 20L;
        getServer().getScheduler().runTaskTimer(this, () -> data.saveAll(), period, period);
    }

    @Override
    public void onDisable() {
        if (mining != null) mining.clearAll();
        if (data != null) data.saveAll();
    }

    // ---------------- accessors ----------------
    public DataManager data() { return data; }
    public XpManager xp() { return xp; }
    public PlacedBlockTracker tracker() { return tracker; }

    // ---------------- join / quit ----------------
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        data.get(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        mining.clear(p);
        data.saveAndUnload(p.getUniqueId());
        ctxSkill.remove(p.getUniqueId());
        ctxTick.remove(p.getUniqueId());
    }

    // ---------------- break context ----------------
    public void markBreak(Player p, Skill s) {
        ctxSkill.put(p.getUniqueId(), s);
        ctxTick.put(p.getUniqueId(), Bukkit.getCurrentTick());
    }

    /** The skill whose block this player broke during the current tick, or null. */
    public Skill breakContext(Player p) {
        Integer t = ctxTick.get(p.getUniqueId());
        if (t == null || t != Bukkit.getCurrentTick()) return null;
        return ctxSkill.get(p.getUniqueId());
    }

    // ---------------- block helpers ----------------
    public static boolean isOre(Material m) {
        return m.name().endsWith("_ORE") || m == Material.ANCIENT_DEBRIS;
    }

    public double miningXp(Material m) {
        String n = m.name();
        String key = (n.startsWith("DEEPSLATE_") && n.endsWith("_ORE")) ? n.substring("DEEPSLATE_".length()) : n;
        double v = getConfig().getDouble("xp.mining.blocks." + key, -1);
        if (v >= 0) return v;
        if (isOre(m)) return getConfig().getDouble("xp.mining.default-ore", 8);
        if (Tag.BASE_STONE_OVERWORLD.isTagged(m) || Tag.BASE_STONE_NETHER.isTagged(m) || STONE_LIKE.contains(m)) {
            return getConfig().getDouble("xp.mining.default-stone", 1);
        }
        return 0;
    }

    public double herbBaseXp(Material m) {
        double xp = getConfig().getDouble("xp.herbalism.blocks." + m.name(), 0);
        if (xp <= 0 && Tag.FLOWERS.isTagged(m)) xp = getConfig().getDouble("xp.herbalism.default-flower", 2);
        return xp;
    }

    /** Herbalism XP for breaking this block (0 if it is an unripe crop or not an herbalism block). */
    public double herbXp(BlockData d) {
        Material m = d.getMaterial();
        double xp = herbBaseXp(m);
        if (xp <= 0) return 0;
        if (AGE_CROPS.contains(m) && d instanceof Ageable a && a.getAge() < a.getMaximumAge()) return 0;
        return xp;
    }

    /** Blocks we remember when a player places them, so place/break farming gives nothing. */
    public boolean isTracked(Material m) {
        if (Tag.LOGS.isTagged(m)) return true;
        if (miningXp(m) > 0) return true;
        return herbBaseXp(m) > 0 && !AGE_CROPS.contains(m);
    }

    public static void dropStacks(Location loc, ItemStack template, int amount) {
        if (amount <= 0 || loc.getWorld() == null) return;
        int max = Math.max(1, template.getMaxStackSize());
        while (amount > 0) {
            int n = Math.min(max, amount);
            ItemStack s = template.clone();
            s.setAmount(n);
            loc.getWorld().dropItemNaturally(loc, s);
            amount -= n;
        }
    }
}
