package x.ovo.jbot.adapter.apad.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.xml.XmlUtil;
import x.ovo.jbot.adapter.apad.APadAdapter;
import x.ovo.jbot.adapter.apad.ApiUtil;
import x.ovo.jbot.adapter.apad.VideoUtil;
import x.ovo.jbot.core.message.Downloadable;
import x.ovo.jbot.core.message.entity.*;
import x.ovo.jbot.core.message.entity.appmsg.AppMessage;
import x.ovo.jbot.core.message.entity.appmsg.AppletMessage;
import x.ovo.jbot.core.message.entity.appmsg.FileMessage;
import x.ovo.jbot.core.message.entity.appmsg.LinkMessage;
import x.ovo.jbot.core.service.MessageService;

import java.time.Instant;

@Slf4j
public enum MessageServiceImpl implements MessageService {
    INSTANCE;

    @Override
    public Future<SentMessage> sendText(TextMessage message) {
        var body = JsonObject.of(
                "At", message.getAts(),
                "Content", message.getContent(),
                "ToWxid", message.getReceiver().getId(),
                "Type", 1,
                "Wxid", APadAdapter.getConfig().getString("wxid")
        );
        return ApiUtil.post("/SendTextMsg", body)
                .map(res -> {
                    var data = res.getJsonObject("Data").getJsonArray("List").getJsonObject(0);
                    data.put("BaseResponse", res.getJsonObject("Data").getJsonObject("BaseResponse"));
                    return data;
                })
                .compose(data -> buildSentMessage(data, message));
    }

    @Override
    public Future<SentMessage> sendFile(FileMessage message) {
        return Future.failedFuture("暂不支持发送文件");
    }

    @Override
    public Future<SentMessage> sendImage(ImageMessage message) {
        var body = JsonObject.of(
                "ToWxid", message.getReceiver().getId(),
                "Wxid", APadAdapter.getConfig().getString("wxid")
        );
        Future<JsonObject> future;
        // 如果文件url不为空，则下载文件发送
        if (StrUtil.isNotBlank(message.getFileUrl())) {
            future = ApiUtil.download(message.getFileUrl())
                    .compose(s -> ApiUtil.post("/SendImageMsg", body.put("Base64", s)));
        } else {
            future = ApiUtil.post("/SendImageMsg", body.put("Base64", message.getHashBase64()));
        }
        return future.map(res -> res.getJsonObject("Data"))
                .compose(data -> this.buildSentMessage(data, message));
    }

    @Override
    public Future<SentMessage> sendVideo(VideoMessage message) {
        return ApiUtil.download(message.getFileUrl())
                .compose(VideoUtil::parse)
                .compose(info -> {
                    var body = JsonObject.of(
                            "Base64", info.getVideo(),
                            "ImageBase64", info.getFrame(),
                            "PlayLength", info.getDuration(),
                            "ToWxid", message.getReceiver().getId(),
                            "Wxid", APadAdapter.getConfig().getString("wxid")
                    );
                    return ApiUtil.post("/SendVideoMsg", body);
                })
                .map(res -> res.getJsonObject("Data"))
                .compose(data -> this.buildSentMessage(data, message));
    }

    @Override
    public Future<SentMessage> sendVoice(VoiceMessage message) {
        return null;
    }

    @Override
    public Future<SentMessage> sendEmoji(EmoteMessage message) {
        var body = JsonObject.of(
                "Md5", message.getMd5(),
                "ToWxid", message.getReceiver().getId(),
                "TotalLen", 0,
                "Wxid", APadAdapter.getConfig().getString("wxid")
        );
        return ApiUtil.post("/SendEmojiMsg", body)
                .map(res -> res.getJsonObject("Data"))
                .compose(data -> this.buildSentMessage(data, message));
    }

    @Override
    public Future<SentMessage> sendLink(LinkMessage message) {
        var body = JsonObject.of(
                "Url", message.getUrl(),
                "Title", message.getTitle(),
                "Desc", message.getDesc(),
                "ThumbUrl", message.getThumbUrl(),
                "ToWxid", message.getReceiver().getId(),
                "Wxid", APadAdapter.getConfig().getString("wxid")
        );
        return ApiUtil.post("/SendShareLink", body)
                .map(res -> res.getJsonObject("Data"))
                .compose(data -> this.buildSentMessage(data, message));
    }

