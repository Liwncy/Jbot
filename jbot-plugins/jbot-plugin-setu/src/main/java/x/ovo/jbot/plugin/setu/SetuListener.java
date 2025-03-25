package x.ovo.jbot.plugin.setu;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.io.file.FileUtil;
import org.dromara.hutool.core.regex.ReUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.util.RandomUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.json.JSONArray;
import org.dromara.hutool.json.JSONUtil;
import x.ovo.jbot.core.contact.Contactable;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.event.MessageEvent;
import x.ovo.jbot.core.message.entity.ImageMessage;
import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.core.message.entity.VideoMessage;
import x.ovo.jbot.core.plugin.Plugin;

import java.io.File;
import java.time.Instant;
import java.util.List;

/**
 * setu 侦听器
 * <p>
 *
 * @author ovo on 2024/10/25.
 * @since 1.0.0
 */
@Slf4j(topic = "Setu")
public class SetuListener extends EventListener<MessageEvent<TextMessage>, TextMessage> {

    private static final String IMG_URL = "https://v2.api-m.com/api/meinvpic?return=302";
    private static final String HEISI_URL = "https://v2.api-m.com/api/heisi?return=302";
    private static final String BAISI_URL = "https://v2.api-m.com/api/baisi?return=302";
    private static final String GIRL_VIDEO_URL = "https://v0-1.qqsuu.cn/xjj/{}.mp4";
    private static final String BOY_URL = "https://api.52vmy.cn/api/img/tu/boy?type=text";
    private static final List<String> IMG_KEYWORD = List.of("plmm", "漂亮妹妹", "来点美女");
    private static final String SEARCH_IMG_URL = "https://image.so.com/i?src=&inact=0&q=";

    public SetuListener(Plugin plugin) {
        super(plugin);
    }

    @Override
    public boolean support(@NonNull MessageEvent<TextMessage> textMessageMessageEvent, TextMessage textMessage) {
        return true;
    }

    @Override
    public boolean onEvent(@NonNull MessageEvent<TextMessage> textMessageMessageEvent, TextMessage message) {
        String content = message.getContent();
        Contactable fromUser = message.getSender();

        try {
            String url;
            if (IMG_KEYWORD.contains(content)) {
                if (Instant.now().getEpochSecond() % 2 == 0) {
                    url = IMG_URL;
                } else {
                    int id = RandomUtil.randomInt(0, 8600);
                    // File file = FileUtil.file(this.plugin.getDataDir(), "video.mp4");
                    // HttpDownloader.downloadFile(StrUtil.format(GIRL_VIDEO_URL, id), file);
                    // this.context.getApi().sendVideo(fromUser.getNickname(), file);
                    var msg = new VideoMessage();
                    msg.setFileUrl(StrUtil.format(GIRL_VIDEO_URL, id));
                    msg.setThumbUrl(HEISI_URL);
                    msg.setDuration(10);
                    msg.setContent(content);
                    fromUser.send(msg);
                    return true;
                }
            } else if ("来点黑丝".equals(content)) {
                url = HEISI_URL;
            } else if ("来点白丝".equals(content)) {
                url = BAISI_URL;
            } else if ("看看腿".equals(content)) {
                url = Instant.now().getEpochSecond() % 2 == 0 ? HEISI_URL : BAISI_URL;
            } else if ("来点帅哥".equals(content)) {
                url = HttpUtil.get(BOY_URL);
            } else if (content.startsWith("来点")) {
                content = StrUtil.removePrefix(content, "来点");
                String s = HttpUtil.get(SEARCH_IMG_URL + content);
                String jsonString = ReUtil.getGroup1("<script type=\"text/data\" id=\"initData\">(.*?)</script>", s);
                JSONArray list = JSONUtil.parseObj(jsonString).getJSONArray("list");
                long index = Instant.now().getEpochSecond() % list.size();
                url = list.getJSONObject((int) index).getStr("img");
            } else {
                return false;
            }
            // File file = FileUtil.file(this.plugin.getDataDir(), "image.jpg");
            // Downloader.download(url, file);
            // this.context.getApi().sendImage(fromUser.getNickname(), file);
            var msg = new ImageMessage();
            msg.setFileUrl(url);
            msg.setContent(content);
            fromUser.send(msg);
            return true;
        } catch (Exception e) {
            log.error("发送图片失败", e);
        }
        return false;
    }

    @Override
    public boolean executeNext() {
        return false;
    }
}
