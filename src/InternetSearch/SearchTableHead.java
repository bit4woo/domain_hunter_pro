package InternetSearch;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SearchTableHead {
	public static final String Index="#";
	public static final String URL="URL";
	public static final String Protocol="Protocol";
	public static final String Host="Host";
	public static final String Port="Port";
	public static final String RootDomain="RootDomain";
	public static final String Title="Title";
	public static final String Server="Server";
	public static final String Source="Source";
	public static final String IP="IP";
	public static final String ASN="ASN"; //ASN编号
	public static final String ASNInfo="ASNInfo";
	public static final String Favicon="Favicon";
	public static final String IconHash="IconHash";

	public static final String CertInfo="CertInfo";
	
	public static final List<String> HeadList = getTableHeadList();

	public static List<String> getTableHeadList() {
		List<String> result = new ArrayList<>();
		Field[] fields = SearchTableHead.class.getDeclaredFields();
		for (Field field : fields) {
			//if (isPublicStaticFinalString(field)) {
			if (field.getType() == String.class) {
				try {
					result.add((String) field.get(null));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	private static boolean isPublicStaticFinalString(Field field) {
		int modifiers = field.getModifiers();
		return java.lang.reflect.Modifier.isPublic(modifiers)
				&& java.lang.reflect.Modifier.isStatic(modifiers)
				&& java.lang.reflect.Modifier.isFinal(modifiers)
				&& field.getType() == String.class;
	}
}
