package org.infinispan.ensemble.test.replicated;

import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.ensemble.test.EnsembleCacheBaseTest;
import org.infinispan.ensemble.test.WebPage;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.infinispan.scripting.ScriptingManager.SCRIPT_CACHE;


/**
 * @author Pierre Sutra
 */
@Test(groups = "functional", testName = "EnsembleReplicatedWeakCacheTest")
public class EnsembleReplicatedWeakCacheTest extends EnsembleCacheBaseTest {

    private EnsembleCache<CharSequence,WebPage> cache;

    @Override
    protected synchronized EnsembleCache<CharSequence, WebPage> cache() {
        if (cache==null)
            cache = getManager().getCache(cacheName,numberOfSites()/2, EnsembleCacheManager.Consistency.WEAK);
        return cache;
    }

    @Override
    protected int numberOfSites() {
        return 3;
    }

}
