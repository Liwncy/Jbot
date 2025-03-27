package x.ovo.jbot.adapter.apad;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.codec.binary.Base64;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.common.exception.ApiException;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@UtilityClass
public class ApiUtil {

    private static final HttpClient CLIENT;

    static {
        var options = new HttpClientOptions()
                .setDefaultHost(Optional.ofNullable(APadAdapter.getConfig().getString("host")).orElse("127.0.0.1"))
                .setDefaultPort(Optional.ofNullable(APadAdapter.getConfig().getInteger("port")).orElse(9000))
                .setSsl(false)
                .setTcpKeepAlive(true)
                .setTcpNoDelay(true)
                .setIdleTimeout(60)
                .setIdleTimeoutUnit(TimeUnit.SECONDS);
        CLIENT = Context.vertx.createHttpClient(options);
        log.info("api连接工具初始化完成, host: {}, port:{}", options.getDefaultHost(), options.getDefaultPort());
    }

    public static Future<JsonObject> get(String path) {
        var option = new RequestOptions()
                .setURI(path)
                .addHeader("Content-Type", "application/json")
                .setMethod(HttpMethod.GET);
        return CLIENT.request(option)
                .compose(HttpClientRequest::send)
                .compose(HttpClientResponse::body)
                .compose(buffer-> Future.<JsonObject>future(promise -> {
                    var data = buffer.toJsonObject();
                    if (data.getInteger("Code") == 0) promise.complete(data);
                    else promise.fail(new ApiException(data.getString("Message")));
                }))
                .onFailure(e-> log.debug("request: GET [{}]\nfailure: {}", path, e.getMessage()));

    }

    public static Future<String> getString(String path) {
        var option = new RequestOptions()
                .setURI(path)
                .addHeader("Content-Type", "application/json")
                .setMethod(HttpMethod.GET);
        return CLIENT.request(option)
                .compose(HttpClientRequest::send)
                .compose(HttpClientResponse::body)
                .map(Buffer::toString)
                .onFailure(e-> log.debug("request: GET [{}]\nfailure: {}", path, e.getMessage()));
    }

    public static Future<JsonObject> post(String path, JsonObject body) {
        var option = new RequestOptions()
                .setURI(path)
                .addHeader("Content-Type", "application/json")
                .setMethod(HttpMethod.POST);
        return CLIENT.request(option)
                .compose(request -> request.send(body.toBuffer()))
                .compose(HttpClientResponse::body)
                .compose(buffer-> Future.<JsonObject>future(promise -> {
                    var data = buffer.toJsonObject();
                    if (data.getInteger("Code") == 0) promise.complete(data);
                    else promise.fail(new ApiException(data.getString("Message")));
                }))
                .onFailure(e-> log.debug("request: POST [{}] {}\nfailure: {}", path, body, e.getMessage()));
    }

    /**
     * 下载文件
     *
     * @param path 路径
     * @return {@link Future }<{@link String }> base64编码字符串
     */
    public static Future<String> download(String path) {
        var option = new RequestOptions()
                .setAbsoluteURI(path)
                .addHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0")
//                .addHeader("Content-Type", "application/json")
                .setMethod(HttpMethod.GET);
        return CLIENT.request(option)
                .compose(HttpClientRequest::send)
                .compose(HttpClientResponse::body)
                .map(buffer -> Base64.encode(buffer.getBytes()))
                .onFailure(e-> log.warn("request: GET [{}]\nfailure: {}", path, e.getMessage()));
    }

}
