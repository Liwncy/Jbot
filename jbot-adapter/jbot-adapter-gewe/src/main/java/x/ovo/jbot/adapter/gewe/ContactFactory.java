package x.ovo.jbot.adapter.gewe;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import x.ovo.jbot.adapter.gewe.service.GroupServiceImpl;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.common.enums.ContactType;
import x.ovo.jbot.core.common.enums.Gender;
import x.ovo.jbot.core.contact.*;

import java.util.Optional;

/**
 * @author ovo created on 2025/02/24.
 */
public class ContactFactory {

    private static ContactManager cm;

    private static ContactManager getManager() {
        return Optional.ofNullable(cm).orElseGet(() -> {
            var m = Context.get().getContactManager();
            cm = m;
            return m;
        });
    }


    public static Future<? extends Contactable> of(JsonObject data) {
        var type = ContactType.of(data.getString("userName"));
        return switch (type) {
            case FRIEND -> buildFriend(data);
            case GROUP -> buildGroup(data);
            case OFFICIAL_ACCOUNT -> buildOfficialAccount(data);
            case SPECIAL -> buildSpecialAccount(data);
            default -> null;
        };
    }

    private static Future<Friend> buildFriend(JsonObject data) {
        var friend = Friend.builder()
                .id(data.getString("userName"))
                .alias(data.getString("alias"))
                .nickname(data.getString("nickName"))
                .remark(data.getString("remark"))
                .gender(Gender.of(data.getInteger("sex")))
                .signature(data.getString("signature"))
                .avatar(data.getString("smallHeadImgUrl"))
                .country(data.getString("country"))
                .province(data.getString("province"))
                .city(data.getString("city"))
                .build();
        getManager().add(friend);
        return Future.succeededFuture(friend);
    }

    private static Future<Group> buildGroup(JsonObject data) {
        return GroupServiceImpl.INSTANCE.getInfo(data.getString("userName"))
                .compose(dto -> Future.future(promise -> {
                    getManager().add(dto.getGroup());
                    getManager().addGroup(dto.getGroup(), dto.getMembers());
                    promise.complete(dto.getGroup());
                }));


//        var dto = GroupServiceImpl.INSTANCE.getInfo(data.getString("userName")).await();
//        getManager().add(dto.getGroup());
//        getManager().addGroup(dto.getGroup(), dto.getMembers());
//        return dto.getGroup();
    }

    private static Future<SpecialAccount> buildSpecialAccount(JsonObject data) {
        var specialAccount = new SpecialAccount();
        specialAccount.setId(data.getString("userName"));
        specialAccount.setNickname(data.getString("nickName"));
        specialAccount.setRemark(data.getString("remarkName"));
        specialAccount.setAvatar(data.getString("bigHeadImgUrl"));
        getManager().add(specialAccount);
        return Future.succeededFuture(specialAccount);
    }

    private static Future<OfficialAccount> buildOfficialAccount(JsonObject data) {
        var officialAccount = new OfficialAccount();
        officialAccount.setId(data.getString("userName"));
        officialAccount.setNickname(data.getString("nickName"));
        officialAccount.setRemark(data.getString("remarkName"));
        officialAccount.setAvatar(data.getString("bigHeadImgUrl"));
        getManager().add(officialAccount);
        return Future.succeededFuture(officialAccount);
    }

}
