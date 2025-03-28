package x.ovo.jbot.core.contact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import x.ovo.jbot.core.common.enums.ContactType;

/**
 * 特殊账户
 *
 * @author ovo created on 2025/02/17.
 */
@Data
//@NoArgsConstructor
//@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SpecialAccount extends Contact {

    {this.type = ContactType.SPECIAL;}

}
