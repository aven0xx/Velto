package com.aven0x.Velto.platform;

/**
 * Process-wide access point for the active {@link VeltoScheduler}.
 *
 * <p>Each runtime module installs its implementation once, first thing in
 * {@code onEnable}, before any manager is constructed:
 * <pre>{@code
 * Schedulers.set(new FoliaScheduler(this));        // paper module
 * Schedulers.set(new BukkitSchedulerAdapter(this)); // bukkit module
 * }</pre>
 *
 * <p>A static holder — rather than injecting the scheduler through every
 * constructor — is a deliberate concession to the existing static-heavy manager
 * style: {@code AfkManager}, {@code UserdataManager} and {@code LangUtil} are all
 * reached through static methods that would otherwise each need the dependency
 * threaded in. The single {@code volatile} reference gives correct publication
 * across the parallel region threads Folia runs.
 */
public final class Schedulers {

    private static volatile VeltoScheduler instance;

    private Schedulers() {
    }

    /** Installs the platform scheduler. Call once, first thing in {@code onEnable}. */
    public static void set(VeltoScheduler scheduler) {
        instance = scheduler;
    }

    /**
     * @return the installed scheduler
     * @throws IllegalStateException if called before {@link #set(VeltoScheduler)} — a programming
     *         error signalling that a manager was touched before its module wired the scheduler.
     */
    public static VeltoScheduler get() {
        VeltoScheduler s = instance;
        if (s == null) {
            throw new IllegalStateException(
                    "VeltoScheduler not initialised — call Schedulers.set(...) in onEnable() first");
        }
        return s;
    }
}
