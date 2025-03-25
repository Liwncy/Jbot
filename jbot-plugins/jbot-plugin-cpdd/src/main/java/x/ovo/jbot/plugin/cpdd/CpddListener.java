package x.ovo.jbot.plugin.cpdd;

import io.vertx.core.Future;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.reflect.TypeReference;
import org.dromara.hutool.core.regex.ReUtil;
import org.dromara.hutool.core.text.StrUtil;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.common.enums.Gender;
import x.ovo.jbot.core.contact.ContactManager;
import x.ovo.jbot.core.contact.Contactable;
import x.ovo.jbot.core.contact.Member;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.event.MessageEvent;
import x.ovo.jbot.core.message.entity.Message;
import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.core.plugin.Plugin;
import x.ovo.jbot.core.service.GroupService;

import java.util.*;

/**
 * setu 侦听器
 * <p>
 *
 * @author ovo on 2024/10/25.
 * @since 1.0.0
 */
@Slf4j(topic = "Cpdd")
public class CpddListener extends EventListener<MessageEvent<TextMessage>, TextMessage> {

    private static final List<String> CPDD_KEYWORD = List.of("cpdd", "找情缘");
    private GroupService groupService;

    private GroupService getGroupService() {
        if (Objects.isNull(this.groupService)) this.groupService = Context.get().getAdapter().getGroupService();
        return this.groupService;
    }

    /**
     * 配置
     */
    private final Map<String, Object> config;

    public CpddListener(Plugin plugin) {
        super(plugin);
        this.config = plugin.getConfig().getMap();
    }

    @Override
    public boolean support(@NonNull MessageEvent<TextMessage> textMessageMessageEvent, TextMessage textMessage) {
        String content = textMessage.getContent();
        if (!textMessage.isGroup()) {
            return false;
        }
        List<String> keywords = MapUtil.get(this.config, "keywords", new TypeReference<List<String>>() {
        });
        if (keywords.stream().anyMatch(keyword -> ReUtil.contains(keyword, textMessage.getContent()))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onEvent(@NonNull MessageEvent<TextMessage> textMessageMessageEvent, TextMessage message) {
        String content = message.getContent();
        Contactable fromUser = message.getSender();
        Member member = message.getMember();
        try {
            Collection<Member> members = this.getGroupService().getMembers(fromUser.getId()).await();
            List<String> memberIds = members.stream().map(Member::getId).toList();
            if (memberIds.size() > 20) {
                Collections.shuffle(new ArrayList<>(memberIds));
                memberIds = new ArrayList<>(memberIds.subList(0, 19));
                memberIds.add(member.getId());
            }
            String mName = member.getDisplayName();
            members = this.getGroupService().getMemberInfo(fromUser.getId(), memberIds).await();
            Member finalMember = member;
            member = members.stream().filter(mb -> Objects.equals(mb.getId(), finalMember.getId())).findFirst().orElse(null);
            // 过滤掉发送者
            Member finalMember1 = member;
            List<Member> filteredMembers = members.stream()
                    .filter(mb -> !Objects.equals(mb.getGender(), finalMember1.getGender()))
                    .toList();
            if (filteredMembers.isEmpty()) {
                message.getSender().send("哎呀，哪有人要跟你cpdd啊，你自己玩去吧！");
                return true;
            }
            // 使用 Random 类随机选择一个成员
            Random random = new Random();
            Member spouse = filteredMembers.get(random.nextInt(filteredMembers.size()));

            TextMessage msg = new TextMessage();
            // msg.setAts(spouse.getId());
            // msg.setContent("@" + spouse.getNickname() + " " + this.judgeSalutation(spouse.getGender()) + "快点来呀！\n" +
            //         mName + " " + this.judgeSalutation(member.getGender()) + "正在找情缘呢！\n" +
            //         this.judgeSalutation(member.getGender()) + "虽然憨憨呆呆的,但" + this.judgeSalutation(member.getGender()) + this.descriptions(member.getGender()) + "\n" +
            //         "小聪明儿觉得你们非常合适呢!!!\n" +
            //         "喜欢就大胆去追求吧！(●'◡'●)！"
            // );
            msg.setAts(member.getId() + "," + spouse.getId() + "," + spouse.getId());
            msg.setContent("哎呀，这不是 @" + member.getNickname() + " " + this.judgeSalutation(member.getGender()) + "吗？\n" +
                    "这么好看也来找CP呀，交给我小聪明儿吧。\n" +
                    "帮你 @" + spouse.getNickname() + " " + this.judgeSalutation(spouse.getGender()) + "怎么样呀？\n" +
                    "@" + spouse.getNickname() + " " + this.judgeSalutation(spouse.getGender()) + this.descriptions(spouse.getGender()) + "\n" +
                    "喜欢就大胆去追求吧！(●'◡'●)！");
            // message.getSender().send("哎呀，这不是 @" + member.getNickname() + " " + this.judgeSalutation(member.getGender()) + "吗？\n" +
            //                 "这么好看也来找CP呀，交给我小聪明儿吧。\n" +
            //                 "帮你 @" + spouse.getNickname() + " " + this.judgeSalutation(spouse.getGender()) + "怎么样呀？\n" +
            //                 "@" + spouse.getNickname() + " " + this.judgeSalutation(spouse.getGender()) + this.descriptions(spouse.getGender()) + "\n" +
            //                 "喜欢就大胆去追求吧！(●'◡'●)！");
                    message.getSender().send(msg);
        } catch (Exception e) {
            log.error("处理消息时发生错误", e);
        }
        return true;
    }

    @Override
    public boolean executeNext() {
        return false;
    }

    private String judgeSalutation(Gender gender) {
        switch (gender) {
            case MALE:
                return "哥哥";
            case FEMALE:
                return "姐姐";
            default:
                return "乖乖";
        }
    }

    private String descriptions(Gender gender) {
        if (gender == Gender.UNKNOWN) {
            return "性别未知,神秘莫测!";
        }
        List<String> descriptions = MapUtil.get(this.config, "descriptions" + (gender == Gender.MALE ? "_gg" : "_jj"), new TypeReference<List<String>>() {
        });
        Random random = new Random();
        return descriptions.get(random.nextInt(descriptions.size()));
    }
}
