package com.aven0x.Velto.platform;

/**
 * A cancellable handle to something scheduled through {@link VeltoScheduler}.
 *
 * <p>Platform-neutral stand-in for the underlying task type — {@code BukkitTask}
 * on Spigot, {@code io.papermc.paper.threadedregions.scheduler.ScheduledTask} on
 * Paper/Folia — so that {@code common} never has to name either.
 */
public interface VeltoTask {

    /** Cancels the task if it has not already run or been cancelled. Safe to call more than once. */
    void cancel();

    /** @return {@code true} once the task has been cancelled (or was a no-op handle). */
    boolean isCancelled();
}
