package com.randomlychosenbytes.caloriescount.data;

import java.util.Comparator;

/**
 * Created by Willi on 08.08.2016.
 */
public class Portion
{
    public String name;
    public float weight;

    public Portion(String name, float weight)
    {
        this.name = name;
        this.weight = weight;
    }

    public static class PortionComparator implements Comparator<Portion>
    {
        @Override
        public int compare(Portion p1, Portion p2)
        {
            return p1.weight > p2.weight ? 1 : -1;
        }
    }
}
