package x.ovo.jbot.core.contact;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import x.ovo.jbot.core.common.enums.ContactType;
import x.ovo.jbot.core.common.enums.Gender;

/**
 * 群成员
 *
 * @author ovo created on 2025/02/17.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Member extends Contact {

    {this.type = ContactType.MEMBER;}

    /** 群昵称 */
    private String displayName;
    /** 邀请人id */
    private String inviter;
    /** 性别 */
    private Gender gender;
    /** 别名 */
    private String alias;
    /** 签名 */
    private String signature;
    /** 国家 */
    private String country;
    /** 省 */
    private String province;
    /** 城市 */
    private String city;

}
