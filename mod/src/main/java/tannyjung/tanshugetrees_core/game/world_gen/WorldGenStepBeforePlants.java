package tannyjung.tanshugetrees_core.game.world_gen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import tannyjung.tanshugetrees_core.Core;
import tannyjung.tanshugetrees_core.game.GameUtils;
import tannyjung.tanshugetrees_handcode.systems.world_gen.WorldGen;

public class WorldGenStepBeforePlants extends Feature <NoneFeatureConfiguration> {

    public WorldGenStepBeforePlants() {

        super(NoneFeatureConfiguration.CODEC);
        // [诊断] 确认 Feature 是否被注册实例化
        if (Core.log_world_gen_step) System.out.println("[THT-DEBUG] WorldGenStepBeforePlants constructor called - Feature registered!");

    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {

        // [诊断] 确认 MC 是否调用了 place()
        if (Core.log_world_gen_step) System.out.println("[THT-DEBUG] WorldGenStepBeforePlants.place() called! Origin: " + context.origin());

        LevelAccessor level_accessor = context.level();
        ServerLevel level_server = context.level().getLevel();
        ChunkGenerator chunk_generator = context.chunkGenerator();
        String dimension = GameUtils.Space.getDimensionID(level_server).replace(":", "-");
        ChunkPos chunk_pos = new ChunkPos(context.origin());

        // [MODS-CONFIG] 维度白/黑名单最先判定: 不在列表里的维度直接 return, 连树数据都不去读盘.
        // 这是世界生成的最外层入口(Forge biome_modifier 对每个新生区块调用), 挡在这里最省事也最彻底.
        if (tannyjung.tanshugetrees_handcode.Handcode.Config.isDimensionAllowed(dimension) == false) {

            once_notice_dimension(dimension);
            return false;

        }

        WorldGen.stepBeforePlants(level_accessor, level_server, chunk_generator, dimension, chunk_pos);
        return true;


    }

    // 被过滤掉的维度只提示一次 (每个世界一次), 避免每区块刷日志
    private static final java.util.Set<String> noticed_dimensions = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void once_notice_dimension (String dimension) {

        if (noticed_dimensions.add(dimension) == true && Core.log_world_gen_step == true) {

            System.out.println("[THT] World generation skipped in dimension '" + dimension + "' (filter: " + tannyjung.tanshugetrees_handcode.Handcode.Config.describeDimensionFilter() + ")");

        }

    }

}
