# EssentialsSpawn Folia compatibility helper

This repository now includes a Folia-compatible scheduler bridge:

- `FoliaCompatScheduler` in `src/main/java/net/essentialsx/spawn/folia/FoliaCompatScheduler.java`
- Uses Folia schedulers when available
- Falls back to Bukkit scheduler on non-Folia servers

## Example

```java
FoliaCompatScheduler scheduler = new FoliaCompatScheduler(plugin);

scheduler.runGlobal(() -> {
    // global safe task
});

scheduler.runAtLocation(player.getLocation(), () -> {
    // region-safe task
});

scheduler.runForEntity(player, () -> {
    // entity-safe task
});

scheduler.runAsync(() -> {
    // async task
});
```
