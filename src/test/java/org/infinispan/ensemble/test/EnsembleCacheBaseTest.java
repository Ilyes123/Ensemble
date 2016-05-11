package org.infinispan.ensemble.test;

import org.infinispan.commons.util.concurrent.NotifyingFuture;
import org.infinispan.ensemble.EnsembleCacheManager;
import org.infinispan.ensemble.cache.EnsembleCache;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.infinispan.scripting.ScriptingManager.SCRIPT_CACHE;


/**
 * @author Pierre Sutra
 */
public abstract class EnsembleCacheBaseTest extends EnsembleAbstractTest<CharSequence, WebPage> {

   public static final String cacheName = "WebPage";

   private static Random rand = new Random(System.currentTimeMillis());
   private EnsembleCacheManager manager;

   @Override
   protected Class<WebPage> valueClass(){
      return WebPage.class;
   }

   @Override
   protected Class<CharSequence> keyClass(){
      return CharSequence.class;
   }

   @Override
   protected EnsembleCacheManager getManager() {
      if (manager==null) {
         synchronized (this) {
            if (manager==null)
               manager = new EnsembleCacheManager(sites());
         }
      }
      return  manager;
   }

   @Override
   protected int numberOfSites() {
      return 2;
   }

   @Override
   protected int numberOfNodes() {
      return 1;
   }

   @Test
   public void baseCacheOperations() {
      WebPage page1 = somePage();
      WebPage page2 = somePage();

      // get
      cache().put(page1.getKey(),page1);
      assert cache().containsKey(page1.getKey());
      assert cache().get(page1.getKey()).equals(page1);

      // putIfAbsent
      assert cache().putIfAbsent(page2.getKey(),page2)==null;
      cache().putIfAbsent(page1.getKey(),page2);
      assert cache().get(page2.getKey()).equals(page2);

   }

   @Test
   public void update(){
      int NITERATIONS = 10;
      for (int i=0; i <NITERATIONS; i++){
         WebPage page = somePage();
         cache().put(page.getKey(),page);
         WebPage page2 = cache().get(page.getKey());
         assert  page2!=null;
         assert page2.equals(page);
      }
   }

   @Test
   public void execution(){
      String script = "multiplicand  * multiplier ";
      String scriptName = "simple.js";

      Map<String,Integer> params = new HashMap<>();
      params.put("multiplicand",2);
      params.put("multiplier",5);

      EnsembleCache scriptCache = getManager().getCache(SCRIPT_CACHE);
      scriptCache.put(scriptName,script);
      Double result =  cache().execute(scriptName,params);
      assert result == 10.0;
   }


   // Helpers

   @Override
   public List<String> cacheNames(){
      List<String> cacheNames = new ArrayList<>();
      cacheNames.add(cacheName);
      return cacheNames;
   }

   public static WebPage somePage(){
      WebPage page = new WebPage();
      page.setKey("http://" + Long.toString(rand.nextLong()) + ".org/index.html");
      page.setContent(ByteBuffer.allocate(100).array());
      return page;
   }

}
