package x.ovo.jbot.impl.plugin;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.convert.ConvertUtil;
import org.dromara.hutool.core.text.StrUtil;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.common.constant.JBotFiles;
import x.ovo.jbot.core.common.exception.PluginException;
import x.ovo.jbot.core.common.util.ContactUtil;
import x.ovo.jbot.core.contact.ContactManager;
import x.ovo.jbot.core.contact.Contactable;
import x.ovo.jbot.core.event.CallListener;
import x.ovo.jbot.core.plugin.LimitMode;
import x.ovo.jbot.core.plugin.Plugin;
import x.ovo.jbot.core.plugin.PluginConfig;
import x.ovo.jbot.core.plugin.PluginManager;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 默认插件管理器
 *
 * @author ovo created on 2025/02/17.
 */
@Slf4j
public class DefaultPluginManager implements PluginManager {

    private final Vertx x = Context.vertx;
    private final ContactManager contactManager = Context.get().getContactManager();
    /** 插件限制模式，默认为白名单模式（只有在限制名单内可用） */
    private LimitMode mode = LimitMode.WHITE_LIST;
    /** 插件容器 {插件名称：插件} */
    protected final Map<String, Plugin> container = new HashMap<>();
    /** 插件列表 */
    protected final List<Plugin> list = new ArrayList<>();
    /** 插件配置 {插件名称：插件配置} */
    protected final Map<String, PluginConfig> config = new HashMap<>();
    /** 限制名单 {wxid: [插件名称]} */
    protected final Map<Contactable, List<String>> limits = new HashMap<>();

    private JsonObject tempData;


    @Override
    public Future<Void> onInit() throws Exception {
        // 1、读取插件配置
        return this.x.fileSystem().exists(JBotFiles.PLUGIN_CONFIG_FILE.getPath())
                .compose(exists -> Future.future(promise -> {
                    if (!exists) {
                        // 如果配置文件不存在，则输出默认配置文件
                        var buffer = this.x.fileSystem().readFileBlocking(JBotFiles.PLUGIN_CONFIG_FILE.getName());
                        this.x.fileSystem().writeFileBlocking(JBotFiles.PLUGIN_CONFIG_FILE.getPath(), buffer);
                    }
                    this.x.fileSystem().readFile(JBotFiles.PLUGIN_CONFIG_FILE.getPath())
                            .onFailure(promise::fail)
                            .onSuccess(buffer -> {
                                var data = buffer.toJsonObject();
                                this.tempData = data;
                                this.mode = LimitMode.valueOf(data.getString("mode", "WHITE_LIST"));
//                                data.getJsonObject("limits").forEach(entry -> this.limits.put(ContactUtil.fromString(entry.getKey(), this.contactManager), ConvertUtil.toList(String.class, entry.getValue())));
                                data.getJsonObject("config").forEach(entry -> this.config.put(entry.getKey(), ((JsonObject) entry.getValue()).mapTo(PluginConfig.class)));
                                log.debug("插件配置文件读取成功，加载模式：{}，限制名单：{}，插件配置：{}", this.mode, this.limits, this.config);
                                promise.complete();
                            });
                }))
                .compose(v -> Future.<Void>future(p -> {
                    // 3、加载插件
                    this.loadPlugins();
                    p.complete();
                }))
                .onFailure(e -> {
                    throw new PluginException("插件配置文件读取失败，请检查后重试，错误原因：" + e.getMessage());
                })
                .onSuccess(v -> log.info("插件管理器初始化完成，共加载 [{}] 个插件", this.list.size()));

    }

    @Override
    public Future<Void> onDestroy() throws Exception {
        // 保存配置文件，将限制模式、限制名单、插件配置持久化
        return this.saveConfig();
    }

    @Override
    public void reInit() {
        this.tempData.getJsonObject("limits").forEach(entry -> this.limits.put(ContactUtil.fromString(entry.getKey()), ConvertUtil.toList(String.class, entry.getValue())));
        this.tempData.getJsonObject("config").forEach(entry -> this.config.put(entry.getKey(), ((JsonObject) entry.getValue()).mapTo(PluginConfig.class)));
        this.tempData = null;
        log.debug("插件配置重加载成功，加载模式：{}，限制名单：{}，插件配置：{}", this.mode, this.limits, this.config);
    }

