/**
 * Created by fuyun on 19/02/15.
 */



    import java.util.LinkedHashMap;
    import java.util.Map;

/**
     *
     *<p>Test</p>
     *<p>Description:</P>
     *<p>Company:Cisco CAS</p>
     *<p>Department:CAS</p>
     *@Author: Tommy Zhou
     *@Since: 1.0
     *@Version:Date:2011-5-13
     *
     **/

    public class cacheLRU<K,V> extends LinkedHashMap<K, V>{
        private LinkedHashMap<K,V>  cache =null ;
        private int cacheSize = 0;

        public cacheLRU(int cacheSize){
            this.cacheSize = cacheSize;
            int hashTableCapacity = (int) Math.ceil (cacheSize / 0.75f) + 1;
            cache = new LinkedHashMap<K, V>(hashTableCapacity, 0.75f,true)
            {
                // (an anonymous inner class)
                private static final long serialVersionUID = 1;

                @Override
                protected boolean removeEldestEntry (Map.Entry<K, V> eldest)
                {
                    System.out.println("size="+size());
                    return size () > cacheLRU.this.cacheSize;
                }
            };
        }

    //V means value
        public V put(K key,V value){
            return cache.put(key, value);
        }

        public V get(Object key){
            return cache.get(key);
        }



        public static void main(String[] args) {


            cacheLRU<String, String> lruCache = new cacheLRU<String, String>(5);
            lruCache.put("1", "one");
            lruCache.put("2", "two");
            lruCache.put("3", "three");
            lruCache.put("4", "four");
            lruCache.put("5", "five");
            lruCache.put("6", "six");
            lruCache.put("7", "seven");

            System.out.println(lruCache.get("1"));
            System.out.println(lruCache.get("2"));
            System.out.println(lruCache.get("3"));
            System.out.println(lruCache.get("4"));
            System.out.println(lruCache.get("5"));
            System.out.println(lruCache.get("6"));
            System.out.println(lruCache.get("7"));
            System.out.println("cachesize="+lruCache.cacheSize);
           // lruCache.get("3");

            for (Map.Entry city : lruCache.entrySet()) {
                System.out.println("Key:- " + city.getKey() + " Value :- "
                        + city.getValue());
            }

            //System.out.println(lruCache.entrySet());
            //System.out.println(lruCache.get(lruCache.entrySet().iterator().next()));
            //lruCache.get("2");
         //   lruCache.put("6", "6");
          //  lruCache.put("5", "5");






        }

    }

