package x.ovo.jbot.plugin;

import lombok.NonNull;
import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.event.MessageEvent;
import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.core.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class RereadPlugin extends Plugin {
    @Override
    public CommandExecutor getCommandExecutor() {
        return null;
    }

    @Override
    public EventListener<MessageEvent<TextMessage>, TextMessage> getEventListener() {
        return new EventListener<>(this) {

            private static final Map<String, String> MAP = new HashMap<>(16);
            private static final Map<String, String> FLAG = new HashMap<>(16);

            @Override
            public boolean support(@NonNull MessageEvent<TextMessage> event, TextMessage source) {
                return source.isGroup();
            }

            @Override
            public boolean onEvent(@NonNull MessageEvent<TextMessage> event, TextMessage source) {
                if (source.getContent().equals(MAP.get(source.getSender().getId())) && !source.getContent().equals(FLAG.get(source.getSender().getId()))) {
                    FLAG.put(source.getSender().getId(), source.getContent());
                    source.getSender().send(source);
                    return true;
                }
                MAP.put(source.getSender().getId(), source.getContent());
                return false;
            }
        };
    }
}
