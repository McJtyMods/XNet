package mcjty.xnet.modules.cables.client;

import com.google.common.base.Function;
import mcjty.lib.client.AbstractDynamicBakedModel;
import mcjty.xnet.XNet;
import mcjty.xnet.modules.cables.CableColor;
import mcjty.xnet.modules.cables.ConnectorType;
import mcjty.xnet.modules.cables.blocks.ConnectorBlock;
import mcjty.xnet.modules.cables.blocks.GenericCableBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static mcjty.xnet.modules.cables.ConnectorType.BLOCK;
import static mcjty.xnet.modules.cables.ConnectorType.CABLE;
import static mcjty.xnet.modules.cables.client.CablePatterns.SpriteIdx.SPRITE_CORNER;
import static mcjty.xnet.modules.cables.client.CablePatterns.SpriteIdx.SPRITE_CROSS;
import static mcjty.xnet.modules.cables.client.CablePatterns.SpriteIdx.SPRITE_END;
import static mcjty.xnet.modules.cables.client.CablePatterns.SpriteIdx.SPRITE_NONE;
import static mcjty.xnet.modules.cables.client.CablePatterns.SpriteIdx.SPRITE_STRAIGHT;
import static mcjty.xnet.modules.cables.client.CablePatterns.SpriteIdx.SPRITE_THREE;

public class GenericCableBakedModel extends AbstractDynamicBakedModel {

    public static class CableTextures {
        private TextureAtlasSprite spriteConnector;
        private TextureAtlasSprite spriteAdvancedConnector;
        private TextureAtlasSprite spriteNoneCable;
        private TextureAtlasSprite spriteNormalCable;
        private TextureAtlasSprite spriteEndCable;
        private TextureAtlasSprite spriteCornerCable;
        private TextureAtlasSprite spriteThreeCable;
        private TextureAtlasSprite spriteCrossCable;
    }

    private CableTextures[] cableTextures;
    private TextureAtlasSprite spriteSide;

