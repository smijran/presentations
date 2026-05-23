package poztech2022.example.vector;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public interface SumOperationsDemo {

    VectorSpecies<Integer> USED_SPECIES = IntVector.SPECIES_PREFERRED;

    static long sum(int[] input) {
        long sum = 0L;
        for (int element : input) {
            sum += element;
        }
        return sum;
    }

    static long sumVector(int[] input) {
        Vector<Integer> sum = USED_SPECIES.broadcast(0);
        int i = 0;
        for (; i < sum.species().loopBound(input.length); i += sum.length()) {
            Vector<Integer> vector = sum.species().fromArray(input, i);
            sum = sum.add(vector);
        }
        long result = sum.reduceLanesToLong(VectorOperators.ADD);
        for (; i < input.length; i++) {
            result += input[i];
        }
        return result;
    }
}
