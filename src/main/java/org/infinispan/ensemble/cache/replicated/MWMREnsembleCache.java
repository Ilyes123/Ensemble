package org.infinispan.ensemble.cache.replicated;

import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.ensemble.cache.EnsembleCache;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * @author Pierre Sutra
 */
public class MWMREnsembleCache<K,V> extends ReplicatedEnsembleCache<K,V> {

    public MWMREnsembleCache(String name, List<EnsembleCache<K, V>> caches){
        super(name,caches);
    }

    @Override
    public <T> T execute(String s, Map<String, ?> map) {
        T ret = null;
        for (EnsembleCache<K,V> cache : caches){
         ret = cache.execute(s,map);
        }
        return ret;
    }

    @Override
    public V get(Object key) {
        Map<EnsembleCache<K,V>, VersionedValue<V>> previous = previousValues((K)key);
        VersionedValue<V> g = greatestValue(previous);
        if(g.getValue()==null)
            return null;
        if(!isStable(previous, g))
            writeStable((K) key, g.getValue(), g.getVersion(), previous.keySet());
        return g.getValue();
    }

    @Override
    public V put(K key, V value) {
        Map<EnsembleCache<K,V>, VersionedValue<V>> previous = previousValues(key);
        VersionedValue<V> g = greatestValue(previous);
        writeStable(key, value, g.getVersion()+1, previous.keySet());
        if(g.getValue()!=null)
            return g.getValue();
        return null;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        Map<EnsembleCache<K,V>, VersionedValue<V>> previous = previousValues(key);
        VersionedValue<V> g = greatestValue(previous);
        if (g.getVersion()==0) {
            writeStable(key, value, g.getVersion()+1, previous.keySet());
            if (g.getValue() != null)
                return g.getValue();
            return null;
        }
        return g.getValue();
    }

    //
    // HELPERS
    //

    private void writeStable(K key, V value, long version, Set<EnsembleCache<K, V>> caches) {
        List<NotifyingFuture<Boolean>> futures = new ArrayList<>();
        for(EnsembleCache<K,V> c : caches) {
            futures.add(c.replaceWithVersionAsync(key, value, version));
        }
        for(NotifyingFuture<Boolean> future : futures){
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

    }

    private Map<EnsembleCache<K,V>, VersionedValue<V>> previousValues(K k){
        Map<EnsembleCache<K,V>,VersionedValue<V>> values = new HashMap<>();
        for(EnsembleCache<K,V> cache : quorumCache()){
            try {
                values.put(
                      cache,
                      ((Callable<VersionedValue<V>>) () -> cache.getVersioned(k)).call());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Map<EnsembleCache<K,V>, VersionedValue<V>> ret = new HashMap<>();
        for(EnsembleCache<K,V> cache : values.keySet()){
            VersionedValue<V> tmp = values.get(cache);
            ret.put(cache,tmp);
        }
        return ret;
    }

    private boolean isStable(Map<EnsembleCache<K, V>, VersionedValue<V>> map, VersionedValue<V> v){
        int count = 0;
        if(v==null) return true;
        for(VersionedValue<V> w: map.values()){
            if( w!=null && w.getVersion()==v.getVersion())
                count++;
        }
        return count >= quorumSize();
    }

    private VersionedValue<V> greatestValue(Map<EnsembleCache<K,V>,VersionedValue<V>> map){
        VersionedValue<V> ret = new VersionedValue<V>() {
            @Override
            public long getVersion() {
                return 0;
            }

            @Override
            public V getValue() {
                return null;
            }
        };
        for(VersionedValue<V> v: map.values()){
            if ( v!=null && (ret.getValue()==null || v.getVersion()>ret.getVersion()))
                ret = v;
        }
        return ret;
    }

}
