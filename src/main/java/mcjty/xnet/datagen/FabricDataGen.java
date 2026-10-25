package mcjty.xnet.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class FabricDataGen implements DataGeneratorEntrypoint {
    @Override public void onInitializeDataGenerator(FabricDataGenerator generator) {
        var pack = generator.createPack();
        pack.addProvider((output, lookup) -> {
            var data = new mcjty.lib.datagen.DataGen(output, lookup);
            mcjty.xnet.XNet.instance.modules.datagen(data, lookup);
            data.generate();
            return data;
        });
        pack.addProvider((output, lookup) -> new mcjty.rftoolsbase.tools.ClientItemDefinitions(output, "xnet", java.util.Map.of()));
    }
}
