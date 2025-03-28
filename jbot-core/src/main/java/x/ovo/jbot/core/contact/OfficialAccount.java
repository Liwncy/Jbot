package x.ovo.jbot.core.contact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import x.ovo.jbot.core.common.enums.ContactType;

/**
 * 公众号
 *
 * @author ovo created on 2025/02/17.
 */
@Data
//@NoArgsConstructor
//@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OfficialAccount extends Contact {

    {this.type = ContactType.OFFICIAL_ACCOUNT;}

}
