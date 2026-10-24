package com.rally.mapper.result;

import lombok.Data;

/** GroupMemberMapper.countByGroupIds 的查询结果：某个群有多少人 */
@Data
public class GroupMemberCount {

    private Long groupId;

    private Long memberCount;
}
