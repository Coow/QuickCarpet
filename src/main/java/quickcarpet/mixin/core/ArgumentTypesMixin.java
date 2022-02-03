package quickcarpet.mixin.core;

import net.minecraft.command.argument.ArgumentTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ArgumentTypes.class)
public class ArgumentTypesMixin {
    @Redirect(method = "register()V", at = @At(value = "FIELD", target = "Lnet/minecraft/SharedConstants;isDevelopment:Z"))
    private static boolean quickcarpet$isDevelopment() {
        return true;
    }
}
