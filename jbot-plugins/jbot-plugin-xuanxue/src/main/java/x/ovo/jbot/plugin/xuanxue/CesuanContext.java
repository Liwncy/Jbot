package x.ovo.jbot.plugin.xuanxue;

import io.vertx.core.json.JsonObject;
import lombok.Data;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.reflect.TypeReference;
import org.dromara.hutool.core.regex.ReUtil;
import org.dromara.hutool.core.text.StrUtil;
import x.ovo.jbot.core.message.entity.TextMessage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 聊天上下文
 *
 * @author ovo on 2024/10/24.
 * @since 1.0.0
 */
@Data
public class CesuanContext {

    private static final Pattern REG = Pattern.compile("(\\[(?<service>[^]]+)?])?(\\[(?<model>[^]]+)?])?(\\[(?<prompt>[^]]+)?])?");

    private String chatId;
    private String serviceName;
    private Map<String, String> service;
    private TextMessage message;

    public static CesuanContext creat(JsonObject config, String serviceName, String chatId, TextMessage message) {
        CesuanContext context = new CesuanContext();
        context.chatId = chatId;
        context.message = message;

        List<Map<String, String>> serviceMapping = config.getJsonArray("keywordMapping").getList();
        context.service=serviceMapping.stream()
                .filter(e -> serviceName.equals(e.get("keyword")))
                .findAny()
                .orElse(null);
        // 替换消息正文配置
        context.message.setContent(ReUtil.delPre(serviceName, context.message.getContent()).trim());
        return context;
    }
}
