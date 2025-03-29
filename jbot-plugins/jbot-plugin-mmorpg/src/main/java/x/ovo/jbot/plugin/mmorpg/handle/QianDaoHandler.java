package x.ovo.jbot.plugin.mmorpg.handle;

import lombok.extern.slf4j.Slf4j;

import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.plugin.mmorpg.dto.Player;

/**
 * 签到处理程序
 * <p>
 *
 * @author Liwncy
 * @since 1.0.0
 */
@Slf4j(topic = "#QianDaoHandler")
public class QianDaoHandler {


    /**
     * 签到处理程序
     *
     * @param message message 聊天上下文
     * @param player
     */
    public static void handle(TextMessage message, Player player) {
        String wxId = message.getMember().getId();


    }


}
