import java.util.Map;
import java.util.Set;

/**
 * Created by fuyun on 20/02/15.
 */
public class LinkedHashMap {


    static int hashTableCapacity = (int) Math.ceil (5 / 0.75f) + 1;
    public static java.util.LinkedHashMap cache2 = new java.util.LinkedHashMap<Integer, String>(hashTableCapacity, 0.75f,true){
        // (an anonymous inner class)
        private static final long serialVersionUID = 1;

        @Override
        protected boolean removeEldestEntry (Map.Entry<Integer, String> eldest)
        {
            System.out.println("size="+size());//size before remove
            System.out.println(eldest);
            return size () > 5;
        }
    };

    public static void main(String[] args) {
        String returnValue = "";

        if(returnValue.equals("")){
            System.out.print(1);
        }

        cache2.put("1", "one");
        cache2.put("2", "two");
        cache2.put("3", "three");
        cache2.put("4", "four");
        cache2.put("5", "five");
        cache2.put("6", "six");
        cache2.put("7", "seven");

        System.out.println(cache2.get("1"));
        System.out.println(cache2.get("2"));
        System.out.println(cache2.get("3"));
        System.out.println(cache2.get("4"));
        System.out.println(cache2.get("5"));
        System.out.println(cache2.get("6"));
        System.out.println(cache2.get("7"));
        System.out.println("cachesize="+cache2.size());
        cache2.get("3");
        cache2.get("3");
        cache2.get("4");

        Set result =  cache2.entrySet();
        System.out.println(result);

    }

}
