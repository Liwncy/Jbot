package x.ovo.jbot.plugin.mmorpg.dto;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.hutool.core.text.StrUtil;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 玩家实体类
 * 对应数据库表 players (玩家表)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    // ==================== 基础信息 ====================
    /**
     * 玩家ID (主键)
     */
    private Long id;

    /**
     * 微信/平台唯一ID
     */
    private String wxId;

    /**
     * 用户名 (登录用)
     */
    private String username;

    /**
     * 玩家昵称 (显示用)
     */
    private String nickname;

    /**
     * 性别 (男/女/无)
     */
    private String gender;

    /**
     * 年龄 (默认18)
     */
    private Integer age;

    // ==================== 游戏属性 ====================
    /**
     * 当前境界ID
     */
    private Integer realmId;

    /**
     * 当前境界经验值
     */
    private Long currentExp;

    /**
     * 当前装备ID
     */
    private Long equipmentId;

    /**
     * 当前功法ID
     */
    private Long techniqueId;

    /**
     * 武力值 (由境界和装备计算得出)
     */
    private Integer attack;

    /**
     * 幸运值
     */
    private Integer luck;

    // ==================== 货币资源 ====================
    /**
     * 灵石数量 (游戏基础货币)
     */
    private Long gold;

    /**
     * 仙玉数量 (高级货币)
     */
    private Long spiritStones;

    // ==================== 活动记录 ====================
    /**
     * 累计签到天数
     */
    private Integer checkinDays;

    /**
     * 连续签到天数
     */
    private Integer checkinStreak;

    /**
     * 最后签到日期
     */
    private LocalDateTime lastCheckinDate;

    /**
     * 当日修炼次数
     */
    private Integer dayCultivateNum;

    /**
     * 最后修炼日期
     */
    private LocalDateTime lastCultivateDate;

    /**
     * 最后占卜日期
     */
    private LocalDateTime fortuneTellingDate;

    // ==================== 系统字段 ====================
    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录最后更新时间
     */
    private LocalDateTime updatedAt;

}