package net.essentialsx.spawn.folia;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler bridge that supports both Folia and non-Folia servers.
 *
 * <p>This class intentionally uses reflection for Folia methods so the same jar
 * can run on both regular Paper/Spigot and Folia without class loading errors.</p>
 */
public final class FoliaCompatScheduler {
    private final Plugin plugin;
    private final boolean foliaAvailable;

    public FoliaCompatScheduler(final Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.foliaAvailable = hasMethod(Bukkit.class, "getGlobalRegionScheduler")
            && hasMethod(Bukkit.class, "getRegionScheduler")
            && hasMethod(Bukkit.class, "getAsyncScheduler");
    }

    public boolean isFoliaAvailable() {
        return foliaAvailable;
    }

    public void runGlobal(final Runnable task) {
        Objects.requireNonNull(task, "task");

        if (foliaAvailable) {
            invokeGlobal(task);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void runAtLocation(final Location location, final Runnable task) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(task, "task");

        if (foliaAvailable) {
            invokeRegion(location, task);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void runForEntity(final Entity entity, final Runnable task) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(task, "task");

        if (foliaAvailable) {
            invokeEntity(entity, task);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void runAsync(final Runnable task) {
        Objects.requireNonNull(task, "task");

        if (foliaAvailable) {
            invokeAsync(task);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    private void invokeGlobal(final Runnable task) {
        try {
            final Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            final Method run = globalScheduler.getClass().getMethod("run", Plugin.class, java.util.function.Consumer.class);
            run.invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) ignored -> task.run());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to schedule global task on Folia", ex);
        }
    }

    private void invokeRegion(final Location location, final Runnable task) {
        try {
            final Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
            final Method run = regionScheduler.getClass().getMethod(
                "run",
                Plugin.class,
                Location.class,
                java.util.function.Consumer.class
            );
            run.invoke(regionScheduler, plugin, location, (java.util.function.Consumer<Object>) ignored -> task.run());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to schedule region task on Folia", ex);
        }
    }

    private void invokeEntity(final Entity entity, final Runnable task) {
        try {
            final Method getScheduler = entity.getClass().getMethod("getScheduler");
            final Object entityScheduler = getScheduler.invoke(entity);
            final Method run = entityScheduler.getClass().getMethod(
                "run",
                Plugin.class,
                java.util.function.Consumer.class,
                Runnable.class
            );
            run.invoke(entityScheduler, plugin, (java.util.function.Consumer<Object>) ignored -> task.run(), null);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to schedule entity task on Folia", ex);
        }
    }

    private void invokeAsync(final Runnable task) {
        try {
            final Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            final Method runNow = asyncScheduler.getClass().getMethod("runNow", Plugin.class, java.util.function.Consumer.class);
            runNow.invoke(asyncScheduler, plugin, (java.util.function.Consumer<Object>) ignored -> task.run());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to schedule async task on Folia", ex);
        }
    }

    private static boolean hasMethod(final Class<?> type, final String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static long ticksToMillis(final long ticks) {
        return TimeUnit.MILLISECONDS.convert(ticks * 50L, TimeUnit.MILLISECONDS);
    }
}
