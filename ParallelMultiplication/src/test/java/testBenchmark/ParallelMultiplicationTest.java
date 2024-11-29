package testBenchmark;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLongArray;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)

public class ParallelMultiplicationTest {

    private static final int SIZE = 1024;
    private double[][] matrixA;
    private double[][] matrixB;
    private long sequentialTime;
    private AtomicLongArray threadTimes;

    @Setup(Level.Iteration)
    public void setup() {
        matrixA = generateMatrix(SIZE, SIZE);
        matrixB = generateMatrix(SIZE, SIZE);
        threadTimes = new AtomicLongArray(Runtime.getRuntime().availableProcessors());
    }

    @Benchmark
    public void parallelStreamMultiplication() {
        long startMemory = usedMemory();
        long startTime = System.nanoTime();

        double[][] result = multiplyParallel(matrixA, matrixB);

        long endTime = System.nanoTime();
        long endMemory = usedMemory();


        double elapsedTime = (endTime - startTime) / 1e6;
        long memoryUsed = endMemory - startMemory;


        System.out.printf(
                "Tiempo total: %.2f ms, Memoria usada: %.2f MB%n",
                elapsedTime, memoryUsed / (1024.0 * 1024.0)
        );

        System.out.println("Tiempo por thread:");
        long totalThreadTime = 0;
        for (int i = 0; i < threadTimes.length(); i++) {
            double threadTimeInMs = threadTimes.get(i) / 1e6;
            System.out.printf("Thread %d: %.2f ms%n", i, threadTimeInMs);
            totalThreadTime += threadTimes.get(i);
        }

        double averageThreadTime = totalThreadTime / (1e6 * threadTimes.length());
        System.out.printf("Tiempo promedio por thread: %.2f ms%n", averageThreadTime);
    }

    private double[][] multiplyParallel(double[][] A, double[][] B) {
        int n = A.length;
        double[][] result = new double[n][n];
        IntStream.range(0, n).parallel().forEach(i -> {
            long startThreadTime = System.nanoTime();
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    result[i][j] += A[i][k] * B[k][j];
                }
            }
            long endThreadTime = System.nanoTime();
            int threadId = (int) (Thread.currentThread().getId() % threadTimes.length());
            threadTimes.addAndGet(threadId, endThreadTime - startThreadTime);
        });
        return result;
    }

    @Setup(Level.Invocation)
    public void measureSequentialTime() {
        long start = System.nanoTime();
        multiplySequential(matrixA, matrixB);
        sequentialTime = System.nanoTime() - start;
    }

    private double[][] multiplySequential(double[][] A, double[][] B) {
        int n = A.length;
        double[][] result = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    result[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return result;
    }

    private double[][] generateMatrix(int rows, int cols) {
        Random random = new Random();
        double[][] matrix = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = random.nextDouble();
            }
        }
        return matrix;
    }

    private long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
