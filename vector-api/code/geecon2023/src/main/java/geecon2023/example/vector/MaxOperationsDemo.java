package geecon2023.example.vector;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.util.OptionalInt;

public interface MaxOperationsDemo {

    VectorSpecies<Integer> USED_SPECIES = IntVector.SPECIES_PREFERRED;
    static OptionalInt max(int[] input) {
        OptionalInt max = OptionalInt.empty();
        for (int element : input) {
            if (max.isEmpty() || max.getAsInt() < element) {
                max = OptionalInt.of(element);
            }
        }
        return max;
    }

    static OptionalInt maxVector(int[] input) {
        if (input.length == 0) {
            return OptionalInt.empty();
        }
        Vector<Integer> max = USED_SPECIES.broadcast(Integer.MIN_VALUE);
        int i = 0;
        for (; i < max.species().loopBound(input.length); i += max.length()) {
            max = max.species()
                    .fromArray(input, i)
                    .max(max);
        }
        long reduction = max.reduceLanesToLong(VectorOperators.MAX);
        var result = OptionalInt.of((int) reduction);
        for (; i < input.length; i++) {
            result = OptionalInt.of(Math.max(result.getAsInt(), input[i]));
        }
        return result;
    }
}
