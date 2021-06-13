package primitives;

import api.EventuallyConsistentMapListener;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class EventuallyConsistentMap<K, V> implements api.EventuallyConsistentMap<K, V> {


    private Map<K, MapValue<V>> items;
    private EventuallyConsistentMapListener<K, MapValue<V>> listeners;
    private String mapName;
    private int localNodeId;
    private final Map<Integer, EventAccumulator> senderPending;
    private final Map<Integer, Long> antiEntropyTimes = Maps.newConcurrentMap();

    public EventuallyConsistentMap(){

        items = Maps.newConcurrentMap();
    }

    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey() {
        return false;
    }

    @Override
    public V get(K key) {
        return null;
    }

    @Override
    public void put(K key, V value) {

    }

    @Override
    public V remove(K key) {
        return null;
    }

    @Override
    public V compute(K key, BiFunction<K, V, V> recomputeFunction) {
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {

    }

    @Override
    public Set<K> keySet() {
        return null;
    }

    @Override
    public Collection<V> values() {
        return null;
    }




}
