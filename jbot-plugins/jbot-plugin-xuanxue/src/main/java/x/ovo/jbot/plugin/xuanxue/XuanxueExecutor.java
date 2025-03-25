package x.ovo.jbot.plugin.xuanxue;

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
@Slf4j(topic = "XuanxueExecutor")
@CommandLine.Command(name = "xuanxue", description = "玄学设置")
public class XuanxueExecutor extends CommandExecutor {

    public XuanxueExecutor(Plugin plugin) {
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
}
