package x.ovo.jbot.impl.plugin;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.thread.ThreadUtil;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.common.constant.JBotConstant;
import x.ovo.jbot.core.common.exception.PluginException;
import x.ovo.jbot.core.contact.Friend;
import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.core.plugin.Plugin;
import x.ovo.jbot.core.plugin.PluginConfig;
import x.ovo.jbot.core.plugin.PluginDescription;

import java.io.File;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 调试插件管理器
 *
 * @author ovo created on 2025/02/18.
 * @apiNote 调试插件管理器，用于调试插件，不建议在生产环境中使用。
 */
@Slf4j
public class DebugPluginManager extends DefaultPluginManager {

    private final Vertx x = Context.vertx;
    private final String path = Context.get().getConfig().getBot().getDebugConfig().getString("path", System.getProperty("user.dir") + File.separator + "jbot-plugins");
    private final String subPath = StrUtil.join(File.separator, "target", "classes");

    private final String raw = """
            {
                "MsgId" : 766190484,
                "FromUserName" : {
                  "string" : "wxid_ge1k25oezgv922"
                },
                "ToUserName" : {
                  "string" : "wxid_6j82xnhri39129"
                },
                "MsgType" : 1,
                "Content" : {
                  "string" : "test"
                },
                "Status" : 3,
                "ImgStatus" : 1,
                "ImgBuf" : {
                  "iLen" : 0
                },
                "CreateTime" : 1740673633,
                "MsgSource" : "<msgsource>\\n\\t<sec_msg_node>\\n\\t\\t<alnode>\\n\\t\\t\\t<fr>1</fr>\\n\\t\\t</alnode>\\n\\t</sec_msg_node>\\n\\t<pua>1</pua>\\n\\t<signature>V1_qh+/bOYf|v1_qh+/bOYf</signature>\\n\\t<tmp_node>\\n\\t\\t<publisher-id></publisher-id>\\n\\t</tmp_node>\\n</msgsource>\\n",
                "PushContent" : "Ooops : test",
                "NewMsgId" : 6895754045683144474,
                "MsgSeq" : 608123455
              }
            """;

    private Runnable scanner() {
        return () -> {
            log.info("插件调试模式开启，开始监听控制台输入");
            var msg = new TextMessage();
            if (Objects.isNull(Context.get().getOwner()))
                Context.get().setOwner(Friend.builder().id("test_sender").nickname("test sender").build());
            // 如果bot信息为空
            if (Objects.isNull(Context.get().getBot()))
                Context.get().setBot(Friend.builder().id("test_receiver").nickname("test receiver").build());

            msg.setSender(Context.get().getOwner());
            msg.setReceiver(Context.get().getBot());

            // 扫描控制台输入
            var scanner = new java.util.Scanner(System.in);
            while (scanner.hasNext()) {
                // 构建文本消息
                var text = scanner.nextLine();
                if (StrUtil.isBlank(text)) {continue;}
                msg.setContent(text);
                msg.setRaw(Buffer.buffer(raw).toJsonObject().put("CreateTime", System.currentTimeMillis() / 1000));
                Context.get().getMessageManager().addReceive(msg);
            }
        };
    }

    @Override
    public void loadPlugins() {
        var files = new File(path).listFiles(File::isDirectory);
        if (Objects.isNull(files)) {return;}
        Stream.of(files).forEach(file -> this.load(file)
                .onFailure(e -> log.error("插件 [{}] 加载失败：{}", file.getName(), e.getMessage(), e))
                .onSuccess(v -> log.info("插件 [{}] 加载成功", file.getName()))
        );
    }

    @Override
    public void reInit() {
        super.reInit();
        ThreadUtil.execute(scanner());
    }

    @Override
    public Future<Plugin> load(String name) {
        return Future.<File>future(promise -> {
            var files = new File(path).listFiles();
            if (Objects.isNull(files)) {
                promise.fail(new PluginException("指定目录下不存在任何文件夹"));
                return;
            }
            var dir = Stream.of(files)
                    .filter(file -> {
                        var f = new File(file, subPath + File.separator + JBotConstant.PLUGIN_JSON5);
                        f = f.exists() ? f : new File(file, subPath + File.separator + "plugin.json");
                        var buf = this.x.fileSystem().readFileBlocking(f.getPath());
                        var desc = buf.toJsonObject();
                        return desc.getString("name").equals(name);
                    }).findAny().orElseThrow(PluginException::new);
            promise.complete(dir);
        }).compose(this::load);
    }

    @Override
    public Future<Plugin> load(File file) {
        var json = new File(file, subPath + File.separator + JBotConstant.PLUGIN_JSON5);
        json = json.exists() ? json : new File(file, subPath + File.separator + "plugin.json");
        var buf = this.x.fileSystem().readFileBlocking(json.getPath());
        return Future.<Plugin>future(promise -> {
                    try {

                        var desc = buf.toJsonObject().mapTo(PluginDescription.class);
                        PluginLoader.load(new File(file.getPath() + File.separator + subPath), desc)
                                .onSuccess(promise::complete)
                                .onFailure(promise::fail);

//                        @Cleanup var loader = new URLClassLoader(new URL[]{new File(file.getPath() + File.separator + subPath).toURI().toURL()});
//                        var plugin = PluginFactory.load(loader, desc.getMain());
//                        plugin.setDescription(desc);
//                        promise.complete(plugin);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        promise.fail(e);
                    }
                })
                .compose(plugin -> Future.future(promise -> {
                    plugin.setEnabled(Optional.ofNullable(this.config.get(plugin.getDescription().getName())).map(PluginConfig::getEnabled).orElse(true));
                    Context.get().getEventManager().register(plugin).onFailure(promise::fail).onSuccess(v -> log.debug("插件 [{}] 注册事件监听器成功", plugin.getDescription().getName()));
                    Context.get().getCommandManager().register(plugin).onFailure(promise::fail).onSuccess(v -> log.debug("插件 [{}] 注册命令执行器成功", plugin.getDescription().getName()));
                    this.list.add(plugin);
                    this.list.sort(Comparator.comparingInt(p -> p.getDescription().getPriority()));
                    this.container.put(plugin.getDescription().getName(), plugin);
                    promise.complete(plugin);
                }));
    }


}
