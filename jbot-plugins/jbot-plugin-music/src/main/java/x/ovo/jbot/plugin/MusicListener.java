package x.ovo.jbot.plugin;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.regex.ReUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.HttpUtil;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.event.MessageEvent;
import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.core.message.entity.appmsg.MusicMessage;
import x.ovo.jbot.core.plugin.Plugin;

@Slf4j
public class MusicListener extends EventListener<MessageEvent<TextMessage>, TextMessage> {

    public static final String URL = "https://www.hhlqilongzhu.cn/api/dg_wyymusic.php?gm={}&n=1&br=2&type=json";

    public static final String XML = "<appmsg appid=\"{provider}\" sdkver=\"0\"><title>{title}</title><des>{singer}</des><action>view</action><type>76</type><showtype>0</showtype><content/><url>{url}</url><dataurl>{music_url}</dataurl><lowurl>{url}</lowurl><lowdataurl>{music_url}</lowdataurl><recorditem/><thumburl>{cover_url}</thumburl><messageaction/><laninfo/><extinfo/><sourceusername/><sourcedisplayname/><songlyric>{lyric}</songlyric><commenturl/><appattach><totallen>0</totallen><attachid/><emoticonmd5/><fileext/><aeskey/></appattach><webviewshared><publisherId/><publisherReqId>0</publisherReqId></webviewshared><weappinfo><pagepath/><username/><appid/><appservicetype>0</appservicetype></weappinfo><websearch/><songalbumurl>{cover_url}</songalbumurl></appmsg><fromusername>{wxid}</fromusername>";

    public MusicListener(Plugin plugin) {
        super(plugin);
    }

    @Override
    public boolean support(@NonNull MessageEvent<TextMessage> event, TextMessage source) {
        return source.getContent().split(" ").length == 2 && ReUtil.contains("^(音乐|点歌|music) ", source.getContent());
    }

    @Override
    public boolean onEvent(@NonNull MessageEvent<TextMessage> event, TextMessage source) {
        var json = Buffer.buffer(HttpUtil.get(StrUtil.format(URL, source.getContent().split(" ")[1]))).toJsonObject();
        var data = JsonObject.of(
                "title", json.getString("title"),
                "singer", json.getString("singer"),
                "url", json.getString("link"),
                "music_url", json.getString("music_url").split("\\?")[0],
                "cover_url", json.getString("cover"),
                "lyric", json.getString("lrc"),
                "wxid", source.getReceiver().getId(),
                "provider", Provider.get()
        );
        var msg = new MusicMessage();
        msg.setTitle(data.getString("title"));
        msg.setDesc(data.getString("singer"));
        msg.setDataUrl(data.getString("music_url"));
        msg.setUrl(data.getString("url"));
        msg.setXml(StrUtil.formatByMap(XML, data.getMap()));
        source.getSender().send(msg);
        return true;
    }
}
