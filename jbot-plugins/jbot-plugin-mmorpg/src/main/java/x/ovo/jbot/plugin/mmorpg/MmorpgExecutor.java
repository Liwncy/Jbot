package x.ovo.jbot.plugin.mmorpg;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.map.MapUtil;
import picocli.CommandLine;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.plugin.Plugin;
import x.ovo.jbot.plugin.mmorpg.util.MySQLClient;

/**
 * Mmorpg 命令执行器
 *
 * @author liwncy on 2025/03/26.
 * @since 1.0.0
 */
@SuppressWarnings("unused")
@Slf4j(topic = "MmorpgExecutor")
@CommandLine.Command(name = "mmorpg", description = "Mmorpg设置")
public class MmorpgExecutor extends CommandExecutor {

    public MmorpgExecutor(Plugin plugin) {
        super(plugin);
    }

    // @Override
    // public boolean hasPermission(String user) {
    //     return true;
    // }

    @CommandLine.Command(name = "help", description = "显示插件帮助信息")
    public String help() {
        return MapUtil.getStr(this.plugin.getConfig().getMap(), "help");
    }

    @CommandLine.Command(name = "start", description = "开始")
    public String start() {
        System.out.println("start");
        MySQLClient client = MySQLClient.getInstance();
        client.query("select * from user")
        .onComplete(ar -> {
            if (ar.succeeded()) {
                RowSet<Row> result = ar.result();
                System.out.println("Got " + result.size() + " rows ");
            } else {
                System.out.println("Failure: " + ar.cause().getMessage());
            }

            // Now close the pool
            // client.close();
        });
        return MapUtil.getStr(this.plugin.getConfig().getMap(), "help");
    }
}
