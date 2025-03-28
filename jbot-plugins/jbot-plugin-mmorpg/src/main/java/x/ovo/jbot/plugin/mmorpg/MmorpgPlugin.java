package x.ovo.jbot.plugin.mmorpg;

import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.plugin.Plugin;
import x.ovo.jbot.plugin.mmorpg.util.MySQLClient;

/**
 * Mmorpg 插件
 * <p>
 *
 * @author liwncy on 2025/03/26.
 * @since 1.0.0
 */
public class MmorpgPlugin extends Plugin {

    @Override
    public void onLoad() {
        MySQLClient.init(this.vertx, this.getConfig().getJsonObject("mysql_config"));
        this.saveDefaultConfig();
    }

    @Override
    public void onUnload() {
        MySQLClient.getInstance().close();
    }

    @Override
    public EventListener<?, ?> getEventListener() {
        MySQLClient.init(this.vertx, this.getConfig().getJsonObject("mysql_config"));
        return new MmorpgListener(this);
    }

    @Override
    public CommandExecutor getCommandExecutor() {
        MySQLClient.init(this.vertx, this.getConfig().getJsonObject("mysql_config"));
        return new MmorpgExecutor(this);
    }
}
