package x.ovo.jbot.adapter.apad.service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import x.ovo.jbot.adapter.apad.APadAdapter;
import x.ovo.jbot.adapter.apad.ApiUtil;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.message.MessageFactory;

import java.time.Instant;
import java.util.Objects;

@Slf4j
public class SyncService {

    public static void start() {
        var body = JsonObject.of(
                "Scene", 0,
                "Synckey", "",
                "Wxid", APadAdapter.getConfig().getString("wxid")
        );
        Context.vertx.setPeriodic(500, id -> ApiUtil.post("/Sync", body)
                .map(res -> res.getJsonObject("Data"))
                .onSuccess(data -> {
                    JsonArray msgs = data.getJsonArray("AddMsgs");
                    if (Objects.nonNull(msgs) && !msgs.isEmpty()) {
                        msgs.forEach(msg -> {
                            if (Instant.now().toEpochMilli() / 1000 - ((JsonObject) msg).getLong("CreateTime") > 60) return;
                            log.debug("接收到新消息：{}", ((JsonObject) msg).encodePrettily());
                            ((JsonObject) msg).put("ToUserName", ((JsonObject) msg).getJsonObject("ToWxid"));
                            Context.get().getMessageManager().addReceive(MessageFactory.convert((JsonObject) msg));
                        });
                    }
                }));
    }

}
