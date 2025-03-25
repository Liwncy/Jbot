package x.ovo.jbot.plugin.ai;

import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.reflect.TypeReference;
import org.dromara.hutool.core.text.StrUtil;
import picocli.CommandLine;
import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.common.enums.ContactType;
import x.ovo.jbot.core.contact.Contactable;
import x.ovo.jbot.core.contact.Member;
import x.ovo.jbot.core.plugin.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * AI 命令执行器
 *
 * @author ovo on 2024/10/29.
 * @since 1.0.0
 */
@SuppressWarnings("unused")
@Slf4j(topic = "AiExecutor")
@CommandLine.Command(name = "ai", description = "ai设置")
public class AiExecutor extends CommandExecutor {

    public AiExecutor(Plugin plugin) {
        super(plugin);
    }

    // @Override
    // public boolean hasPermission(String user) {
    //     return true;
    // }

    @CommandLine.Command(name = "help", description = "显示插件帮助信息")
    public String help() {
        return MapUtil.getStr(this.plugin.getConfig().getMap(), "help");
    }

    @CommandLine.Command(name = "start", description = "开启自由对话", sortOptions = false)
    public String add(@CommandLine.Option(names = {"-s", "--service"}, description = "服务") String service,
                      @CommandLine.Option(names = {"-m", "--model"}, description = "模型") String model,
                      @CommandLine.Option(names = {"-p", "--prompt"}, description = "提示词") String prompt,
                      @CommandLine.Option(names = {"--as-member", "--asmember"}, description = "是否模拟群成员发言") boolean asMember
    ) {
        log.debug("message: {}, service: {}, model: {}, prompt: {}, as-member: {}", this.command.getMessage().getContent(), service, model, prompt, asMember);
        Contactable from = this.command.getMessage().getSender();
        Contactable member = this.command.getMessage().getMember();
        String username = Optional.ofNullable(this.command.getMessage().getMember()).map(Contactable::getNickname).orElse(this.command.getMessage().getSender().getNickname());
        Map<String, Object> config = this.plugin.getConfig().getMap();
        // 如果已经开启了模拟群成员或自由对话，则返回提示
        if (x.ovo.jbot.plugin.ai.AiListener.AS_MEMBER.contains(from.getNickname()) || x.ovo.jbot.plugin.ai.AiListener.FREE_TALK.containsKey(username)) {
            return StrUtil.format("已经开启过自由对话了，当前对话模型为 {}，如果需要关闭，请发送 ai stop",
                    StrUtil.defaultIfBlank(x.ovo.jbot.plugin.ai.AiListener.FREE_TALK.get(from.getNickname()), x.ovo.jbot.plugin.ai.AiListener.FREE_TALK.get(username)));
        }

        // 如果开启模拟群成员
        if (asMember && from.getType() == ContactType.GROUP) {
            x.ovo.jbot.plugin.ai.AiListener.AS_MEMBER.add(from.getNickname());
            prompt = StrUtil.defaultIfBlank(prompt, "群成员");
        }
        // 设置自由对话的配置
        service = StrUtil.defaultIfBlank(service, MapUtil.getStr(config, "default"));
        model = StrUtil.defaultIfBlank(model, MapUtil.get(config, "services", new TypeReference<Map<String, Map<String, String>>>() {}).get(service).get("model"));
        prompt = StrUtil.defaultIfBlank(prompt, MapUtil.getStr(config, "default_prompt"));
        String data = StrUtil.format("[{}][{}][{}]", service, model, prompt);
        x.ovo.jbot.plugin.ai.AiListener.FREE_TALK.put(asMember ? from.getNickname() : username, data);
        log.info("开启自由对话成功 {}", data);
        return "开启自由对话成功，当前对话模型为 " + data + "，如果需要关闭，请发送 /ai stop";
    }

    @CommandLine.Command(name = "stop", description = "关闭自由对话")
    public String stop() {
        Contactable from = this.command.getMessage().getSender();
        Member member = this.command.getMessage().getMember();
        // 判断联系人是否未开启了自由对话
        boolean contactNotEnable = !x.ovo.jbot.plugin.ai.AiListener.FREE_TALK.containsKey(from.getNickname());
        // 判断群成员是否未开启自由对话
        boolean memberNotEnable = !(Objects.nonNull(member) && x.ovo.jbot.plugin.ai.AiListener.FREE_TALK.containsKey(member.getNickname()));
        // 都没有开启过
        if (contactNotEnable && memberNotEnable) return "你没有开启过自由对话";
        x.ovo.jbot.plugin.ai.AiListener.AS_MEMBER.remove(from.getNickname());
        x.ovo.jbot.plugin.ai.AiListener.FREE_TALK.remove(from.getNickname());
        if (Objects.nonNull(member)) x.ovo.jbot.plugin.ai.AiListener.FREE_TALK.remove(member.getNickname());
        this.clear();
        return "关闭自由对话成功";
    }

    @CommandLine.Command(name = "clear", description = "清除上下文")
    public String clear() {
        x.ovo.jbot.plugin.ai.AiListener.CACHE.remove(Optional.ofNullable(this.command.getMessage().getMember()).map(Contactable::getNickname).orElse(this.command.getMessage().getSender().getNickname()));
        return "清除上下文成功";
    }
}
