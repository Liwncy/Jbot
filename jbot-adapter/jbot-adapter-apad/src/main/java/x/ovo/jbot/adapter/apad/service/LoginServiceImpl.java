package x.ovo.jbot.adapter.apad.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.text.StrUtil;
import x.ovo.jbot.adapter.apad.APadAdapter;
import x.ovo.jbot.adapter.apad.ApiUtil;
import x.ovo.jbot.adapter.apad.Util;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.common.enums.ContactType;
import x.ovo.jbot.core.common.enums.Gender;
import x.ovo.jbot.core.common.enums.LoginCheckStatus;
import x.ovo.jbot.core.common.enums.QrcodeCheckStatus;
import x.ovo.jbot.core.contact.Friend;
import x.ovo.jbot.core.domain.dto.CheckLoginDTO;
import x.ovo.jbot.core.domain.dto.QrcodeDTO;
import x.ovo.jbot.core.event.LoginSystemEvent;
import x.ovo.jbot.core.service.LoginService;

import java.util.Objects;

@Slf4j
public enum LoginServiceImpl implements LoginService {
    INSTANCE;

    @Override
    public Future<Void> login() {
        if (StrUtil.isBlank(APadAdapter.getConfig().getString("wxid"))) {
            // 如果config内没有wxid，则需要扫码登录
            return this.ScanQrLogin();
        } else {
            // config内存在wxid，则检查登录状态，重连或弹窗登录
            return this.otherLogin();
        }
    }

    private Future<Void> ScanQrLogin() {
        return Future.<Void>future(promise -> {
                    this.getQrcode().onSuccess(dto -> {
                        // 发送事件
                        LoginSystemEvent.of("请打开链接扫描二维码登录: " + dto.getQrData()).publish();
                        this.check(promise, dto.getUuid());
                    });
                })
                .onFailure(t -> {
                    log.error("登录失败：{}", t.getMessage());
                    System.exit(1);
                });
    }

    private Future<Void> otherLogin() {
        return this.checkOnline().compose(online -> {
            // 在线，返回
            if (online) return Future.succeededFuture();
            // 不在线，尝试重连
            return this.reConnect().recover(e -> Future.future(promise -> {
                log.warn("重连失败：{}", e.getMessage());
                // 重连失败，尝试弹窗登录
                this.dialogLogin()
                        .onSuccess(uuid -> this.check(promise, uuid))
                        .onFailure(t -> {
                            log.info("弹窗登录失败：{}", t.getMessage());
                            this.ScanQrLogin();
                        });
//                        .transform((s, t) -> {
//                            log.info("弹窗登录失败：{}", t.getMessage());
//                            return this.ScanQrLogin();
//                        });
            }));
        });
    }

    private void check(Promise<Void> promise, String uuid) {
        Context.vertx.setPeriodic(5000, id -> {
            this.checkLogin(uuid)
                    .onFailure(t -> {
                        Context.vertx.cancelTimer(id);
                        log.info("检查登录状态失败：{}", t.getMessage());
                        promise.fail(t.getMessage());
                    }).onSuccess(check -> {
                        log.debug(JsonObject.mapFrom(check).encodePrettily());
                        log.info("[{}] 登录状态：{}", check.getNickName(), QrcodeCheckStatus.of(check.getStatus()).getDesc());
                        // 如果登录成功，则取消定时器，开启自动心跳
                        if (QrcodeCheckStatus.of(check.getStatus()) == QrcodeCheckStatus.SUCCESS) {
                            Context.vertx.cancelTimer(id);
                            this.autoHeartbeat(check.getLoginInfo().getWxid());
                            APadAdapter.getConfig().put("wxid", check.getLoginInfo().getWxid());
                            APadAdapter.saveConfig();
                            promise.complete();
                            return;
                        }
                        // 如果二维码到期了，取消定时器
                        if (Objects.isNull(check.getExpiredTime()) || check.getExpiredTime() <= 0) {
                            Context.vertx.cancelTimer(id);
                            log.info("二维码已过期，请重新扫码登录");
                            promise.complete();
                        }
                    });
        });
    }


