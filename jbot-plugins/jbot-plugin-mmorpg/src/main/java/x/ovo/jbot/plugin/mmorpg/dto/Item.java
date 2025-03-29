package x.ovo.jbot.plugin.mmorpg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 物品实体类
 * 对应数据库表 item (物品表)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    // ==================== 基础字段 ====================
    /**
     * 物品ID (主键)
     */
    private Integer id;

    /**
     * 物品名称
     */
    private String name;

    /**
     * 物品类型 (装备/功法)
     */
    private String type;

    /**
     * 物品品质
     */
    private String quality;

    // ==================== 属性加成 ====================
    /**
     * 武力值加成百分比
     */
    private Integer attackBonus;

    /**
     * 每日修炼次数加成
     */
    private Integer dayCultivateBonus;

    /**
     * 修炼速率加成百分比
     */
    private Integer cultivateSpeed;

    // ==================== 描述信息 ====================
    /**
     * 物品描述
     */
    private String description;

    /**
     * 特殊效果
     */
    private String effect;

    // ==================== 价格信息 ====================
    /**
     * 购买价格
     */
    private Integer buyPrice;

    /**
     * 出售价格
     */
    private Integer sellPrice;

    // ==================== 系统字段 ====================
    /**
     * 记录创建时间
     */
    private Date createdAt;

    /**
     * 记录最后更新时间
     */
    private Date updatedAt;

}