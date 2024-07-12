package domain.target;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssetTrustLevel {
    
    public static final String Maybe = "Maybe";
    public static final String Confirm = "Confirm";
    public static final String NonTarget = "NonTarget";
    public static final String Cloud = "Cloud";
    
	private static final String[]  AssetTrustLevelArray = {Maybe,Confirm,NonTarget,Cloud};
	private static List<String> AssetTrustLevelList = new ArrayList<>(Arrays.asList(AssetTrustLevelArray));
	
	public static List<String> getLevelList(){
		return AssetTrustLevelList;
	}
	
	
	public static String getNextLevel(String level) {
	    int index = AssetTrustLevelList.indexOf(level);
	    int nextIndex = (index + 1) % AssetTrustLevelList.size();
	    String nextTrustLevel = AssetTrustLevelList.get(nextIndex);
	    return nextTrustLevel;
	}

}