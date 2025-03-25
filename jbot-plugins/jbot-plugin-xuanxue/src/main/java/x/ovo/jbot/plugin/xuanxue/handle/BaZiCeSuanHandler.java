package x.ovo.jbot.plugin.xuanxue.handle;

import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.bean.BeanUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.json.JSONUtil;
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

import java.util.*;

/**
 * 八字测算处理程序
 * <p>
 *
 * @author Liwncy
 * @since 1.0.0
 */
@Slf4j(topic = "BaZiCeSuanHandler")
public class BaZiCeSuanHandler {

    // 定义关键词数组（建议声明为常量）
    private static final String[] CESUAN_KEYWORDS = {"基本信息", "姻缘分析", "运程分析", "命运分析"};

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
        String[] split = content.split("\\|");
        if (split.length != 7) {
            from.send("格式错误，请按照格式发送：八字测算|姓名|性别|年|月|日|时");
            return;
        }
        CesuanDto dto = CesuanDto.builder().name(split[1]).sex(StrUtil.equals("男", split[2]) ? "male" : "female").year(split[3]).month(split[4]).day(split[5]).hours(split[6]).build();
        String result = HttpUtil.post(url, BeanUtil.beanToMap(dto));
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
            sb.append("\n").append("--------").append(element.text()).append("--------").append("\n");
            sb.append(element.nextElementSibling().text());
        }
        msg.setContent(sb.toString());
        from.send(msg);
    }


}
