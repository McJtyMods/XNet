package mcjty.xnet.compat;

import mcjty.lib.compat.theoneprobe.McJtyLibTOPDriver;
import mcjty.lib.compat.theoneprobe.TOPDriver;
import mcjty.lib.varia.Tools;
import mcjty.rftoolsbase.api.xnet.keys.ConsumerId;
import mcjty.rftoolsbase.api.xnet.keys.NetworkId;
import mcjty.theoneprobe.api.CompoundText;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.cables.blocks.ConnectorBlock;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.modules.cables.blocks.GenericCableBlock;
import mcjty.xnet.modules.controller.ControllerModule;
import mcjty.xnet.modules.controller.blocks.TileEntityController;
import mcjty.xnet.modules.facade.FacadeModule;
import mcjty.xnet.modules.router.RouterModule;
import mcjty.xnet.modules.router.blocks.TileEntityRouter;
import mcjty.xnet.modules.wireless.WirelessRouterModule;
import mcjty.xnet.modules.wireless.blocks.TileEntityWirelessRouter;
import mcjty.xnet.multiblock.BlobId;
import mcjty.xnet.multiblock.ColorId;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static mcjty.theoneprobe.api.TextStyleClass.ERROR;

public class XNetTOPDriver implements TOPDriver {

    public static final XNetTOPDriver DRIVER = new XNetTOPDriver();

