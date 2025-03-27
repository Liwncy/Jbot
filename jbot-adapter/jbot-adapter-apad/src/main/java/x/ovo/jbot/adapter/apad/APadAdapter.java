package x.ovo.jbot.adapter.apad;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import x.ovo.jbot.adapter.apad.service.*;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.adapter.Adapter;
import x.ovo.jbot.core.common.constant.JBotFiles;
import x.ovo.jbot.core.service.*;

import java.io.File;

@Slf4j
public class APadAdapter implements Adapter {

    private static @Getter JsonObject config;
    public static final String NAME = "apad-adapter";
    private static final String CONFIG_PATH = new File(JBotFiles.ADAPTER_DIR, NAME + File.separator + "config.json").getPath();


    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Future<Void> start() {
        //  1、执行登录流程或重连
        return this.getLoginService().login()
                .onSuccess(v -> log.info("登录成功"))
                .compose(v -> this.getLoginService().getUserInfo().onSuccess(me -> {
                    Context.get().getContactManager().add(me);
                    Context.get().setBot(me);
                }))
                //  2、获取联系人信息存入联系人管理器中
                .compose(v -> this.getContactService().list())
                //  3、开启消息同步
                .onSuccess(v -> SyncService.start())
                .mapEmpty();
    }

    @Override
    public ContactService getContactService() {
        return ContactServiceImpl.INSTANCE;
    }

    @Override
    public DownloadService getDownloadService() {
        return null;
    }

    @Override
    public FavoriteService getFavoriteService() {
        return null;
    }

    @Override
    public GroupService getGroupService() {
        return GroupServiceImpl.INSTANCE;
    }

    @Override
    public ImService getImService() {
        return null;
    }

    @Override
    public LabelService getLabelService() {
        return null;
    }

    @Override
    public LoginService getLoginService() {
        return LoginServiceImpl.INSTANCE;
    }

    @Override
    public MessageService getMessageService() {
        return MessageServiceImpl.INSTANCE;
    }

    @Override
    public PersonalService getPersonalService() {
        return null;
    }

    @Override
    public SnsService getSnsService() {
        return null;
    }

    @Override
    public Future<Void> onInit(JsonObject cfg) {
        FileSystem fs = Context.vertx.fileSystem();
        return fs.mkdirs(new File(JBotFiles.ADAPTER_DIR, NAME).getPath())
                .onSuccess(v -> {
                    // 如果存在配置文件，则合并配置
                    if (fs.existsBlocking(CONFIG_PATH)) cfg.mergeIn(fs.readFileBlocking(CONFIG_PATH).toJsonObject());
                    log.debug("adapter config: {}", cfg.encodePrettily());
                    // 启动协议服务
                    Util.startServer(cfg);
                    APadAdapter.config = cfg;
                    log.info(NAME + " 初始化完成");
                });
    }

    @Override
    public Future<Void> onDestroy() {
        return saveConfig();
    }

    public static Future<Void> saveConfig() {
        return Context.vertx.fileSystem()
                .writeFile(CONFIG_PATH, Buffer.buffer(config.encodePrettily()))
                .onSuccess(v -> log.debug("配置文件保存成功"));
    }
}
