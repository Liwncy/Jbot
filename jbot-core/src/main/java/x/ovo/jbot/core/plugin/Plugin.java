package x.ovo.jbot.core.plugin;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import lombok.Cleanup;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.common.constant.JBotConstant;
import x.ovo.jbot.core.common.constant.JBotFiles;
import x.ovo.jbot.core.common.exception.PluginException;
import x.ovo.jbot.core.event.CallListener;
import x.ovo.jbot.core.event.EventListener;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Objects;
import java.util.Optional;

/**
 * 插件
 *
 * @author ovo created on 2025/02/17.
 */
@Slf4j
@Data
public abstract class Plugin implements PluginLifeCycle, Comparable<Plugin> {

    protected Vertx vertx;
    protected Context ctx;
    protected boolean enabled;
    protected org.graalvm.polyglot.Context runtime;
    protected @Getter JsonObject config;
    protected URLClassLoader classLoader;
    protected PluginDescription description;

    public abstract CommandExecutor getCommandExecutor();

    public abstract EventListener<?, ?> getEventListener();

    public CallListener getCallListener() {
        return null;
    }

    @Override
    public int compareTo(Plugin o) {
        return this.description.getPriority() - o.description.getPriority();
    }

    public final void enable(Promise<Void> promise) {
        this.enabled = true;
        try {this.onEnable();} catch (Exception e) {
            promise.fail(new PluginException(StrUtil.format("[{}] 在执行 onEnable 时出现异常：{}", this.description.getName(), e.getMessage()), e));
        }
    }

    public final void disable(Promise<Void> promise) {
        this.enabled = false;
        try {this.onDisable();} catch (Exception e) {
            promise.fail(new PluginException(StrUtil.format("[{}] 在执行 onDisable 时出现异常：{}", this.description.getName(), e.getMessage()), e));
        }
    }

    /**
     * 获取数据目录
     *
     * @return {@link File }
     */
    public final File getDataDir() {
        return new File(JBotFiles.PLUGIN_DIR, this.description.getName());
    }

    /**
     * 刷新配置，重新从配置文件中读取
     */
    public final void flushConfig() {
        var files = this.getDataDir().listFiles((dir, name) -> name.startsWith(JBotConstant.CONFIG_JSON));
        // 如果插件目录下存在插件配置文件，则读取配置文件
        if (Objects.nonNull(files) && files.length > 0) {
            var config = Context.vertx.fileSystem().readFileBlocking(files[0].getPath()).toJsonObject();
            this.setConfig(config);
        }
    }

    /**
     * 保存配置
     * <p>如果插件数据目录下不存在配置文件，将会输出默认配置文件，否则会将内存中的配置持久化到配置文件中</p>
     *
     * @apiNote 如说明所述，当插件数据目录已经存在配置文件时调用此方法，在持久化内存中配置时，会覆盖原配置文件，配置文件中的注释将消失
     */
    public final void saveConfig() throws IOException {
        var files = this.getDataDir().listFiles((dir, name) -> name.startsWith(JBotConstant.CONFIG_JSON));
        if (Objects.nonNull(files) && files.length > 0) {
            // 如果插件数据目录下已经存在配置文件，将会覆盖原配置文件
            this.vertx.fileSystem().writeFileBlocking(files[0].getPath(), Buffer.buffer(this.config.encodePrettily()));
        } else {
            // 如果插件数据目录下不存在配置文件，将会输出默认配置文件
            @Cleanup var is = Optional.ofNullable(
                    Optional.ofNullable(this.getClassLoader().getResourceAsStream(JBotConstant.CONFIG_JSON5))
                            .orElse(this.getClassLoader().getResourceAsStream(JBotConstant.CONFIG_JSON))
            ).orElseThrow(() -> new PluginException("无法找到默认配置文件"));
            this.vertx.fileSystem().mkdirsBlocking(this.getDataDir().getPath());
            this.vertx.fileSystem().writeFileBlocking(new File(this.getDataDir(), JBotConstant.CONFIG_JSON5).getPath(), Buffer.buffer(is.readAllBytes()));
        }
    }


    public void saveDefaultConfig() {
        // 如果存在配置文件，则创建相应文件夹并输出默认配置文件
        var file = Optional.ofNullable(this.getClassLoader().findResource(JBotConstant.CONFIG_JSON5)).orElse(this.getClassLoader().findResource(JBotConstant.CONFIG_JSON));
        if (Objects.nonNull(file) && !this.getDataDir().exists()) {
            try {
                // 如果插件数据目录下不存在配置文件，将会输出默认配置文件
                @Cleanup var is = Optional.ofNullable(
                        Optional.ofNullable(this.getClassLoader().getResourceAsStream(JBotConstant.CONFIG_JSON5))
                                .orElse(this.getClassLoader().getResourceAsStream(JBotConstant.CONFIG_JSON))
                ).orElseThrow(() -> new PluginException("无法找到默认配置文件"));
                this.vertx.fileSystem().mkdirsBlocking(this.getDataDir().getPath());
                this.vertx.fileSystem().writeFileBlocking(new File(this.getDataDir(), JBotConstant.CONFIG_JSON5).getPath(), Buffer.buffer(is.readAllBytes()));
            } catch (IOException e) {
                log.warn("无法保存配置文件: {}", e.getMessage());
            }
        }
    }
}