    @Override
    public void loadPlugins() {
        // 1、遍历插件目录，获得插件文件
        var files = JBotFiles.PLUGIN_DIR.listFiles((dir, name) -> name.endsWith("jar") || name.endsWith("zip"));
        if (Objects.isNull(files)) {return;}
        // 2、加载插件并注册事件监听器和命令执行器
        for (File file : files) {
            this.load(file)
                    .onSuccess(p -> log.debug("插件包 [{}] 加载成功", file.getName()))
                    .onFailure(e -> log.warn("插件包 [{}] 加载失败：{}", file.getName(), e.getMessage()));
        }
    }

    @Override
    public Plugin get(String name) {
        return Optional.ofNullable(this.container.get(name)).orElseThrow(() -> new PluginException("插件不存在"));
    }

    @Override
    public PluginConfig getConfig(String name) {
        return this.container.containsKey(name) ? this.config.computeIfAbsent(name, n -> new PluginConfig()) : null;
    }

    @Override
    public Future<Plugin> load(String name) {
        // 1、遍历插件目录
        var files = JBotFiles.PLUGIN_DIR.listFiles((dir, fileName) -> fileName.endsWith("jar") || fileName.endsWith("zip"));
        if (Objects.isNull(files)) {
            throw new PluginException("插件目录中不存在任何插件包");
        }
        // 2、过滤出指定插件
        var file = Stream.of(files)
                .filter(f -> name.equals(PluginLoader.getPluginDescription(f).result().getName()))
                .findAny()
                .orElseThrow(() -> new PluginException(StrUtil.format("插件 [{}] 加载失败，未能找到指定的插件包", name)));
        return this.load(file);
    }

    @Override
    public Future<Plugin> load(File file) {
        return PluginLoader.getPluginDescription(file)
                .compose(desc -> PluginLoader.load(file, desc))
                .compose(plugin -> Future.future(promise -> {
                    try {
                        var p = Promise.<Void>promise();
                        plugin.onLoad(p);
                        p.future().onFailure(t -> promise.fail(StrUtil.format("加载插件 [{}] 时出现异常：{}", plugin.getDescription().getName(), t.getMessage())));
                    } catch (Exception e) {
                        promise.fail(StrUtil.format("加载插件 [{}] 时出现异常：{}", plugin.getDescription().getName(), e.getMessage()));
                    }
                    plugin.setEnabled(Optional.ofNullable(this.config.get(plugin.getDescription().getName())).map(PluginConfig::getEnabled).orElse(true));
                    Context.get().getEventManager().register(plugin).onFailure(promise::fail);
                    Context.get().getCommandManager().register(plugin).onFailure(promise::fail);
                    Optional.ofNullable(plugin.getCallListener()).ifPresent(CallListener::register);
                    this.list.add(plugin);
                    this.list.sort(Comparator.comparingInt(p -> p.getDescription().getPriority()));
                    this.container.put(plugin.getDescription().getName(), plugin);
                    promise.complete(plugin);
                }));
    }

    @Override
    public Future<Void> unload(String name) {
        return this.unload(this.get(name));
    }

    @Override
    public Future<Void> unload(Plugin plugin) {
        return Future.future(promise -> {
            var name = plugin.getDescription().getName();
            // 从容器中移除插件
            this.container.remove(name);
            this.list.remove(plugin);

            try {
                var p = Promise.<Void>promise();
                plugin.onUnload(p);
                p.future().onFailure(e -> log.warn(StrUtil.format("卸载插件 [{}] 时出现异常：{}", name, e.getMessage())));
            } catch (Exception e) {
                log.warn(StrUtil.format("卸载插件 [{}] 时出现异常：{}", name, e.getMessage()));
            }

            // 注销插件事件监听器和命令执行器
            Context.get().getEventManager().unregister(plugin).onFailure(e -> log.warn(StrUtil.format("卸载插件 [{}] 事件监听器时出现异常：{}", name, e.getMessage())));
            Context.get().getCommandManager().unregister(plugin).onFailure(e -> log.warn(StrUtil.format("卸载插件 [{}] 命令执行器时出现异常：{}", name, e.getMessage())));
            promise.complete();
        });
    }

