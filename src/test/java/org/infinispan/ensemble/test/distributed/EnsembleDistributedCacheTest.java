package org.infinispan.ensemble.test.distributed;

import org.infinispan.ensemble.Site;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.infinispan.ensemble.cache.distributed.DistributedEnsembleCache;
import org.infinispan.ensemble.cache.distributed.partitioning.HashBasedPartitioner;
import org.infinispan.ensemble.cache.distributed.partitioning.Partitioner;
import org.infinispan.ensemble.test.EnsembleCacheBaseTest;
import org.infinispan.ensemble.test.WebPage;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.infinispan.scripting.ScriptingManager.SCRIPT_CACHE;

/**
 * @author Pierre Sutra
 */
@Test(groups = "functional", testName = "EnsembleDistributedCacheTest")
public class EnsembleDistributedCacheTest extends EnsembleCacheBaseTest {

   private DistributedEnsembleCache<CharSequence, WebPage> cache;
   private Partitioner<CharSequence, WebPage> partitioner;
   private boolean frontierMode = false;

   @Override
   protected synchronized EnsembleCache<CharSequence, WebPage> cache() {
      if (cache == null) {
         List<EnsembleCache<CharSequence, WebPage>> list = new ArrayList<>();
         for (Site s : getManager().sites())
            list.add(s.<CharSequence, WebPage>getCache(cacheName));
         partitioner = new HashBasedPartitioner<>(list);
         cache = (DistributedEnsembleCache<CharSequence, WebPage>) getManager().getCache(cacheName, list, partitioner, frontierMode);
      }
      return cache;
   }

   @Test
   @Override
   public void baseCacheOperations() {

      WebPage page1 = somePage();
      WebPage page2 = somePage();

      // get, put
      cache().put(page1.getKey(),page1);
      assert frontierMode || cache().containsKey(page1.getKey());
      assert frontierMode || cache().get(page1.getKey()).equals(page1);


      // putIfAbsent
      for(int i=0; i<1000; i++){
         page2 = somePage();
         cache().putIfAbsent(page1.getKey(), page2);
      }
      assert frontierMode || cache().get(page1.getKey()).equals(page1);

      // Frontier mode check
      WebPage page3= somePage();
      cache().put(page3.getKey(), page3);
      EnsembleCache<CharSequence, WebPage> location = partitioner.locate(page3.getKey());
      if (!frontierMode || location.equals(cache.getFrontierCache()))
         assert cache.containsKey(page3.getKey());
      else
         assert !cache.containsKey(page3.getKey());

   }

   @Test
   public void execution(){
      String script = "multiplicand  * multiplier ";
      String scriptName = "simple.js";

      Map<String,Integer> params = new HashMap<>();
      params.put("multiplicand",1);
      params.put("multiplier",5);

      EnsembleCache scriptCache = getManager().getCache(SCRIPT_CACHE);
      scriptCache.put(scriptName,script);
      List<Double> vector =  cache().execute(scriptName,params);
      assert vector.size()==sites().size();
      assert vector.get(0) == 5.0;
   }

   @Test
   public void update() {
      if (frontierMode==false)
         super.update();
   }

   }
