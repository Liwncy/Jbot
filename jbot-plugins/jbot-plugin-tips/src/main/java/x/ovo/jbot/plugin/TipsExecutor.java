package x.ovo.jbot.plugin;

import picocli.CommandLine;
import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.plugin.Plugin;

import java.util.concurrent.Callable;

@SuppressWarnings("unused")
@CommandLine.Command(name = "tips", description = "群成员进群提示")
public class TipsExecutor extends CommandExecutor implements Callable<String> {

    @CommandLine.Parameters(index = "0", description = "操作方式：[add|edit|del]", defaultValue = CommandLine.Option.NULL_VALUE)
    private String action;
    @CommandLine.Option(names = {"-m", "--member"}, description = "群成员", defaultValue = CommandLine.Option.NULL_VALUE)
    private String member;
    @CommandLine.Option(names = {"-t", "--tip"}, description = "提示", defaultValue = CommandLine.Option.NULL_VALUE)
    private String tip;

    public TipsExecutor(Plugin plugin) {
        super(plugin);
    }

    @Override
    public String call() throws Exception {
        return switch (action) {
            case "add" -> {
                this.plugin.getConfig().put(member, tip);
                this.plugin.saveConfig();
                yield "添加成功";
            }
            case "edit" -> {
                this.plugin.getConfig().put(member, tip);
                this.plugin.saveConfig();
                yield "修改成功";
            }
            case "del" -> {
                this.plugin.getConfig().remove(member);
                this.plugin.saveConfig();
                yield "删除成功";
            }
            default -> "操作方式错误";
        };
    }
}
