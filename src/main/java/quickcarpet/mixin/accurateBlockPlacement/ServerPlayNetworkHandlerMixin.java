package quickcarpet.mixin.accurateBlockPlacement;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import quickcarpet.settings.Settings;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Redirect(method = "onPlayerInteractBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;subtract(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;"), require = 0)
    private Vec3d quickcarpet$accurateBlockPlacement$removeHitPosCheck(Vec3d hitVec, Vec3d blockCenter) {
        return Settings.accurateBlockPlacement ? Vec3d.ZERO : hitVec.subtract(blockCenter);
    }
}
