package InternetSearch;

/**
 * 构造一个类似python tuple的对象，用于返回搜索类型和搜索值
 * @author dell
 *
 * @param <A>
 * @param <B>
 */
public class InfoTuple<A, B>  {
    public final A first;
    public final B second;

    public InfoTuple(A first, B second) {
        this.first = first;
        this.second = second;
    }
}