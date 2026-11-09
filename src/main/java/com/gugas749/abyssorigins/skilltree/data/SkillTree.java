package com.gugas749.abyssorigins.skilltree.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class SkillTree {

    public static class SkillConnection {
        private final String  fromSkillId;
        private final String  toSkillId;
        private final boolean optional;

        public SkillConnection(String from, String to, boolean optional) {
            this.fromSkillId = from;
            this.toSkillId   = to;
            this.optional    = optional;
        }

        public String  getFromSkillId() { return fromSkillId; }
        public String  getToSkillId()   { return toSkillId; }
        /** True for OR-prerequisite links (drawn dashed). */
        public boolean isOptional()     { return optional; }
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
            connections.add(new SkillConnection(prereqId, node.getId(), false));
        for (String prereqId : node.getAnyPrerequisites())
            connections.add(new SkillConnection(prereqId, node.getId(), true));
        return this;
    }

    public ResourceLocation      getOriginId()    { return originId; }
    public Component             getDisplayName()  { return displayName; }

    public Collection<SkillNode> getNodes()        { return Collections.unmodifiableCollection(nodes.values()); }

    public SkillNode             getSkill(String id){ return nodes.get(id); }

    public List<SkillConnection> getConnections()  { return Collections.unmodifiableList(connections); }

    public int getMinCol() { return nodes.values().stream().mapToInt(SkillNode::getGridX).min().orElse(0); }
    public int getMaxCol() { return nodes.values().stream().mapToInt(SkillNode::getGridX).max().orElse(0); }
    public int getMaxRow() { return nodes.values().stream().mapToInt(SkillNode::getGridY).max().orElse(0); }

    /** Every node excluded by the given unlocked set. */
    public Set<String> excludedBy(Collection<String> unlocked) {
        Set<String> out = new HashSet<>();
        for (String id : unlocked) {
            SkillNode n = nodes.get(id);
            if (n != null) out.addAll(n.getExclusions());
        }
        // Exclusions are symmetric in practice — a node that excludes an unlocked node is also locked out.
        for (SkillNode n : nodes.values())
            for (String ex : n.getExclusions())
                if (unlocked.contains(ex)) out.add(n.getId());
        return out;
    }
}
