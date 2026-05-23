package geecon2023.example.vector;

import java.util.concurrent.TimeUnit;

import static geecon2023.example.vector.MaxOperationsDemo.max;
import static geecon2023.example.vector.MaxOperationsDemo.maxVector;
import static geecon2023.example.vector.QuadraticEquationsDemo.solveQuadraticEquations;
import static geecon2023.example.vector.QuadraticEquationsDemo.solveQuadraticEquationsVector;
import static geecon2023.example.vector.SumOperationsDemo.sum;
import static geecon2023.example.vector.SumOperationsDemo.sumVector;
import static geecon2023.example.vector.Util.runAndTime;

public class SimpleVectorOperationsDemo {


    public static void main(String[] args) throws InterruptedException {
        // Demo reduction operations
        int[] ints = Util.randomIntArray(100_000_000, 10);
        runAndTime("Sum scalar", () -> sum(ints), 100);
        runAndTime("Sum scalar vector", () -> sumVector(ints), 100);
        System.gc();
        TimeUnit.SECONDS.sleep(1);

        int[] intsForMax = Util.randomIntArray(100_000_000, 1000_000_000);
        runAndTime("Max scalar", () -> max(intsForMax), 100);
        runAndTime("Max scalar vector", () -> maxVector(intsForMax), 100);
        System.gc();
        TimeUnit.SECONDS.sleep(1);

        // Demo solving quadratic equations
        final int numberOfEquations = 30_000_000;
        double[] a_factors = Util.randomDoubleArray(numberOfEquations, 100.0);
        double[] b_factors = Util.randomDoubleArray(numberOfEquations, 100.0);
        double[] c_factors = Util.randomDoubleArray(numberOfEquations, 100.0);
        QuadraticEquationsDemo.Result[] results = runAndTime("Quadratic equations : ", () -> solveQuadraticEquations(a_factors, b_factors, c_factors), 10);
        QuadraticEquationsDemo.Result[] resultsVector = runAndTime("Quadratic equations vector : ", () -> solveQuadraticEquationsVector(a_factors, b_factors, c_factors), 10);
//        for (int i = 0; i < numberOfEquations; i++) {
//            System.out.println(a_factors[i] + " x^2 + " + b_factors[i] + " x + " + c_factors[i] + " = 0");
//            System.out.println("Result [" + i + "] " + results[i]);
//            System.out.println("Result vector [" + i + "] " + resultsVector[i]);
//        }

    }
}
