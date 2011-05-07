import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;


public class StringList {
	
	
	IntCompare intCompare = new IntCompare();
	
	TreeMap<String,Boolean> list = new TreeMap<String,Boolean>(intCompare);
	
	
	@Override
	public String toString() {
		
		Iterator<String> itr = list.keySet().iterator();
		
		StringBuilder sb = new StringBuilder();
		
		boolean first = true;
		
		while( itr.hasNext() ) {
			
			if( !first ) {
				sb.append(",");
			}
			first = false;
			sb.append(itr.next());
		}
		
		return sb.toString();
		
		
	}
	
	public void setValues( String commaString ) {
		
		list = new TreeMap<String,Boolean>(intCompare);
		
		String[] split = (commaString.trim()).split(",");
		
		for( String line : split ) {
			if( line.length() > 0 ) {
				list.put(line, true);
			}
		}
		
	}
		
	String toggle( String listName , String string ) {
		
		if( string.indexOf(",") != -1 ) {
			return "Comma may not be used as int the string";
		}
		
		if( string.equals("clear")) {
			list = new TreeMap<String,Boolean>(intCompare);
			return listName + " cleared";
		}
		
		if( list.containsKey(string)) {
			list.remove(string);
			return string + " removed from " + listName;
		} else {
			list.put(string, true);
			return string + " added to " + listName;
		}
				
	}
	
	boolean test( String string ) {
		
		return list.containsKey(string.trim());
		
	}
	
	class IntCompare implements Comparator<String> {
        public int compare(String string1, String string2)
        {
        	
        	if( !MiscUtils.isInt((String)string1) || !MiscUtils.isInt((String)string2) ) {
        		
        		return ((String)string1).compareTo((String)string2);
        		
        	}
        	
        	return MiscUtils.getInt((String)string1)-MiscUtils.getInt((String)string2);

        }
    }
	
	TreeMap<String,Boolean> getValues() {
		
		return list;
		
	}
	
}
