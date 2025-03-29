package x.ovo.jbot.plugin.mmorpg;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.regex.ReUtil;
import org.dromara.hutool.core.text.StrUtil;
import org.dromara.hutool.core.util.RandomUtil;
import org.dromara.hutool.http.HttpUtil;
import org.dromara.hutool.json.JSONArray;
import org.dromara.hutool.json.JSONObject;
import org.dromara.hutool.json.JSONUtil;
import x.ovo.jbot.core.contact.Contactable;
import x.ovo.jbot.core.contact.Member;
import x.ovo.jbot.core.event.EventListener;
import x.ovo.jbot.core.event.MessageEvent;
import x.ovo.jbot.core.message.entity.ImageMessage;
import x.ovo.jbot.core.message.entity.TextMessage;
import x.ovo.jbot.core.message.entity.VideoMessage;
import x.ovo.jbot.core.plugin.Plugin;
import x.ovo.jbot.plugin.mmorpg.dto.Player;
import x.ovo.jbot.plugin.mmorpg.handle.QianDaoHandler;
import x.ovo.jbot.plugin.mmorpg.util.MySQLClient;

import java.time.Instant;
import java.util.*;

/**
 * Mmorpg 侦听器
 * <p>
 *
 * @author liwncy on 2025/03/26.
 * @since 1.0.0
 */
@Slf4j(topic = "Mmorpg")
public class MmorpgListener extends EventListener<MessageEvent<TextMessage>, TextMessage> {

    public MmorpgListener(Plugin plugin) {
        super(plugin);
    }

    @Override
    public boolean support(@NonNull MessageEvent<TextMessage> textMessageMessageEvent, TextMessage textMessage) {
        return true;
    }

    @Override
    public boolean onEvent(@NonNull MessageEvent<TextMessage> textMessageMessageEvent, TextMessage message) {
        String content = message.getContent();
        Contactable group = message.getSender();
        Member member = message.getMember();
        String code = "13471fc6200bce798d70526724b5de24";
        String uid = "9083";
//        name: 夜夜念
//        playerTypeId:
//        jueji: 碧落凝珠
//        qunxia: 兵锋现世
//        ptai: 电脑
//        pvpzb: PVP
//        dazhao: 蛇毒
//        note:
//        houbu: 否
//        lingdi: 否
//        ctype: 报名
//        code: 13471fc6200bce798d70526724b5de24
//        fuzhi:
//        pianhao: 进攻
//        jiaoliu: 开麦
//        uid: 9083
//        gpcode:
        if(content.startsWith("帮我报名")){
            String result = HttpUtil.post("https://www.liansaifenxi.top/com/shareAddGroupCalled.do", new HashMap<String, Object>() {{
                put("code", code);
                put("uid", uid);
                put("name", content.replace("帮我报名", "").trim());
                put("playerTypeId", "");
                put("jueji", "钧天浩意");
                put("qunxia", "龙驰雷渊");
                put("ptai", "电脑");
                put("pvpzb", "有");
                put("dazhao", "有");
                put("note", "");
                put("houbu", "否");
                put("lingdi", "否");
                put("ctype", "报名");
                put("fuzhi", "");
                put("pianhao", "机动");
                put("jiaoliu", "开麦");
            }});
            message.getSender().send(JSONUtil.parseObj(result).getStr("message"));
        }
        if(content.equals("查看报名")){
            String result = HttpUtil.post("https://www.liansaifenxi.top/com/shareListGroupCalled.do", new HashMap<String, Object>() {{
                put("code", code);
                put("uid", uid);
            }});
            JSONArray array = JSONUtil.parseObj(result).getJSONObject("data").getJSONArray("list");
            StringBuilder bmnames = new StringBuilder("");
            StringBuilder qjnames = new StringBuilder("请假:");
            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if("请假".equals(obj.getStr("callType"))){
                    qjnames.append(obj.get("name")).append(", ");
                }
                if("报名".equals(obj.getStr("callType"))){
                    bmnames.append(obj.get("name")).append(", ");
                }
            }
            message.getSender().send("当前参与人数为"+array.size()+"人\n"
                    +"报名("+(bmnames.toString().split(",").length-1)+"):"+bmnames + "\n"
                    + "请假("+(qjnames.toString().split(",").length-1)+"):"+qjnames);
            return false;
        }
        if (content.startsWith("#")) {
            // 验证是否加入游戏世界
            MySQLClient client = MySQLClient.getInstance();
            RowSet<Row> rows = client.getPool()
                    .preparedQuery("select * from player where wx_id = ?")
                    .execute(Tuple.of(member.getId()))
                    .await();
            if (rows.size() == 0) {
                TextMessage msg = new TextMessage();
                msg.setAts(message.getMember().getId());
                msg.setContent("芈仙世界暂未查询到仙友 @" + message.getMember().getNickname() + " 的气息!");
                message.getSender().send(msg);
                return false;
            }
            Player player = new Player();
            for (Row row : rows) {
                log.info("玩家信息: {}", row.toJson());
                player = Player
                        .builder().id(row.getLong("id")).wxId(row.getString("wx_id")).nickname(row.getString("nickname")).gender(row.getString("gender")).age(row.getInteger("age"))
                        .realmId(row.getInteger("realm_id")).currentExp(row.getLong("current_exp")).equipmentId(row.getLong("equipment_id")).techniqueId(row.getLong("technique_id")).attack(row.getInteger("attack"))
                        .luck(row.getInteger("luck")).gold(row.getLong("gold")).spiritStones(row.getLong("spirit_stones")).checkinDays(row.getInteger("checkin_days")).checkinStreak(row.getInteger("checkin_streak"))
                        .lastCheckinDate(row.getLocalDateTime("last_checkin_date")).lastCultivateDate(row.getLocalDateTime("last_cultivate_date"))
                        .dayCultivateNum(row.getInteger("day_cultivate_num")).fortuneTellingDate(row.getLocalDateTime("fortune_telling_date"))
                        .createdAt(row.getLocalDateTime("created_at")).updatedAt(row.getLocalDateTime("updated_at"))
                        .build();
            }
            if (content.equals("#签到")) {
                QianDaoHandler.handle(message, player);
            }
        }
        return false;
    }

    @Override
    public boolean executeNext() {
        return false;
    }
}
