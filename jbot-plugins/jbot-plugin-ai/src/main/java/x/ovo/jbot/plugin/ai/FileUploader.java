package x.ovo.jbot.plugin.ai;

import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.io.file.FileUtil;
import org.dromara.hutool.core.io.resource.FileResource;
import org.dromara.hutool.core.io.resource.HttpResource;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.http.client.Request;
import org.dromara.hutool.http.client.Response;
import org.dromara.hutool.http.client.body.ResourceBody;
import org.dromara.hutool.http.client.engine.jdk.JdkClientEngine;
import org.dromara.hutool.http.meta.ContentType;
import org.dromara.hutool.http.meta.Method;
import org.dromara.hutool.json.JSONObject;
import org.dromara.hutool.json.JSONUtil;

import java.io.File;

/**
 * 文件上传器
 * <p>
 *
 * @author ovo on 2024/10/25.
 * @since 1.0.0
 */
@Slf4j
public class FileUploader {

    private static final String PREFIX = "https://file.upfile.live/";
    private static final String URL = "https://upfile.live/api/file/getUploadLink/";

    public static String upload(File file) {
        String s = HttpUtil.post(URL, JSONUtil.ofObj().append("vipCode", null).append("file_name", file.getName()).toJSONString(0));
        JSONObject obj = JSONUtil.parseObj(s).getJSONObject("data");
        String uploadUrl = obj.getStr("upload_url");
        String fileKey = obj.getStr("file_key");

        Request request = HttpUtil.createRequest(uploadUrl, Method.PUT);
        ResourceBody resourceBody = ResourceBody.of(new HttpResource(new FileResource(file), ContentType.TEXT_PLAIN.getValue()));
        request.body(resourceBody);

        Response response = HttpUtil.createRequest(uploadUrl, Method.PUT)
                .body(resourceBody)
                .send(new JdkClientEngine());
        if (response.isOk()) {
            log.info("file upload success, url: {}", PREFIX + fileKey);
            return PREFIX + fileKey;
        } else {
            log.error("file upload failure, error: {}", response.bodyStr());
            return null;
        }
    }

    public static void main(String[] args) {
        File file = FileUtil.file("\u202AC:\\Users\\Administrator\\Desktop\\test.txt");
        upload(file);
    }

}
