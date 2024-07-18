package ca.cutterslade.gradle.analyze.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.stream.Collector;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.AbstractSetValuedMap;

public final class JavaUtil {
  private JavaUtil() {}

  public static <T, K, U> Collector<T, ?, MultiValuedMap<K, U>> toMultiValuedMap(
      Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
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
