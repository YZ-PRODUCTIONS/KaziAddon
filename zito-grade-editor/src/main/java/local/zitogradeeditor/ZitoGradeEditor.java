package local.zitogradeeditor;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.Zitoen.zitoenpiece.blades.grades.GradeHelper;
import net.Zitoen.zitoenpiece.blades.grades.WeaponGrade;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

@Mod(ZitoGradeEditor.ID)
@Mod.EventBusSubscriber(modid = ZitoGradeEditor.ID, value = Dist.CLIENT)
public final class ZitoGradeEditor {
    static final String ID = "zitogradeeditor";

    public ZitoGradeEditor() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (version, remoteIsServer) -> true));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        if (event.getEnvironment() != Commands.EnvironmentType.INTEGRATED) {
            return;
        }
        LiteralArgumentBuilder<CommandSource> root = Commands.literal("zgrade")
                .requires(ZitoGradeEditor::isPrivateWorldOwner);
        for (WeaponGrade grade : WeaponGrade.values()) {
            root.then(Commands.literal(grade.getId())
                    .executes(ctx -> apply(ctx.getSource(), grade, false))
                    .then(Commands.literal("all").executes(ctx -> apply(ctx.getSource(), grade, true))));
        }
        event.getDispatcher().register(root);
    }

    private static boolean isPrivateWorldOwner(CommandSource source) {
        return source.getEntity() instanceof ServerPlayerEntity
                && source.getServer().isSingleplayer() && !source.getServer().isPublished()
                && source.getServer().isSingleplayerOwner(((ServerPlayerEntity) source.getEntity()).getGameProfile());
    }

    private static int apply(CommandSource source, WeaponGrade grade, boolean all) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        int matched = 0;
        if (all) {
            for (int slot = 0; slot < player.inventory.getContainerSize(); slot++) {
                if (gradeItem(player.inventory.getItem(slot), grade, player)) {
                    matched++;
                }
            }
        } else if (gradeItem(player.getMainHandItem(), grade, player)) {
            matched = 1;
        }
        if (matched == 0) {
            source.sendFailure(new StringTextComponent(all
                    ? "No gradeable items in your inventory."
                    : "Hold a gradeable item in your main hand."));
        } else {
            player.inventory.setChanged();
            player.containerMenu.broadcastChanges();
            source.sendSuccess(new StringTextComponent("Set " + matched + (matched == 1 ? " stack" : " stacks")
                    + " to " + grade.getDisplayName() + "."), false);
        }
        return matched;
    }

    private static boolean gradeItem(ItemStack stack, WeaponGrade grade, ServerPlayerEntity player) {
        if (!GradeHelper.canBeGraded(stack)) {
            return false;
        }
        if (!GradeHelper.isAppraised(stack)) {
            GradeHelper.setAppraisedBy(stack, player.getUUID().toString());
            GradeHelper.setAppraised(stack, true);
        }
        GradeHelper.setGrade(stack, grade);
        return true;
    }
}
