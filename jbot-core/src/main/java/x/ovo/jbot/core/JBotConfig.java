package x.ovo.jbot.core;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import x.ovo.jbot.core.common.constant.JBotFiles;

/**
 * jbot 配置
 *
 * @author ovo created on 2025/02/18.
 */
@Data
@Slf4j
public class JBotConfig {

    private BotConfig bot = new BotConfig();
    private LoginConfig login = new LoginConfig();
    private CommandConfig command = new CommandConfig();
    private JsonObject adapter = new JsonObject();


    public static @Data class BotConfig {
        private Boolean debug = false;
        private JsonObject debugConfig = new JsonObject();
        private Boolean saveMedia = false;
        private String owner = "";
        private String redis = "";
    }

    public static @Data class LoginConfig {
        private Boolean autoLogin = true;
        private Boolean printQrcode = true;
        private Integer retryCount = 3;
        private Boolean encryptLoginInfo = true;
        private String encryptKey = "jbot";
    }

    public static @Data class CommandConfig {
        private Boolean showTip = true;
        private String noPermissionTip = "你没有权限执行此命令";
    }

    public static Future<JBotConfig> load() {
        var fs = Context.vertx.fileSystem();
        var exists = fs.existsBlocking(JBotFiles.CONFIG_FILE.getPath());
        if (!exists) {
            saveDefault().await();
        }
        return Future.future(p -> fs.readFile(JBotFiles.CONFIG_FILE.getPath())
                .onFailure(p::fail)
                .onSuccess(buffer -> {
                    log.info("读取配置文件成功");
                    var config = buffer.toJsonObject().mapTo(JBotConfig.class);
                    log.trace(config.toString());
                    Context.get().setConfig(config);
                    p.complete(config);
                })
        );
    }

    private static Future<Void> saveDefault() {
        log.debug("配置文件不存在，输出默认配置文件");
        var fs = Context.vertx.fileSystem();
        // 输出默认配置文件
        return fs.mkdir(JBotFiles.CONFIG_DIR.getPath())
                .compose(v -> fs.readFile(JBotFiles.CONFIG_FILE.getName()))
                .compose(buffer -> fs.writeFile(JBotFiles.CONFIG_FILE.getPath(), buffer))
                .onFailure(t -> log.warn("输出默认配置文件时出现异常：{}", t.getMessage()))
                .onSuccess(v -> log.debug("输出默认配置文件成功"));
    }

}
