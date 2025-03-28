package x.ovo.jbot.core;

import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.Setter;
import x.ovo.jbot.core.adapter.Adapter;
import x.ovo.jbot.core.command.CommandManager;
import x.ovo.jbot.core.contact.ContactManager;
import x.ovo.jbot.core.contact.Friend;
import x.ovo.jbot.core.event.EventManager;
import x.ovo.jbot.core.message.MessageManager;
import x.ovo.jbot.core.plugin.PluginManager;

/**
 * 上下文
 *
 * @author ovo created on 2025/02/17.
 */
@Setter
@Getter
public final class Context {

    public static final Vertx vertx = Vertx.vertx();
    private static final Context INSTANCE = new Context();

    static {
        vertx.exceptionHandler(Throwable::printStackTrace);
    }

    private Friend bot;
    private Friend owner;
    private JBotConfig config;

    private EventManager eventManager;
    private PluginManager pluginManager;
    private ContactManager contactManager;
    private MessageManager messageManager;
    private CommandManager commandManager;

    private Adapter adapter;

    public static Context get() {
        return INSTANCE;
    }

}
