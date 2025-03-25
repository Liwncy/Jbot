package x.ovo.jbot.plugin.xuanxue;

import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.plugin.Plugin;

public class XuanxuePlugin extends Plugin {

    @Override
    public void onLoad() {
        this.saveDefaultConfig();
    }

    @Override
    public EventListener<?, ?> getEventListener() {
        return new XuanxueListener(this);
    }

    @Override
    public CommandExecutor getCommandExecutor() {
        return new XuanxueExecutor(this);
    }
}
