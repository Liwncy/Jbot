package x.ovo.jbot.plugin;

import io.vertx.core.json.JsonObject;
import lombok.Cleanup;
import org.dromara.hutool.core.codec.binary.Base64;
import org.dromara.hutool.core.convert.ConvertUtil;
import org.dromara.hutool.core.util.RandomUtil;
import org.dromara.hutool.http.HttpUtil;
import x.ovo.jbot.core.command.CommandExecutor;
import x.ovo.jbot.core.event.CallListener;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.plugin.Plugin;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class CardPlugin extends Plugin {
    @Override
    public CommandExecutor getCommandExecutor() {
        return null;
    }

    @Override
    public EventListener<?, ?> getEventListener() {
        return null;
    }

    @Override
    public void onLoad() throws Exception {
        // 自定义 TrustManager 接受所有证书
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}

                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                }
        };

        // 安装 TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        SSLContext.setDefault(sslContext);
    }

    @Override
    public CallListener getCallListener() {
        return new CallListener(this) {

            public static final String URL = "https://fireflycard-api.302ai.cn/api/saveImg";

            @Override
            public JsonObject onCall(JsonObject data) {
                var temp_path = this.plugin.getClassLoader().getResource("card_template.json").getPath();
                var template = this.plugin.getVertx().fileSystem().readFileBlocking(temp_path).toJsonObject();
                var color_path = this.plugin.getClassLoader().getResource("colors.json").getPath();
                var colors = this.plugin.getVertx().fileSystem().readFileBlocking(color_path).toJsonArray();
                var background = JsonObject.of("backgroundName", RandomUtil.randomEle(ConvertUtil.toList(String.class, colors.getList())));
                template = template.mergeIn(data);
                template.put("style", template.getJsonObject("style").mergeIn(background));
                template = template.mergeIn(JsonObject.of("style", background));

                try {
                    @Cleanup var s = HttpUtil.createPost(URL).body(template.encode()).send();
                    return JsonObject.of("data", Base64.encode(s.bodyBytes()));
                } catch (Exception e) {
                    return JsonObject.of("error", e.getMessage());
                }

            }
        };
    }
}
