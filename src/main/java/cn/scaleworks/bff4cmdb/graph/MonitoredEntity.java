package cn.scaleworks.bff4cmdb.graph;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Getter
@EqualsAndHashCode(of = "id")
@ToString
public class MonitoredEntity {
    private String id;
    private String host;
    private String type;
    private String text;
    private Set<String> dependsOn = new HashSet<>();
    private Set<String> dependencyOf = new HashSet<>();
    private Set<String> groups = new HashSet<>();

    public MonitoredEntity(String id, String host, String type, String text) {
        this.id = id;
        this.host = host;
        this.type = type;
        this.text = text;
    }

    public void markAsDependencyOf(Set<String> ids) {
        this.dependencyOf = ids;
    }

    public void assignDependencies(Set<String> ids) {
        this.dependsOn = ids;
    }

    public void assignDependency(String id) {
        this.dependsOn.add(id);
    }

    public void assignGroups(Set<String> groups) {
        this.groups = groups;
    }

    public boolean belongToGroup(String group) {
        return this.groups.contains(group);
    }
}
