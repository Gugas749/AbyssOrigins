package com.gugas749.abyssorigins.skilltree.data;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class SkillTree {

    // ---- Connection (used by GUI to draw lines between nodes) -----------

    public static class SkillConnection {
        private final String fromSkillId;
        private final String toSkillId;

        public SkillConnection(String from, String to) {
            this.fromSkillId = from;
            this.toSkillId   = to;
        }

        public String getFromSkillId() { return fromSkillId; }
        public String getToSkillId()   { return toSkillId; }
    }

    // ---- Fields ---------------------------------------------------------

    private final ResourceLocation originId;
    private final String displayName;
    private final Map<String, SkillNode> nodes = new LinkedHashMap<>();
    // Connections are auto-derived from prerequisites — no need to define separately
    private final List<SkillConnection> connections = new ArrayList<>();

    public SkillTree(ResourceLocation originId, String displayName) {
        this.originId    = originId;
        this.displayName = displayName;
    }

    public SkillTree addNode(SkillNode node) {
        nodes.put(node.getId(), node);
        // Auto-generate connections from prerequisites
        for (String prereqId : node.getPrereqs()) {
            connections.add(new SkillConnection(prereqId, node.getId()));
        }
        return this;
    }

    // ---- Accessors ------------------------------------------------------

    public ResourceLocation      getOriginId()    { return originId; }
    public String                getDisplayName()  { return displayName; }

    public Collection<SkillNode> getNodes()        { return Collections.unmodifiableCollection(nodes.values()); }
    /** Alias used by the GUI */
    public Collection<SkillNode> getAllSkills()     { return getNodes(); }

    public Optional<SkillNode>   getNode(String id){ return Optional.ofNullable(nodes.get(id)); }
    /** Alias used by the GUI */
    public SkillNode             getSkill(String id){ return nodes.get(id); }

    public List<SkillConnection> getConnections()  { return Collections.unmodifiableList(connections); }

    public int getMaxCol() {
        return nodes.values().stream().mapToInt(SkillNode::getGridX).max().orElse(0);
    }

    public int getMaxRow() {
        return nodes.values().stream().mapToInt(SkillNode::getGridY).max().orElse(0);
    }
}
