package ca.cutterslade.gradle.analyze.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.AbstractSetValuedMap;

public final class JavaUtil {
  private JavaUtil() {}

  public static <T, K, U, E extends Exception>
      Collector<T, ?, MultiValuedMap<K, U>> toMultiValuedMap(
          Function<? super T, ? extends K, ? extends E> keyMapper,
          Function<? super T, ? extends U, ? extends E> valueMapper) {
    return Collector.of(
        LinkedHashSetValuedLinkedHashMap::new, // Supplier
        (map, element) ->
            map.put(keyMapper.apply(element), valueMapper.apply(element)), // Accumulator
        (left, right) -> { // Combiner
          right.entries().forEach(e -> left.put(e.getKey(), e.getValue()));
          return left;
        },
        Collector.Characteristics.IDENTITY_FINISH // Finisher
        );
  }

  public static <I> Set<I> findAll(Set<I> input, Predicate<I> filter) {
    return input.stream().filter(filter).collect(Collectors.toSet());
  }

  public static <I, O, S extends Collection<O>, E extends Exception> Set<O> collectMany(
      Collection<I> input, Function<I, S, E> mapper) {
    return input.stream().map(mapper).flatMap(Collection::stream).collect(Collectors.toSet());
  }

  public static <I, O, E extends Exception> Set<O> collect(
      Collection<I> input, Function<I, O, E> mapper) {
    return input.stream().map(mapper).collect(Collectors.toSet());
  }

  @FunctionalInterface
  public interface Function<T, R, E extends Exception> extends java.util.function.Function<T, R> {
    @Override
    default R apply(T t) {
      try {
        return applyWithThrowing(t);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    R applyWithThrowing(T t) throws E;
  }

  public static class LinkedHashSetValuedLinkedHashMap<K, V> extends AbstractSetValuedMap<K, V>
      implements Serializable {

    public LinkedHashSetValuedLinkedHashMap() {
      super(new LinkedHashMap<K, LinkedHashSet<V>>());
    }

    @Override
    protected HashSet<V> createCollection() {
      return new LinkedHashSet<>();
    }

    private void writeObject(final ObjectOutputStream oos) throws IOException {
      oos.defaultWriteObject();
      doWriteObject(oos);
    }

    private void readObject(final ObjectInputStream ois)
        throws IOException, ClassNotFoundException {
      ois.defaultReadObject();
      setMap(new LinkedHashMap<K, LinkedHashSet<V>>());
      doReadObject(ois);
    }
  }
}
