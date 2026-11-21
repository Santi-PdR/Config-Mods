package tannyjung.tanshugetrees_handcode.systems.tree_generator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import tannyjung.tanshugetrees_core.Core;
import tannyjung.tanshugetrees_handcode.Handcode;
import tannyjung.tanshugetrees_core.game.GameUtils;
import tannyjung.tanshugetrees.init.TanshugetreesModBlocks;
import tannyjung.tanshugetrees_core.outside.FileManager;
import tannyjung.tanshugetrees_core.outside.LogLimiter;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class Sapling {

    /**
     * [MODS-CONFIG][SAPLING FIX] 让树苗能被"自然"启动 (bone meal 之外的第二条路).
     *
     * 旧逻辑: 只有对着树苗用骨粉, 并且 (创造模式 or 5% 概率) 才会把 sapling_started 置 true.
     * 单人/服务器里玩家放了树苗跑去干别的, 它就永远停在那里 —— 这就是"树苗卡住不长"的直接原因.
     *
     * 现在: CustomSaplingBlock 每 20 tick 调 tick(), tick 里按 config 的 sapling_auto_grow_chance
     * 掷骰; 命中就走和骨粉完全相同的启动路径 (解析 preset -> 写 path -> 置 sapling_started).
     * 骨粉逻辑保持原样 (仍然是"催熟", 非创造 5% 一次).
     */
    public static void autoStart (LevelAccessor level_accessor, BlockPos pos) {

        if (level_accessor instanceof ServerLevel level_server) {

            if (GameUtils.Data.getBlockLogic(level_accessor, pos, "sapling_started") == true) {

                return;

            }

            if (Handcode.Config.sapling_auto_grow_chance <= 0.0) {

                return;

            }

            if (Math.random() >= Handcode.Config.sapling_auto_grow_chance) {

                return;

            }

            String path = GameUtils.Data.getBlockText(level_accessor, pos, "path");

            if (path.isEmpty() == true) {

                path = resolvePresetPath(level_accessor, pos);

            }

            if (path.isEmpty() == true) {

                // 没有对应 preset (纯装饰方块) 就别管它, 也让 config 的几率无从作用
                return;

            }

            GameUtils.Data.setBlockText(level_accessor, level_server, pos, "path", path);
            // 创造模式同款: countdown = -1 表示"下一 tick 立刻生成"
            GameUtils.Data.setBlockNumber(level_accessor, level_server, pos, "countdown", -1);
            GameUtils.Data.setBlockLogic(level_accessor, level_server, pos, "sapling_started", true);

        }

    }

    public static void click (LevelAccessor level_accessor, Entity entity, BlockPos pos) {

        if (level_accessor instanceof ServerLevel level_server) {

            if (GameUtils.Data.getBlockLogic(level_accessor, pos, "sapling_started") == false) {

                if (GameUtils.Item.getSlot(entity, EquipmentSlot.MAINHAND).is(Items.BONE_MEAL) == true) {

                    String error = "";
                    boolean creative_mode = GameUtils.Mob.isCreativeMode(entity);
                    GameUtils.Misc.spawnParticle(level_server, pos.getCenter(), 0, 0, 0, 0, 10, "minecraft:happy_villager");

                    if (creative_mode == false) {

                        GameUtils.Item.addCount(entity, EquipmentSlot.MAINHAND, -1);
                        GameUtils.Data.addBlockNumber(level_accessor, level_server, pos, "bone_meal_usage", 1);

                    }

                    if (creative_mode == true || Math.random() < 0.05) {

                        if (level_accessor.getBlockState(pos).getBlock() == TanshugetreesModBlocks.CUSTOM_SAPLING.get()) {

                            // Custom Sapling
                            {

                                if (GameUtils.Data.getBlockText(level_accessor, pos, "path").isEmpty() == true) {

                                    if (GameUtils.Data.getBlockNumber(level_accessor, pos, "tree_generator_speed_tick") == 0) {

                                        error = "Empty Sapling Preset";

                                    }

                                }

                            }

                        } else {

                            // General
                            {

                                String path = resolvePresetPath(level_accessor, pos);

                                if (path.isEmpty() == true) {

                                    error = "Sapling Not Available Yet";

                                } else {

                                    GameUtils.Data.setBlockText(level_accessor, level_server, pos, "path", path);

                                }

                            }

                        }

                        if (error.isEmpty() == false) {

                            // Error
                            {

                                for (int test = (int) GameUtils.Data.getBlockNumber(level_accessor, pos, "bone_meal_usage"); test > 0; test--) {

                                    GameUtils.Mob.summon(level_server, pos.getCenter(), "minecraft:item", "", "", "{Item:{id:\"minecraft:bone_meal\",Count:1b}}");

                                }

                                GameUtils.Data.setBlockNumber(level_accessor, level_server, pos, "bone_meal_usage", 0);

                                // Display Text
                                {

                                    GameUtils.Mob.summon(level_server, pos.getCenter().add(0, 0.75, 0), "minecraft:text_display", "Sapling Error", "TANSHUGETREES-sapling_error", "{see_through:1b,alignment:\"left\",brightness:{block:15, sky:15},line_width:1000,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]},billboard:vertical,text:'{\"text\":\"" + error + "\",\"color\":\"red\"}'}");

                                    Core.DelayedWork.create(false, 200, () -> {

                                        for (Entity entity_import : GameUtils.Mob.getAtArea(level_server, pos.getCenter().add(0, 0.75, 0), 1, true, 0, "minecraft:text_display", "TANSHUGETREES-sapling_error")) {

                                            entity_import.discard();
                                            
                                        }
                                        
                                    });

                                }

                            }

                        } else {

                            if (creative_mode == true) {

                                GameUtils.Data.setBlockNumber(level_accessor, level_server, pos, "countdown", -1);

                            }

                            GameUtils.Data.setBlockLogic(level_accessor, level_server, pos, "sapling_started", true);
                            GameUtils.Mob.summon(level_server, pos.getCenter().add(0, 0.75, 0), "minecraft:text_display", "Sapling Countdown", "TANSHUGETREES-sapling_countdown", "{see_through:1b,alignment:\"left\",brightness:{block:15, sky:15},line_width:1000,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1f,1f,1f]},billboard:vertical}");

                        }

                    }

                }

            }

        }

    }

    public static void tick (LevelAccessor level_accessor, BlockPos pos) {

        if (level_accessor instanceof ServerLevel level_server) {

            // [MODS-CONFIG][SAPLING FIX] 存档救援: 旧版本用骨粉启动时先写 path, 再写 sapling_started=true,
            // 而写 sapling_started 会把整个 mod compound 顶掉 (见 GameUtils.setBlockLogic 修复) ->
            // path 丢失, tick 认为已启动却没有 preset, 于是召唤 marker 后删方块, 树永久消失.
            // 这里在 tick 里补一次: 已启动但 path 为空 -> 重新按方块名解析, 找到就补回 path
            // (下一段照常走 countdown), 找不到才撤销状态, 让它安静待着等骨粉.
            if (GameUtils.Data.getBlockLogic(level_accessor, pos, "sapling_started") == true
                    && GameUtils.Data.getBlockText(level_accessor, pos, "path").isEmpty() == true) {

                String recovered = resolvePresetPath(level_accessor, pos);

                if (recovered.isEmpty() == false) {

                    GameUtils.Data.setBlockText(level_accessor, level_server, pos, "path", recovered);

                    if (GameUtils.Data.getBlockNumber(level_accessor, pos, "countdown") < 0) {

                        // 之前已经数到 0 了: 下一 tick 立刻开始生成
                        GameUtils.Data.setBlockNumber(level_accessor, level_server, pos, "countdown", 0);

                    }

                } else {

                    // 找不到 preset: 撤销"已启动"状态, 让方块安静待着 (骨粉会照常报 "Sapling Not Available Yet"),
                    // 千万不要在没数据的情况下召唤 tree_generator marker —— 那个 marker 会被 Loops 每 tick
                    // 重跑一次并反复报错, 正是服务器 console 被刷屏的来源.
                    GameUtils.Data.setBlockLogic(level_accessor, level_server, pos, "sapling_started", false);
                    GameUtils.Data.setBlockNumber(level_accessor, level_server, pos, "countdown", 0);

                }

            }

            if (GameUtils.Data.getBlockLogic(level_accessor, pos, "sapling_started") == true) {

                int countdown = (int) GameUtils.Data.getBlockNumber(level_accessor, pos, "countdown");

                if (countdown >= 0) {

                    // Update Countdown Number
                    {

                        for (Entity entity_import : GameUtils.Mob.getAtArea(level_server, pos.getCenter(), 1, true, 0, "minecraft:text_display", "TANSHUGETREES-sapling_countdown")) {

                            GameUtils.Command.runEntity(entity_import, "data merge entity @s {text:'{\"text\":\"" + countdown + "\",\"color\":\"red\"}'}");

                        }

                    }

                    // When Break Sapling
                    {

                        Core.DelayedWork.create(false, 20, () -> {

                            for (Entity entity_import : GameUtils.Mob.getAtArea(level_server, pos.getCenter(), 1, true, 0, "minecraft:text_display", "TANSHUGETREES-sapling_countdown")) {

                                entity_import.discard();

                            }

                        });

                    }

                    GameUtils.Data.addBlockNumber(level_accessor, level_server, pos, "countdown", -1);

                } else {

                    // End
                    {

                        for (Entity entity_import : GameUtils.Mob.getAtArea(level_server, pos.getCenter(), 1, true, 0, "minecraft:text_display", "TANSHUGETREES-sapling_countdown")) {

                            entity_import.discard();

                        }

                        if (GameUtils.Data.getBlockText(level_accessor, pos, "path").isEmpty() == false) {

                            TreeGenerator.create(level_accessor, level_server, null, pos, GameUtils.Data.getBlockText(level_accessor, pos, "path"));

                        } else {

                            // [MODS-CONFIG] 真找不到 preset 时, 不要留下一个永远不动的 marker (它会让
                            // Loops 每 tick 再跑一次 TreeGenerator.run, 该坐标于是反复报错刷 console).
                            // 只提示一次, 然后把方块换成空气, 避免残留.
                            Entity entity_summon = GameUtils.Mob.summon(level_server, pos.getCenter(), "minecraft:marker", "Tree Generator", "TANSHUGETREES-tree_generator", "");

                            if (entity_summon == null) {

                                return;

                            }

                            GameUtils.Command.runEntity(entity_summon, "data modify entity @s ForgeData.tanshugetrees set from block ~ ~ ~ ForgeData.tanshugetrees");

                            if (no_preset_notified.add(pos.toString()) == true) {

                                Core.logger.warn("[THT] Sapling at " + pos + " has no preset path and no matching preset file. Check config/tanshugetrees/dev/temporary/presets or run /" + Core.mod_id_big + " restart.");

                            }

                        }

                        level_accessor.removeBlock(new BlockPos(pos), false);

                    }

                }

            }

        }

    }

    /**
     * [MODS-CONFIG] 找到这个 sapling 方块对应的 preset 路径.
     *
     * 旧实现写死扫 "dev/temporary/presets/#main/<arbol>" (1.5.x 的目录结构), 但内置主包 1.8.0
     * 实际把 preset 放在 "presets/<arbol>/<arbol>.txt" (见 #main.zip), 于是每个 sapling 都匹配不到
     * → bone meal 报 "Sapling Not Available Yet" → sapling_started 永不为 true → CustomSapling 的
     * tick 每 20 tick 空转一次, 表现就是"sapling 卡住不长".
     *
     * 现在: 在全部 preset 文件里按文件名匹配 (不分目录层级), 同时兼容旧的 #main 结构与
     * "custom_packs" 里用户自己解压的包. 匹配到多个时优先路径更短的 (顶层预设优先).
     */
    // 同一个坐标只提示一次 (有容量上限, 长期跑图不会吃内存)
    private static final LogLimiter.BoundedSet no_preset_notified = new LogLimiter.BoundedSet(1024);

    // [MODS-CONFIG] 方块名 -> preset 路径缓存 (每个方块名只扫一次盘; preset 数量是固定的)
    private static final Map<String, String> preset_path_cache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Set<String> preset_path_missing = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static String resolvePresetPath (LevelAccessor level_accessor, BlockPos pos) {

        String name = GameUtils.Tile.toText(level_accessor.getBlockState(pos))[0];

        if (name.startsWith("tanshugetrees:sapling_") == false) {

            return "";

        }

        name = name.substring("tanshugetrees:sapling_".length());

        if (preset_path_cache.containsKey(name) == true) {

            return preset_path_cache.get(name);

        }

        if (preset_path_missing.contains(name) == true) {

            return "";

        }

        String resolved = scanPresetPath(name);

        if (resolved.isEmpty() == true) {

            preset_path_missing.add(name);

        } else {

            preset_path_cache.put(name, resolved);

        }

        return resolved;

    }

    private static String scanPresetPath (String name) {

        String relative = "";

        // 1) 主包解压目录 (dev/temporary) — 1.8.0 实际布局 presets/<nombre>/<nombre>.txt
        {

            String root = Core.path_config + "/dev/temporary";
            File best = null;
            String best_rel = "";

            // 目录不存在时 getAllFiles 返回空表 (异常已由 OutsideUtils 限流输出)
            for (File scan : FileManager.getAllFiles(root + "/presets")) {

                if (scan.getName().equals(name + ".txt") == false) {

                    continue;

                }

                String candidate = Path.of(root).relativize(scan.toPath()).toString().replace("\\", "/");
                candidate = candidate.substring(0, candidate.length() - ".txt".length());

                if (best == null || candidate.length() < best_rel.length()) {

                    best = scan;
                    best_rel = candidate;

                }

            }

            if (best != null) {

                relative = best_rel;

            }

        }

        // 2) 用户自定义包目录 (custom_packs) — TreeGenerator.create 会优先查这里
        if (relative.isEmpty() == true) {

            String root = Core.path_config + "/custom_packs";
            File best = null;
            String best_rel = "";

            for (File scan : FileManager.getAllFiles(root)) {

                if (scan.getName().equals(name + ".txt") == false) {

                    continue;

                }

                String candidate = Path.of(root).relativize(scan.toPath()).toString().replace("\\", "/");
                candidate = candidate.substring(0, candidate.length() - ".txt".length());

                if (candidate.startsWith("presets/") == false) {

                    continue;

                }

                if (best == null || candidate.length() < best_rel.length()) {

                    best = scan;
                    best_rel = candidate;

                }

            }

            if (best != null) {

                relative = best_rel;

            }

        }

        return relative;

    }

}
