package x.ovo.jbot.adapter.gewe.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import x.ovo.jbot.adapter.gewe.ApiUtil;
import x.ovo.jbot.adapter.gewe.GeweAdapter;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.contact.ContactManager;
import x.ovo.jbot.core.contact.Contactable;
import x.ovo.jbot.core.message.Downloadable;
import x.ovo.jbot.core.message.entity.*;
import x.ovo.jbot.core.message.entity.appmsg.AppMessage;
import x.ovo.jbot.core.message.entity.appmsg.AppletMessage;
import x.ovo.jbot.core.message.entity.appmsg.FileMessage;
import x.ovo.jbot.core.message.entity.appmsg.LinkMessage;
import x.ovo.jbot.core.service.MessageService;

import java.util.Arrays;

@Slf4j
public enum MessageServiceImpl implements MessageService {
    INSTANCE;

    @Override
    public Future<SentMessage> sendText(TextMessage message) {
        var content = message.getContent();
//        if (StrUtil.isNotBlank(message.getAts())) {
//            ContactManager manager = Context.get().getContactManager();
//            var ats = Arrays.stream(message.getAts().split(","))
//                    .map(String::trim)
//                    .map(manager::get)
//                    .map(Contactable::getNickname)
//                    .map(nickname -> "@" + nickname)
//                    .reduce(String::concat)
//                    .orElse("");
//            content = ats + " " + content;
//        }
        JsonObject body = JsonObject.of(
                "toWxid", message.getReceiver().getId(),
                "content", content,
                "ats", message.getAts()
        );
        return ApiUtil.post("/message/postText", body)
                .compose(json -> this.buildSentMessage(json, message));
    }

    @Override
    public Future<SentMessage> sendFile(FileMessage message) {
        JsonObject body = JsonObject.of(
                "toWxid", message.getReceiver().getId(),
                "fileUrl", message.getFileUrl(),
                "fileName", message.getName()
        );
        return ApiUtil.post("/message/postFile", body)
                .compose(json -> this.buildSentMessage(json, message));
    }

    @Override
    public Future<SentMessage> sendImage(ImageMessage message) {
        JsonObject body = JsonObject.of("toWxid", message.getReceiver().getId(), "imgUrl", message.getFileUrl());
        return ApiUtil.post("/message/postImage", body)
                .compose(json -> this.buildSentMessage(json, message));
    }

    @Override
    public Future<SentMessage> sendVideo(VideoMessage message) {
        JsonObject body = JsonObject.of(
                "toWxid", message.getReceiver().getId(),
                "videoUrl", message.getFileUrl(),
                "thumbUrl", message.getThumbUrl(),
                "videoDuration", message.getDuration()
        );
        return ApiUtil.post("/message/postVideo", body)
                .compose(json -> this.buildSentMessage(json, message));
    }

    @Override
    public Future<SentMessage> sendVoice(VoiceMessage message) {
        var body = JsonObject.of(
                "toWxid", message.getReceiver().getId(),
                "voiceUrl", message.getFileUrl(),
                "voiceDuration", message.getDuration()
        );
        return ApiUtil.post("/message/postVoice", body)
                .compose(json -> this.buildSentMessage(json, message));
    }

    @Override
    public Future<SentMessage> sendEmoji(EmoteMessage message) {
        var body = JsonObject.of(
                "toWxid", message.getReceiver().getId(),
                "emojiMd5", message.getMd5()
        );
        return ApiUtil.post("/message/postEmoji", body)
                .compose(json -> this.buildSentMessage(json, message));
    }

    @Override
    public Future<SentMessage> sendLink(LinkMessage message) {
        var body = JsonObject.of(
                "toWxid", message.getReceiver().getId(),
                "title", message.getTitle(),
                "desc", message.getDesc(),
                "linkUrl", message.getUrl(),
                "thumbUrl", message.getThumbUrl()
        );
        return ApiUtil.post("/message/postLink", body)
                .compose(json -> this.buildSentMessage(json, message));
    }

    @Override
    public Future<SentMessage> sendAppmsg(AppMessage message) {
        var body = JsonObject.of("toWxid", message.getReceiver().getId(), "appmsg", message.getXml());
        return ApiUtil.post("/message/postAppMsg", body)
                .compose(json -> this.buildSentMessage(json, message));
    }

