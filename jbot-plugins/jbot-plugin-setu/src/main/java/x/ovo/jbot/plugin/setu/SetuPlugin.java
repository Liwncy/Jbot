package x.ovo.jbot.plugin.setu;

import org.dromara.hutool.core.lang.Singleton;
import org.dromara.hutool.http.client.ClientConfig;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.dromara.hutool.http.client.engine.ClientEngineFactory;
import org.dromara.hutool.http.client.engine.jdk.JdkClientEngine;
import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.plugin.Plugin;

/**
 * setu 插件
 * <p>
 *
 * @author ovo on 2024/10/25.
 * @since 1.0.0
 */
public class SetuPlugin extends Plugin {

    @Override
    public void onLoad() {
        // 设置hutool-http使用jdk的httpclient实现，避免302无法重定向，hutool后续的新版本有可能会解决此问题
        Singleton.get(ClientEngine.class.getName(), () -> {
            ClientEngine engine = ClientEngineFactory.createEngine(JdkClientEngine.class.getSimpleName());
            engine.init(ClientConfig.of().setFollowRedirects(true));
            return engine;
        });
    }

    @Override
    public EventListener<?, ?> getEventListener() {
        return new SetuListener(this);
    }

    @Override
    public CommandExecutor getCommandExecutor() {
        return null;
    }
}
