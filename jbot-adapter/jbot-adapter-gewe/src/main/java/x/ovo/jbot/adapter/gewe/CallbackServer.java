package x.ovo.jbot.adapter.gewe;

import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import x.ovo.jbot.adapter.gewe.service.LoginServiceImpl;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.message.MessageFactory;
import x.ovo.jbot.core.message.MessageManager;

import java.util.Optional;

/**
 * 回调服务
 *
 * @author ovo created on 2025/02/23.
 */
@Slf4j
public class CallbackServer {

    private static MessageManager manager;

    public static void start() {
        Context.vertx.createHttpServer()
                .requestHandler(req -> req.bodyHandler(body -> {
                    JsonObject data = body.toJsonObject();
                    log.debug("收到消息：{}", data.encodePrettily());
                    req.response().end();
                    if (!data.containsKey("data") && !data.containsKey("Data")) return;
                    // 拒收历史消息
                    if(System.currentTimeMillis()/1000 - data.getJsonObject("Data").getLong("CreateTime") >= 100)
                        return;
                    Optional.ofNullable(manager).orElseGet(() -> {
                        var m = Context.get().getMessageManager();
                        manager = m;
                        return m;
                    }).addReceive(MessageFactory.convert(Optional.ofNullable(data.getJsonObject("data")).orElse(data.getJsonObject("Data"))));
                }))
                .listen(GeweAdapter.getConfig().getInteger("callback_port", 8511))
                .onSuccess(server -> {
                    log.info("消息回调服务启动成功，监听端口: {}", server.actualPort());
                    LoginServiceImpl.INSTANCE.setCallback()
                            .onSuccess(v -> log.debug("消息回调设置成功"))
                            .onFailure(t -> log.warn("消息回调设置失败: {}", t.getMessage()));
                })
                .onFailure(err -> log.warn("消息回调服务启动失败: {}", err.getMessage()));
    }
}