    @Override
    public Future<SentMessage> sendCard(PersonalCardMessage message) {
        var body = JsonObject.of(
                "toWxid", message.getReceiver().getId(),
                "nickName", message.getNickname(),
                "nameCardWxid", message.getSender().getId()
        );
        return ApiUtil.post("/message/postNameCard", body)
                .compose(json -> this.buildSentMessage(json, message));
    }

    @Override
    public Future<SentMessage> sendApplet(AppletMessage message) {
        var body = JsonObject.of(
                "toWxid", message.getReceiver().getId(),
                "miniAppId", message.getAppid(),
                "displayName", message.getAppName(),
                "pagePath", message.getPagePath(),
                "coverImgUrl", message.getCoverImgUrl(),
                "title", message.getTitle(),
                "userName", message.getOwner()
        );
        return ApiUtil.post("/message/postMiniApp", body)
                .compose(json -> this.buildSentMessage(json, message));
    }

    @Override
    public Future<Void> revoke(SentMessage message) {
        var body = JsonObject.of(
                "toWxid", message.getReceiver(),
                "msgId", message.getMsgId(),
                "newMsgId", message.getNewMsgId(),
                "createTime", message.getCreateTime()
        );
        return ApiUtil.post("/message/revoke", body).mapEmpty();
    }

    @Override
    public Future<String> downloadImage(int type, String xml) {
        return ApiUtil.post("/message/downloadImage", JsonObject.of("xml", xml, "type", type))
                .compose(json -> Future.future(promise -> {
                    var host = GeweAdapter.getConfig().getString("host");
                    var port = GeweAdapter.getConfig().getInteger("down_port");
                    promise.complete(host + ":" + port + json.getJsonObject("data").getString("fileUrl"));
                }));
    }

    @Override
    public Future<String> downloadVoice(int msgId, String xml) {
        return ApiUtil.post("/message/downloadVoice", JsonObject.of("msgId", msgId, "xml", xml))
                .compose(json -> Future.future(promise -> {
                    var host = GeweAdapter.getConfig().getString("host");
                    var port = GeweAdapter.getConfig().getInteger("down_port");
                    promise.complete(host + ":" + port + json.getJsonObject("data").getString("fileUrl"));
                }));
    }

    @Override
    public Future<String> downloadVideo(String xml) {
        return ApiUtil.post("/message/downloadVideo", JsonObject.of("xml", xml))
                .compose(json -> Future.future(promise -> {
                    var host = GeweAdapter.getConfig().getString("host");
                    var port = GeweAdapter.getConfig().getInteger("down_port");
                    promise.complete(host + ":" + port + json.getJsonObject("data").getString("fileUrl"));
                }));
    }

    @Override
    public Future<String> downloadCdn(Downloadable downloadable) {
        var ext = "";
        if (downloadable instanceof FileMessage fileMessage) {
            ext = fileMessage.getExt();
        }
        var body = JsonObject.of(
                "aesKey", downloadable.getAesKey(),
                "fileId", downloadable.getFileUrl(),
                "type", downloadable.getFileType(),
                "totalSize", downloadable.getSize(),
                "suffix", ext
        );
        return ApiUtil.post("/message/downloadCdn", body)
                .compose(json -> Future.future(promise -> {
                    var host = GeweAdapter.getConfig().getString("host");
                    var port = GeweAdapter.getConfig().getInteger("down_port");
                    promise.complete(host + ":" + port + json.getJsonObject("data").getString("fileUrl"));
                }));
    }

    private Future<SentMessage> buildSentMessage(JsonObject json, Message message) {
        return Future.future(promise -> {
            var data = json.getJsonObject("data");
            var msg = new SentMessage();
            msg.setMsgId(data.getInteger("msgId"));
            msg.setNewMsgId(data.getLong("newMsgId"));
            msg.setType(data.getInteger("type"));
            msg.setReceiver(data.getString("toWxid"));
            msg.setCreateTime(data.getLong("createTime"));
            promise.complete(msg);
        });
    }
}
