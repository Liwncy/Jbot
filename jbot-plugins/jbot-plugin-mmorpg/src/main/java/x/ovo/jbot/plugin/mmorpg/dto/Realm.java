package x.ovo.jbot.plugin.mmorpg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Realm {

    // ==================== 基础字段 ====================
    /**
     * 境界ID (主键)
     */
    private Integer id;

    /**
     * 境界名称 (唯一)
     */
    private String name;

    /**
     * 境界等级 (唯一)
     */
    private Integer level;

    /**
     * 境界描述
     */
    private String description;

    /**
     * 升级所需经验值
     */
    private Long requiredExp;

    /**
     * 武力值加成
     */
    private Integer attackBonus;

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
