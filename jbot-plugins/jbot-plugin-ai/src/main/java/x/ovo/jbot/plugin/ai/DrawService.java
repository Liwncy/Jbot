package x.ovo.jbot.plugin.ai;

import org.dromara.hutool.core.io.file.FileUtil;
import org.dromara.hutool.core.net.url.UrlBuilder;
import org.dromara.hutool.core.regex.ReUtil;
import org.dromara.hutool.core.thread.ThreadUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.http.client.HttpDownloader;
import org.dromara.hutool.json.JSONObject;
import org.dromara.hutool.json.JSONUtil;

import java.io.File;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class DrawService {

    private static final String GENERATE_URL = "https://image.baidu.com/aigc/generate";
    private static final String QUERY_URL = "https://image.baidu.com/aigc/query";
    private static final String DEFAULT_WIDTH = "512";
    private static final String DEFAULT_HEIGHT = "512";
    private static final Pattern PATTERN = Pattern.compile("(\\S+)\\s?(\\S+)?");
    private static final Map<String, int[]> SCALES = Map.of(
            "1:1", new int[]{512, 512},
            "3:4", new int[]{480, 640},
            "4:3", new int[]{640, 480},
            "16:9", new int[]{640, 360},
            "9:16", new int[]{360, 640}
    );

    public static File draw(File floder, String content) {
        String prompt = ReUtil.get(PATTERN, content, 1);
        int[] scale = SCALES.getOrDefault(Optional.ofNullable(ReUtil.get(PATTERN, content, 2)).orElse("1:1"), new int[]{512, 512});
        JSONObject object = JSONUtil.ofObj()
                .append("querycate", 10)
                .append("query", prompt)
                .append("width", scale[0])
                .append("height", scale[1]);
        JSONObject json = JSONUtil.parseObj(HttpUtil.post(GENERATE_URL, object.toJSONString(0)));
        // 轮询，每隔500ms查询一次生成结果
        for (int i = 0; i < 20; i++) {
            ThreadUtil.safeSleep(500);
            String url = UrlBuilder.of(QUERY_URL)
                    .addQuery("taskid", json.getStr("taskid"))
                    .addQuery("token", json.getStr("token"))
                    .addQuery("timestamp", json.getStr("timestamp"))
                    .build();
            JSONObject result = JSONUtil.parseObj(HttpUtil.get(url));
            if (Objects.nonNull(result.get("status"))) throw new RuntimeException(result.getStr("message"));
            if (result.getBool("isGenerate") && result.containsKey("picArr")) {
                String src = result.getByPath("picArr[0].src", String.class);
                File file = FileUtil.file(floder, Instant.now().toEpochMilli() + ".jpg");
                // HttpDownloader.downloadFile(src, file);
                return file;
            }
        }
        throw new RuntimeException("生成失败");
    }

}