    @Override
    public Future<QrcodeDTO> getQrcode() {
        var name = APadAdapter.getConfig().getString("deviceName", Util.generateDeviceName());
        var id = APadAdapter.getConfig().getString("deviceId", Util.generateDeviceID(name));
        var body = JsonObject.of(
                "DeviceID", id,
                "DeviceName", name
        );
        if (StrUtil.isNotBlank(APadAdapter.getConfig().getString("wxid"))) body.put("Wxid", APadAdapter.getConfig().getString("wxid"));
        return ApiUtil.post("/GetQRCode", body)
                .map(res -> res.getJsonObject("Data"))
                .map(data -> QrcodeDTO.builder()
                        .uuid(data.getString("Uuid"))
                        .qrData(data.getString("QRCodeURL"))
                        .imgBase64(data.getString("QRCodeBase64"))
                        .build()
                ).onSuccess(v -> APadAdapter.saveConfig());
    }

    @Override
    public Future<CheckLoginDTO> checkLogin(String uuid) {
        return ApiUtil.post("/CheckUuid", JsonObject.of("Uuid", uuid))
                .map(res -> res.getJsonObject("Data"))
                .map(data -> {
                    var dto = new CheckLoginDTO();
                    if (data.containsKey("baseResponse")) {
                        // 登录成功
                        dto.setStatus(LoginCheckStatus.LOGIN_SUCCESS.getCode());
                        var info = new CheckLoginDTO.LoginInfo();
                        info.setAlias(data.getJsonObject("acctSectResp").getString("alias"));
                        info.setWxid(data.getJsonObject("acctSectResp").getString("userName"));
                        dto.setNickName(data.getJsonObject("acctSectResp").getString("nickName"));
                        dto.setLoginInfo(info);
                    } else {
                        // 未登录
                        dto.setUuid(data.getString("uuid"));
                        dto.setStatus(data.getInteger("status"));
                        dto.setHeadImgUrl(data.getString("headImgUrl"));
                        dto.setNickName(data.getString("nickName"));
                        dto.setExpiredTime(data.getInteger("expiredTime"));
                    }
                    return dto;
                });
    }

    @Override
    public Future<String> dialogLogin() {
        return ApiUtil.post("/AwakenLogin", JsonObject.of("Wxid", APadAdapter.getConfig().getString("wxid")))
                .map(res -> res.getJsonObject("Data"))
                .map(data -> data.getJsonObject("QrCodeResponse").getString("Uuid"));
    }

    @Override
    public Future<Void> reConnect() {
        return Future.failedFuture("暂不支持");
    }

    @Override
    public Future<Void> logout() {
        return ApiUtil.post("/LogOut", JsonObject.of("Wxid", APadAdapter.getConfig().getString("wxid"))).mapEmpty();
    }

    @Override
    public Future<Boolean> checkOnline() {
        return ApiUtil.post("/GetProfile", JsonObject.of("Wxid", APadAdapter.getConfig().getString("wxid")))
                .map(res -> true)
                .recover(t -> Future.succeededFuture(false));
    }

    @Override
    public Future<Friend> getUserInfo() {
        return ApiUtil.post("/GetProfile", JsonObject.of("Wxid", APadAdapter.getConfig().getString("wxid")))
                .map(res -> res.getJsonObject("Data").getJsonObject("userInfo"))
                .map(data ->
                     Friend.builder()
                            .id(data.getJsonObject("UserName").getString("string"))
                            .nickname(data.getJsonObject("NickName").getString("string"))
                            .gender(Gender.of(data.getInteger("Sex")))
//                            .avatar(data.getString("smallHeadImgUrl"))
//                            .signature(data.getString("signature"))
//                            .country(data.getString("country"))
//                            .province(data.getString("province"))
//                            .city(data.getString("city"))
                            .type(ContactType.USER)
                            .build()
                );
    }

    /**
     * 自动心跳
     *
     * @param wxid WXID
     */
    private void autoHeartbeat(String wxid) {
        ApiUtil.post("/AutoHeartbeatStart", JsonObject.of("Wxid", wxid));
    }
}
