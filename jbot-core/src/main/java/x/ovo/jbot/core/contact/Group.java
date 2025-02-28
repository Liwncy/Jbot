package x.ovo.jbot.core.contact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import x.ovo.jbot.core.common.enums.ContactType;


/**
 * 群
 *
 * @author ovo created on 2025/02/17.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Group extends Contact {

    {this.type = ContactType.GROUP;}

    /** 所有者 */
    private Member owner;
}
