package com.aven0x.VeltoPaper.platform;

import com.aven0x.Velto.platform.VeltoScheduler;
import com.aven0x.Velto.platform.VeltoTask;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * {@link VeltoScheduler} backed by Paper 1.21.8's region schedulers.
 *
 * <p>Every method routes to one of the four schedulers exposed on {@code Bukkit}:
 * {@code GlobalRegionScheduler}, {@code RegionScheduler}, an entity's own
 * {@code EntityScheduler}, or {@code AsyncScheduler}. All four exist on ordinary
 * Paper as well as Folia — Paper simply routes them to its single main thread —
 * so this one implementation, and therefore the same {@code Velto-paper} jar,
 * runs unchanged on both. Nothing here is Folia-only or reflection-based.
 */
public final class FoliaScheduler implements VeltoScheduler {

    private final Plugin plugin;

    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    // A ScheduledTask this plugin never received a handle for (e.g. the entity was
    // already gone) is represented by an already-cancelled no-op handle.
    private static final VeltoTask NOOP = new VeltoTask() {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    };

    private static VeltoTask wrap(ScheduledTask task) {
        if (task == null) return NOOP;
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

    @Override
    public void global(Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    @Override
    public VeltoTask globalDelayed(Runnable task, long delayTicks) {
        return wrap(Bukkit.getGlobalRegionScheduler()
                .runDelayed(plugin, t -> task.run(), Math.max(1L, delayTicks)));
    }

    @Override
    public VeltoTask globalTimer(Runnable task, long delayTicks, long periodTicks) {
        return wrap(Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, t -> task.run(), Math.max(1L, delayTicks), Math.max(1L, periodTicks)));
    }

    @Override
    public void entity(Entity entity, Runnable task, Runnable retired) {
        entity.getScheduler().run(plugin, t -> task.run(), retired);
    }

    @Override
    public VeltoTask entityDelayed(Entity entity, Runnable task, Runnable retired, long delayTicks) {
        return wrap(entity.getScheduler()
                .runDelayed(plugin, t -> task.run(), retired, Math.max(1L, delayTicks)));
    }

    @Override
    public VeltoTask entityTimer(Entity entity, Runnable task, Runnable retired, long delayTicks, long periodTicks) {
        return wrap(entity.getScheduler()
                .runAtFixedRate(plugin, t -> task.run(), retired, Math.max(1L, delayTicks), Math.max(1L, periodTicks)));
    }

    @Override
    public void region(Location location, Runnable task) {
        Bukkit.getRegionScheduler().execute(plugin, location, task);
    }

    @Override
    public VeltoTask async(Runnable task) {
        return wrap(Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run()));
    }

    @Override
    public VeltoTask asyncTimer(Runnable task, long delayMillis, long periodMillis) {
        return wrap(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> task.run(),
                Math.max(1L, delayMillis), Math.max(1L, periodMillis), TimeUnit.MILLISECONDS));
    }

    @Override
    public boolean owns(Entity entity) {
        return Bukkit.isOwnedByCurrentRegion(entity);
    }

    @Override
    public boolean owns(Location location) {
        return Bukkit.isOwnedByCurrentRegion(location);
    }

    @Override
    public void cancelAll() {
        // Global and async tasks are cancellable in bulk. Per-entity and per-region
        // tasks have no bulk-cancel API on Folia; their owners (e.g. TeleportManager)
        // cancel them individually through the VeltoTask handles they retain.
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
    }
}
