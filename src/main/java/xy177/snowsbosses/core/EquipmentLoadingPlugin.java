package xy177.snowsbosses.core;

import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name("Snow Bosses Equipment")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1100)
@IFMLLoadingPlugin.TransformerExclusions("xy177.snowsbosses.core")
public final class EquipmentLoadingPlugin implements IFMLLoadingPlugin {
    @Override public String[] getASMTransformerClass() { return new String[]{EquipmentTransformer.class.getName(), InnerVoiceTransformer.class.getName(), CoralTridentTransformer.class.getName()}; }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public String getAccessTransformerClass() { return null; }
    @Override public void injectData(Map<String,Object> data) { }
}
