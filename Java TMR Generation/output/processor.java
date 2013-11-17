package output;

import knowledgeBase.semanticDeriver.TMR;
import leia.parse.SentencePart;
import org.json.simple.JSONObject;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

public class Processor
{
    public String

    public void test()
    {
        // Create a hash map
        Hashtable balance = new Hashtable();
        Enumeration names;
        String str;
        double bal;

        balance.put("Zara", new Double(3434.34));
        balance.put("Mahnaz", new Double(123.22));
        balance.put("Ayan", new Double(1378.00));
        balance.put("Daisy", new Double(99.22));
        balance.put("Qadir", new Double(-19.08));

        // Show all balances in hash table.
        names = balance.keys();
        while(names.hasMoreElements()) {
            str = (String) names.nextElement();
            System.out.println(str + ": " +
                    balance.get(str));
        }
    }

    public boolean feedTMR(Hashtable<SentencePart, TMR> providedTMR)
    {
        Iterator<TMR> iterator = providedTMR.values().iterator();
        HashSet<TMR> printed = new HashSet<TMR>();

        Enumeration<SentencePart> test = providedTMR.keys();
        while(test.hasMoreElements()) {
            SentencePart str = test.nextElement();
            System.out.println(str + "ASD");
        }

        while (iterator.hasNext()) {
            TMR next = iterator.next();
            if (!printed.contains(next))
            {
                next.print();
            }
        }
        return true;
    }

    public String getResult()
    {
        JSONObject obj=new JSONObject();
        obj.put("LOOK-FOR-0","foo");
        obj.put("MEAL-0",new Integer(100));
        obj.put("balance",new Double(1000.21));
        obj.put("is_vip",new Boolean(true));
        obj.put("nickname",null);
        System.out.print(obj);
    }
}


