package poztech2022.example.vector;

import jdk.incubator.vector.*;

public class VectorCitizens {
    public static void main(String[] args) {
        VectorSpecies<Integer> species = IntVector.SPECIES_PREFERRED;
        species.zero();
        species.broadcast(10);
        Vector<Integer> integerVector = species.fromArray(new int[]{0, 1, 2, 3, 4, 5, 6, 7}, 0);
        VectorMask<Integer> compare = integerVector.compare(VectorOperators.EQ, 10);
        long lanesToLong = integerVector.reduceLanesToLong(VectorOperators.ADD);
        integerVector.lanewise(VectorOperators.NEG);
        integerVector.lanewise(VectorOperators.MUL, integerVector);
        IntVector intVector = integerVector.reinterpretAsInts();
        int lane = intVector.lane(0);

    }
}
