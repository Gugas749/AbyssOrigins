package com.gugas749.abyssorigins.skilltree.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class SkillTree {

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

    private final ResourceLocation originId;
    private final Component        displayName;
    private final Map<String, SkillNode> nodes = new LinkedHashMap<>();
    private final List<SkillConnection>  connections = new ArrayList<>();

    /**
     * @param displayNameKey  A lang key — stored as Component.translatable(displayNameKey)
     */
    public SkillTree(ResourceLocation originId, String displayNameKey) {
        this.originId    = originId;
        this.displayName = Component.translatable(displayNameKey);
    }

    public SkillTree addNode(SkillNode node) {
        nodes.put(node.getId(), node);
        for (String prereqId : node.getPrereqs())
            connections.add(new SkillConnection(prereqId, node.getId()));
        return this;
    }

    public ResourceLocation      getOriginId()    { return originId; }
    public Component             getDisplayName()  { return displayName; }

    public Collection<SkillNode> getNodes()        { return Collections.unmodifiableCollection(nodes.values()); }
    public Collection<SkillNode> getAllSkills()     { return getNodes(); }

    public Optional<SkillNode>   getNode(String id){ return Optional.ofNullable(nodes.get(id)); }
    public SkillNode             getSkill(String id){ return nodes.get(id); }

    public List<SkillConnection> getConnections()  { return Collections.unmodifiableList(connections); }

    public int getMaxCol() { return nodes.values().stream().mapToInt(SkillNode::getGridX).max().orElse(0); }
    public int getMaxRow() { return nodes.values().stream().mapToInt(SkillNode::getGridY).max().orElse(0); }
}
