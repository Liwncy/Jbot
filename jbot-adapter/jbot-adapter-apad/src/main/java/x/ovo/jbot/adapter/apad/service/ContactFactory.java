package x.ovo.jbot.adapter.apad.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.common.enums.ContactType;
import x.ovo.jbot.core.common.enums.Gender;
import x.ovo.jbot.core.contact.*;

import java.util.Optional;

public class ContactFactory {


    private static ContactManager cm;

    private static ContactManager getManager() {
        return Optional.ofNullable(cm).orElseGet(() -> {
            var m = Context.get().getContactManager();
            cm = m;
            return m;
        });
    }


    /// {
    ///         "UserName": {
    ///           "string": "wxid_i2f43nojmred21"
    ///         },
    ///         "NickName": {
    ///           "string": "徐海里"
    ///         },
    ///         "Pyinitial": {
    ///           "string": "XHL"
    ///         },
    ///         "QuanPin": {
    ///           "string": "xuhaili"
    ///         },
    ///         "Sex": 1,
    ///         "ImgBuf": {
    ///           "iLen": 0
    ///         },
    ///         "BitMask": 4294967295,
    ///         "BitVal": 8388611,
    ///         "ImgFlag": 3,
    ///         "Remark": {
    ///           "string": "wechat-bot 徐海里"
    ///         },
    ///         "RemarkPyinitial": {
    ///           "string": "WECHATBOTXHL"
    ///         },
    ///         "RemarkQuanPin": {
    ///           "string": "wechatbotxuhaili"
    ///         },
    ///         "ContactType": 0,
    ///         "RoomInfoCount": 0,
    ///         "DomainList": {},
    ///         "ChatRoomNotify": 0,
    ///         "AddContactScene": 0,
    ///         "Province": "Guangxi",
    ///         "City": "Yulin",
    ///         "PersonalCard": 1,
    ///         "HasWeiXinHdHeadImg": 1,
    ///         "VerifyFlag": 0,
    ///         "Level": 0,
    ///         "Source": 9,
    ///         "Alias": "echostar_leo",
    ///         "WeiboFlag": 0,
    ///         "AlbumStyle": 0,
    ///         "AlbumFlag": 0,
    ///         "SnsUserInfo": {
    ///           "SnsFlag": 1,
    ///           "SnsBgimgId": "http://szmmsns.qpic.cn/mmsns/8ziatt1wuPVD9C6uqnyBYyzOkUO94p8qewNStTuxubmicpHT4raUx3KUgl2KEvFVQ405QEcJS6sPs/0",
    ///           "SnsBgobjectId": 13178270520639959173,
    ///           "SnsFlagEx": 7297
    ///         },
    ///         "Country": "CN",
    ///         "BigHeadImgUrl": "https://wx.qlogo.cn/mmhead/ver_1/5gm7e5g7Gmfs4iaXyoTdl4jrhIE1qewVdbhIhhbU20Bm8jyjMhf2s4luK5yotiapLPiaibZ6LZDJiackAqQNgs2pUFG5fEsd9y3JFEhTZYnhF3Mf8J6dBoPl9Xw45ePoTB1jx/0",
    ///         "SmallHeadImgUrl": "https://wx.qlogo.cn/mmhead/ver_1/5gm7e5g7Gmfs4iaXyoTdl4jrhIE1qewVdbhIhhbU20Bm8jyjMhf2s4luK5yotiapLPiaibZ6LZDJiackAqQNgs2pUFG5fEsd9y3JFEhTZYnhF3Mf8J6dBoPl9Xw45ePoTB1jx/132",
    ///         "MyBrandList": "<brandlist count=\"0\" ver=\"823733465\"></brandlist>",
    ///         "CustomizedInfo": {
    ///           "BrandFlag": 0
    ///         },
    ///         "HeadImgMd5": "2630916458e5e3ee7d33c52688e0bdbb",
    ///         "EncryptUserName": "v3_020b3826fd030100000000006d6a09d1b7e4b9000000501ea9a3dba12f95f6b60a0536a1adb6c8880b75136666c284ab5fc3f1b435835ee622f7fb7855b66efa3cf2cbdcb033db3db9a2343615acae74ca255d74c336aeb86f1c7911e2e469c00403e1@stranger",
    ///         "AdditionalContactList": {
    ///           "LinkedinContactItem": {}
    ///         },
    ///         "ChatroomVersion": 0,
    ///         "ChatroomMaxCount": 0,
    ///         "ChatroomAccessType": 0,
    ///         "NewChatroomData": {
    ///           "MemberCount": 0,
    ///           "InfoMask": 0
    ///         },
    ///         "DeleteFlag": 0,
    ///         "LabelIdlist": "1",
    ///         "PhoneNumListInfo": {
    ///           "Count": 0
    ///         },
    ///         "ChatroomInfoVersion": 0,
    ///         "DeleteContactScene": 0,
    ///         "ChatroomStatus": 0,
    ///         "ExtFlag": 0
    ///       },
    public static Future<? extends Contactable> of(JsonObject data) {
        var type = ContactType.of(data.getJsonObject("UserName").getString("string"));
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
                .id(data.getJsonObject("UserName").getString("string"))
                .alias(data.getString("Alias"))
                .nickname(data.getJsonObject("NickName").getString("string"))
                .remark(data.getJsonObject("Remark").getString("string"))
                .gender(Gender.of(data.getInteger("Sex")))
                .signature(data.getString("Signature"))
                .avatar(data.getString("SmallHeadImgUrl"))
                .country(data.getString("Country"))
                .province(data.getString("Province"))
                .city(data.getString("City"))
                .build();
        getManager().add(friend);
        return Future.succeededFuture(friend);
    }

    private static Future<Group> buildGroup(JsonObject data) {
        return GroupServiceImpl.INSTANCE.getInfo(data.getJsonObject("UserName").getString("string"))
                .compose(dto -> Future.future(promise -> {
                    getManager().add(dto.getGroup());
                    getManager().addGroup(dto.getGroup(), dto.getMembers());
                    promise.complete(dto.getGroup());
                }));
    }

    private static Future<SpecialAccount> buildSpecialAccount(JsonObject data) {
        var specialAccount = new SpecialAccount();
        specialAccount.setId(data.getJsonObject("UserName").getString("string"));
        specialAccount.setNickname(data.getJsonObject("NickName").getString("string"));
        specialAccount.setRemark(data.getJsonObject("Remark").getString("string"));
        specialAccount.setAvatar(data.getString("SmallHeadImgUrl"));
        getManager().add(specialAccount);
        return Future.succeededFuture(specialAccount);
    }

    private static Future<OfficialAccount> buildOfficialAccount(JsonObject data) {
        var officialAccount = new OfficialAccount();
        officialAccount.setId(data.getJsonObject("UserName").getString("string"));
        officialAccount.setNickname(data.getJsonObject("NickName").getString("string"));
        officialAccount.setRemark(data.getJsonObject("Remark").getString("string"));
        officialAccount.setAvatar(data.getString("SmallHeadImgUrl"));
        getManager().add(officialAccount);
        return Future.succeededFuture(officialAccount);
    }


}
