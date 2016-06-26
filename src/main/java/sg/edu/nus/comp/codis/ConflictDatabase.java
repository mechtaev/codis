package sg.edu.nus.comp.codis;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.edu.nus.comp.codis.ast.Node;

import java.util.*;


/**
 * A variation of Set-Trie
 */
public class ConflictDatabase {

    private Logger logger = LoggerFactory.getLogger(ConflictDatabase.class);

    private int numberOfComponents;
    private Map<Node, Integer> componentToId;
    private TrieNode root = new TrieNode();

    public ConflictDatabase(Multiset<Node> components) {
        Set<Node> nodeSet = components.elementSet();
        numberOfComponents = nodeSet.size();
        componentToId = new HashMap<>();
        int id = 0;
        for (Node component : nodeSet) {
            componentToId.put(component, id);
            id++;
        }
        root = new TrieNode();
    }

    private class TrieNode {
        List<Node> conflicts;
        TrieNode[] children;

        TrieNode() {
            this.conflicts = new ArrayList<>();
            this.children = new TrieNode[numberOfComponents];
        }
    }

    public void insert(Multiset<Node> components, Node conflict) {
        Multiset<Integer> ids = getIds(components);
        TrieNode current = root;
        for (int id = 0; id < numberOfComponents; id++) {
            while (ids.contains(id)) {
                ids.remove(id);
                if (current.children[id] == null) {
                    TrieNode next = new TrieNode();
                    current.children[id] = next;
                    current = next;
                } else {
                    current = current.children[id];
                }
            }
        }
        current.conflicts.add(conflict);
    }

    private Multiset<Integer> getIds(Multiset<Node> components) {
        Multiset<Integer> ids = HashMultiset.create();
        for (Node component : components) {
            ids.add(componentToId.get(component));
        }
        return ids;
    }

    //bfs
    public List<Node> query(Multiset<Node> components) {
        Multiset<Integer> ids = getIds(components);
        Queue<Pair<TrieNode, Multiset<Integer>>> searchQueue = new LinkedList<>();
        Queue<TrieNode> supersetQueue = new LinkedList<>();
        searchQueue.add(new ImmutablePair<>(root, ids));

        while (!searchQueue.isEmpty()) {
            Pair<TrieNode, Multiset<Integer>> current = searchQueue.remove();
            Multiset<Integer> multiset = current.getRight();
            TrieNode node = current.getLeft();
                for (int id = 0; id < numberOfComponents; id++) {
                    if (node.children[id] != null) {
                        if (multiset.contains(id)) {
                            if (multiset.size() == 1) {
                                supersetQueue.add(node);
                                break;
                            } else {
                                Multiset<Integer> next = HashMultiset.create();
                                next.addAll(multiset);
                                next.remove(id);
                                searchQueue.add(new ImmutablePair<>(node.children[id], next));
                            }
                        } else {
                            searchQueue.add(new ImmutablePair<>(node.children[id], multiset));
                        }
                    }
                }
        }
        List<Node> conflicts = new ArrayList<>();
        while (!supersetQueue.isEmpty()) {
            TrieNode current = supersetQueue.remove();
            conflicts.addAll(current.conflicts);
            for (int id = 0; id < numberOfComponents; id++) {
                if (current.children[id] != null) {
                    supersetQueue.add(current.children[id]);
                }
            }
        }
        return conflicts;
    }
}
