package x.ovo.jbot.plugin.xuanxue;

import io.vertx.core.json.JsonObject;
import lombok.NonNull;
import org.dromara.hutool.core.cache.CacheUtil;
import org.dromara.hutool.core.cache.impl.TimedCache;
import org.dromara.hutool.core.data.id.IdUtil;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.event.MessageEvent;
import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.core.plugin.Plugin;
import x.ovo.jbot.plugin.xuanxue.handle.BaZiCeSuanHandler;
import x.ovo.jbot.plugin.xuanxue.handle.MeiRiYiGuaHandler;
import x.ovo.jbot.plugin.xuanxue.handle.TaLuoPaiZhanBuGuaHandler;

import java.util.*;

/**
 * 玄学测算 侦听器
 *
 * @author Liwncy created on 2025/03/17.
 */
public class XuanxueListener extends EventListener<MessageEvent<TextMessage>, TextMessage> {

    private JsonObject config;
    private static final Random RANDOM = new Random();
    public static List<String> KEYWORDS = new ArrayList<>();
    public static List<Map<String, String>> KEYWORD_MAPPING = new ArrayList<>();

    /**
     * 开启自由对话的用户{用户名username：[service][model][prompt_name]}
     */
    public static final Map<String, String> FREE_TALK = new HashMap<>(16);
    public static final TimedCache<String, String> CACHE = CacheUtil.newTimedCache(900000000, 60000);

    public XuanxueListener(Plugin plugin) {
        super(plugin);
        // KEYWORD_MAPPING = (List<Map<String, String>>) config.getJsonArray("keywordMapping").getList();
        config = plugin.getConfig();
        KEYWORDS = config.getJsonArray("keywords").getList();
    }

    @Override
    public boolean support(@NonNull MessageEvent<TextMessage> event, TextMessage source) {
        return KEYWORDS.stream().anyMatch(s -> source.getContent().toLowerCase().startsWith(s));
    }

    @Override
    public boolean onEvent(@NonNull MessageEvent<TextMessage> event, TextMessage source) {
        String username = source.getSender().getNickname();
        if (source.getContent().toLowerCase().startsWith("八字测算")) {
            CesuanContext chatContext = CesuanContext.creat(this.config, "八字测算", CACHE.get(username, IdUtil::fastSimpleUUID), source);
            BaZiCeSuanHandler.handle(chatContext);
        }
        if(source.getContent().toLowerCase().startsWith("每日一卦")){
            CesuanContext chatContext = CesuanContext.creat(this.config, "每日一卦", CACHE.get(username, IdUtil::fastSimpleUUID), source);
            MeiRiYiGuaHandler.handle(chatContext);
        }
        if(source.getContent().toLowerCase().startsWith("翻塔罗牌")){
            CesuanContext chatContext = CesuanContext.creat(this.config, "翻塔罗牌", CACHE.get(username, IdUtil::fastSimpleUUID), source);
            TaLuoPaiZhanBuGuaHandler.handle(chatContext);
        }
        // var res = HttpUtil.get("https://vme.im/api?format=text");
        // source.getSender().send(res);
        return true;
    }
}