    static {
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(false, false, false, false), new CablePatterns.QuadSetting(SPRITE_NONE, 0));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(true, false, false, false), new CablePatterns.QuadSetting(SPRITE_END, 3));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(false, true, false, false), new CablePatterns.QuadSetting(SPRITE_END, 0));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(false, false, true, false), new CablePatterns.QuadSetting(SPRITE_END, 1));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(false, false, false, true), new CablePatterns.QuadSetting(SPRITE_END, 2));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(true, true, false, false), new CablePatterns.QuadSetting(SPRITE_CORNER, 0));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(false, true, true, false), new CablePatterns.QuadSetting(SPRITE_CORNER, 1));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(false, false, true, true), new CablePatterns.QuadSetting(SPRITE_CORNER, 2));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(true, false, false, true), new CablePatterns.QuadSetting(SPRITE_CORNER, 3));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(false, true, false, true), new CablePatterns.QuadSetting(SPRITE_STRAIGHT, 0));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(true, false, true, false), new CablePatterns.QuadSetting(SPRITE_STRAIGHT, 1));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(true, true, true, false), new CablePatterns.QuadSetting(SPRITE_THREE, 0));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(false, true, true, true), new CablePatterns.QuadSetting(SPRITE_THREE, 1));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(true, false, true, true), new CablePatterns.QuadSetting(SPRITE_THREE, 2));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(true, true, false, true), new CablePatterns.QuadSetting(SPRITE_THREE, 3));
        CablePatterns.PATTERNS.put(new CablePatterns.Pattern(true, true, true, true), new CablePatterns.QuadSetting(SPRITE_CROSS, 0));
    }

    private synchronized void initTextures() {
        if (cableTextures == null) {
            CableTextures[] tt = new CableTextures[CableColor.VALUES.length];
            for (CableColor color : CableColor.VALUES) {
                int i = color.ordinal();
                tt[i] = new CableTextures();
                tt[i].spriteConnector = getTexture(Identifier.fromNamespaceAndPath(XNet.MODID, "block/cable" + i + "/connector"));
                tt[i].spriteAdvancedConnector = getTexture(Identifier.fromNamespaceAndPath(XNet.MODID, "block/cable" + i + "/advanced_connector"));

                tt[i].spriteNormalCable = getTexture(Identifier.fromNamespaceAndPath(XNet.MODID, "block/cable" + i + "/normal_netcable"));
                tt[i].spriteNoneCable = getTexture(Identifier.fromNamespaceAndPath(XNet.MODID, "block/cable" + i + "/normal_none_netcable"));
                tt[i].spriteEndCable = getTexture(Identifier.fromNamespaceAndPath(XNet.MODID, "block/cable" + i + "/normal_end_netcable"));
                tt[i].spriteCornerCable = getTexture(Identifier.fromNamespaceAndPath(XNet.MODID, "block/cable" + i + "/normal_corner_netcable"));
                tt[i].spriteThreeCable = getTexture(Identifier.fromNamespaceAndPath(XNet.MODID, "block/cable" + i + "/normal_three_netcable"));
                tt[i].spriteCrossCable = getTexture(Identifier.fromNamespaceAndPath(XNet.MODID, "block/cable" + i + "/normal_cross_netcable"));
            }

            spriteSide = getTexture(Identifier.fromNamespaceAndPath(XNet.MODID, "block/connector_side"));
            cableTextures = tt;
        }
    }

    private TextureAtlasSprite getSpriteNormal(CablePatterns.SpriteIdx idx, int index) {
        initTextures();
        CableTextures cableTexture = cableTextures[index];
        return switch (idx) {
            case SPRITE_NONE -> cableTexture.spriteNoneCable;
            case SPRITE_END -> cableTexture.spriteEndCable;
            case SPRITE_STRAIGHT -> cableTexture.spriteNormalCable;
            case SPRITE_CORNER -> cableTexture.spriteCornerCable;
            case SPRITE_THREE -> cableTexture.spriteThreeCable;
            case SPRITE_CROSS -> cableTexture.spriteCrossCable;
        };
    }

    private BakedQuad createQuad(Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4, TextureAtlasSprite sprite, int rotation, float hilight) {
        return switch (rotation) {
            case 0 -> createQuad(v1, v2, v3, v4, sprite, hilight);
            case 1 -> createQuad(v2, v3, v4, v1, sprite, hilight);
            case 2 -> createQuad(v3, v4, v1, v2, sprite, hilight);
            case 3 -> createQuad(v4, v1, v2, v3, sprite, hilight);
            default -> createQuad(v1, v2, v3, v4, sprite, hilight);
        };
    }

    @Override
    public void collectParts(net.minecraft.client.renderer.block.BlockAndTintGetter world, net.minecraft.core.BlockPos pos,
            BlockState state, RandomSource random, List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts) {
        if (state.getBlock() instanceof GenericCableBlock cable) {
            // Use the same neighboring blocks as the outline, including during placement updates.
            var connectedState = cable.calculateState(world, pos, state);
            parts.add(new mcjty.rftoolsbase.client.QuadModelPart(buildQuads(connectedState), particleMaterial()));
        }
        if (world.getBlockEntity(pos) instanceof mcjty.xnet.modules.facade.IFacadeSupport facade) {
            BlockState mimic = facade.getMimicBlock();
            if (mimic != null && mimic.getBlock() != state.getBlock()) {
                Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(mimic).collectParts(world, pos, mimic, random, parts);
            }
        }
    }

    private List<BakedQuad> buildQuads(BlockState state) {
        List<BakedQuad> quads = new ArrayList<>();
        {
            // Called with the blockstate from our block. Here we get the values of the six properties and pass that to
            // our baked model implementation.
            ConnectorType north = state.getValue(GenericCableBlock.NORTH);
            ConnectorType south = state.getValue(GenericCableBlock.SOUTH);
            ConnectorType west = state.getValue(GenericCableBlock.WEST);
            ConnectorType east = state.getValue(GenericCableBlock.EAST);
            ConnectorType up = state.getValue(GenericCableBlock.UP);
            ConnectorType down = state.getValue(GenericCableBlock.DOWN);
            CableColor cableColor = state.getValue(GenericCableBlock.COLOR);
            int index = cableColor.ordinal();

            initTextures();
            CableTextures ct = cableTextures[index];
            TextureAtlasSprite spriteCable = ct.spriteNormalCable;
            GenericCableBlock block = (GenericCableBlock) state.getBlock();
            TextureAtlasSprite spriteConnector;
            if (block.isAdvancedConnector()) {
                spriteConnector = ct.spriteAdvancedConnector;
            } else {
                spriteConnector = ct.spriteConnector;
            }
            Function<CablePatterns.SpriteIdx, TextureAtlasSprite> getSprite = idx -> getSpriteNormal(idx, index);
            float hilight = 1.0f;
            if (block instanceof ConnectorBlock) {
                if (north != BLOCK && south != BLOCK && west != BLOCK && east != BLOCK && up != BLOCK && down != BLOCK) {
                    hilight = 0.5f; // To make connectors with no actual connections visible
                }
            }

            double o = .4;      // Thickness of the cable. .0 would be full block, .5 is infinitely thin.
            double p = .1;      // Thickness of the connector as it is put on the connecting block
            double q = .2;      // The wideness of the connector

            // For each side we either cap it off if there is no similar block adjacent on that side
            // or else we extend so that we touch the adjacent block:

            if (up == CABLE) {
                quads.add(createQuad(v(1 - o, 1, o), v(1 - o, 1, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, o), spriteCable, hilight));
                quads.add(createQuad(v(o, 1, 1 - o), v(o, 1, o), v(o, 1 - o, o), v(o, 1 - o, 1 - o), spriteCable, hilight));
                quads.add(createQuad(v(o, 1, o), v(1 - o, 1, o), v(1 - o, 1 - o, o), v(o, 1 - o, o), spriteCable, hilight));
                quads.add(createQuad(v(o, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1, 1 - o), v(o, 1, 1 - o), spriteCable, hilight));
            } else if (up == BLOCK) {
                quads.add(createQuad(v(1 - o, 1 - p, o), v(1 - o, 1 - p, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, o), spriteCable, hilight));
                quads.add(createQuad(v(o, 1 - p, 1 - o), v(o, 1 - p, o), v(o, 1 - o, o), v(o, 1 - o, 1 - o), spriteCable, hilight));
                quads.add(createQuad(v(o, 1 - p, o), v(1 - o, 1 - p, o), v(1 - o, 1 - o, o), v(o, 1 - o, o), spriteCable, hilight));
                quads.add(createQuad(v(o, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - p, 1 - o), v(o, 1 - p, 1 - o), spriteCable, hilight));

                quads.add(createQuad(v(1 - q, 1 - p, q), v(1 - q, 1, q), v(1 - q, 1, 1 - q), v(1 - q, 1 - p, 1 - q), spriteSide, hilight));
                quads.add(createQuad(v(q, 1 - p, 1 - q), v(q, 1, 1 - q), v(q, 1, q), v(q, 1 - p, q), spriteSide, hilight));
                quads.add(createQuad(v(q, 1, q), v(1 - q, 1, q), v(1 - q, 1 - p, q), v(q, 1 - p, q), spriteSide, hilight));
                quads.add(createQuad(v(q, 1 - p, 1 - q), v(1 - q, 1 - p, 1 - q), v(1 - q, 1, 1 - q), v(q, 1, 1 - q), spriteSide, hilight));

                quads.add(createQuad(v(q, 1 - p, q), v(1 - q, 1 - p, q), v(1 - q, 1 - p, 1 - q), v(q, 1 - p, 1 - q), spriteConnector, hilight));
                quads.add(createQuad(v(q, 1, q), v(q, 1, 1 - q), v(1 - q, 1, 1 - q), v(1 - q, 1, q), spriteSide, hilight));
            } else {
                CablePatterns.QuadSetting pattern = CablePatterns.findPattern(west, south, east, north);
                quads.add(createQuad(v(o, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, o), v(o, 1 - o, o), getSprite.apply(pattern.getSprite()), pattern.getRotation(), hilight));
            }

            if (down == CABLE) {
                quads.add(createQuad(v(1 - o, o, o), v(1 - o, o, 1 - o), v(1 - o, 0, 1 - o), v(1 - o, 0, o), spriteCable, hilight));
                quads.add(createQuad(v(o, o, 1 - o), v(o, o, o), v(o, 0, o), v(o, 0, 1 - o), spriteCable, hilight));
                quads.add(createQuad(v(o, o, o), v(1 - o, o, o), v(1 - o, 0, o), v(o, 0, o), spriteCable, hilight));
                quads.add(createQuad(v(o, 0, 1 - o), v(1 - o, 0, 1 - o), v(1 - o, o, 1 - o), v(o, o, 1 - o), spriteCable, hilight));
            } else if (down == BLOCK) {
                quads.add(createQuad(v(1 - o, o, o), v(1 - o, o, 1 - o), v(1 - o, p, 1 - o), v(1 - o, p, o), spriteCable, hilight));
                quads.add(createQuad(v(o, o, 1 - o), v(o, o, o), v(o, p, o), v(o, p, 1 - o), spriteCable, hilight));
                quads.add(createQuad(v(o, o, o), v(1 - o, o, o), v(1 - o, p, o), v(o, p, o), spriteCable, hilight));
                quads.add(createQuad(v(o, p, 1 - o), v(1 - o, p, 1 - o), v(1 - o, o, 1 - o), v(o, o, 1 - o), spriteCable, hilight));

                quads.add(createQuad(v(1 - q, 0, q), v(1 - q, p, q), v(1 - q, p, 1 - q), v(1 - q, 0, 1 - q), spriteSide, hilight));
                quads.add(createQuad(v(q, 0, 1 - q), v(q, p, 1 - q), v(q, p, q), v(q, 0, q), spriteSide, hilight));
                quads.add(createQuad(v(q, p, q), v(1 - q, p, q), v(1 - q, 0, q), v(q, 0, q), spriteSide, hilight));
                quads.add(createQuad(v(q, 0, 1 - q), v(1 - q, 0, 1 - q), v(1 - q, p, 1 - q), v(q, p, 1 - q), spriteSide, hilight));

                quads.add(createQuad(v(q, p, 1 - q), v(1 - q, p, 1 - q), v(1 - q, p, q), v(q, p, q), spriteConnector, hilight));
                quads.add(createQuad(v(q, 0, 1 - q), v(q, 0, q), v(1 - q, 0, q), v(1 - q, 0, 1 - q), spriteSide, hilight));
            } else {
                CablePatterns.QuadSetting pattern = CablePatterns.findPattern(west, north, east, south);
                quads.add(createQuad(v(o, o, o), v(1 - o, o, o), v(1 - o, o, 1 - o), v(o, o, 1 - o), getSprite.apply(pattern.getSprite()), pattern.getRotation(), hilight));
            }

            if (east == CABLE) {
                quads.add(createQuad(v(1, 1 - o, 1 - o), v(1, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, 1 - o), spriteCable, hilight));
                quads.add(createQuad(v(1, o, o), v(1, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, o, o), spriteCable, hilight));
                quads.add(createQuad(v(1, 1 - o, o), v(1, o, o), v(1 - o, o, o), v(1 - o, 1 - o, o), spriteCable, hilight));
                quads.add(createQuad(v(1, o, 1 - o), v(1, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, o, 1 - o), spriteCable, hilight));
            } else if (east == BLOCK) {
                quads.add(createQuad(v(1 - p, 1 - o, 1 - o), v(1 - p, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, 1 - o), spriteCable, hilight));
                quads.add(createQuad(v(1 - p, o, o), v(1 - p, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, o, o), spriteCable, hilight));
                quads.add(createQuad(v(1 - p, 1 - o, o), v(1 - p, o, o), v(1 - o, o, o), v(1 - o, 1 - o, o), spriteCable, hilight));
                quads.add(createQuad(v(1 - p, o, 1 - o), v(1 - p, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, o, 1 - o), spriteCable, hilight));

                quads.add(createQuad(v(1 - p, 1 - q, 1 - q), v(1, 1 - q, 1 - q), v(1, 1 - q, q), v(1 - p, 1 - q, q), spriteSide, hilight));
                quads.add(createQuad(v(1 - p, q, q), v(1, q, q), v(1, q, 1 - q), v(1 - p, q, 1 - q), spriteSide, hilight));
                quads.add(createQuad(v(1 - p, 1 - q, q), v(1, 1 - q, q), v(1, q, q), v(1 - p, q, q), spriteSide, hilight));
                quads.add(createQuad(v(1 - p, q, 1 - q), v(1, q, 1 - q), v(1, 1 - q, 1 - q), v(1 - p, 1 - q, 1 - q), spriteSide, hilight));

                quads.add(createQuad(v(1 - p, q, 1 - q), v(1 - p, 1 - q, 1 - q), v(1 - p, 1 - q, q), v(1 - p, q, q), spriteConnector, hilight));
                quads.add(createQuad(v(1, q, 1 - q), v(1, q, q), v(1, 1 - q, q), v(1, 1 - q, 1 - q), spriteSide, hilight));
            } else {
                CablePatterns.QuadSetting pattern = CablePatterns.findPattern(down, north, up, south);
                quads.add(createQuad(v(1 - o, o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, 1 - o), v(1 - o, o, 1 - o), getSprite.apply(pattern.getSprite()), pattern.getRotation(), hilight));
            }

            if (west == CABLE) {
                quads.add(createQuad(v(o, 1 - o, 1 - o), v(o, 1 - o, o), v(0, 1 - o, o), v(0, 1 - o, 1 - o), spriteCable, hilight));
                quads.add(createQuad(v(o, o, o), v(o, o, 1 - o), v(0, o, 1 - o), v(0, o, o), spriteCable, hilight));
                quads.add(createQuad(v(o, 1 - o, o), v(o, o, o), v(0, o, o), v(0, 1 - o, o), spriteCable, hilight));
                quads.add(createQuad(v(o, o, 1 - o), v(o, 1 - o, 1 - o), v(0, 1 - o, 1 - o), v(0, o, 1 - o), spriteCable, hilight));
            } else if (west == BLOCK) {
                quads.add(createQuad(v(o, 1 - o, 1 - o), v(o, 1 - o, o), v(p, 1 - o, o), v(p, 1 - o, 1 - o), spriteCable, hilight));
                quads.add(createQuad(v(o, o, o), v(o, o, 1 - o), v(p, o, 1 - o), v(p, o, o), spriteCable, hilight));
                quads.add(createQuad(v(o, 1 - o, o), v(o, o, o), v(p, o, o), v(p, 1 - o, o), spriteCable, hilight));
                quads.add(createQuad(v(o, o, 1 - o), v(o, 1 - o, 1 - o), v(p, 1 - o, 1 - o), v(p, o, 1 - o), spriteCable, hilight));

                quads.add(createQuad(v(0, 1 - q, 1 - q), v(p, 1 - q, 1 - q), v(p, 1 - q, q), v(0, 1 - q, q), spriteSide, hilight));
                quads.add(createQuad(v(0, q, q), v(p, q, q), v(p, q, 1 - q), v(0, q, 1 - q), spriteSide, hilight));
                quads.add(createQuad(v(0, 1 - q, q), v(p, 1 - q, q), v(p, q, q), v(0, q, q), spriteSide, hilight));
                quads.add(createQuad(v(0, q, 1 - q), v(p, q, 1 - q), v(p, 1 - q, 1 - q), v(0, 1 - q, 1 - q), spriteSide, hilight));

                quads.add(createQuad(v(p, q, q), v(p, 1 - q, q), v(p, 1 - q, 1 - q), v(p, q, 1 - q), spriteConnector, hilight));
                quads.add(createQuad(v(0, q, q), v(0, q, 1 - q), v(0, 1 - q, 1 - q), v(0, 1 - q, q), spriteSide, hilight));
            } else {
                CablePatterns.QuadSetting pattern = CablePatterns.findPattern(down, south, up, north);
                quads.add(createQuad(v(o, o, 1 - o), v(o, 1 - o, 1 - o), v(o, 1 - o, o), v(o, o, o), getSprite.apply(pattern.getSprite()), pattern.getRotation(), hilight));
            }

            if (north == CABLE) {
                quads.add(createQuad(v(o, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, 0), v(o, 1 - o, 0), spriteCable, hilight));
                quads.add(createQuad(v(o, o, 0), v(1 - o, o, 0), v(1 - o, o, o), v(o, o, o), spriteCable, hilight));
                quads.add(createQuad(v(1 - o, o, 0), v(1 - o, 1 - o, 0), v(1 - o, 1 - o, o), v(1 - o, o, o), spriteCable, hilight));
                quads.add(createQuad(v(o, o, o), v(o, 1 - o, o), v(o, 1 - o, 0), v(o, o, 0), spriteCable, hilight));
            } else if (north == BLOCK) {
                quads.add(createQuad(v(o, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, 1 - o, p), v(o, 1 - o, p), spriteCable, hilight));
                quads.add(createQuad(v(o, o, p), v(1 - o, o, p), v(1 - o, o, o), v(o, o, o), spriteCable, hilight));
                quads.add(createQuad(v(1 - o, o, p), v(1 - o, 1 - o, p), v(1 - o, 1 - o, o), v(1 - o, o, o), spriteCable, hilight));
                quads.add(createQuad(v(o, o, o), v(o, 1 - o, o), v(o, 1 - o, p), v(o, o, p), spriteCable, hilight));

                quads.add(createQuad(v(q, 1 - q, p), v(1 - q, 1 - q, p), v(1 - q, 1 - q, 0), v(q, 1 - q, 0), spriteSide, hilight));
                quads.add(createQuad(v(q, q, 0), v(1 - q, q, 0), v(1 - q, q, p), v(q, q, p), spriteSide, hilight));
                quads.add(createQuad(v(1 - q, q, 0), v(1 - q, 1 - q, 0), v(1 - q, 1 - q, p), v(1 - q, q, p), spriteSide, hilight));
                quads.add(createQuad(v(q, q, p), v(q, 1 - q, p), v(q, 1 - q, 0), v(q, q, 0), spriteSide, hilight));

                quads.add(createQuad(v(q, q, p), v(1 - q, q, p), v(1 - q, 1 - q, p), v(q, 1 - q, p), spriteConnector, hilight));
                quads.add(createQuad(v(q, q, 0), v(q, 1 - q, 0), v(1 - q, 1 - q, 0), v(1 - q, q, 0), spriteSide, hilight));
            } else {
                CablePatterns.QuadSetting pattern = CablePatterns.findPattern(west, up, east, down);
                quads.add(createQuad(v(o, 1 - o, o), v(1 - o, 1 - o, o), v(1 - o, o, o), v(o, o, o), getSprite.apply(pattern.getSprite()), pattern.getRotation(), hilight));
            }

            if (south == CABLE) {
                quads.add(createQuad(v(o, 1 - o, 1), v(1 - o, 1 - o, 1), v(1 - o, 1 - o, 1 - o), v(o, 1 - o, 1 - o), spriteCable, hilight));
                quads.add(createQuad(v(o, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, o, 1), v(o, o, 1), spriteCable, hilight));
                quads.add(createQuad(v(1 - o, o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, 1), v(1 - o, o, 1), spriteCable, hilight));
                quads.add(createQuad(v(o, o, 1), v(o, 1 - o, 1), v(o, 1 - o, 1 - o), v(o, o, 1 - o), spriteCable, hilight));
            } else if (south == BLOCK) {
                quads.add(createQuad(v(o, 1 - o, 1 - p), v(1 - o, 1 - o, 1 - p), v(1 - o, 1 - o, 1 - o), v(o, 1 - o, 1 - o), spriteCable, hilight));
                quads.add(createQuad(v(o, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, o, 1 - p), v(o, o, 1 - p), spriteCable, hilight));
                quads.add(createQuad(v(1 - o, o, 1 - o), v(1 - o, 1 - o, 1 - o), v(1 - o, 1 - o, 1 - p), v(1 - o, o, 1 - p), spriteCable, hilight));
                quads.add(createQuad(v(o, o, 1 - p), v(o, 1 - o, 1 - p), v(o, 1 - o, 1 - o), v(o, o, 1 - o), spriteCable, hilight));

                quads.add(createQuad(v(q, 1 - q, 1), v(1 - q, 1 - q, 1), v(1 - q, 1 - q, 1 - p), v(q, 1 - q, 1 - p), spriteSide, hilight));
                quads.add(createQuad(v(q, q, 1 - p), v(1 - q, q, 1 - p), v(1 - q, q, 1), v(q, q, 1), spriteSide, hilight));
                quads.add(createQuad(v(1 - q, q, 1 - p), v(1 - q, 1 - q, 1 - p), v(1 - q, 1 - q, 1), v(1 - q, q, 1), spriteSide, hilight));
                quads.add(createQuad(v(q, q, 1), v(q, 1 - q, 1), v(q, 1 - q, 1 - p), v(q, q, 1 - p), spriteSide, hilight));

                quads.add(createQuad(v(q, 1 - q, 1 - p), v(1 - q, 1 - q, 1 - p), v(1 - q, q, 1 - p), v(q, q, 1 - p), spriteConnector, hilight));
                quads.add(createQuad(v(q, 1 - q, 1), v(q, q, 1), v(1 - q, q, 1), v(1 - q, 1 - q, 1), spriteSide, hilight));
            } else {
                CablePatterns.QuadSetting pattern = CablePatterns.findPattern(west, down, east, up);
                quads.add(createQuad(v(o, o, 1 - o), v(1 - o, o, 1 - o), v(1 - o, 1 - o, 1 - o), v(o, 1 - o, 1 - o), getSprite.apply(pattern.getSprite()), pattern.getRotation(), hilight));
            }
        }

        return quads;
    }


    @Nonnull
    @Override
    public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
        return new net.minecraft.client.resources.model.sprite.Material.Baked(getTexture(Identifier.fromNamespaceAndPath(XNet.MODID, "block/cable0/normal_netcable")), false);
    }


}
