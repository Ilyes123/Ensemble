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

   public static final String cacheName = EnsembleCacheManager.DEFAULT_CACHE_NAME;

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
   public void piCalculation(){
      String script = "function piApprox() {\n" +
              "    var i = 0;" +
              "    var inside = 0;\n" +
              "    var all = 0;\n" +
              "    while (i<nbPoints) {\n" +
              "        var x = Math.random();\n" +
              "        var y = Math.random();\n" +
              "        var distance = Math.sqrt(x * x + y * y);\n" +
              "        all++;\n" +
              "        if (distance < 1) {\n" +
              "            inside++;\n" +
              "        }\n" +
              "        i++;\n" +
              "    }\n" +
              "    var approximation= 4 * (inside / all);\n" +
              "    return approximation;\n" +
              "}\n" +
              "piApprox()";

      String scriptName = "monteCarlo.js";

      Map<String,Integer> params = new HashMap<>();
      params.put("nbPoints",1000000);

      EnsembleCache scriptCache = getManager().getCache(SCRIPT_CACHE);
      scriptCache.put(scriptName,script);
      List<Double> vector =  cache().execute(scriptName,params);

      Double pi = new Double(0);

      for (Double d:vector){
         pi += d;

      }
      System.out.println("the value of pi is " + pi +" " + cache().getCaches().size());
      pi = pi / cache().getCaches().size();

      System.out.println("the value of pi is " + pi);
      assert(pi>3.10 && pi<3.20);
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
      System.out.println(result);
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
