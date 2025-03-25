package x.ovo.jbot.plugin.ai;

import lombok.Data;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.reflect.TypeReference;
import org.dromara.hutool.core.regex.ReUtil;
import org.dromara.hutool.core.text.StrUtil;
import x.ovo.jbot.core.message.entity.TextMessage;

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
public class ChatContext {

    private static final Pattern REG = Pattern.compile("(\\[(?<service>[^]]+)?])?(\\[(?<model>[^]]+)?])?(\\[(?<prompt>[^]]+)?])?");

    private String model;
    private String prompt;
    private String chatId;
    private String serviceName;
    private boolean stream = false;
    private Map<String, Object> service;
    private TextMessage message;

    public static ChatContext creat(Map<String, ?> config, String chatConfig, String chatId, TextMessage message) {
        ChatContext context = new ChatContext();
        context.chatId = chatId;
        context.message = message;

        Set<String> services = MapUtil.get(config, "services", new TypeReference<Map<String, Object>>() {}).keySet();
        String s = ReUtil.get(REG, StrUtil.defaultIfBlank(chatConfig, message.getContent()), "service");
        context.serviceName = StrUtil.defaultIfBlank(services.contains(s) ? s : "", MapUtil.getStr(config, "default"));
        context.service = MapUtil.get(config, "services", new TypeReference<Map<String, Map<String, Object>>>() {}).get(context.getServiceName());
        context.model = StrUtil.defaultIfBlank(ReUtil.get(REG, StrUtil.defaultIfBlank(chatConfig, message.getContent()), "model"), MapUtil.getStr(context.service, "model"));
        context.prompt = MapUtil.get(config, "prompts", new TypeReference<Map<String, String>>() {})
                .get(StrUtil.defaultIfBlank(ReUtil.get(REG, StrUtil.defaultIfBlank(chatConfig, message.getContent()), "prompt"), MapUtil.getStr(config, "default_prompt")));
        // 替换消息正文配置
        context.message.setContent(ReUtil.delPre(REG, context.message.getContent()));
        return context;
    }
}
