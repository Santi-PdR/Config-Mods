package tannyjung.tanshugetrees_core.game;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import tannyjung.tanshugetrees_core.Core;
import tannyjung.tanshugetrees_core.game.GameUtils;
import tannyjung.tanshugetrees_core.outside.OutsideUtils;
import tannyjung.tanshugetrees_core.outside.TannyPackManager;
import tannyjung.tanshugetrees_core.outside.TXTFunction;

import java.util.function.Consumer;

/*
(1.20.1)
import net.minecraftforge.event.RegisterCommandsEvent;
(1.21.1)
import net.neoforged.neoforge.event.RegisterCommandsEvent;
*/
import net.minecraftforge.event.RegisterCommandsEvent;

public class CommandMaker {

    public static void create (Object event_object, int permission, String structure, Consumer<CommandContext<CommandSourceStack>> consumer) {

        RegisterCommandsEvent event = (RegisterCommandsEvent) event_object;
        String[] split = structure.split(" / ");
        String structure_short = "";

        // Convert
        {

            StringBuilder convert = new StringBuilder();

            for (String get : split) {

                convert.append("/");

                if (get.startsWith("<") == false) {

                    convert.append("#");

                } else {

                    {

                        if (get.equals("<number>") == true) {

                            convert.append("N");

                        } else if (get.equals("<text>") == true) {

                            convert.append("T");

                        }

                    }

                }

            }

            structure_short = convert.substring(1);

        }

        // Registry
        {

            if (structure_short.equals("#/#") == true) {

                {

                    event.getDispatcher().register(Commands.literal(split[0]).requires(test -> test.hasPermission(0)).then(Commands.literal(split[1]).executes(arguments -> {

                        if (CommandMaker.testPermission(arguments, permission) == true) {

                            consumer.accept(arguments);

                        }

                        return 0;

                    })));

                }

            } else if (structure_short.equals("#/#/#") == true) {

                {

                    event.getDispatcher().register(Commands.literal(split[0]).requires(test -> test.hasPermission(0)).then(Commands.literal(split[1]).then(Commands.literal(split[2]).executes(arguments -> {

                        if (CommandMaker.testPermission(arguments, permission) == true) {

                            consumer.accept(arguments);

                        }

                        return 0;

                    }))));

                }

            } else if (structure_short.equals("#/#/#/#") == true) {

                {

                    event.getDispatcher().register(Commands.literal(split[0]).requires(test -> test.hasPermission(0)).then(Commands.literal(split[1]).then(Commands.literal(split[2]).then(Commands.literal(split[3]).executes(arguments -> {

                        if (CommandMaker.testPermission(arguments, permission) == true) {

                            consumer.accept(arguments);

                        }

                        return 0;

                    })))));

                }

            } else if (structure_short.equals("#/#/#/#/#") == true) {

                {

                    event.getDispatcher().register(Commands.literal(split[0]).requires(test -> test.hasPermission(0)).then(Commands.literal(split[1]).then(Commands.literal(split[2]).then(Commands.literal(split[3]).then(Commands.literal(split[4]).executes(arguments -> {

                        if (CommandMaker.testPermission(arguments, permission) == true) {

                            consumer.accept(arguments);

                        }

                        return 0;

                    }))))));

                }

            } else if (structure_short.equals("#/#/#/#/N") == true) {

                {

                    event.getDispatcher().register(Commands.literal(split[0]).requires(s -> s.hasPermission(0)).then(Commands.literal(split[1]).then(Commands.literal(split[2]).then(Commands.literal(split[3]).then(Commands.argument("number", DoubleArgumentType.doubleArg()).executes(arguments -> {

                        if (CommandMaker.testPermission(arguments, permission) == true) {

                            consumer.accept(arguments);

                        }

                        return 0;

                    }))))));

                }

            } else if (structure_short.equals("#/#/#/T") == true) {

                {

                    event.getDispatcher().register(Commands.literal(split[0]).requires(s -> s.hasPermission(0)).then(Commands.literal(split[1]).then(Commands.literal(split[2]).then(Commands.argument("text", MessageArgument.message()).executes(arguments -> {

                        if (CommandMaker.testPermission(arguments, permission) == true) {

                            consumer.accept(arguments);

                        }

                        return 0;

                    })))));

                }

            }

        }

    }

    public static class Argument {

