package x.ovo.jbot.plugin;

import lombok.NonNull;
import org.dromara.hutool.core.convert.ConvertUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.util.RandomUtil;
import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.common.util.ContactUtil;
import x.ovo.jbot.core.contact.Contactable;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.event.MessageEvent;
import x.ovo.jbot.core.message.entity.Message;
import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.core.plugin.Plugin;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TipsPlugin extends Plugin {
    @Override
    public CommandExecutor getCommandExecutor() {
        return new TipsExecutor(this);
    }


    @Override
    public EventListener<MessageEvent<Message>, Message> getEventListener() {
        return new EventListener<>(this) {

            private static boolean replace = false;
            private static final Map<String, Long> LAST = new HashMap<>(16);
            private static final Map<String, List<String>> MAP = new HashMap<>(16);

            @Override
            public boolean support(@NonNull MessageEvent<Message> event, Message source) {
                // 非群消息或群成员为空，返回false
                if (!source.isGroup() || source.getMember() == null) return false;
                // 配置文件中不包含该成员，返回false
                this.replaceName();
                if (!MAP.containsKey(source.getMember().getId())) return false;
                // 距离该成员上次发言时间未超过配置时间，返回false
                var flag = source.getSender().getId() + ":" + source.getMember().getId();
                var now = System.currentTimeMillis();
                var last = LAST.computeIfAbsent(flag, k -> 0L);
                var duration = Duration.ofMillis(now - last).toMinutes();
                // 更新时间
                LAST.put(flag, now);
                return duration > this.plugin.getConfig().getInteger("timeout");
            }

            @Override
            public boolean onEvent(@NonNull MessageEvent<Message> event, Message source) {
                // 获取被提示群成员群备注或昵称
                var name = StrUtil.defaultIfBlank(source.getMember().getDisplayName(), source.getMember().getNickname());
                var msg = new TextMessage();
                if (this.plugin.getConfig().getBoolean("at")) msg.setAts(source.getMember().getId());
                // 获取提示词
                var list = MAP.get(source.getMember().getId());
                var tip = list.get(RandomUtil.randomInt(list.size()));
                msg.setContent("@" + name + " " + tip);
                source.getSender().send(msg);
                return true;
            }

            private void replaceName() {
                if (replace) return;
                this.plugin.getConfig().getJsonObject("tips").getMap()
                        .forEach((key, value) -> MAP.put(
                                Optional.ofNullable(ContactUtil.fromString(key)).map(Contactable::getId).orElse("null"),
                                ConvertUtil.toList(String.class, value))
                        );
                replace = true;
            }
        };
    }
}
