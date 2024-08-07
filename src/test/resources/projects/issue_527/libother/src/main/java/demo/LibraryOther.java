/*
 * This source file was generated by the Gradle 'init' task
 */
package demo;

import java.util.Random;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.math3.random.RandomGeneratorFactory;

public class LibraryOther {

    @Nonnull
    public Double method1() {
        return RandomGeneratorFactory.createRandomGenerator(new Random()).nextDouble();
    }

    @Nullable
    public Double method2() {
        return null;
    }
}
