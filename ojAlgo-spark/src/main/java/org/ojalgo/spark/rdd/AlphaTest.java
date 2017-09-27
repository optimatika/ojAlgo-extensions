package org.ojalgo.spark.rdd;

// Other imports left out
import org.apache.spark.Dependency;
import org.apache.spark.Partition;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.TaskContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.rdd.RDD;

import scala.collection.AbstractIterator;
import scala.collection.Iterator;
import scala.collection.mutable.ArrayBuffer;
import scala.reflect.ClassManifestFactory$;
import scala.reflect.ClassTag;

/**
 * Example copied from StackOverflow:
 * https://stackoverflow.com/questions/30446706/implementing-custom-spark-rdd-in-java
 */
public class AlphaTest {

    /**
     * A partition representing letters of the Alphabet between a range
     */
    public static class AlphabetRangePartition implements Partition {

        private static final long serialVersionUID = 1L;
        private final int index;
        private final char from;
        private final char to;

        public AlphabetRangePartition(final int index, final char c, final char d) {
            this.index = index;
            from = c;
            to = d;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof AlphabetRangePartition)) {
                return false;
            }
            return ((AlphabetRangePartition) obj).index != index;
        }

        @Override
        public int hashCode() {
            return this.index();
        }

        @Override
        public int index() {
            return index;
        }
    }

    public static class AlphabetRDD extends RDD<String> {

        private static final long serialVersionUID = 1L;

        public AlphabetRDD(final SparkContext sc) {
            super(sc, new ArrayBuffer<Dependency<?>>(), STRING_TAG);
        }

        @Override
        public Iterator<String> compute(final Partition arg0, final TaskContext arg1) {
            final AlphabetRangePartition p = (AlphabetRangePartition) arg0;
            return new CharacterIterator(p.from, p.to);
        }

        @Override
        public Partition[] getPartitions() {
            return new Partition[] { new AlphabetRangePartition(0, 'A', 'M'), new AlphabetRangePartition(1, 'P', 'Z') };
        }

    }

    /**
     * Iterators over all characters between two characters
     */
    public static class CharacterIterator extends AbstractIterator<String> {

        private char next;
        private final char last;

        public CharacterIterator(final char from, final char to) {
            next = from;
            last = to;
        }

        @Override
        public boolean hasNext() {
            return next <= last;
        }

        @Override
        public String next() {
            // Post increments next after returning it
            return Character.toString(next++);
        }
    }

    private static final ClassTag<String> STRING_TAG = ClassManifestFactory$.MODULE$.fromClass(String.class);

    public static void main(final String[] args) {
        final SparkConf conf = new SparkConf().setMaster("local[2]").setAppName("Learn ABCs");
        try (JavaSparkContext sc = new JavaSparkContext(conf)) {
            System.out.println(new AlphabetRDD(sc.sc()).toJavaRDD().collect());
        }
    }
}
