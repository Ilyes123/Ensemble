# Ensemble: a client-side geo-distributed Infinispan federation

Ensemble is a key-value store that sits on top of [Infinispan](http://infinispan.org), 
an open-source data grid platform (key value store, distributed and transactional). 
Ensemble federates under the same banner multiple vanilla Infinispan grid deployments, 
each running at distinct geographically distributed locations, and orchestrates them to provide 
the illusion of a single store spanning all the deployments. 
The end-user of Ensemble decides what replication and consistency guarantees the federated storage should provide.

## Design

Ensemble exposes a Java interface that consists of two key components: an EnsembleCache and an EnsembleCacheManager. An EnsembleCache is a named and typed instance of the key-value store that spans several Infinispan deployments. An EnsembleCacheManager is a container of EnsembleCaches. Both abstractions are directly inherited from the Infinispan API (respectively a Cache and a CacheContainer).

An EnsembleCache contains multiple RemoteCaches; each RemoteCache represents an Infinispan deployment at the scale of a single micro-cloud. Calls to a RemoteCache, and thus to the backing Infinispan instances, are implemented via the HotRod protocol [Ispn-doc]. Once an EnsembleCache is created, the user can store/retrieve data using the regular get() and put() operations. These operations are executed on the appropriate Infinispan instances according to the replication degree and the consistency criteria that characterize the EnsembleCache. We briefly detail such parameters in what follows.

Ensemble supports an eventing mechanisms atop HotRod, that is a full listener API in geo-distributed client-server mode. When a client application wants to listen to cache events, it simply registers to the appropriate Ensemble cache. In the case where the cache is distributed across multiple micro-clouds, a registration per micro-cloud is done by Ensemble, and duplicates are removed automatically at the client-side library (this last filtering is transparent to the application).

## Installation

Being a client-side library, Ensemble requires no specific installation. The necesary steps to install Infinispan are described in the [user guide](http://infinispan.org/docs/8.0.x/user_guide/user_guide.html). 

## Code Snippet

To understand finely how Ensemble works, you can have a look in the [test](https://github.com/leads-project/Ensemble/tree/master/src/test/java/org/infinispan/ensemble/test) directory of this project. All the clusters are emulated on your local machine, and these tests do not require a distributed to Infinispan.

```java
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
      EnsembleCache<CharSequence, WebPage> location 
                            = partitioner.locate(page3.getKey());
      if (!frontierMode || location.equals(cache.getFrontierCache()))
         assert cache.containsKey(page3.getKey());
      else
         assert !cache.containsKey(page3.getKey());

