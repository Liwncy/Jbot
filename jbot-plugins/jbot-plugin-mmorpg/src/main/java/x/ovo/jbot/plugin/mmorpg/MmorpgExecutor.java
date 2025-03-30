package x.ovo.jbot.plugin.mmorpg;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.map.MapUtil;
import picocli.CommandLine;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.contact.Member;
import x.ovo.jbot.core.plugin.Plugin;
import x.ovo.jbot.plugin.mmorpg.util.MySQLClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        Collection<Member> members = Context.get().getAdapter().getGroupService().getMembers(this.command.getMessage().getSender().getId()).await();
        MySQLClient client = MySQLClient.getInstance();
        client.query("select wx_id from player where 1=1")
                .onComplete(ar -> {
                    if (ar.succeeded()) {

                    } else {
                        System.out.println("Failure: " + ar.cause().getMessage());
                    }
                }).onSuccess(rows -> {
                    List<String> wxIds = new ArrayList<>();
                    for (Row row : rows) {
                        wxIds.add(row.getString("wx_id"));
                    }
                    List<Tuple> batch = members.stream().filter(member -> !wxIds.contains(member.getId())).map(member -> Tuple.of(member.getId(), member.getNickname())).toList();
                    client.getPool()
                            .preparedQuery("insert into player (wx_id, nickname) values (?, ?)")
                            .executeBatch(batch)
                            .onComplete(res -> {
                                if (res.succeeded()) {
                                    // Process rows
//                                    RowSet<Row> rows = res.result();
                                } else {
                                    System.out.println("Batch failed " + res.cause());
                                }
                            });
                });
        return "欢迎"+members.size()+"名仙友踏入芈仙世界。\n" +
                "此间十洲浮沉，皆由你指尖灵枢流转。\n" +
                "观此青铜司南，北斗所指之处，即是你的道途——是执天工之术，炼弑神之器？\n" +
                "或入蓬莱秘境，以符箓召来八方神明？抑或……化身九黎巫祝，与山海共鸣？\n" +
                "天地不仁，以万物为刍狗。你的选择，将重写太虚幻境的终焉。";
    }
}
