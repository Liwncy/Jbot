package x.ovo.jbot.plugin.ai;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.collection.CollUtil;
import org.dromara.hutool.core.collection.ListWrapper;
import org.dromara.hutool.core.io.file.FileUtil;
import org.dromara.hutool.core.map.MapUtil;
import org.dromara.hutool.core.reflect.TypeReference;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.json.JSON;
import org.dromara.hutool.json.JSONArray;
import org.dromara.hutool.json.JSONObject;
import org.dromara.hutool.json.JSONUtil;
import x.ovo.jbot.core.common.constant.JBotConstant;
import x.ovo.jbot.core.contact.Contactable;
import x.ovo.jbot.core.plugin.Plugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 聊天处理程序
 * <p>
 *
 * @author ovo on 2024/10/25.
 * @since 1.0.0
 */
@Slf4j(topic = "ChatHandler")
public class ChatHandler {

    private static final String SUFFIX = "/v1/chat/completions";

    private static Plugin plugin;
    private static Map<String, ?> config;
    /**
     * {
     * "service":[
     * {
     * "token":"token",
     * "error":{
     * "message":"message",
     * "type":"xx"
     * }
     * }
     * ]
     * }
     */
    private static JSONObject ERROR_TOKENS = JSONUtil.ofObj();
    /** 上下文缓存 {chat_id: Queue<{"role":"","content":""}>} */
    private static final Map<String, Queue<JSONObject>> CONTEXT_CACHE = new HashMap<>();

    public static void init(Plugin p) {
        plugin = p;
        config = p.getConfig().getMap();
        File file = FileUtil.file(p.getDataDir(), "error_token.json");
        if (file.exists()) {
            JSON json = JSONUtil.readJSON(file, StandardCharsets.UTF_8);
            ERROR_TOKENS = JSONUtil.parseObj(json);
        }
    }

    public static void clearContext(String chatId) {
        CONTEXT_CACHE.remove(chatId);
    }

    /**
     * 处理聊天
     *
     * @param ctx CTX 聊天上下文
     */
    public static void handle(x.ovo.jbot.plugin.ai.ChatContext ctx) {
        Contactable from = ctx.getMessage().getSender();
        Map<String, Object> service = ctx.getService();
        String url = service.get("host") + SUFFIX;
        List<String> tokens = MapUtil.get(service, "tokens", new TypeReference<>() {});

        // token检测，移除配置中的失效token
        Optional.ofNullable(ERROR_TOKENS.getJSONArray(ctx.getServiceName()))
                .stream()
                .flatMap(ListWrapper::stream)
                .map(JSON::asJSONObject)
                .map(e -> e.getStr("token"))
                .filter(tokens::contains)
                .forEach(tokens::remove);
        if (CollUtil.isEmpty(tokens)) {
            log.warn("服务 [{}] 无可用token，请检查配置文件修改或添加token", ctx.getServiceName());
            from.send(StrUtil.format("服务 [{}] 无可用token，请联系bot管理员修改或添加token", ctx.getServiceName()));
            return;
        }

        JSONObject chat = chat(ctx, url, tokens);

        String header = ctx.getMessage().isGroup() ? StrUtil.format("@{} {}{}",
                StrUtil.defaultIfBlank(ctx.getMessage().getMember().getDisplayName(), ctx.getMessage().getMember().getNickname()),
                ctx.getMessage().getContent(),
                JBotConstant.DELIMITER) : "";

        Optional.ofNullable(chat).ifPresent(e -> from.send((MapUtil.getBool(config, "group_with_raw_msg") ? header : "") + chat.getStr("content")));
    }

    /**
     * 构建请求并发送，获取
     *
     * @param ctx    聊天上下文
     * @param url    url
     * @param tokens token集合
     * @return {@link JSONObject }
     */
    @SneakyThrows
    private static JSONObject chat(x.ovo.jbot.plugin.ai.ChatContext ctx, String url, List<String> tokens) {
        // 构建messages
        Queue<JSONObject> queue = null;
        Integer contextLength = MapUtil.getInt(ctx.getService(), "context_length", 20);
        Boolean enableContext = MapUtil.getBool(ctx.getService(), "enable_context", true);
        // 如果开启了上下文缓存，则获取上下文集合或创建一个
        if (enableContext) {
            queue = CONTEXT_CACHE.computeIfAbsent(ctx.getChatId(), k -> new ArrayBlockingQueue<>(contextLength * 2));
        }

        JSONArray messages = JSONUtil.ofArray().addValue(JSONUtil.ofObj().append("role", "system").append("content", ctx.getPrompt()));
        if (enableContext) messages.addAll(queue);
        JSONObject user = JSONUtil.ofObj().append("role", "user").append("content", ctx.getMessage().getContent());
        messages.addValue(user);

        JSONObject params = JSONUtil.ofObj().append("model", ctx.getModel()).append("messages", messages).append("stream", ctx.isStream());
        // 添加参数
        Optional.ofNullable(MapUtil.getFloat(ctx.getService(), "temperature")).ifPresent(e -> params.append("temperature", e));
        Optional.ofNullable(MapUtil.getFloat(ctx.getService(), "presence_penalty")).ifPresent(e -> params.append("presence_penalty", e));
        Optional.ofNullable(MapUtil.getFloat(ctx.getService(), "frequency_penalty")).ifPresent(e -> params.append("frequency_penalty", e));

        for (String token : tokens) {
            @Cleanup Response response = HttpUtil.createPost(url)
                    .bearerAuth(token)
                    .body(params.toString())
                    .send();
            String str = response.bodyStr();
            try {
                JSONObject res = JSONUtil.parseObj(StrUtil.defaultIfBlank(str, "{}"));

                // 请求失败
                if (!response.isOk()) {
                    log.warn("请求失败，http状态码：{}，已将token [{}] 添加到ERROR_TOKENS", response.getStatus(), token);
                    // 将token添加到RRROR_TOKENS内
                    // ERROR_TOKENS.computeIfAbsent(ctx.getServiceName(), k -> JSONUtil.ofArray()).asJSONArray().add(JSONUtil.ofObj().append("token", token).append("error", "请求失败，http状态码：" + response.getStatus()));
                    // 将ERROR_TOKENS写入文件
                    // FileUtil.writeString(JSONUtil.toJsonPrettyStr(ERROR_TOKENS), FileUtil.file(plugin.getDataDir(), "error_token.json"), StandardCharsets.UTF_8);
                    continue;
                }
                if (res.containsKey("error")) {
                    log.warn("请求失败，错误信息：{}，已将token [{}] 添加到ERROR_TOKENS", res.getStr("error"), token);
                    // 将token添加到RRROR_TOKENS内
                    // ERROR_TOKENS.computeIfAbsent(ctx.getServiceName(), k -> JSONUtil.ofObj()).asJSONArray().add(JSONUtil.ofObj().append("token", token).append("error", res.getJSONObject("error")));
                    // 将ERROR_TOKENS写入文件
                    // FileUtil.writeString(JSONUtil.toJsonPrettyStr(ERROR_TOKENS), FileUtil.file(plugin.getDataDir(), "error_token.json"), StandardCharsets.UTF_8);
                    continue;
                }
                // 请求成功
                JSONObject json = JSONUtil.getByPath(res, "choices[0].message").asJSONObject();
                if (enableContext) {
                    if (queue.size() == contextLength * 2 - 1) queue.poll();
                    queue.offer(user);
                    queue.offer(json);
                }
                return json;
            } catch (Exception e) {
                log.warn("ai response json parse error: {}", str);
            }
        }
        return null;
    }

}
