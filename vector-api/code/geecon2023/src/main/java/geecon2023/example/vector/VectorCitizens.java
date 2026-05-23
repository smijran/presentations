package geecon2023.example.vector;

import jdk.incubator.vector.*;

import java.util.Arrays;
import java.util.Random;

public class VectorCitizens {
    public static void main(String[] args) {

        VectorSpecies<Integer> shortVectorSpecies = VectorSpecies.ofPreferred(int.class);
        int[] stringArray = "ASDFASDFASDFASDFAd".chars().toArray();

        int i = 0;
        for (; i < shortVectorSpecies.loopBound(stringArray.length); i+= shortVectorSpecies.length()) {
            Vector<Integer> fromArray = shortVectorSpecies.fromArray(stringArray, i);
            VectorMask<Integer> compare = fromArray.compare(VectorOperators.EQ, fromArray);
            compare.anyTrue()
            System.out.println(fromArray);
        }
        for(; i< ints.length; i++) {
            //tODO
        }




        VectorSpecies<Integer> vectorSpecies = VectorSpecies.of(int.class, VectorShape.S_128_BIT);
        Vector<Integer> zero = vectorSpecies.zero();
        System.out.println(zero);
        Vector<Integer> lanewised = zero.lanewise(VectorOperators.ADD, 3);
        System.out.println(lanewised);
        Vector<Integer> mul = zero.mul(lanewised);
        int[] ints = Util.randomIntArray(123, 10);
        System.out.println(Arrays.toString(ints));
        Vector<Integer> array = vectorSpecies.fromArray(ints, 0);


    }
}
