package x.ovo.jbot.impl;

import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.thread.ThreadUtil;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.JBot;
import x.ovo.jbot.core.JBotConfig;
import x.ovo.jbot.core.adapter.Adapter;
import x.ovo.jbot.core.common.constant.JBotFiles;
import x.ovo.jbot.core.common.exception.AdapterException;
import x.ovo.jbot.core.common.util.ContactUtil;
import x.ovo.jbot.core.contact.Friend;
import x.ovo.jbot.impl.command.DefaultCommandManager;
import x.ovo.jbot.impl.config.SpiClassLoader;
import x.ovo.jbot.impl.contact.DefaultContactManager;
import x.ovo.jbot.impl.contact.NickNameStrategy;
import x.ovo.jbot.impl.contact.RemarkStrategy;
import x.ovo.jbot.impl.contact.UserNameStrategy;
import x.ovo.jbot.impl.event.DefaultEventManager;
import x.ovo.jbot.impl.message.DefaultMessageManager;
import x.ovo.jbot.impl.plugin.DebugPluginManager;
import x.ovo.jbot.impl.plugin.DefaultPluginManager;

import java.util.ServiceLoader;

/**
 * JBot 实现
 *
 * @author ovo created on 2025/02/19.
 */
@Slf4j
public class JBotImpl implements JBot {

    private final Adapter adapter;

    {
        // 加载配置文件
        JBotConfig config = JBotConfig.load().await();
        // 通过SPI加载适配器
        var it = ServiceLoader.load(Adapter.class, new SpiClassLoader()).iterator();
        if (!it.hasNext()) throw new AdapterException("未找到任何适配器，请检查adapter目录下是否存在适配器jar");
        Adapter adapter = null;
        while (it.hasNext() && adapter == null) {
            Adapter next = it.next();
            adapter = StrUtil.equals(next.name(), config.getAdapter().getString("name")) ? next : null;
        }
        if (adapter == null)
            throw new AdapterException("未找到适配器：[" + config.getAdapter().getString("name") + "]，请检查配置中适配器名称或adapter目录中是否存在对应适配器");

        adapter.init(config.getAdapter());
        this.adapter = adapter;
        Context.get().setAdapter(adapter);

        // 初始化并注册检索策略
        new UserNameStrategy().init();
        new NickNameStrategy().init();
        new RemarkStrategy().init();
        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(ThreadUtil.newThread(this::stop, "bot-stop"));

    }


    @Override
    public Future<Void> start() {
        log.info("JBot 启动中...");
        // 启动适配器，执行登录、开启心跳流程
        this.adapter.start().await();

        Context context = Context.get();
        context.setOwner((Friend) ContactUtil.fromString(context.getConfig().getBot().getOwner()));
        context.getPluginManager().reInit();
        return Future.succeededFuture();
    }

    @Override
    public Future<Void> stop() {
        this.adapter.destroy();
        Context context = Context.get();
        context.getEventManager().close().onSuccess(v -> log.info("事件管理器关闭成功")).onFailure(t -> log.warn("事件管理器关闭时出现异常：{}", t.getMessage()));
        context.getPluginManager().close().onSuccess(v -> log.info("插件管理器关闭成功")).onFailure(t -> log.warn("插件管理器关闭时出现异常：{}", t.getMessage()));
        context.getCommandManager().close().onSuccess(v -> log.info("命令管理器关闭成功")).onFailure(t -> log.warn("命令管理器关闭时出现异常：{}", t.getMessage()));
        context.getContactManager().close().onSuccess(v -> log.info("联系人管理器关闭成功")).onFailure(t -> log.warn("联系人管理器关闭时出现异常：{}", t.getMessage()));
        context.getMessageManager().close().onSuccess(v -> log.info("消息管理器关闭成功")).onFailure(t -> log.warn("消息管理器关闭时出现异常：{}", t.getMessage()));
        return Context.vertx.close().onSuccess(v -> log.info("vertx关闭成功")).onFailure(t -> log.warn("vertx关闭时出现异常：{}", t.getMessage()));
    }

    @Override
    public Future<Void> initManager() {
        return new DefaultContactManager().init()
                .compose(v -> new DefaultEventManager().init())
                .compose(v -> new DefaultCommandManager().init())
                .compose(v -> new DefaultMessageManager().init())
                .compose(v -> Context.get().getConfig().getBot().getDebug() ? new DebugPluginManager().init() : new DefaultPluginManager().init());
    }

    @Override
    public Future<Void> mkdir() {
        var fs = Context.vertx.fileSystem();
        return fs.mkdirs(JBotFiles.CONFIG_DIR.getPath())
                .compose(v -> fs.mkdirs(JBotFiles.ADAPTER_DIR.getPath()))
                .compose(v -> fs.mkdirs(JBotFiles.PLUGIN_DIR.getPath()))
                .compose(v -> fs.mkdirs(JBotFiles.LOG_DIR.getPath()))
                .onFailure(t -> {
                    log.warn("创建目录时出现异常：{}", t.getMessage());
                    System.exit(1);
                });
    }
}
