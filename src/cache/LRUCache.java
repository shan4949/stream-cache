package cache;

import java.util.HashMap;
import java.util.Map;

public class LRUCache<K, V> {

    private static class Node<K, V> {
        K key; V value;
        Node<K, V> prev, next;
        Node(K key, V value) { this.key = key; this.value = value; }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> map = new HashMap<>();
    private final Node<K, V> head = new Node<>(null, null); // MRU sentinel
    private final Node<K, V> tail = new Node<>(null, null); // LRU sentinel

    public LRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) return null;
        moveToFront(node);
        return node.value;
    }

    public void put(K key, V value) {
        Node<K, V> node = map.get(key);
        if (node != null) { node.value = value; moveToFront(node); return; }
        if (map.size() >= capacity) evictLRU();
        node = new Node<>(key, value);
        map.put(key, node);
        insertAtFront(node);
    }

    public int size()     { return map.size(); }
    public int capacity() { return capacity; }

    private void moveToFront(Node<K, V> n) { remove(n); insertAtFront(n); }

    private void insertAtFront(Node<K, V> n) {
        n.next = head.next; n.prev = head;
        head.next.prev = n; head.next = n;
    }

    private void remove(Node<K, V> n) {
        n.prev.next = n.next; n.next.prev = n.prev;
    }

    private void evictLRU() {
        Node<K, V> lru = tail.prev;
        remove(lru);
        map.remove(lru.key);
    }
}
