package poztech2022.example.vector;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public interface QuadraticEquationsDemo {
    sealed interface Result permits One, Both {
    }

    record One(double single) implements Result {
    }

    record Both(double left, double right) implements Result {
    }

    static Result[] solveQuadraticEquations(double[] a_factors, double[] b_factors, double[] c_factors) {
        Result[] results = new Result[a_factors.length];
        for (int i = 0; i < results.length; i++) {
            results[i] = solveQuadraticEquation(a_factors[i], b_factors[i], c_factors[i]);
        }
        return results;
    }

    static Result solveQuadraticEquation(double a_factor, double b_factor, double c_factor) {
        double delta = b_factor * b_factor - 4 * a_factor * c_factor;
        if (delta == 0.0) {
            return new One((-1 * b_factor) / (2 * a_factor));
        } else if (delta < 0.0) {
            return null;
        }
        return new Both((-1 * b_factor - Math.sqrt(delta)) / (2 * a_factor), (-1 * b_factor + Math.sqrt(delta)) / (2 * a_factor));
    }

    VectorSpecies<Double> SPECIES_USED = DoubleVector.SPECIES_PREFERRED;
    Vector<Double> ZERO = SPECIES_USED.zero();

    static Result[] solveQuadraticEquationsVector(double[] a_factors, double[] b_factors, double[] c_factors) {
        Result[] results = new Result[a_factors.length];
        for (int i = 0; i < a_factors.length; i += SPECIES_USED.length()) {
            var a_factors_vector = SPECIES_USED.fromArray(a_factors, i);
            var b_factors_vector = SPECIES_USED.fromArray(b_factors, i);
            var c_factors_vector = SPECIES_USED.fromArray(c_factors, i);
            solveQuadraticEquationVector(a_factors_vector, b_factors_vector, c_factors_vector, results, i);
        }
        return results;
    }

    static void solveQuadraticEquationVector(Vector<Double> a_factors,
                                                    Vector<Double> b_factors,
                                                    Vector<Double> c_factors,
                                                    Result[] subResult,
                                                    int startIndex) {
        Vector<Double> deltas = b_factors.mul(b_factors).sub(a_factors.mul(c_factors).lanewise(VectorOperators.MUL, 4L));
        if (deltas.lt(ZERO).allTrue()) {
            return;
        }
        Vector<Double> sqrtDeltas = deltas.lanewise(VectorOperators.SQRT);
        Vector<Double> neg_b_factors = b_factors.lanewise(VectorOperators.NEG);
        Vector<Double> twice_a_factors = a_factors.lanewise(VectorOperators.MUL, 2);
        DoubleVector deltasVec = deltas.reinterpretAsDoubles();
        DoubleVector result1 = neg_b_factors.sub(sqrtDeltas).div(twice_a_factors).reinterpretAsDoubles();
        DoubleVector result2 = neg_b_factors.add(sqrtDeltas).div(twice_a_factors).reinterpretAsDoubles();
        for (int i = 0; i < deltas.length(); i++) {
            double element = deltasVec.lane(i);
            if (element > 0.0) {
                subResult[startIndex + i] = new One(result1.lane(i));
            } else if (element == 0.0) {
                subResult[startIndex + i] = new Both(result1.lane(i), result2.lane(i));
            }
        }
    }
}
