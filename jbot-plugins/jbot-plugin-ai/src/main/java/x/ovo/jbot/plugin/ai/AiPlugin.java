package x.ovo.jbot.plugin.ai;

import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.plugin.Plugin;

/**
 * AI 插件
 *
 * @author ovo on 2024/10/24.
 * @since 1.0.0
 */
public class AiPlugin extends Plugin {
    @Override
    public void onLoad() {
        this.saveDefaultConfig();
    }

    @Override
    public EventListener<?, ?> getEventListener() {
        return new x.ovo.jbot.plugin.ai.AiListener(this);
    }

    @Override
    public CommandExecutor getCommandExecutor() {
        return new x.ovo.jbot.plugin.ai.AiExecutor(this);
    }
}
