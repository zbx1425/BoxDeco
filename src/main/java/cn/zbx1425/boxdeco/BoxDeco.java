package cn.zbx1425.boxdeco;

import cn.zbx1425.boxdeco.block.GlassRailingBlock;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(BoxDeco.MODID)
public class BoxDeco {

    public static final String MODID = "boxdeco";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<GlassRailingBlock> BLOCK_GLASS_RAILING = BLOCKS.registerBlock(
        "glass_railing", GlassRailingBlock::new);
    public static final DeferredItem<BlockItem> ITEM_BLOCK_GLASS_RAILING = ITEMS.registerSimpleBlockItem(
        "glass_railing", BLOCK_GLASS_RAILING);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_BOXDECO = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.boxdeco"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ITEM_BLOCK_GLASS_RAILING.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ITEM_BLOCK_GLASS_RAILING.get());
            }).build());


    public BoxDeco(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onCommonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("HELLO FROM UNCOMMON SETUP");
        LOGGER.info("HELLO FROM RARE SETUP");
        LOGGER.info("HELLO FROM EPIC SETUP");
        LOGGER.info("HELLO FROM LEGENDARY SETUP");
        LOGGER.info("Note that legendary quality represents a 2-tier improvement over epic.");
    }
}
