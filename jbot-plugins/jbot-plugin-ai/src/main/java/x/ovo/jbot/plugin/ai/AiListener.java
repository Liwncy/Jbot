package x.ovo.jbot.plugin.ai;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.cache.CacheUtil;
import org.dromara.hutool.core.cache.impl.TimedCache;
import org.dromara.hutool.core.data.id.IdUtil;
import org.dromara.hutool.core.io.file.FileUtil;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.reflect.TypeReference;
import org.dromara.hutool.core.regex.ReUtil;
import org.dromara.hutool.core.text.StrUtil;
import x.ovo.jbot.core.common.enums.ContactType;
import x.ovo.jbot.core.common.enums.MessageType;
import x.ovo.jbot.core.contact.Member;
import x.ovo.jbot.core.event.MessageEvent;
import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.core.plugin.Plugin;

import java.io.File;
import java.util.*;
// import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * AI 插件事件监听器
 *
 * @author ovo on 2024/10/24.
 * @since 1.0.0
 */
@Slf4j(topic = "AiListener")
public class AiListener extends x.ovo.jbot.core.event.EventListener<MessageEvent<TextMessage>, TextMessage> {

    /**
     * 开启自由对话的用户{用户名username：[service][model][prompt_name]}
     */
    public static final Map<String, String> FREE_TALK = new HashMap<>(16);
    /**
     * 上下文缓存 {username: chat_id}
     */
    // public static final TimedCache<String, String> CACHE = CacheUtil.newTimedCache(TimeUnit.MINUTES.toMicros(15), TimeUnit.MINUTES.toMillis(5));
    public static final TimedCache<String, String> CACHE = CacheUtil.newTimedCache(900000000, 60000);
    /**
     * 启用模拟成员发言的群聊username集合
     */
    public static final Set<String> AS_MEMBER = new HashSet<>();
    /**
     * 正则表达式
     */
    private static final Pattern REGEX = Pattern.compile("(?<asMember>as-?[mM]ember\\s?)?(\\[(?<service>[^]]+)?])?(\\[(?<model>[^]]+)?])?(\\[(?<prompt>[^]]+)?])?");

    /**
     * 配置
     */
    private final Map<String, Object> config;

    public AiListener(Plugin plugin) {
        super(plugin);
        this.config = plugin.getConfig().getMap();
        ChatHandler.init(plugin);
    }

    @Override
    public boolean support(@NonNull MessageEvent<TextMessage> event, TextMessage message) {
        if (message.getSender() == null || (message.getSender().getType() != ContactType.FRIEND && message.getSender().getType() != ContactType.GROUP)) {
            return false;
        }
        // 是否直接响应消息
        Boolean directResponse = MapUtil.getBool(this.config, "direct_response", false);
        if (directResponse) return true;
        // 是否随机响应消息
        Boolean randomResponse = MapUtil.getBool(this.config, "random_response", false);
        if (randomResponse && new Random().nextDouble() < 0.05) return true;

        // 是否响应艾特消息，如果是，则响应私聊信息和群聊艾特消息
        Boolean atMe = MapUtil.getBool(this.config, "at_bot", false);
        if (atMe && (!message.isGroup() || StrUtil.isNotEmpty(message.getAts()) || StrUtil.equals("小聪明儿", message.getAts()))) {
            if (message.isGroup()) {
                // 群聊内@消息，需要去掉@bot的部分
                String regex = StrUtil.format("@({}|{})\\s?", message.getReceiver().getNickname(), StrUtil.defaultIfBlank(((Member) message.getReceiver()).getDisplayName(), "null"));
                String content = ReUtil.replaceAll(message.getContent(), regex, "");
                message.setContent(StrUtil.equals("", content) ? "你好" : content);
            }
            return true;
        }

        // 是否已开启自由对话或开启bot模拟成员
        // String username = message.isGroup() ? message.getMember().getUserName() : message.getFrom().getUserName();
        String username = message.isGroup() ? message.getMember().getNickname() : message.getSender().getNickname();
        if (AS_MEMBER.contains(message.getSender().getNickname()) || FREE_TALK.containsKey(username)) return true;

        // 关键词匹配
        List<String> keywords = MapUtil.get(this.config, "keywords", new TypeReference<List<String>>() {
        });
        if (keywords.stream().anyMatch(keyword -> ReUtil.contains(keyword, message.getContent()))) {
            return true;
        }

        // 判断是否以聊天配置正则开头的消息且服务存在于配置文件中
        String regex = "^(\\[(?<service>[^]]+)?])(\\[(?<model>[^]]+)?])?(\\[(?<prompt>[^]]+)?])?\\s?";
        Set<String> services = MapUtil.get(this.config, "services", new TypeReference<Map<String, Object>>() {
        }).keySet();
        return ReUtil.contains(regex, message.getContent()) && services.contains(ReUtil.get(REGEX, message.getContent(), "service"));
    }

    @Override
    public boolean onEvent(@NonNull MessageEvent<TextMessage> event, TextMessage message) {
        String username = message.getSender().getNickname();
        // 如果是群聊，且没有开启模拟成员发言，则username为成员的username
        if (message.isGroup() && !AS_MEMBER.contains(username)) username = message.getMember().getNickname();

        // 绘图
        if (message.getContent().startsWith("draw ")) {
            File file = null;
            try {
                // file = x.ovo.jbot.plugin.ai.DrawService.draw(this.plugin.getDataDir(), message.getContent().substring(5));
                // message.getSender().sendImage(file);
            } catch (Exception e) {
                log.error("绘图失败");
                message.getSender().send("绘图失败");
            } finally {
                if (Objects.nonNull(file) && file.exists())
                    FileUtil.del(file);
            }
            return true;
        }

        // 聊天
        ChatContext chatContext = ChatContext.creat(this.config, FREE_TALK.get(username), CACHE.get(username, IdUtil::fastSimpleUUID), message);
        ChatHandler.handle(chatContext);
        return true;
    }

    @Override
    public boolean executeNext() {
        return false;
    }
}
