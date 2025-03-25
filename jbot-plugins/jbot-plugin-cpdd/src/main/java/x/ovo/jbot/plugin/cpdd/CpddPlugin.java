package x.ovo.jbot.plugin.cpdd;

import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.plugin.Plugin;

/**
 * cpdd 插件
 * <p>
 *
 * @author liwncy on 2024/12/16.
 * @since 1.0.0
 */
public class CpddPlugin extends Plugin {

    @Override
    public void onLoad() {
        this.saveDefaultConfig();
    }

    @Override
    public EventListener<?, ?> getEventListener() {
        return new CpddListener(this);
    }

    @Override
    public CommandExecutor getCommandExecutor() {
        return null;
    }
}
