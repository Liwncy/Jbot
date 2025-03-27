package x.ovo.jbot.impl.config;

import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.io.file.FileUtil;
import x.ovo.jbot.core.common.constant.JBotFiles;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Adapter 类加载器
 *
 * @author ovo created on 2025/03/25.
 */
@Slf4j
public class SpiClassLoader extends URLClassLoader {

    public SpiClassLoader() {
        super(new URL[0], ClassLoader.getSystemClassLoader());
        loadAdapterJars();
    }

    private void loadAdapterJars() {
        FileUtil.loopFiles(JBotFiles.ADAPTER_DIR, (file) -> {
            if (file.getName().endsWith(".jar")) {
                try {
                    this.addURL(file.toURI().toURL());
                } catch (Exception e) {
                    log.warn("加载适配器失败", e);
                }
            }
            return true;
        });
    }
}
