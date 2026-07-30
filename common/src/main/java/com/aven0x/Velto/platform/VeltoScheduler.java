package com.aven0x.Velto.platform;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.CompletableFuture;

/**
 * Platform-neutral scheduling contract.
 *
 * <p>Defined in {@code common} so shared managers, listeners and commands can
 * schedule work without naming any platform-specific type. Each runtime module
 * supplies its own implementation and installs it in {@code onEnable} through
 * {@link Schedulers#set(VeltoScheduler)}:
 * <ul>
 *   <li>the {@code paper} module maps every method onto the four Folia
 *       schedulers (global / region / entity / async), which are present on
 *       both Folia and ordinary Paper;</li>
 *   <li>the {@code bukkit} module maps them onto the classic, single-threaded
 *       {@code BukkitScheduler}.</li>
 * </ul>
 *
 * <p>The four "lanes" mirror Folia's execution model, and choosing the right
 * one is the whole point of the abstraction:
 * <ul>
 *   <li><b>global</b> — state that belongs to no location: time, weather, game
 *       rules, console commands, server-wide broadcasts;</li>
 *   <li><b>entity</b> — anything touching a specific entity/player; the task
 *       follows that entity even if it changes region;</li>
 *   <li><b>region</b> — block/chunk work anchored at a {@link Location};</li>
 *   <li><b>async</b> — off-tick work that never touches server state (file
 *       I/O), expressed in milliseconds rather than ticks.</li>
 * </ul>
 *
 * <p>Delays and periods are given in ticks (20 ticks = 1 second) except on the
 * async lane, which uses milliseconds. Values below {@code 1} are clamped to
 * {@code 1} on every platform, so the two jars behave identically.
 */
public interface VeltoScheduler {

    /** Runs work that belongs to no location: time, weather, gamerules, console commands, broadcasts. */
    void global(Runnable task);

    /** As {@link #global(Runnable)} but after {@code delayTicks}. */
    VeltoTask globalDelayed(Runnable task, long delayTicks);

    /** Repeating global task, first run after {@code delayTicks}, then every {@code periodTicks}. */
    VeltoTask globalTimer(Runnable task, long delayTicks, long periodTicks);

    /**
     * Runs work touching a specific entity, on the region that owns it.
     *
     * @param retired invoked instead of {@code task} if the entity is gone before it could run;
     *                may be {@code null}. Keep it cheap — it runs inside critical server code, so
     *                it must not load chunks, remove entities or perform I/O.
     */
    void entity(Entity entity, Runnable task, Runnable retired);

    /** As {@link #entity(Entity, Runnable, Runnable)} but after {@code delayTicks}. */
    VeltoTask entityDelayed(Entity entity, Runnable task, Runnable retired, long delayTicks);

    /** Repeating entity task, first run after {@code delayTicks}, then every {@code periodTicks}. */
    VeltoTask entityTimer(Entity entity, Runnable task, Runnable retired, long delayTicks, long periodTicks);

    /** Runs work touching blocks/chunks at {@code location}, on the region that owns it. */
    void region(Location location, Runnable task);

    /** Off-tick work: file I/O only, never server state. */
    VeltoTask async(Runnable task);

    /** Repeating off-tick work, expressed in milliseconds. */
    VeltoTask asyncTimer(Runnable task, long delayMillis, long periodMillis);

    /**
     * Teleports an entity on the region that owns it, completing when the teleport resolves.
     * On Folia this is the only supported teleport path (a synchronous teleport throws); on
     * Spigot it is the classic chunk-load-then-teleport. The returned future may complete on
     * any thread, so re-check ownership before touching the entity in a continuation.
     */
    CompletableFuture<Boolean> teleport(Entity entity, Location location);

    /** True when the caller may safely touch this entity right now (its region is the current thread). */
    boolean owns(Entity entity);

    /** True when the caller may safely touch this location right now. */
    boolean owns(Location location);

    /** Cancels every task this plugin scheduled through this scheduler. Call from {@code onDisable}. */
    void cancelAll();
}
