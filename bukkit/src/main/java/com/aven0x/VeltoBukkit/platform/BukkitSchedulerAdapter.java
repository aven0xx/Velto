package com.aven0x.VeltoBukkit.platform;

import com.aven0x.Velto.platform.VeltoScheduler;
import com.aven0x.Velto.platform.VeltoTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;

/**
 * {@link VeltoScheduler} backed by the classic single-threaded
 * {@code BukkitScheduler}, for the Spigot/Bukkit module.
 *
 * <p>Spigot has no regions and one main thread, so the region distinctions
 * collapse: {@code global}, {@code entity} and {@code region} all become plain
 * main-thread tasks, and the {@code retired} callback (a Folia concept — "the
 * entity vanished before the task ran") has no analogue and is dropped. Ticks
 * are the native unit here; the async lane converts its milliseconds back to
 * ticks. Delays are clamped to {@code >= 1} to match the {@link VeltoScheduler}
 * contract so the two jars behave identically.
 */
public final class BukkitSchedulerAdapter implements VeltoScheduler {

    private final Plugin plugin;

    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    private static VeltoTask wrap(BukkitTask task) {
        return new VeltoTask() {
            @Override
            public void cancel() {
                task.cancel();
            }

            @Override
            public boolean isCancelled() {
                return task.isCancelled();
            }
        };
    }

    // Convert a millisecond duration to whole ticks (20 ticks = 1 second), never below 1.
    private static long toTicks(long millis) {
        return Math.max(1L, millis / 50L);
    }

    @Override
    public void global(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public VeltoTask globalDelayed(Runnable task, long delayTicks) {
        return wrap(Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(1L, delayTicks)));
    }

    @Override
    public VeltoTask globalTimer(Runnable task, long delayTicks, long periodTicks) {
        return wrap(Bukkit.getScheduler().runTaskTimer(plugin, task, Math.max(1L, delayTicks), Math.max(1L, periodTicks)));
    }

    @Override
    public void entity(Entity entity, Runnable task, Runnable retired) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public VeltoTask entityDelayed(Entity entity, Runnable task, Runnable retired, long delayTicks) {
        return wrap(Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(1L, delayTicks)));
    }

    @Override
    public VeltoTask entityTimer(Entity entity, Runnable task, Runnable retired, long delayTicks, long periodTicks) {
        return wrap(Bukkit.getScheduler().runTaskTimer(plugin, task, Math.max(1L, delayTicks), Math.max(1L, periodTicks)));
    }

    @Override
    public void region(Location location, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public VeltoTask async(Runnable task) {
        return wrap(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
    }

    @Override
    public VeltoTask asyncTimer(Runnable task, long delayMillis, long periodMillis) {
        return wrap(Bukkit.getScheduler()
                .runTaskTimerAsynchronously(plugin, task, toTicks(delayMillis), toTicks(periodMillis)));
    }

    @Override
    public CompletableFuture<Boolean> teleport(Entity entity, Location location) {
        // Spigot has no async teleport; load the destination chunk, then teleport synchronously.
        // This is the classic path Velto used before the SPI existed.
        if (location.getWorld() == null) {
            return CompletableFuture.completedFuture(false);
        }
        location.getWorld().getChunkAt(location);
        return CompletableFuture.completedFuture(entity.teleport(location));
    }

    @Override
    public boolean owns(Entity entity) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean owns(Location location) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public void cancelAll() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