        public static int getNumber (CommandContext<CommandSourceStack> data) {

            return (int) DoubleArgumentType.getDouble(data, "number");

        }

        public static String getText (CommandContext<CommandSourceStack> data) {

            String return_text = "";

            return_text = (new Object() {

                public String get () {

                    try {

                        return MessageArgument.getMessage(data, "text").getString();

                    } catch (Exception exception) {

                        OutsideUtils.exception(new Exception(), exception, "");
                        return "";

                    }

                }

            }).get();

            return return_text;

        }

    }

    public static boolean testPermission (CommandContext<CommandSourceStack> data, int permission) {

        if (data.getSource().getEntity() instanceof Player player) {

            if (player.hasPermissions(permission) == false) {

                GameUtils.Misc.sendChatMessagePrivate(player, "You must have server permission minimum level " + permission + " to use this command. If you're in singleplayer, try enable cheat mode or temporary open LAN. If you're in multiplayer, try give yourself OP or contact server admin. / red");
                return false;

            }

        }

        return true;

    }

    public static class BuiltinCommands {

        public static void registry (Object event_object) {

            CommandMaker.create(event_object, 2, Core.mod_id_big + " / command / txt_function / <text>", run.command::txt_function);
            CommandMaker.create(event_object, 2, Core.mod_id_big + " / pack / check_update_main", run.pack::check_update_main);
            CommandMaker.create(event_object, 2, Core.mod_id_big + " / pack / update_main", run.pack::update_main);
            CommandMaker.create(event_object, 2, Core.mod_id_big + " / restart", run::restart);
            // [MODS-CONFIG] 维度白/黑名单: 列出服务器维度 + 不重启就重读配置
            CommandMaker.create(event_object, 2, Core.mod_id_big + " / dimensions_list", run::dimensions_list);
            CommandMaker.create(event_object, 2, Core.mod_id_big + " / dimensions_reload", run::dimensions_reload);

        }

        private static class run {

            private static class command {

                private static void txt_function (CommandContext<CommandSourceStack> data) {

                    LevelAccessor level_accessor = data.getSource().getLevel();
                    ServerLevel level_server = data.getSource().getLevel();
                    BlockPos pos = new BlockPos((int) Math.floor(data.getSource().getPosition().x()), (int) Math.floor(data.getSource().getPosition().y()), (int) Math.floor(data.getSource().getPosition().z()));
                    String variable_text = Argument.getText(data);
                    TXTFunction.run(level_accessor, level_server, pos, variable_text, true);

                }

            }

            private static class pack {

                private static void check_update_main (CommandContext<CommandSourceStack> data) {

                    ServerLevel level_server = data.getSource().getLevel();

                    Core.thread_main.submit(() -> {

                        TannyPackManager.runCheckUpdate(level_server);

                    });

                }

                private static void update_main (CommandContext<CommandSourceStack> data) {

                    ServerLevel level_server = data.getSource().getLevel();

                    Core.thread_main.submit(() -> {

                        TannyPackManager.runUpdate(level_server);

                    });

                }

            }

            private static void restart (CommandContext<CommandSourceStack> data) {

                ServerLevel level_server = data.getSource().getLevel();
                Core.restart(level_server, true, true);

            }

            private static void dimensions_list (CommandContext<CommandSourceStack> data) {

                CommandSourceStack source = data.getSource();
                java.util.List<String> dimensions = tannyjung.tanshugetrees_handcode.Handcode.Config.listServerDimensions(source.getServer());
                String filter = tannyjung.tanshugetrees_handcode.Handcode.Config.describeDimensionFilter();
                source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("[" + Core.mod_name + "] dimension filter: " + filter), false);

                for (String line : dimensions) {

                    source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(" - " + line), false);

                }

            }

            private static void dimensions_reload (CommandContext<CommandSourceStack> data) {

                ServerLevel level_server = data.getSource().getLevel();
                tannyjung.tanshugetrees_handcode.Handcode.Config.reloadDimensions();
                String filter = tannyjung.tanshugetrees_handcode.Handcode.Config.describeDimensionFilter();
                GameUtils.Misc.sendChatMessage(level_server, "Dimension filter reloaded: " + filter + " / gray");
                Core.logger.info("[THT] Dimension filter reloaded: {}", filter);

            }

        }

    }

}