    @Override
    public Future<SentMessage> sendAppmsg(AppMessage message) {
        var body = JsonObject.of(
//                "Type", 3,
                "Xml", message.getXml(),
                "ToWxid", message.getReceiver().getId(),
                "Wxid", APadAdapter.getConfig().getString("wxid")
        );
        return ApiUtil.post("/SendAppMsg", body)
                .map(res -> res.getJsonObject("Data"))
                .compose(data -> this.buildSentMessage(data, message));
    }

    @Override
    public Future<SentMessage> sendCard(PersonalCardMessage message) {
        var body = JsonObject.of(
//                "CardAlias", message.getNickname(),
                "CardNickname", message.getNickname(),
                "CardWxid", message.getUsername(),
                "ToWxid", message.getReceiver().getId(),
                "Wxid", APadAdapter.getConfig().getString("wxid")
        );
        return ApiUtil.post("/SendCardMsg", body)
                .map(res -> res.getJsonObject("Data"))
                .compose(data -> this.buildSentMessage(data, message));
    }

    @Override
    public Future<SentMessage> sendApplet(AppletMessage message) {
        return Future.failedFuture("暂不支持发送小程序");
    }

    @Override
    public Future<Void> revoke(SentMessage message) {
        var body = JsonObject.of(
                "NewMsgId", message.getNewMsgId(),
                "CreateTime", message.getCreateTime(),
//                "ClientMsgId", "",
                "ToWxid", message.getReceiver(),
                "Wxid", APadAdapter.getConfig().getString("wxid")
        );
        return ApiUtil.post("/RevokeMsg", body).mapEmpty();
    }

    @Override
    public Future<String> downloadImage(int type, String xml) {
        var root = XmlUtil.getRootElement(XmlUtil.parseXml(xml));
        var image = XmlUtil.getElement(root, "img");
        var body = JsonObject.of(
                "AesKey", image.getAttribute("aeskey"),
                "Cdnmidimgurl", image.getAttribute("cdnthumburl"),
                "Wxid", APadAdapter.getConfig().getString("wxid")
        );
        return ApiUtil.post("/CdnDownloadImg", body)
                .map(res -> res.getString("Data"));
    }

    @Override
    public Future<String> downloadVoice(int msgId, String xml) {
        var root = XmlUtil.getRootElement(XmlUtil.parseXml(xml));
        var image = XmlUtil.getElement(root, "voicemsg");
        var body = JsonObject.of(
                "Voiceurl", image.getAttribute("voiceurl"),
                "Wxid", APadAdapter.getConfig().getString("wxid")
        );
        return ApiUtil.post("/DownloadVoice", body)
                .map(res -> res.getString("Data"));
    }

    @Override
    public Future<String> downloadVideo(String xml) {
        return Future.failedFuture("暂不支持下载视频");
    }

    @Override
    public Future<String> downloadCdn(Downloadable downloadable) {
        return Future.failedFuture("暂不支持下载CDN");
    }

    private Future<SentMessage> buildSentMessage(JsonObject json, Message message) {
        if (json.getJsonObject("BaseResponse").getInteger("ret") != 0) return Future.failedFuture(json.getJsonObject("BaseResponse").getString("errMsg", "发送失败"));
        var msg = new SentMessage();
        msg.setMsgId(json.getInteger("MsgId", json.getInteger("msgId", json.getInteger("Msgid"))));
        msg.setNewMsgId(json.getLong("NewMsgId", json.getLong("newMsgId", json.getLong("Newmsgid"))));
        msg.setType(message.getType().getCode());
        msg.setReceiver(message.getReceiver().getId());
        msg.setCreateTime(json.getLong("Createtime", json.getLong("CreateTime", Instant.now().toEpochMilli() / 1000)));
        return Future.succeededFuture(msg);
    }

}
