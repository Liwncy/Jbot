package x.ovo.jbot.plugin;

import io.vertx.core.buffer.Buffer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.date.DateUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.HttpUtil;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.event.MessageEvent;
import x.ovo.jbot.core.message.entity.ImageMessage;
import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.core.message.entity.VideoMessage;
import x.ovo.jbot.core.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class NewsListener extends EventListener<MessageEvent<TextMessage>, TextMessage> {


    /** 新闻网址 */
//    private static final String NEWS_URL = "https://api.jun.la/60s.php?format=imgapi";
//    private static final String NEWS_URL = "https://api.lbbb.cc/api/60s";
    private static final String NEWS_URL = "https://jx.iqfk.top/60s/{}.png";
    /** Chigua 网址 */
    private static final String CHIGUA_URL = "https://dayu.qqsuu.cn/mingxingbagua/apis.php";
    /** 摸鱼日历api */
    private static final String RILI_URL = "https://api.vvhan.com/api/moyu?type=json";
    /** 视频日报 URL */
    private static final String VIDEO_URL = "https://dayu.qqsuu.cn/moyuribaoshipin/apis.php";

    private final Map<String, Consumer<TextMessage>> handlers = new HashMap<>();

    public NewsListener(Plugin plugin) {
        super(plugin);
        this.handlers.put("今日新闻", this.news());
        this.handlers.put("今日吃瓜", this.chigua());
        this.handlers.put("摸鱼日历", this.moyurili());
        this.handlers.put("视频日报", this.video());
    }

    @Override
    public boolean support(@NonNull MessageEvent<TextMessage> event, TextMessage source) {
        return this.handlers.containsKey(source.getContent());
    }

    @Override
    public boolean onEvent(@NonNull MessageEvent<TextMessage> event, TextMessage source) {
        try {
            this.handlers.get(source.getContent()).accept(source);
            return true;
        } catch (Exception e) {
            log.warn(e.getMessage());
            return false;
        }
    }

    private Consumer<TextMessage> news() {
        return message -> {
            var img = new ImageMessage();
            var url = StrUtil.format(NEWS_URL, DateUtil.formatToday().replace("-", ""));
            img.setFileUrl(url);
            img.send(message.getSender()).onComplete(ar -> log.info("发送 每日新闻 {}", ar.succeeded() ? "成功" : "失败"));
        };
    }

    private Consumer<TextMessage> chigua() {
        return message -> {
            var img = new ImageMessage();
            img.setFileUrl(CHIGUA_URL);
            img.send(message.getSender()).onComplete(ar -> log.info("发送 每日吃瓜 {}", ar.succeeded() ? "成功" : "失败"));
        };
    }

    private Consumer<TextMessage> moyurili() {
        return message -> {
            var url = Buffer.buffer(HttpUtil.get(RILI_URL)).toJsonObject().getString("url");
            var img = new ImageMessage();
            img.setFileUrl(url);
            img.send(message.getSender()).onComplete(ar -> log.info("发送 摸鱼日历 {}", ar.succeeded() ? "成功" : "失败"));
        };
    }

    private Consumer<TextMessage> video() {
        return message -> {
            var img = new VideoMessage();
            var url = Buffer.buffer(HttpUtil.get(RILI_URL)).toJsonObject().getString("url");
            img.setFileUrl(VIDEO_URL);
            img.setThumbUrl(url);
            img.setDuration(14);
            img.send(message.getSender()).onComplete(ar -> log.info("发送 视频日报 {}", ar.succeeded() ? "成功" : "失败"));
        };
    }

    @Override
    public boolean executeNext() {
        return false;
    }


}