    private final Map<Identifier, TOPDriver> drivers = new HashMap<>();

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level world, BlockState blockState, IProbeHitData data) {
        Block block = blockState.getBlock();
        Identifier id = Tools.getId(block);
        if (!drivers.containsKey(id)) {
            if (block == CableModule.NETCABLE.get() || block == FacadeModule.FACADE.get()) {
                drivers.put(id, new CableDriver());
            } else if (block instanceof ConnectorBlock) {
                drivers.put(id, new ConnectorDriver());
            } else if (block == ControllerModule.CONTROLLER.block().get()) {
                drivers.put(id, new ControllerDriver());
            } else if (block == RouterModule.ROUTER.block().get()) {
                drivers.put(id, new RouterDriver());
            } else if (block == WirelessRouterModule.WIRELESS_ROUTER.block().get()) {
                drivers.put(id, new WirelessRouterDriver());
            } else {
                drivers.put(id, new DefaultDriver());
            }
        }
        TOPDriver driver = drivers.get(id);
        if (driver != null) {
            driver.addProbeInfo(mode, probeInfo, player, world, blockState, data);
        }
    }

    private static class DefaultDriver implements TOPDriver {
        @Override
        public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level world, BlockState blockState, IProbeHitData data) {
            McJtyLibTOPDriver.DRIVER.addStandardProbeInfo(mode, probeInfo, player, world, blockState, data);
        }
    }

    private static class CableDriver extends DefaultDriver {
        @Override
        public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level world, BlockState blockState, IProbeHitData data) {
            super.addProbeInfo(mode, probeInfo, player, world, blockState, data);
            Tools.safeConsume(world.getBlockState(data.getPos()).getBlock(), (GenericCableBlock block) -> {
                WorldBlob worldBlob = XNetBlobData.get(world).getWorldBlob(world);

                if (mode == ProbeMode.DEBUG) {
                    BlobId blobId = worldBlob.getBlobAt(data.getPos());
                    if (blobId != null) {
                        probeInfo.text(CompoundText.createLabelInfo("Blob: ", blobId.id()));
                    }

                    ColorId colorId = worldBlob.getColorAt(data.getPos());
                    if (colorId != null) {
                        probeInfo.text(CompoundText.createLabelInfo("Color: ", colorId.id()));
                    }
                }

                Set<NetworkId> networks = worldBlob.getNetworksAt(data.getPos());
                for (NetworkId network : networks) {
                    if (mode == ProbeMode.DEBUG) {
                        probeInfo.text(CompoundText.createLabelInfo("Network: ", network.id() + ", V: " +
                                worldBlob.getNetworkVersion(network)));
                    } else {
                        probeInfo.text(CompoundText.createLabelInfo("Network: ", network.id()));
                    }
                }

                ConsumerId consumerId = worldBlob.getConsumerAt(data.getPos());
                if (consumerId != null) {
                    probeInfo.text(CompoundText.createLabelInfo("Consumer: ", consumerId.id()));
                }
            }, "Bad block!");
        }
    }

    private static class ConnectorDriver extends CableDriver {
        @Override
        public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level world, BlockState blockState, IProbeHitData data) {
            super.addProbeInfo(mode, probeInfo, player, world, blockState, data);
            Tools.safeConsume(world.getBlockEntity(data.getPos()), (ConnectorTileEntity te) -> {
                String name = te.getConnectorName();
                if (!name.isEmpty()) {
                    probeInfo.text(CompoundText.createLabelInfo("Name: ", name));
                }
            }, "Bad tile entity!");
        }
    }

    private static class ControllerDriver extends DefaultDriver {
        @Override
        public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level world, BlockState blockState, IProbeHitData data) {
            super.addProbeInfo(mode, probeInfo, player, world, blockState, data);
            Tools.safeConsume(world.getBlockEntity(data.getPos()), (TileEntityController te) -> {
                WorldBlob worldBlob = XNetBlobData.get(world).getWorldBlob(world);

                NetworkId networkId = te.getNetworkId();
                if (networkId != null) {
                    if (mode == ProbeMode.DEBUG) {
                        probeInfo.text(CompoundText.createLabelInfo("Network: ",networkId.id() + ", V: " +
                                worldBlob.getNetworkVersion(networkId)));
                    } else {
                        probeInfo.text(CompoundText.createLabelInfo("Network: ", networkId.id()));
                    }
                }

                if (mode == ProbeMode.DEBUG) {
                    String s = "";
                    for (NetworkId id : te.getNetworkChecker().get().getAffectedNetworks()) {
                        s += id.id() + " ";
                        if (s.length() > 15) {
                            probeInfo.text(CompoundText.createLabelInfo("InfNet: ", s));
                            s = "";
                        }
                    }
                    if (!s.isEmpty()) {
                        probeInfo.text(CompoundText.createLabelInfo("InfNet: ", s));
                    }
                }
                if (blockState.getValue(TileEntityController.ERROR)) {
                    probeInfo.text(CompoundText.create().style(ERROR).text("Too many controllers on network!"));
                }

                if (mode == ProbeMode.DEBUG) {
                    BlobId blobId = worldBlob.getBlobAt(data.getPos());
                    if (blobId != null) {
                        probeInfo.text(CompoundText.createLabelInfo("Blob: ", blobId.id()));
                    }
                    ColorId colorId = worldBlob.getColorAt(data.getPos());
                    if (colorId != null) {
                        probeInfo.text(CompoundText.createLabelInfo("Color: ", colorId.id()));
                    }

                    probeInfo.text(CompoundText.createLabelInfo("Color mask: ", te.getColors()));
                }
            }, "Bad tile entity!");
        }
    }

    private static class RouterDriver extends DefaultDriver {
        @Override
        public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level world, BlockState blockState, IProbeHitData data) {
            super.addProbeInfo(mode, probeInfo, player, world, blockState, data);
            Tools.safeConsume(world.getBlockEntity(data.getPos()), (TileEntityRouter te) -> {
                XNetBlobData blobData = XNetBlobData.get(world);
                WorldBlob worldBlob = blobData.getWorldBlob(world);
                Set<NetworkId> networks = worldBlob.getNetworksAt(data.getPos());
                for (NetworkId networkId : networks) {
                    probeInfo.text(CompoundText.createLabelInfo("Network: ", networkId.id()));
                    if (mode != ProbeMode.EXTENDED) {
                        break;
                    }
                }
                if (blockState.getValue(TileEntityController.ERROR)) {
                    probeInfo.text(CompoundText.create().style(ERROR).text("Too many channels on router!"));
                } else {
                    probeInfo.text(CompoundText.createLabelInfo("Channels: ", te.getChannelCount()));
                }

                if (mode == ProbeMode.DEBUG) {
                    BlobId blobId = worldBlob.getBlobAt(data.getPos());
                    if (blobId != null) {
                        probeInfo.text(CompoundText.createLabelInfo("Blob: ", blobId.id()));
                    }
                    ColorId colorId = worldBlob.getColorAt(data.getPos());
                    if (colorId != null) {
                        probeInfo.text(CompoundText.createLabelInfo("Color: ", colorId.id()));
                    }
                }
            }, "Bad tile entity!");
        }
    }

    private static class WirelessRouterDriver extends DefaultDriver {
        @Override
        public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level world, BlockState blockState, IProbeHitData data) {
            super.addProbeInfo(mode, probeInfo, player, world, blockState, data);
            Tools.safeConsume(world.getBlockEntity(data.getPos()), (TileEntityWirelessRouter te) -> {
                XNetBlobData blobData = XNetBlobData.get(world);
                WorldBlob worldBlob = blobData.getWorldBlob(world);
                Set<NetworkId> networks = worldBlob.getNetworksAt(data.getPos());
                for (NetworkId networkId : networks) {
                    probeInfo.text(CompoundText.createLabelInfo("Network: ", networkId.id()));
                    if (mode != ProbeMode.EXTENDED) {
                        break;
                    }
                }
                if (blockState.getValue(TileEntityController.ERROR)) {
                    probeInfo.text(CompoundText.create().style(ERROR).text("Missing antenna!"));
                } else {
//            probeInfo.text(new StringTextComponent(TextStyleClass.LABEL + "Channels: " + TextStyleClass.INFO + getChannelCount()));
                }

                if (mode == ProbeMode.DEBUG) {
                    BlobId blobId = worldBlob.getBlobAt(data.getPos());
                    if (blobId != null) {
                        probeInfo.text(CompoundText.createLabelInfo("Blob: ", blobId.id()));
                    }
                    ColorId colorId = worldBlob.getColorAt(data.getPos());
                    if (colorId != null) {
                        probeInfo.text(CompoundText.createLabelInfo("Color: ", colorId.id()));
                    }
                }
            }, "Bad tile entity!");
        }
    }
}
