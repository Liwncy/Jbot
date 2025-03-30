package x.ovo.jbot.impl.plugin;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.common.constant.JBotConstant;
import x.ovo.jbot.core.common.exception.PluginException;
import x.ovo.jbot.core.plugin.Plugin;
import x.ovo.jbot.core.plugin.PluginDescription;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.zip.ZipFile;

/**
 * 插件加载器
 *
 * @author ovo created on 2025/02/18.
 * @implNote 扫描指定目录下的jar或zip文件，加载其中的插件类
 */
@Slf4j
public class PluginLoader {

    /**
     * 获取插件说明
     *
     * @param file 插件包文件
     * @return {@link Future }<{@link PluginDescription }> 插件描述信息
     */
    public static Future<PluginDescription> getPluginDescription(File file) {
        return Future.future(promise -> {
            try {
                @Cleanup var zip = new ZipFile(file);
                var entry = Optional.ofNullable(Optional.ofNullable(zip.getEntry(JBotConstant.PLUGIN_JSON5)).orElse(zip.getEntry(JBotConstant.PLUGIN_JSON)))
                        .orElseThrow(() -> new PluginException("插件包内不存在插件描述文件"));
                var desc = Buffer.buffer(zip.getInputStream(entry).readAllBytes()).toJsonObject().mapTo(PluginDescription.class);
                promise.complete(desc);
            } catch (IOException e) {
                promise.fail(e);
            }
        });
    }

    /**
     * 获取类加载器
     *
     * @param file 插件包文件
     * @return {@link URLClassLoader } 类加载器
     * @throws IOException io异常
     */
    public static URLClassLoader getClassLoader(File file) throws IOException {
        return new URLClassLoader(new URL[]{file.toURI().toURL()}, Thread.currentThread().getContextClassLoader());
    }

    /**
     * 加载插件
     *
     * @param file        插件包文件
     * @param description 插件描述
     * @return {@link Future }<{@link Plugin }> 插件
     * @throws PluginException 插件异常
     */
    public static Future<Plugin> load(File file, PluginDescription description) {
        return Future.future(promise -> {
            try {
                var loader = getClassLoader(file);
                var plugin = PluginFactory.load(loader, description.getMain());
                plugin.setClassLoader(loader);
                plugin.setCtx(Context.get());
                plugin.setVertx(Context.vertx);
                plugin.setDescription(description);
                plugin.saveDefaultConfig();
                plugin.flushConfig();
                promise.complete(plugin);
            } catch (Exception e) {
                promise.fail(e);
            }
        });
    }

}
