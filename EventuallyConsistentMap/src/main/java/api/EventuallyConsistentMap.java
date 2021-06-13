package api;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public interface EventuallyConsistentMap<K, V> {


    int size();
    boolean isEmpty();
    boolean containsKey();
    V get(K key);
    void put(K key, V value);
    V remove(K key);
    V compute(K key, BiFunction<K, V, V> recomputeFunction);
    void putAll(Map<? extends K, ? extends V> map);
    Set<K> keySet();
    Collection<V> values();

}
