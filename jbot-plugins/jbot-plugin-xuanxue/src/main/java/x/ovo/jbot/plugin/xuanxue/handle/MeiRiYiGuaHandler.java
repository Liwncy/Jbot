package x.ovo.jbot.plugin.xuanxue.handle;

import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.bean.BeanUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.http.client.ClientConfig;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.engine.ClientEngine;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import x.ovo.jbot.core.contact.Contactable;
import x.ovo.jbot.core.contact.Member;
import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.core.plugin.Plugin;
import x.ovo.jbot.plugin.xuanxue.CesuanContext;
import x.ovo.jbot.plugin.xuanxue.dto.CesuanDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 每日一占处理程序
 * <p>
 *
 * @author Liwncy
 * @since 1.0.0
 */
@Slf4j(topic = "MeiRiYiZhanHandler")
public class MeiRiYiGuaHandler {

    private final static String KEYWORD = "每日一卦";
    private static Plugin plugin;
    private static String url;
    public static List<Map<String, String>> KEYWORD_MAPPING = new ArrayList<>();
    // 定义关键词数组（建议声明为常量）
    private static final String[] CESUAN_KEYWORDS = {"基本信息", "解卦信息"};

    /**
     * 处理聊天
     *
     * @param ctx CTX 聊天上下文
     */
    public static void handle(CesuanContext ctx) {
        Contactable from = ctx.getMessage().getSender();
        Member member = ctx.getMessage().getMember();
        Map<String, String> service = ctx.getService();
        String url = service.get("url");
        // "八字测算|姓名|性别|年|月|日|时"
        String content = ctx.getMessage().getContent();
        // ClientConfig clientConfig = ClientConfig.of()
        //         .setHttpProxy("155.54.239.64", 80);
        // ClientEngine engine = HttpUtil.createClient("engine_name").init(clientConfig);
        // clientConfig.setHttpProxy("155.54.239.64", 80);
        String result = HttpUtil.post(url,"{}");
        // 解析 HTML 字符串
        Document document = Jsoup.parse(result);
        Elements elements = document.select("div.panel-heading");
        TextMessage msg = new TextMessage();
        msg.setAts(member.getId());
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(member.getNickname());
        for (Element element : elements) {
            if (!StrUtil.containsAny(element.toString(), CESUAN_KEYWORDS)) {
                continue;
            }
            sb.append("\n").append(element.text()).append("\n");
            for (Element ep : element.nextElementSibling().children()){
                sb.append(ep.text()).append("\n");
            }
        }
        msg.setContent(sb.toString());
        from.send(msg);
    }


}
