import java.util.ArrayList;
import java.util.List;

public class toArrayTest {
	public static void main(String[] args) {
		String[] myArray = null;
		List<String> myList = new ArrayList<>();
		myList.add("apple");
		myList.add("banana");
		myList.add("orange");

		myArray = myList.toArray(new String[0]);
		System.out.println(myArray.toString());
	}
}
