package x.ovo.jbot.plugin.kfc;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import lombok.NonNull;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.HttpUtil;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.event.MessageEvent;
import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.core.plugin.Plugin;

import java.util.List;
import java.util.Random;

/**
 * KFC 侦听器
 *
 * @author ovo created on 2025/03/05.
 */
public class KfcListener extends EventListener<MessageEvent<TextMessage>, TextMessage> {

    private JsonArray doc;
    private static final Random RANDOM = new Random();
    public static final List<String> LIST = List.of("kfc", "肯德基", "疯狂星期四", "v我50", "v me 50");

    public KfcListener(Plugin plugin) {
        super(plugin);
//        try (var is = plugin.getClassLoader().getResourceAsStream("doc.json")) {
//            if (is != null) {
//                doc = Buffer.buffer(is.readAllBytes()).toJsonArray();
//            }
//        } catch (Exception ignore){}
    }

    @Override
    public boolean support(@NonNull MessageEvent<TextMessage> event, TextMessage source) {
        return LIST.stream().anyMatch(s -> source.getContent().toLowerCase().contains(s));
    }

    @Override
    public boolean onEvent(@NonNull MessageEvent<TextMessage> event, TextMessage source) {
        var res = HttpUtil.get("https://vme.im/api?format=text");
        source.getSender().send(res);
        return true;
    }
}
