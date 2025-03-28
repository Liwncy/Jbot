package x.ovo.jbot.core.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import x.ovo.jbot.core.contact.*;

import java.util.List;

/**
 * 联系人类型
 *
 * @author ovo created on 2025/02/17.
 */
@Getter
@RequiredArgsConstructor
public enum ContactType {

    USER("自己", Friend.class),
    FRIEND("好友", Friend.class),
    GROUP("群组", Group.class),
    STRANGER("陌生人", Friend.class),
    OFFICIAL_ACCOUNT("公众号", OfficialAccount.class),
    SPECIAL("特殊账号", SpecialAccount.class),
    MEMBER("群成员", Member.class),
    UNKNOWN("未知", Friend.class);

    private final String desc;
    private final Class<? extends Contactable> clazz;
    private static final String[] SPECIAL_ACCOUNT = {"weixin", "filebox", "tmessage", "qmessage", "fmessage", "qqmail", "qqsafe", "medianote"};

    public static ContactType of(String id) {
        if (id.endsWith("@chatroom")) return ContactType.GROUP;
        if (id.startsWith("gh_")) return ContactType.OFFICIAL_ACCOUNT;
        if (List.of(SPECIAL_ACCOUNT).contains(id)) return ContactType.SPECIAL;
        return ContactType.FRIEND;
    }
}
