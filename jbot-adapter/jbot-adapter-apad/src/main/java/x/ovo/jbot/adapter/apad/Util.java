package x.ovo.jbot.adapter.apad;

import io.vertx.core.json.JsonObject;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.codec.binary.HexUtil;
import org.dromara.hutool.core.data.id.IdUtil;
import org.dromara.hutool.core.io.IoUtil;
import org.dromara.hutool.core.io.file.FileUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.util.RandomUtil;
import org.dromara.hutool.core.util.RuntimeUtil;
import x.ovo.jbot.core.common.constant.JBotFiles;
import x.ovo.jbot.core.common.exception.AdapterException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Slf4j
@UtilityClass
public class Util {

    List<String> firstNames = List.of(
            "Oliver", "Emma", "Liam", "Ava", "Noah", "Sophia", "Elijah", "Isabella",
            "James", "Mia", "William", "Amelia", "Benjamin", "Harper", "Lucas", "Evelyn",
            "Henry", "Abigail", "Alexander", "Ella", "Jackson", "Scarlett", "Sebastian",
            "Grace", "Aiden", "Chloe", "Matthew", "Zoey", "Samuel", "Lily", "David",
            "Aria", "Joseph", "Riley", "Carter", "Nora", "Owen", "Luna", "Daniel",
            "Sofia", "Gabriel", "Ellie", "Matthew", "Avery", "Isaac", "Mila", "Leo",
            "Julian", "Layla"
    );

    List<String> lastNames = List.of(
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
            "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
            "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker",
            "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill",
            "Flores", "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell",
            "Mitchell", "Carter", "Roberts", "Gomez", "Phillips", "Evans"
    );

    public static String generateDeviceName() {
        var index = RandomUtil.randomInt(firstNames.size());
        return firstNames.get(index) + " " + lastNames.get(index) + "'s Pad";
    }

    public static String generateDeviceID(String name) {
        name = Optional.ofNullable(name).orElse(IdUtil.fastSimpleUUID());
        try {
            var md = MessageDigest.getInstance("MD5");
            var bytes = md.digest(name.getBytes());
            return "49" + HexUtil.encodeStr(bytes).substring(2);
        } catch (NoSuchAlgorithmException e) {
            throw new AdapterException("MD5算法不可用", e);
        }
    }

    public static void startServer(JsonObject config) {
        var os = System.getProperty("os.name").toLowerCase();
        var fileName = StrUtil.format("XYWechatPad{}", os.startsWith("win") ? ".exe" : "");
        // 输出文件
        File file = FileUtil.file(JBotFiles.ADAPTER_DIR, APadAdapter.NAME, fileName);
        if (!file.exists()) {
            BufferedOutputStream stream = FileUtil.getOutputStream(file);
            IoUtil.copy(Util.class.getClassLoader().getResourceAsStream(fileName), stream);
            try {
                stream.flush();
                stream.close();
            } catch (Exception ignore) {}
        }
        log.debug("协议服务文件路径：{}", file.getPath());
        var cmd = StrUtil.format("{} -p {} -m {} -rh {} -rp {} -rpwd {} -rdb {}",
                file.getPath(),
                config.getInteger("port", 9000),
                config.getString("mode", "release"),
                config.getString("redis_host", "127.0.0.1"),
                config.getInteger("redis_port", 6379),
                config.getString("redis_pwd", ""),
                config.getInteger("redis_db", 0)
        );
        log.debug("协议服务启动命令：{}", cmd);
        var res = RuntimeUtil.execForStr(cmd);
        log.debug("协议服务启动结果：{}", res);
    }

}
