package helpers;

import java.util.ArrayList;

public class Converter<K> {

	public ArrayList<K> toArrayList(K[] values) {
		ArrayList<K> ret = new ArrayList<K>(values.length);
		for (int i = 0; i < values.length; i++) {
			ret.set(i, values[i]);
		}
		return ret;
	}
}
