package x.ovo.jbot.plugin;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.http.HttpUtil;
import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.event.MessageEvent;
import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.core.message.entity.appmsg.LinkMessage;
import x.ovo.jbot.core.plugin.Plugin;

@Slf4j
public class EpicPlugin extends Plugin {
    @Override
    public CommandExecutor getCommandExecutor() {
        return null;
    }

    @Override
    public EventListener<?, ?> getEventListener() {
        return new EventListener<MessageEvent<TextMessage>, TextMessage>(this) {

            public static final String URL = "https://60s-api.viki.moe/v2/epic";

            @Override
            public boolean support(@NonNull MessageEvent<TextMessage> event, TextMessage source) {
                return "epic".equals(source.getContent());
            }

            @Override
            public boolean onEvent(@NonNull MessageEvent<TextMessage> event, TextMessage source) {
                var json = Buffer.buffer(HttpUtil.get(URL)).toJsonObject();
                if (json.getInteger("code") != 200) {
                    log.warn("获取失败：{}", json.getString("message"));
                    source.getSender().send("获取失败：" + json.getString("message"));
                    return false;
                } else {
                    json.getJsonArray("data").stream()
                            .map(JsonObject::mapFrom)
                            .filter(data -> data.getBoolean("is_free_now"))
                            .forEach(data -> {
                                var msg = new LinkMessage();
                                msg.setTitle(data.getString("title"));
                                // 图片大小超出：51200byte
                                // msg.setThumbUrl(data.getString("cover"));
                                msg.setThumbUrl("https://bkimg.cdn.bcebos.com/pic/0dd7912397dda14478a7d273beb7d0a20df48641");
                                msg.setDesc(data.getString("description"));
                                msg.setUrl(data.getString("link"));
                                source.getSender().send(msg);
                            });
                }
                return true;
            }

            @Override
            public boolean executeNext() {
                return false;
            }
        };
    }
}
