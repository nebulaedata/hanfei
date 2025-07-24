package cn.nebulaedata.entity2;

import jdk.nashorn.internal.objects.annotations.Property;
import org.springframework.data.annotation.Id;

/**
 * @author 徐衍旭
 * @date 2022/5/20 14:43
 * @note
 */

public class UserNode {
    @Id
    private Long nodeId;
    @Property
    private String userId;
    @Property
    private String name;

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
