package x.ovo.jbot.impl.contact;

import io.vertx.core.Future;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.json.JSONUtil;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.common.enums.ContactType;
import x.ovo.jbot.core.common.util.ContactUtil;
import x.ovo.jbot.core.contact.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class DefaultContactManager implements ContactManager {

    /** 联系人数据：[wxid: 联系人] */
    private final Map<String, Contactable> contacts = new HashMap<>();
    /** 群组数据：[群组wxid: Coll<群成员>] */
    private final Map<String, Collection<Member>> groups = new HashMap<>();
    private RedisAPI redis;


    @Override
    public void add(Contactable contact) {
        this.contacts.put(contact.getId(), contact);
    }

    @Override
    public void addAll(Collection<Contactable> list) {
        this.contacts.putAll(list.stream().collect(Collectors.toMap(Contactable::getId, contact -> contact)));
    }

    @Override
    public void addGroup(Group group, Collection<Member> members) {
        this.groups.put(group.getId(), members);
        log.info("群组 [{}] 成员获取完毕，共 {} 个成员", group.getNickname(), members.size());
    }

    @Override
    public Future<Void> onInit() throws Exception {
        var client = Redis.createClient(Context.vertx, Context.get().getConfig().getBot().getRedis());
        this.redis = RedisAPI.api(client);

        // 如果 redis 存在，则从redis中获取联系人数据
        this.redis.get("contacts")
                .map(res -> res.toBuffer().toJsonObject().getMap())
                .map(map -> map.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> JSONUtil.toBean(e.getValue(), ContactType.of(e.getKey()).getClazz())
                        ))
                ).onSuccess(map -> {
                    this.contacts.putAll(map);
                    Context.get().setOwner((Friend) ContactUtil.fromString(Context.get().getConfig().getBot().getOwner()));
                    log.info("联系人数据获取完毕，共 {} 个联系人", this.contacts.size());
                });
        this.redis.get("groups")
                .map(res -> res.toBuffer().toJsonObject().getMap())
                .map(map -> map.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> JSONUtil.toList(JSONUtil.parseArray(e.getValue()), Member.class)
                        ))
                ).onSuccess(map -> {
                    this.groups.putAll(map);
                    log.info("群组数据获取完毕，共 {} 个群组", this.groups.size());
                });

        log.info("联系人管理器初始化完成");
        return ContactManager.super.onInit();
    }

    @Override
    public Contactable get(String id) {
        return this.contacts.get(id);
    }

    @Override
    public Contactable get(String name, RetrievalType type) {
        return RetrievalType.USER_NAME == type ? this.get(name) : RetrievalStrategy.of(type).get(this.contacts.values().iterator(), name);
    }

    @Override
    public void remove(String username) {
        this.contacts.remove(username);
    }

    @Override
    public void remove(String id, RetrievalType type) {
        if (RetrievalType.USER_NAME == type) this.remove(id);
        RetrievalStrategy.of(type).remove(this.contacts.values().iterator(), id);
    }

    @Override
    public Member get(String group, String id, RetrievalType type) {
        return (Member) RetrievalStrategy.of(type).get(this.groups.get(group).iterator(), id);
    }

    @Override
    public void remove(String group, String id, RetrievalType type) {
        RetrievalStrategy.of(type).remove(this.groups.get(group).iterator(), id);
    }


    @Override
    public Future<Void> onDestroy() throws Exception {
        this.redis.set(List.of("contacts", JSONUtil.toJsonStr(this.contacts)))
                .onSuccess(v -> {
                    this.redis.expire(List.of("contacts", String.valueOf(TimeUnit.MINUTES.toSeconds(10))));
                    log.info("联系人数据保存成功");
                })
                .onFailure(t -> log.warn("联系人数据保存时出现异常：{}", t.getMessage()));

        this.redis.set(List.of("groups", JSONUtil.toJsonStr(this.groups)))
                .onSuccess(v -> {
                    this.redis.expire(List.of("groups", String.valueOf(TimeUnit.MINUTES.toSeconds(10))));
                    log.info("群组数据保存成功");
                })
                .onFailure(t -> log.warn("群组数据保存时出现异常：{}", t.getMessage()));
        this.redis.close();

        return ContactManager.super.onDestroy();
    }
}
