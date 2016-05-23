package cn.scaleworks.graph.core;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Getter
@EqualsAndHashCode(of = "id")
@ToString
//TO-DO The setter and no args constructor should be private as they only serve for framework only
@NoArgsConstructor
@Setter
public class MonitoredEntity {
    private String id;
    private String host;
    private String type;
    private String text;
    private List<String> dependsOn = new ArrayList<>();
    private List<String> dependencyOf = new ArrayList<>();
    private List<String> groups = new ArrayList<>();

    public MonitoredEntity(String id, String host, String type, String text) {
        this.id = id;
        this.host = host;
        this.type = type;
        this.text = text;
    }

    public void markAsDependencyOf(Set<String> ids) {
        this.dependencyOf = ids.stream().collect(toList());
    }

    public void assignDependencies(Set<String> ids) {
        this.dependsOn = ids.stream().collect(toList());
    }

    public void assignDependency(String id) {
        this.dependsOn.add(id);
    }

    public void assignGroups(Set<String> groups) {
        this.groups = groups.stream().collect(toList());
    }

    public boolean belongToGroup(String group) {
        return this.groups.contains(group);
    }
}
