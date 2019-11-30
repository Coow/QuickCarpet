package quickcarpet.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerTickScheduler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import quickcarpet.QuickCarpet;
import quickcarpet.annotation.Feature;
import quickcarpet.settings.Settings;
import quickcarpet.utils.CarpetProfiler;
import quickcarpet.utils.Waypoint;
import quickcarpet.utils.WaypointContainer;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements WaypointContainer {
    @Shadow @Final private List<ServerPlayerEntity> players;
    @Shadow private boolean allPlayersSleeping;

    private Map<String, Waypoint> waypoints = new TreeMap<>();

    protected ServerWorldMixin(LevelProperties levelProperties_1, DimensionType dimensionType_1, BiFunction<World, Dimension, ChunkManager> biFunction_1, Profiler profiler_1, boolean boolean_1) {
        super(levelProperties_1, dimensionType_1, biFunction_1, profiler_1, boolean_1);
    }

    @Override
    public Map<String, Waypoint> getWaypoints() {
        return waypoints;
    }

    @Override
    public DimensionType getDimensionType() {
        return dimension.getType();
    }

    @Feature("profiler")
    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerTickScheduler;tick()V", ordinal = 0)
    )
    private void tickBlocks(ServerTickScheduler blockTickScheduler) {
        CarpetProfiler.startSection(this, CarpetProfiler.SectionType.BLOCKS);
        blockTickScheduler.tick();
        CarpetProfiler.endSection(this);
    }

    @Feature("profiler")
    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerTickScheduler;tick()V", ordinal = 1)
    )
    private void tickFluids(ServerTickScheduler fluidTickScheduler) {
        CarpetProfiler.startSection(this, CarpetProfiler.SectionType.FLUIDS);
        fluidTickScheduler.tick();
        CarpetProfiler.endSection(this);
    }

    @Feature("profiler")
    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void startTickChunk(WorldChunk worldChunk_1, int int_1, CallbackInfo ci) {
        CarpetProfiler.startSection(this, CarpetProfiler.SectionType.RANDOM_TICKS);
    }

    @Feature("profiler")
    @Inject(method = "tickChunk", at = @At("TAIL"))
    private void endTickChunk(WorldChunk worldChunk_1, int int_1, CallbackInfo ci) {
        CarpetProfiler.endSection(this);
    }

    @Feature("profiler")
    @Inject(method = "tick", at = @At(value = "CONSTANT", args = "stringValue=raid"))
    private void startRaid(BooleanSupplier booleanSupplier_1, CallbackInfo ci) {
        CarpetProfiler.startSection(this, CarpetProfiler.SectionType.VILLAGES);
    }

    @Feature("profiler")
    @Inject(method = "tick", at = @At(value = "CONSTANT", args = "stringValue=blockEvents"))
    private void endRaidStartBlockEvents(BooleanSupplier booleanSupplier_1, CallbackInfo ci) {
        CarpetProfiler.endSection(this);
        CarpetProfiler.startSection(this, CarpetProfiler.SectionType.BLOCK_EVENTS);
    }

    @Feature("profiler")
    @Inject(method = "tick", at = @At(value = "CONSTANT", args = "stringValue=entities"))
    private void endBlockEventsStartEntities(BooleanSupplier booleanSupplier_1, CallbackInfo ci) {
        CarpetProfiler.endSection(this);
        CarpetProfiler.startSection(this, CarpetProfiler.SectionType.ENTITIES);
    }

    @Feature("spawnChunkLevel")
    @ModifyConstant(method = "setSpawnPos", constant = @Constant(intValue = 11), require = 2)
    private int adjustSpawnChunkLevel(int level) {
        return Settings.spawnChunkLevel;
    }

    @Feature("tickSpeed")
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tickFreeze(BooleanSupplier shouldContinueTicking, CallbackInfo ci) {
        if (QuickCarpet.getInstance().tickSpeed.isPaused()) {
            for (ServerPlayerEntity p : this.players) p.tick();
            this.getChunkManager().tick(shouldContinueTicking);
            ci.cancel();
        }
    }

    @Feature("sleepingThreshold")
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;noneMatch(Ljava/util/function/Predicate;)Z", remap = false))
    private boolean testSleepingLongEnough(Stream<ServerPlayerEntity> stream, Predicate<ServerPlayerEntity> predicate) {
        return arePlayersSleeping(ServerPlayerEntity::isSleepingLongEnough);
    }

    /**
     * @author skyrising
     * @reason Whole method is changed anyway
     */
    @Overwrite
    @Feature("sleepingThreshold")
    public void updatePlayersSleeping() {
        this.allPlayersSleeping = arePlayersSleeping(ServerPlayerEntity::isSleeping);
    }

    private boolean arePlayersSleeping(Predicate<ServerPlayerEntity> condition) {
        int nonSpectators = 0;
        int sleeping = 0;
        for (ServerPlayerEntity player : this.players) {
            if (player.isSpectator()) continue;
            nonSpectators++;
            if (condition.test(player)) sleeping++;
        }
        if (sleeping == 0) return false;
        return sleeping * 100 >= nonSpectators * Settings.sleepingThreshold;
    }
}
