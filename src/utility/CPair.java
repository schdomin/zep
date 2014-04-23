package utility;

//ds snippet from: http://stackoverflow.com/questions/156275/what-is-the-equivalent-of-the-c-pairl-r-in-java
public class CPair<FIRST, SECOND> implements Comparable<CPair<FIRST, SECOND>> {

    public final FIRST first;
    public final SECOND second;

    private CPair(FIRST first, SECOND second) {
        this.first = first;
        this.second = second;
    }

    public static <FIRST, SECOND> CPair<FIRST, SECOND> of(FIRST first,
            SECOND second) {
        return new CPair<FIRST, SECOND>(first, second);
    }

    //ds compare only the first elements
    public int compareTo(CPair<FIRST, SECOND> o) {
        return compare(o.first, first);
    }

    // todo move this to a helper class.
    private static int compare(Object o1, Object o2) {
        return o1 == null ? o2 == null ? 0 : -1 : o2 == null ? +1
                : ((Comparable<Object>) o1).compareTo(o2);
    }

    public int hashCode() {
        return 31 * hashcode(first) + hashcode(second);
    }

    // todo move this to a helper class.
    private static int hashcode(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof CPair))
            return false;
        if (this == obj)
            return true;
        return equal(first, ((CPair<?, ?>) obj).first)
                && equal(second, ((CPair<?, ?>) obj).second);
    }

    // todo move this to a helper class.
    private boolean equal(Object o1, Object o2) {
        return o1 == null ? o2 == null : (o1 == o2 || o1.equals(o2));
    }

    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}