    @Override
    public Future<String> enable(String name, Contactable target) {
        return Future.future(promise -> {
            try {
                var plugin = this.get(name);
                // 如果target为空且插件已启用
                if (Objects.isNull(target) && plugin.isEnabled()) {
                    promise.complete(StrUtil.format("插件 [{}] 已是启用状态，请勿重复操作", name));
                } else if (Objects.isNull(target)) {
                    // 全局启用插件
                    Optional.ofNullable(this.config.get(name)).ifPresent(config -> config.setEnabled(true));
                    Future.future(plugin::enable).onSuccess(v -> log.info("插件 [{}] 启用成功", name)).onFailure(promise::fail);
                } else {
                    if (this.mode == LimitMode.WHITE_LIST) {
                        // 白名单模式下为目标启用插件需要将插件添加到限制名单
                        this.limits.computeIfAbsent(target, k -> new ArrayList<>()).add(name);
                    } else {
                        // 黑名单模式下为目标启用插件需要将插件从限制名单移除
                        Optional.ofNullable(this.limits.get(target)).ifPresent(list -> list.remove(name));
                    }
                    var tip = plugin.isEnabled() ? "" : "(全局禁用)";
                    promise.complete(StrUtil.format("已为 [{}] 启用插件 [{}]{}", target.getNickname(), name, tip));
                }
                this.saveConfig();
            } catch (Exception e) {promise.fail(e);}
        });
    }

    @Override
    public Future<String> disable(String name, Contactable target) {
        return Future.future(promise -> {
            try {
                var plugin = this.get(name);
                // 如果target为空且插件已禁用
                if (Objects.isNull(target) && !plugin.isEnabled()) {
                    promise.complete(StrUtil.format("插件 [{}] 已是禁用状态，请勿重复操作", name));
                } else if (Objects.isNull(target)) {
                    // 全局禁用插件
                    this.config.computeIfAbsent(name, n -> new PluginConfig()).setEnabled(false);
//                    Optional.ofNullable(this.config.get(name)).ifPresent(config -> config.setEnabled(false));
                    Future.future(plugin::disable).onSuccess(v -> promise.complete(StrUtil.format("插件 [{}] 禁用成功", name))).onFailure(promise::fail);
                } else {
                    if (this.mode == LimitMode.BLACK_LIST) {
                        // 黑名单模式下为目标禁用插件需要将插件添加到限制名单
                        this.limits.computeIfAbsent(target, k -> new ArrayList<>()).add(name);
                    } else {
                        // 白名单模式下为目标禁用插件需要将插件从限制名单移除
                        Optional.ofNullable(this.limits.get(target)).ifPresent(list -> list.remove(name));
                    }
                    var tip = plugin.isEnabled() ? "" : "(全局禁用)";
                    promise.complete(StrUtil.format("已为 [{}] 禁用插件 [{}]{}", target.getNickname(), name, tip));
                }
                this.saveConfig();
            } catch (Exception e) {promise.fail(e);}
        });
    }

    @Override
    public Collection<Plugin> list() {
        return this.list;
    }

    @Override
    public Collection<Plugin> availableList(Contactable target) {
        return Objects.isNull(target) ? List.of() : Optional.ofNullable(this.limits.get(target)).orElse(List.of()).stream().map(this::get).toList();
    }

    @Override
    public LimitMode getLimitMode() {
        return this.mode;
    }

    @Override
    public boolean isLimited(Plugin plugin, Contactable target) {
        return this.limits.containsKey(target) && this.limits.get(target).contains(plugin.getDescription().getName());
    }

    @Override
    public Future<Void> saveConfig() {
        Map<String, List<String>> map = this.limits.entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getKey()) && CollUtil.isNotEmpty(entry.getValue()))
                .collect(Collectors.toMap(entry -> ContactUtil.toString(entry.getKey()), Map.Entry::getValue));
        var buffer = Buffer.buffer(JsonObject.of("mode", this.mode, "limits", map, "config", this.config).encodePrettily());
        return this.x.fileSystem().writeFile(JBotFiles.PLUGIN_CONFIG_FILE.getPath(), buffer)
                .onFailure(e -> log.warn("保存插件配置文件时出现异常：{}", e.getMessage()))
                .onSuccess(v -> log.debug("插件配置文件保存成功"));
    }
}
