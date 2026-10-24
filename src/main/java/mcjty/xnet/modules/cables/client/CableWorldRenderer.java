package mcjty.xnet.modules.cables.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mcjty.lib.client.CustomRenderTypes;
import mcjty.lib.client.RenderHelper;
import mcjty.xnet.modules.cables.CableColor;
import mcjty.xnet.modules.cables.ConnectorType;
import mcjty.xnet.modules.cables.blocks.GenericCableBlock;
import mcjty.xnet.modules.facade.IFacadeSupport;
import mcjty.xnet.modules.facade.blocks.FacadeBlock;
import mcjty.xnet.setup.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.List;

import static mcjty.xnet.modules.cables.ConnectorType.BLOCK;
import static mcjty.xnet.modules.cables.ConnectorType.CABLE;

public class CableWorldRenderer {

    // @todo 1.20 is this correct?
    public static void tick(SubmitCustomGeometryEvent evt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ItemStack heldItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!heldItem.isEmpty()) {
            if (heldItem.getItem() instanceof BlockItem) {
                if (((BlockItem) heldItem.getItem()).getBlock() instanceof GenericCableBlock) {
                    renderCables(evt, mc);
                }
            }
        }
    }

    private static void renderCables(SubmitCustomGeometryEvent evt, Minecraft mc) {
        Player p = mc.player;

        PoseStack matrixStack = evt.getPoseStack();
        Level world = mc.level;

        matrixStack.pushPose();

        Vec3 projectedView = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        matrixStack.translate(-projectedView.x, -projectedView.y, -projectedView.z);

        for (int dx = -20 ; dx <= 20 ; dx++) {
            for (int dy = -20 ; dy <= 20 ; dy++) {
                for (int dz = -20 ; dz <= 20 ; dz++) {
                    BlockPos c = p.blockPosition().offset(dx, dy, dz);
                    BlockState state = world.getBlockState(c);
                    Block block = state.getBlock();
                    if (block instanceof GenericCableBlock) {
                        BlockEntity te = world.getBlockEntity(c);
                        if (te instanceof IFacadeSupport facadeSupport) {
                            BlockState facadeId = facadeSupport.getMimicBlock();
                            if (((!Config.showNonFacadedCablesWhileSneaking.get()) || (!p.isShiftKeyDown())) && facadeId == null && !(block instanceof FacadeBlock)) {
                                continue;
                            }
                        } else if (!Config.showNonFacadedCablesWhileSneaking.get() || !p.isShiftKeyDown()) {
                            continue;
                        }
                        CableColor color = state.getValue(GenericCableBlock.COLOR);
                        float r = 0;
                        float g = 0;
                        float b = 0;
                        switch (color) {
                            case BLUE -> {
                                r = .4f;
                                g = .4f;
                                b = 1f;
                            }
                            case RED -> {
                                r = 1f;
                                g = .4f;
                                b = .4f;
                            }
                            case YELLOW -> {
                                r = 1f;
                                g = 1f;
                                b = .4f;
                            }
                            case GREEN -> {
                                r = .4f;
                                g = 1f;
                                b = .4f;
                            }
                            case ROUTING -> {
                                r = .7f;
                                g = .7f;
                                b = .7f;
                            }
                        }
                        List<RenderHelper.Rect> quads = getQuads(state);
                        float red = r, green = g, blue = b;
                        evt.getSubmitNodeCollector().submitCustomGeometry(matrixStack, CustomRenderTypes.OVERLAY_LINES, (captured, builder) -> {
                            PoseStack pose = new PoseStack();
                            pose.last().pose().set(captured.pose());
                            for (var quad : quads) RenderHelper.renderRect(pose, builder, quad, c, red, green, blue, .5f);
                        });
                    }
                }
            }
        }

        matrixStack.popPose();


    }


    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    private static List<RenderHelper.Rect> getQuads(BlockState state) {
        ConnectorType north = state.getValue(GenericCableBlock.NORTH);
        ConnectorType south = state.getValue(GenericCableBlock.SOUTH);
        ConnectorType west = state.getValue(GenericCableBlock.WEST);
        ConnectorType east = state.getValue(GenericCableBlock.EAST);
        ConnectorType up = state.getValue(GenericCableBlock.UP);
        ConnectorType down = state.getValue(GenericCableBlock.DOWN);
        List<RenderHelper.Rect> quads = new ArrayList<>();

        double o = .4;      // Thickness of the cable. .0 would be full block, .5 is infinitely thin.
        double p = .1;      // Thickness of the connector as it is put on the connecting block
        double q = .2;      // The wideness of the connector

        // For each side we either cap it off if there is no similar block adjacent on that side
        // or else we extend so that we touch the adjacent block:

        if (up == CABLE) {
            quads.add(new RenderHelper.Rect(v(1 - o, 1, o), v(1 - o, 1, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, o)));
            quads.add(new RenderHelper.Rect(v(o, 1, 1 - o), v(o, 1, o), v(o, 1 - o, o), v(o, 1 - o, 1 - o)));
            quads.add(new RenderHelper.Rect(v(o, 1, o), v(1 - o, 1, o), v(1 - o, 1 - o, o), v(o, 1 - o, o)));
            quads.add(new RenderHelper.Rect(v(o, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1, 1 - o), v(o, 1, 1 - o)));
        } else if (up == BLOCK) {
            quads.add(new RenderHelper.Rect(v(1 - o, 1 - p, o), v(1 - o, 1 - p, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, o)));
            quads.add(new RenderHelper.Rect(v(o, 1 - p, 1 - o), v(o, 1 - p, o), v(o, 1 - o, o), v(o, 1 - o, 1 - o)));
            quads.add(new RenderHelper.Rect(v(o, 1 - p, o), v(1 - o, 1 - p, o), v(1 - o, 1 - o, o), v(o, 1 - o, o)));
            quads.add(new RenderHelper.Rect(v(o, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - p, 1 - o), v(o, 1 - p, 1 - o)));

            quads.add(new RenderHelper.Rect(v(1 - q, 1 - p, q), v(1 - q, 1, q), v(1 - q, 1, 1 - q), v(1 - q, 1 - p, 1 - q)));
            quads.add(new RenderHelper.Rect(v(q, 1 - p, 1 - q), v(q, 1, 1 - q), v(q, 1, q), v(q, 1 - p, q)));
            quads.add(new RenderHelper.Rect(v(q, 1, q), v(1 - q, 1, q), v(1 - q, 1 - p, q), v(q, 1 - p, q)));
            quads.add(new RenderHelper.Rect(v(q, 1 - p, 1 - q), v(1 - q, 1 - p, 1 - q), v(1 - q, 1, 1 - q), v(q, 1, 1 - q)));

            quads.add(new RenderHelper.Rect(v(q, 1 - p, q), v(1 - q, 1 - p, q), v(1 - q, 1 - p, 1 - q), v(q, 1 - p, 1 - q)));
            quads.add(new RenderHelper.Rect(v(q, 1, q), v(q, 1, 1 - q), v(1 - q, 1, 1 - q), v(1 - q, 1, q)));
        } else {
            quads.add(new RenderHelper.Rect(v(o,     1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, o),     v(o,     1 - o, o)));
        }

        if (down == CABLE) {
            quads.add(new RenderHelper.Rect(v(1 - o, o, o), v(1 - o, o, 1 - o), v(1 - o, 0, 1 - o), v(1 - o, 0, o)));
            quads.add(new RenderHelper.Rect(v(o, o, 1 - o), v(o, o, o), v(o, 0, o), v(o, 0, 1 - o)));
            quads.add(new RenderHelper.Rect(v(o, o, o), v(1 - o, o, o), v(1 - o, 0, o), v(o, 0, o)));
            quads.add(new RenderHelper.Rect(v(o, 0, 1 - o), v(1 - o, 0, 1 - o), v(1 - o, o, 1 - o), v(o, o, 1 - o)));
        } else if (down == BLOCK) {
            quads.add(new RenderHelper.Rect(v(1 - o, o, o), v(1 - o, o, 1 - o), v(1 - o, p, 1 - o), v(1 - o, p, o)));
            quads.add(new RenderHelper.Rect(v(o, o, 1 - o), v(o, o, o), v(o, p, o), v(o, p, 1 - o)));
            quads.add(new RenderHelper.Rect(v(o, o, o), v(1 - o, o, o), v(1 - o, p, o), v(o, p, o)));
            quads.add(new RenderHelper.Rect(v(o, p, 1 - o), v(1 - o, p, 1 - o), v(1 - o, o, 1 - o), v(o, o, 1 - o)));

            quads.add(new RenderHelper.Rect(v(1 - q, 0, q), v(1 - q, p, q), v(1 - q, p, 1 - q), v(1 - q, 0, 1 - q)));
            quads.add(new RenderHelper.Rect(v(q, 0, 1 - q), v(q, p, 1 - q), v(q, p, q), v(q, 0, q)));
            quads.add(new RenderHelper.Rect(v(q, p, q), v(1 - q, p, q), v(1 - q, 0, q), v(q, 0, q)));
            quads.add(new RenderHelper.Rect(v(q, 0, 1 - q), v(1 - q, 0, 1 - q), v(1 - q, p, 1 - q), v(q, p, 1 - q)));

            quads.add(new RenderHelper.Rect(v(q, p, 1 - q), v(1 - q, p, 1 - q), v(1 - q, p, q), v(q, p, q)));
            quads.add(new RenderHelper.Rect(v(q, 0, 1 - q), v(q, 0, q), v(1 - q, 0, q), v(1 - q, 0, 1 - q)));
        } else {
            quads.add(new RenderHelper.Rect(v(o, o, o), v(1 - o, o, o), v(1 - o, o, 1 - o), v(o, o, 1 - o)));
        }

        if (east == CABLE) {
            quads.add(new RenderHelper.Rect(v(1, 1 - o, 1 - o), v(1, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, 1 - o)));
            quads.add(new RenderHelper.Rect(v(1, o, o), v(1, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, o, o)));
            quads.add(new RenderHelper.Rect(v(1, 1 - o, o), v(1, o, o), v(1 - o, o, o), v(1 - o, 1 - o, o)));
            quads.add(new RenderHelper.Rect(v(1, o, 1 - o), v(1, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, o, 1 - o)));
        } else if (east == BLOCK) {
            quads.add(new RenderHelper.Rect(v(1 - p, 1 - o, 1 - o), v(1 - p, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, 1 - o)));
            quads.add(new RenderHelper.Rect(v(1 - p, o, o), v(1 - p, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, o, o)));
            quads.add(new RenderHelper.Rect(v(1 - p, 1 - o, o), v(1 - p, o, o), v(1 - o, o, o), v(1 - o, 1 - o, o)));
            quads.add(new RenderHelper.Rect(v(1 - p, o, 1 - o), v(1 - p, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, o, 1 - o)));

            quads.add(new RenderHelper.Rect(v(1 - p, 1 - q, 1 - q), v(1, 1 - q, 1 - q), v(1, 1 - q, q), v(1 - p, 1 - q, q)));
            quads.add(new RenderHelper.Rect(v(1 - p, q, q), v(1, q, q), v(1, q, 1 - q), v(1 - p, q, 1 - q)));
            quads.add(new RenderHelper.Rect(v(1 - p, 1 - q, q), v(1, 1 - q, q), v(1, q, q), v(1 - p, q, q)));
            quads.add(new RenderHelper.Rect(v(1 - p, q, 1 - q), v(1, q, 1 - q), v(1, 1 - q, 1 - q), v(1 - p, 1 - q, 1 - q)));

            quads.add(new RenderHelper.Rect(v(1 - p, q, 1 - q), v(1 - p, 1 - q, 1 - q), v(1 - p, 1 - q, q), v(1 - p, q, q)));
            quads.add(new RenderHelper.Rect(v(1, q, 1 - q), v(1, q, q), v(1, 1 - q, q), v(1, 1 - q, 1 - q)));
        } else {
            quads.add(new RenderHelper.Rect(v(1 - o, o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, 1 - o), v(1 - o, o, 1 - o)));
        }

        if (west == CABLE) {
            quads.add(new RenderHelper.Rect(v(o, 1 - o, 1 - o), v(o, 1 - o, o), v(0, 1 - o, o), v(0, 1 - o, 1 - o)));
            quads.add(new RenderHelper.Rect(v(o, o, o), v(o, o, 1 - o), v(0, o, 1 - o), v(0, o, o)));
            quads.add(new RenderHelper.Rect(v(o, 1 - o, o), v(o, o, o), v(0, o, o), v(0, 1 - o, o)));
            quads.add(new RenderHelper.Rect(v(o, o, 1 - o), v(o, 1 - o, 1 - o), v(0, 1 - o, 1 - o), v(0, o, 1 - o)));
        } else if (west == BLOCK) {
            quads.add(new RenderHelper.Rect(v(o, 1 - o, 1 - o), v(o, 1 - o, o), v(p, 1 - o, o), v(p, 1 - o, 1 - o)));
            quads.add(new RenderHelper.Rect(v(o, o, o), v(o, o, 1 - o), v(p, o, 1 - o), v(p, o, o)));
            quads.add(new RenderHelper.Rect(v(o, 1 - o, o), v(o, o, o), v(p, o, o), v(p, 1 - o, o)));
            quads.add(new RenderHelper.Rect(v(o, o, 1 - o), v(o, 1 - o, 1 - o), v(p, 1 - o, 1 - o), v(p, o, 1 - o)));

            quads.add(new RenderHelper.Rect(v(0, 1 - q, 1 - q), v(p, 1 - q, 1 - q), v(p, 1 - q, q), v(0, 1 - q, q)));
            quads.add(new RenderHelper.Rect(v(0, q, q), v(p, q, q), v(p, q, 1 - q), v(0, q, 1 - q)));
            quads.add(new RenderHelper.Rect(v(0, 1 - q, q), v(p, 1 - q, q), v(p, q, q), v(0, q, q)));
            quads.add(new RenderHelper.Rect(v(0, q, 1 - q), v(p, q, 1 - q), v(p, 1 - q, 1 - q), v(0, 1 - q, 1 - q)));

            quads.add(new RenderHelper.Rect(v(p, q, q), v(p, 1 - q, q), v(p, 1 - q, 1 - q), v(p, q, 1 - q)));
            quads.add(new RenderHelper.Rect(v(0, q, q), v(0, q, 1 - q), v(0, 1 - q, 1 - q), v(0, 1 - q, q)));
        } else {
            quads.add(new RenderHelper.Rect(v(o, o, 1 - o), v(o, 1 - o, 1 - o), v(o, 1 - o, o), v(o, o, o)));
        }

        if (north == CABLE) {
            quads.add(new RenderHelper.Rect(v(o, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, 0), v(o, 1 - o, 0)));
            quads.add(new RenderHelper.Rect(v(o, o, 0), v(1 - o, o, 0), v(1 - o, o, o), v(o, o, o)));
            quads.add(new RenderHelper.Rect(v(1 - o, o, 0), v(1 - o, 1 - o, 0), v(1 - o, 1 - o, o), v(1 - o, o, o)));
            quads.add(new RenderHelper.Rect(v(o, o, o), v(o, 1 - o, o), v(o, 1 - o, 0), v(o, o, 0)));
        } else if (north == BLOCK) {
            quads.add(new RenderHelper.Rect(v(o, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, p), v(o, 1 - o, p)));
            quads.add(new RenderHelper.Rect(v(o, o, p), v(1 - o, o, p), v(1 - o, o, o), v(o, o, o)));
            quads.add(new RenderHelper.Rect(v(1 - o, o, p), v(1 - o, 1 - o, p), v(1 - o, 1 - o, o), v(1 - o, o, o)));
            quads.add(new RenderHelper.Rect(v(o, o, o), v(o, 1 - o, o), v(o, 1 - o, p), v(o, o, p)));

            quads.add(new RenderHelper.Rect(v(q, 1 - q, p), v(1 - q, 1 - q, p), v(1 - q, 1 - q, 0), v(q, 1 - q, 0)));
            quads.add(new RenderHelper.Rect(v(q, q, 0), v(1 - q, q, 0), v(1 - q, q, p), v(q, q, p)));
            quads.add(new RenderHelper.Rect(v(1 - q, q, 0), v(1 - q, 1 - q, 0), v(1 - q, 1 - q, p), v(1 - q, q, p)));
            quads.add(new RenderHelper.Rect(v(q, q, p), v(q, 1 - q, p), v(q, 1 - q, 0), v(q, q, 0)));

            quads.add(new RenderHelper.Rect(v(q, q, p), v(1 - q, q, p), v(1 - q, 1 - q, p), v(q, 1 - q, p)));
            quads.add(new RenderHelper.Rect(v(q, q, 0), v(q, 1 - q, 0), v(1 - q, 1 - q, 0), v(1 - q, q, 0)));
        } else {
            quads.add(new RenderHelper.Rect(v(o, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, o, o), v(o, o, o)));
        }

        if (south == CABLE) {
            quads.add(new RenderHelper.Rect(v(o, 1 - o, 1), v(1 - o, 1 - o, 1), v(1 - o, 1 - o, 1 - o), v(o, 1 - o, 1 - o)));
            quads.add(new RenderHelper.Rect(v(o, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, o, 1), v(o, o, 1)));
            quads.add(new RenderHelper.Rect(v(1 - o, o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, 1), v(1 - o, o, 1)));
            quads.add(new RenderHelper.Rect(v(o, o, 1), v(o, 1 - o, 1), v(o, 1 - o, 1 - o), v(o, o, 1 - o)));
        } else if (south == BLOCK) {
            quads.add(new RenderHelper.Rect(v(o, 1 - o, 1 - p), v(1 - o, 1 - o, 1 - p), v(1 - o, 1 - o, 1 - o), v(o, 1 - o, 1 - o)));
            quads.add(new RenderHelper.Rect(v(o, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, o, 1 - p), v(o, o, 1 - p)));
            quads.add(new RenderHelper.Rect(v(1 - o, o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - p), v(1 - o, o, 1 - p)));
            quads.add(new RenderHelper.Rect(v(o, o, 1 - p), v(o, 1 - o, 1 - p), v(o, 1 - o, 1 - o), v(o, o, 1 - o)));

            quads.add(new RenderHelper.Rect(v(q, 1 - q, 1), v(1 - q, 1 - q, 1), v(1 - q, 1 - q, 1 - p), v(q, 1 - q, 1 - p)));
            quads.add(new RenderHelper.Rect(v(q, q, 1 - p), v(1 - q, q, 1 - p), v(1 - q, q, 1), v(q, q, 1)));
            quads.add(new RenderHelper.Rect(v(1 - q, q, 1 - p), v(1 - q, 1 - q, 1 - p), v(1 - q, 1 - q, 1), v(1 - q, q, 1)));
            quads.add(new RenderHelper.Rect(v(q, q, 1), v(q, 1 - q, 1), v(q, 1 - q, 1 - p), v(q, q, 1 - p)));

            quads.add(new RenderHelper.Rect(v(q, 1 - q, 1 - p), v(1 - q, 1 - q, 1 - p), v(1 - q, q, 1 - p), v(q, q, 1 - p)));
            quads.add(new RenderHelper.Rect(v(q, 1 - q, 1), v(q, q, 1), v(1 - q, q, 1), v(1 - q, 1 - q, 1)));
        } else {
            quads.add(new RenderHelper.Rect(v(o, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, 1 - o, 1 - o), v(o, 1 - o, 1 - o)));
        }

        return quads;
    }

}
